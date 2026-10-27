package com.uptrendinvestments.clickmeapp.screens.chat.reportandblock

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.components.safety.REPORT_DETAILS_MAX
import com.uptrendinvestments.clickmeapp.managers.CurrentUserIdProvider
import com.uptrendinvestments.clickmeapp.model.enums.UserReportReason
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.chat.BlockState
import com.uptrendinvestments.clickmeapp.screens.chat.ChatLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.chat.ChatSessionState
import com.uptrendinvestments.clickmeapp.screens.chat.ChatsRepository
import com.uptrendinvestments.clickmeapp.screens.chat.capText
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
                    reason = UserReportReason.fromWire(savedStateHandle.get<String>(KEY_REASON)),
                    details = capText(savedStateHandle.get<String>(KEY_DETAILS).orEmpty(), REPORT_DETAILS_MAX),
                ),
            )
        val uiState: StateFlow<ReportAndBlockUiState> = _uiState.asStateFlow()

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

        private companion object {
            const val KEY_REASON = "reason"
            const val KEY_DETAILS = "details"
        }
    }
