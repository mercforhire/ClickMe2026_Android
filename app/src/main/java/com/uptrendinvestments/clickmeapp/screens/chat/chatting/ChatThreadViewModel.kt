package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.managers.CurrentUserIdProvider
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.realtime.ConnectionState
import com.uptrendinvestments.clickmeapp.realtime.RealtimeConnection
import com.uptrendinvestments.clickmeapp.realtime.RealtimeEvent
import com.uptrendinvestments.clickmeapp.screens.chat.ChatLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.chat.ChatSessionState
import com.uptrendinvestments.clickmeapp.screens.chat.ChatsRepository
import com.uptrendinvestments.clickmeapp.screens.chat.ClientMsgIdGenerator
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Chat thread state machine, read side (CHAT-02, CHAT-04, SAFE-03). Port of iOS `ChattingViewModel`.
 *
 * Page 1 holds the newest [ChatsRepository.PAGE_SIZE] messages (ascending within the page); every page is merged
 * into the current rows by [ThreadReducer.mergePage], so reloads keep older pages and local unsent bubbles.
 * The constructor already takes everything the send flow (12-08) needs so that plan adds no parameter.
 */
@HiltViewModel(assistedFactory = ChatThreadViewModel.Factory::class)
class ChatThreadViewModel
    @AssistedInject
    constructor(
        @Assisted private val key: ChatThreadKey,
        private val savedStateHandle: SavedStateHandle,
        private val repository: ChatsRepository,
        private val realtime: RealtimeConnection,
        private val sessionState: ChatSessionState,
        private val currentUser: CurrentUserIdProvider,
        private val launchRequest: ChatLaunchRequest,
        private val clientMsgIds: ClientMsgIdGenerator,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(key: ChatThreadKey): ChatThreadViewModel
        }

        private enum class LoadMode {
            /** Shows the spinner and a failure message. */
            Loud,

            /** Silent: keeps the rows and swallows a failure. */
            Quiet,

            /** Pull-to-refresh: shows the refresh indicator and flags a failure. */
            Pull,
        }

        private val threadId: String = key.threadId

        private val _uiState =
            MutableStateFlow(
                ChatThreadUiState(
                    threadId = key.threadId,
                    peerId = key.peerId,
                    peerName = key.peerName,
                    peerAvatarUrl = key.peerAvatarUrl,
                    peerIsExpert = key.peerIsExpert,
                    isSelfThread = key.peerId != null && key.peerId.equals(currentUser.currentUserId(), true),
                ),
            )
        val uiState: StateFlow<ChatThreadUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null
        private var earlierJob: Job? = null
        private var blockJob: Job? = null
        private var oldestLoadedPage = FIRST_PAGE
        private var totalPages = FIRST_PAGE

        init {
            observeRealtime()
            // Block state is read in parallel with page 1 (SAFE-03); a failed scan keeps the normal composer.
            refreshBlockState()
            // A notice posted before this ViewModel existed, or while it lives, is taken when it is for this thread.
            viewModelScope.launch {
                launchRequest.threadNotice.pending
                    .filterNotNull()
                    .collect { takeNoticeIfForThisThread() }
            }
            startLoad(LoadMode.Loud)
        }

        /** Reloads page 1 after a failed first load. */
        fun retry() = startLoad(LoadMode.Loud)

        /** Pull-to-refresh: reloads page 1 with an indicator; a failure sets `refreshFailed`. */
        fun refresh() = startLoad(LoadMode.Pull)

        fun consumeRefreshFailure() {
            _uiState.update { it.copy(refreshFailed = false) }
        }

        fun consumeNotice() {
            _uiState.update { it.copy(notice = null) }
        }

        /**
         * Requests the next older page and merges it by server id. A failure is silent and leaves the button
         * retryable (iOS parity); the end of the history is `oldestLoadedPage >= totalPages`.
         */
        fun loadEarlier() {
            val state = _uiState.value
            if (!state.hasMoreEarlier || state.isLoadingEarlier) return
            val page = oldestLoadedPage + 1
            _uiState.update { it.copy(isLoadingEarlier = true) }
            earlierJob =
                viewModelScope.launch {
                    try {
                        val payload = repository.messages(threadId, page)
                        val rows = payload.messages.map { it.toThreadItem(currentUser.currentUserId()) }
                        oldestLoadedPage = page
                        totalPages = if (rows.isEmpty()) page else payload.pagination?.totalPages ?: totalPages
                        _uiState.update { it.withMergedPage(rows).copy(isLoadingEarlier = false) }
                    } catch (e: ApiException) {
                        _uiState.update { it.copy(isLoadingEarlier = false) }
                    }
                }
        }

        /**
         * The thread is on screen (first composition, `ON_RESUME`): marks it open, takes a notice for this thread,
         * reloads quietly (a call while a load runs does nothing, so first composition never double-loads) and reads
         * the block state again, since it may have changed while the screen was away.
         */
        fun onScreenShown() {
            sessionState.onThreadShown(threadId)
            takeNoticeIfForThisThread()
            if (loadJob?.isActive != true) {
                startLoad(if (_uiState.value.load is ThreadLoad.Loaded) LoadMode.Quiet else LoadMode.Loud)
            }
            refreshBlockState()
        }

        /** The thread left the screen (`ON_PAUSE`, disposal). */
        fun onScreenHidden() {
            sessionState.onThreadHidden(threadId)
        }

        override fun onCleared() {
            sessionState.onThreadHidden(threadId)
        }

        private fun observeRealtime() {
            viewModelScope.launch {
                realtime.events.filterIsInstance<RealtimeEvent.MessageNew>().collect { event ->
                    if (event.threadId.equals(threadId, ignoreCase = true)) onMessageNew(event)
                }
            }
            viewModelScope.launch {
                realtime.events.filterIsInstance<RealtimeEvent.MessageRead>().collect { event ->
                    if (event.threadId.equals(threadId, ignoreCase = true)) {
                        _uiState.update { it.copy(items = ThreadReducer.markRead(it.items, event.messageIds)) }
                    }
                }
            }
            // A6: booking system rows reach a thread only through a reload; the payload is never read.
            viewModelScope.launch {
                realtime.events.filterIsInstance<RealtimeEvent.BookingUpdate>().collect { reloadQuietly() }
            }
            // Catch-up after a reconnect: events missed while the socket was down are not replayed (Pitfall 5).
            viewModelScope.launch {
                var seenConnected = false
                realtime.state.collect { state ->
                    if (state == ConnectionState.Connected) {
                        if (seenConnected) reloadQuietly()
                        seenConnected = true
                    }
                }
            }
        }

        private fun onMessageNew(event: RealtimeEvent.MessageNew) {
            val myId = currentUser.currentUserId()
            if (event.senderId.equals(myId, ignoreCase = true)) return
            val message = event.toThreadItem(myId)
            _uiState.update {
                it.copy(items = ThreadReducer.onIncoming(it.items, message, myId), peerTyping = false)
            }
        }

        /** Loud while the first load has not finished (so a failure stays visible), quiet otherwise. */
        private fun reloadQuietly() {
            startLoad(if (_uiState.value.load is ThreadLoad.Loading) LoadMode.Loud else LoadMode.Quiet)
        }

        private fun takeNoticeIfForThisThread() {
            val pending = launchRequest.threadNotice.pending.value ?: return
            if (!pending.threadId.equals(threadId, ignoreCase = true)) return
            launchRequest.threadNotice.consume()?.let { notice ->
                if (notice.threadId.equals(threadId, ignoreCase = true)) _uiState.update { it.copy(notice = notice) }
            }
        }

        private fun refreshBlockState() {
            blockJob?.cancel()
            blockJob =
                viewModelScope.launch {
                    try {
                        val found = repository.findThread(threadId)
                        val scanned = found?.toScannedBlock()
                        val sessionBlocked = sessionState.isBlockedByMe(threadId)
                        _uiState.update { state ->
                            val partner = found?.partner
                            state.copy(
                                block = state.block.applyScan(scanned, sessionBlocked),
                                peerId = state.peerId ?: partner?.id,
                                peerName = state.peerName ?: partner?.name,
                                peerAvatarUrl = state.peerAvatarUrl ?: partner?.avatarUrl,
                                peerIsExpert =
                                    state.peerIsExpert
                                        ?: partner?.roles?.any { it.equals(ROLE_EXPERT, ignoreCase = true) },
                            )
                        }
                    } catch (e: ApiException) {
                        // Keep the current state: a failed scan must not lock or unlock the composer.
                    }
                }
        }

        private fun startLoad(mode: LoadMode) {
            loadJob?.cancel()
            if (mode == LoadMode.Loud) {
                earlierJob?.cancel()
                oldestLoadedPage = FIRST_PAGE
            }
            _uiState.update {
                when (mode) {
                    LoadMode.Loud -> it.copy(load = ThreadLoad.Loading, isLoadingEarlier = false)
                    LoadMode.Pull -> it.copy(isRefreshing = true)
                    LoadMode.Quiet -> it
                }
            }
            loadJob =
                viewModelScope.launch {
                    try {
                        val payload = repository.messages(threadId, FIRST_PAGE)
                        val rows = payload.messages.map { it.toThreadItem(currentUser.currentUserId()) }
                        totalPages = payload.pagination?.totalPages ?: FIRST_PAGE
                        _uiState.update { it.withMergedPage(rows).copy(load = ThreadLoad.Loaded, isRefreshing = false) }
                    } catch (e: ApiException) {
                        _uiState.update { loadFailed(it, mode, e) }
                    }
                }
        }

        private fun loadFailed(
            state: ChatThreadUiState,
            mode: LoadMode,
            error: ApiException,
        ): ChatThreadUiState =
            when (mode) {
                LoadMode.Loud -> state.copy(load = ThreadLoad.Failed(error.toSectionMessage()), isRefreshing = false)
                LoadMode.Pull -> state.copy(refreshFailed = true, isRefreshing = false)
                LoadMode.Quiet -> state.copy(isRefreshing = false)
            }

        /** Merges [rows] into the current items and refreshes the paging flag and the peer id fallback. */
        private fun ChatThreadUiState.withMergedPage(rows: List<ThreadItem>): ChatThreadUiState {
            val merged = ThreadReducer.mergePage(items, rows)
            return copy(
                items = merged,
                hasMoreEarlier = oldestLoadedPage < totalPages,
                peerId = peerId ?: firstPeerSenderId(merged),
            )
        }

        companion object {
            const val FIRST_PAGE = 1
            private const val ROLE_EXPERT = "expert"
        }
    }
