package com.uptrendinvestments.clickmeapp.screens.chat.reportandblock

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.components.safety.REPORT_DETAILS_MAX
import com.uptrendinvestments.clickmeapp.managers.CurrentUserIdProvider
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.model.enums.UserReportReason
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.chat.BlockState
import com.uptrendinvestments.clickmeapp.screens.chat.ChatLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.chat.ChatSessionState
import com.uptrendinvestments.clickmeapp.screens.chat.ChatsRepository
import com.uptrendinvestments.clickmeapp.screens.chat.ConversationsNotice
import com.uptrendinvestments.clickmeapp.screens.chat.ScannedBlock
import com.uptrendinvestments.clickmeapp.screens.chat.ThreadNotice
import com.uptrendinvestments.clickmeapp.screens.chat.capText
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Everything the Report & Block screen shows. [reason] and [details] are the report inputs; [details] is stored
 * untrimmed (it is trimmed on send). [inFlight] is true while any of the three actions runs, which locks Back.
 */
data class ReportAndBlockUiState(
    val peerName: String? = null,
    val isSelfThread: Boolean = false,
    val block: BlockState = BlockState(),
    val reason: UserReportReason? = null,
    val details: String = "",
    val reportSubmitting: Boolean = false,
    val reportSubmitted: Boolean = false,
    val blockInFlight: Boolean = false,
    val unblockInFlight: Boolean = false,
    val showBlockConfirm: Boolean = false,
) {
    val inFlight: Boolean get() = reportSubmitting || blockInFlight || unblockInFlight
}

/** One-time navigation events of the screen. */
sealed interface ReportAndBlockEvent {
    /** The peer was blocked: leave to the Conversations root. */
    data object Blocked : ReportAndBlockEvent

    /** The peer was unblocked: return to the thread. */
    data object Unblocked : ReportAndBlockEvent
}

/**
 * Report & Block for one chat thread (SAFE-01..04). Ports iOS `ReportChatViewModel`, with CHAT-UNBLOCK.md and
 * USER-REPORTING.md overriding iOS: the report reason goes out as its enum wire value, the block state is real
 * (`is_blocked` plus `blocked_by_me`), and Unblock is only offered to the blocker.
 *
 * Reason and details are mirrored to [SavedStateHandle] so they survive rotation and process death. The details are
 * report text: they are never logged.
 */
@HiltViewModel(assistedFactory = ReportAndBlockViewModel.Factory::class)
class ReportAndBlockViewModel
    @AssistedInject
    constructor(
        @Assisted key: ChatReportBlockKey,
        private val savedStateHandle: SavedStateHandle,
        private val repository: ChatsRepository,
        private val sessionState: ChatSessionState,
        currentUser: CurrentUserIdProvider,
        private val launchRequest: ChatLaunchRequest,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(key: ChatReportBlockKey): ReportAndBlockViewModel
        }

        private val threadId = key.threadId

        private val _uiState =
            MutableStateFlow(
                ReportAndBlockUiState(
                    peerName = key.peerName,
                    isSelfThread = key.peerId != null && key.peerId.equals(currentUser.currentUserId(), true),
                    // A block this session made is known at once; the scan below refines it.
                    block = BlockState().applyScan(null, sessionState.isBlockedByMe(key.threadId)),
                    reason = UserReportReason.fromWire(savedStateHandle.get<String>(KEY_REASON)),
                    details = capText(savedStateHandle.get<String>(KEY_DETAILS).orEmpty(), REPORT_DETAILS_MAX),
                ),
            )
        val uiState: StateFlow<ReportAndBlockUiState> = _uiState.asStateFlow()

        private val _events = Channel<ReportAndBlockEvent>(Channel.BUFFERED)

        /** One-time navigation events; each is delivered once. */
        val events: Flow<ReportAndBlockEvent> = _events.receiveAsFlow()

        private val peerName = key.peerName

        init {
            scanBlockState()
        }

        /**
         * Reads `is_blocked` and `blocked_by_me` for this thread from `GET /chats`. There is no loading UI and a
         * failed scan is silent: the card keeps showing Block, the safe default.
         */
        private fun scanBlockState() {
            viewModelScope.launch {
                try {
                    val found = repository.findThread(threadId)
                    val scanned = found?.let { ScannedBlock(it.isBlocked, it.blockedByMe) }
                    _uiState.update {
                        it.copy(block = it.block.applyScan(scanned, sessionState.isBlockedByMe(threadId)))
                    }
                } catch (e: ApiException) {
                    // Silent by design (UI-SPEC surface 3).
                }
            }
        }

        fun selectReason(reason: UserReportReason) {
            if (!reportInputsOpen()) return
            savedStateHandle[KEY_REASON] = reason.wire
            _uiState.update { it.copy(reason = reason) }
        }

        fun onDetailsChange(text: String) {
            if (!reportInputsOpen()) return
            val capped = capText(text, REPORT_DETAILS_MAX)
            savedStateHandle[KEY_DETAILS] = capped
            _uiState.update { it.copy(details = capped) }
        }

        private fun reportInputsOpen(): Boolean = _uiState.value.let { !it.reportSubmitting && !it.reportSubmitted }

        /**
         * Sends the report. Ignored without a reason, while a report is in flight, after it was accepted and on a
         * self-thread. The submitted state is only set after the server answered with success; a failure keeps the
         * inputs.
         */
        fun submitReport() {
            val snapshot = _uiState.value
            val reason = snapshot.reason ?: return
            if (snapshot.reportSubmitting || snapshot.reportSubmitted || snapshot.isSelfThread) return
            _uiState.update { it.copy(reportSubmitting = true) }
            viewModelScope.launch {
                try {
                    repository.reportInChat(threadId, reason, snapshot.details)
                    _uiState.update { it.copy(reportSubmitting = false, reportSubmitted = true) }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(reportSubmitting = false) }
                }
            }
        }

        fun requestBlock() {
            _uiState.update {
                if (it.inFlight || it.isSelfThread || !it.block.showsBlock) it else it.copy(showBlockConfirm = true)
            }
        }

        fun dismissBlockConfirm() {
            _uiState.update { if (it.blockInFlight) it else it.copy(showBlockConfirm = false) }
        }

        /**
         * Blocks the peer after the confirm dialog. Single flight. On success the thread is marked blocked by me for
         * this session, the Conversations notice is posted and [ReportAndBlockEvent.Blocked] leaves the screen; the
         * server answer is authoritative, so nothing changes on a failure.
         */
        fun confirmBlock() {
            val snapshot = _uiState.value
            if (snapshot.inFlight || snapshot.isSelfThread) return
            _uiState.update { it.copy(blockInFlight = true) }
            viewModelScope.launch {
                try {
                    repository.block(threadId)
                    sessionState.markBlockedByMe(threadId)
                    launchRequest.conversationsNotice.post(ConversationsNotice.Blocked(peerName))
                    _uiState.update {
                        it.copy(block = it.block.onBlockSucceeded(), blockInFlight = false, showBlockConfirm = false)
                    }
                    _events.send(ReportAndBlockEvent.Blocked)
                } catch (e: ApiException) {
                    _uiState.update { it.copy(blockInFlight = false, showBlockConfirm = false) }
                }
            }
        }

        /**
         * Unblocks the peer (no confirmation). Offered and accepted only while the block state says the caller is the
         * blocker. A 403 NOT_BLOCKER is authoritative: the thread is blocked, not by me, and Unblock latches off.
         */
        fun unblock() {
            val snapshot = _uiState.value
            if (snapshot.inFlight || snapshot.isSelfThread || !snapshot.block.showsUnblock) return
            _uiState.update { it.copy(unblockInFlight = true) }
            viewModelScope.launch {
                try {
                    repository.unblock(threadId)
                    sessionState.clearBlockedByMe(threadId)
                    launchRequest.threadNotice.post(ThreadNotice.Unblocked(threadId, peerName))
                    _uiState.update { it.copy(block = it.block.onUnblockSucceeded(), unblockInFlight = false) }
                    _events.send(ReportAndBlockEvent.Unblocked)
                } catch (e: ApiException) {
                    val notBlocker = e is ApiException.Http && e.code == ErrorCode.NotBlocker
                    // The server says this caller is not the blocker, so a session mark claiming otherwise is stale.
                    if (notBlocker) sessionState.clearBlockedByMe(threadId)
                    _uiState.update {
                        it.copy(
                            block = if (notBlocker) it.block.onNotBlocker() else it.block,
                            unblockInFlight = false,
                        )
                    }
                }
            }
        }

        private companion object {
            const val KEY_REASON = "reason"
            const val KEY_DETAILS = "details"
        }
    }
