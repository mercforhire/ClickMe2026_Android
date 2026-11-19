package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.di.ApplicationScope
import com.uptrendinvestments.clickmeapp.managers.CurrentUserIdProvider
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.realtime.ConnectionState
import com.uptrendinvestments.clickmeapp.realtime.RealtimeConnection
import com.uptrendinvestments.clickmeapp.realtime.RealtimeEvent
import com.uptrendinvestments.clickmeapp.screens.chat.ChatLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.chat.ChatSessionState
import com.uptrendinvestments.clickmeapp.screens.chat.ChatsRepository
import com.uptrendinvestments.clickmeapp.screens.chat.ClientMsgIdGenerator
import com.uptrendinvestments.clickmeapp.screens.chat.ComposerMode
import com.uptrendinvestments.clickmeapp.screens.chat.ThreadNotice
import com.uptrendinvestments.clickmeapp.screens.chat.capText
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Chat thread state machine (CHAT-02, CHAT-03, CHAT-04, SAFE-03, SAFE-04). Port of iOS `ChattingViewModel`.
 *
 * Page 1 holds the newest [ChatsRepository.PAGE_SIZE] messages (ascending within the page); every page is merged
 * into the current rows by [ThreadReducer.mergePage], so reloads keep older pages and local unsent bubbles.
 * Sends are optimistic (D-01): [send] shows a Sending bubble at once; [retry] and [delete] act on a Failed one.
 */
@HiltViewModel(assistedFactory = ChatThreadViewModel.Factory::class)
class ChatThreadViewModel
    @AssistedInject
    constructor(
        @Assisted key: ChatThreadKey,
        private val savedStateHandle: SavedStateHandle,
        private val repository: ChatsRepository,
        private val realtime: RealtimeConnection,
        private val sessionState: ChatSessionState,
        private val currentUser: CurrentUserIdProvider,
        private val launchRequest: ChatLaunchRequest,
        private val clientMsgIds: ClientMsgIdGenerator,
        @ApplicationScope private val sendScope: CoroutineScope,
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

            /** A reload a booking event or a socket reconnect asked for: keeps the rows like Quiet, flags a failure. */
            Realtime,
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
                    composerText = capText(savedStateHandle.get<String>(KEY_COMPOSER).orEmpty(), COMPOSER_MAX),
                ),
            )
        val uiState: StateFlow<ChatThreadUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null
        private var earlierJob: Job? = null
        private var blockJob: Job? = null
        private var typingStopJob: Job? = null
        private var peerTypingJob: Job? = null
        private var oldestLoadedPage = FIRST_PAGE
        private var totalPages = FIRST_PAGE

        private var typingActive = false
        private var typingRecipientId: String? = null

        /** True between [onScreenShown] and [onScreenHidden]; read receipts are only sent while it is true. */
        private var screenShown = false

        /** client_msg_ids with a send request running (main thread only). */
        private val inFlight = mutableSetOf<String>()

        /** Lower-cased ids a `message.read` was actually emitted for. */
        private val emittedReadIds = mutableSetOf<String>()

        init {
            observeRealtime()
            // Block state is read in parallel with page 1 (SAFE-03); a failed scan keeps the normal composer.
            refreshBlockState()
            // Text of a send that failed after its screen closed returns to the composer (WR-03). The first emission
            // takes text kept before this ViewModel existed; later ones take text kept by an older ViewModel of this
            // thread whose send failed after this one opened.
            viewModelScope.launch {
                sessionState.unsentKeys.collect {
                    sessionState.takeUnsentText(threadId, currentUser.currentUserId())?.let(::restoreToComposer)
                }
            }
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
         * The composer text changed. The first non-blank edit after a quiet period emits `typing.start` once; every
         * edit restarts a [TYPING_IDLE_MS] timer that emits `typing.stop`; an emptied composer stops at once.
         * Nothing is emitted without a known peer id. Typing is best-effort: it rides the socket only and is dropped
         * while the socket is down. The text is capped at [COMPOSER_MAX] UTF-16 code units (the backend limit)
         * before anything else, without splitting a surrogate pair.
         */
        fun onComposerChanged(text: String) {
            val capped = capText(text, COMPOSER_MAX)
            _uiState.update { it.copy(composerText = capped) }
            savedStateHandle[KEY_COMPOSER] = capped
            if (capped.isBlank()) {
                stopTypingNow()
                return
            }
            val state = _uiState.value
            val peerId = state.peerId
            if (!typingActive && peerId != null && !state.isSelfThread) {
                realtime.emitTypingStart(threadId, peerId)
                typingActive = true
                typingRecipientId = peerId
            }
            typingStopJob?.cancel()
            if (typingActive) {
                typingStopJob =
                    viewModelScope.launch {
                        delay(TYPING_IDLE_MS)
                        stopTypingNow()
                    }
            }
        }

        /**
         * Sends the composer text (CHAT-03). The trimmed text is added at once as a Sending bubble with a fresh
         * client_msg_id, the composer is cleared, typing stops and the POST starts. Sends never wait for each other.
         *
         * Deviates from iOS, which waits for the ack before showing the message (D-01). The server dedupes by
         * (thread, sender, client_msg_id) and returns the existing row with 201, so [retry] with the same id is safe.
         * Does nothing unless [ChatThreadUiState.canSend].
         */
        fun send() {
            val state = _uiState.value
            val trimmed = state.composerText.trim()
            if (trimmed.isEmpty() || !state.canSend) return
            val id = clientMsgIds.next()
            val bubble =
                ThreadItem.Message(
                    serverId = null,
                    clientMsgId = id,
                    senderId = currentUser.currentUserId(),
                    text = trimmed,
                    createdAt = Instant.now(),
                    mine = true,
                    delivery = Delivery.Sending,
                )
            _uiState.update { it.copy(items = ThreadReducer.appendLocal(it.items, bubble), composerText = "") }
            savedStateHandle[KEY_COMPOSER] = ""
            stopTypingNow()
            deliver(id, trimmed)
        }

        /**
         * Posts one message and applies the outcome to its bubble. Several deliveries can be in flight.
         *
         * The POST runs in the application scope ([sendScope]) and is only awaited in `viewModelScope`, so leaving the
         * thread right after Send does not cancel it (WR-03). iOS does the same with an unstructured `Task`. A request
         * delivered late or twice is harmless: the server dedupes by (thread, sender, client_msg_id).
         *
         * While the screen is alive the outcome is applied to the bubble as before (D-01, D-03). If the screen goes
         * away first and the request then fails, there is no bubble left, so the text is kept in [ChatSessionState]
         * and returned to this thread's composer the next time this user opens it. That is the only state shared across
         * screens: there is no queue and no automatic retry (D-02). A request that succeeds keeps nothing, because the
         * server has the message and the next load shows it as sent.
         */
        private fun deliver(
            clientMsgId: String,
            text: String,
        ) {
            inFlight += clientMsgId
            val ownerId = currentUser.currentUserId()
            val request = sendScope.async { repository.send(threadId, text, clientMsgId) }
            viewModelScope.launch {
                try {
                    val ack = request.await()
                    _uiState.update {
                        it.copy(items = ThreadReducer.ack(it.items, clientMsgId, ack.messageId, ack.timestamp))
                    }
                } catch (e: ApiException) {
                    if (e is ApiException.Http && isRefusal(e.status)) {
                        handleSendFailure(clientMsgId, text, e)
                    } else {
                        // A network error, a 5xx, a 408/429 or an unreadable response keeps the bubble Failed (D-01).
                        _uiState.update { it.copy(items = ThreadReducer.fail(it.items, clientMsgId)) }
                    }
                } catch (e: CancellationException) {
                    // This ViewModel was cleared while the request ran; the request itself carries on.
                    if (ownerId != null) {
                        request.invokeOnCompletion { cause ->
                            if (cause != null) sessionState.keepUnsentText(threadId, ownerId, text)
                        }
                    }
                    throw e
                } finally {
                    inFlight -= clientMsgId
                }
            }
        }

        /**
         * A 4xx answer to a send (D-03): the server refused the message, so the bubble goes away and its text returns
         * to the composer instead of staying as a Failed bubble that could never succeed.
         * - 403 THREAD_BLOCKED: the thread is blocked; the scan of `GET /chats` decides who blocked it (no dialog).
         * - 403 RECIPIENT_UNAVAILABLE: the composer switches to the unavailable mode for this ViewModel only.
         * - anything else: a "Couldn't send message" dialog with the server message or the generic copy.
         */
        private fun handleSendFailure(
            clientMsgId: String,
            text: String,
            error: ApiException.Http,
        ) {
            _uiState.update { it.copy(items = ThreadReducer.remove(it.items, clientMsgId)) }
            restoreToComposer(text)
            when (error.code) {
                ErrorCode.ThreadBlocked -> {
                    _uiState.update { it.copy(block = it.block.onThreadBlocked()) }
                    refreshBlockState()
                }
                ErrorCode.RecipientUnavailable -> _uiState.update { it.copy(recipientUnavailable = true) }
                else ->
                    _uiState.update {
                        it.copy(
                            sendError =
                                error.userMessage?.let { message -> FieldError.Text(message) }
                                    ?: FieldError.Res(R.string.login_error_unknown),
                        )
                    }
            }
        }

        /**
         * Puts [text] back in the composer ahead of whatever the user has typed since (D-03), separated by a new line,
         * capped at [COMPOSER_MAX]. A refused message is never lost.
         */
        private fun restoreToComposer(text: String) {
            val draft = _uiState.value.composerText
            val restored = capText(if (draft.isBlank()) text else text + "\n" + draft, COMPOSER_MAX)
            _uiState.update { it.copy(composerText = restored) }
            savedStateHandle[KEY_COMPOSER] = restored
        }

        /** Closes the "Couldn't send message" dialog. */
        fun dismissSendError() {
            _uiState.update { it.copy(sendError = null) }
        }

        /**
         * Resends a Failed bubble with the same client_msg_id (the server dedupes, so a double delivery is harmless).
         * Ignored while that id is already in flight, when the bubble is not Failed, or when the composer is not in
         * the Normal mode.
         */
        fun retry(clientMsgId: String) {
            val bubble = ThreadReducer.findMine(_uiState.value.items, clientMsgId) ?: return
            if (bubble.delivery != Delivery.Failed || bubble.serverId != null) return
            if (inFlight.any { it.equals(clientMsgId, ignoreCase = true) }) return
            if (_uiState.value.composerMode != ComposerMode.Normal) return
            _uiState.update { it.copy(items = ThreadReducer.retry(it.items, clientMsgId)) }
            deliver(bubble.clientMsgId ?: clientMsgId, bubble.text)
        }

        /** Deletes an unsent bubble locally. Nothing is sent and there is no confirmation (UI-SPEC A10). */
        fun delete(clientMsgId: String) {
            _uiState.update { it.copy(items = ThreadReducer.remove(it.items, clientMsgId)) }
        }

        /** Ends an active typing session now (send, emptied composer, hidden screen). The send flow calls it too. */
        internal fun stopTypingNow() {
            typingStopJob?.cancel()
            typingStopJob = null
            if (!typingActive) return
            typingActive = false
            typingRecipientId?.let { realtime.emitTypingStop(threadId, it) }
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
                        emitPendingReads()
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
            screenShown = true
            sessionState.onThreadShown(threadId)
            takeNoticeIfForThisThread()
            if (loadJob?.isActive != true) {
                startLoad(if (_uiState.value.load is ThreadLoad.Loaded) LoadMode.Quiet else LoadMode.Loud)
            }
            refreshBlockState()
            emitPendingReads()
        }

        /** The thread left the screen (`ON_PAUSE`, disposal): no more read receipts, typing stops. */
        fun onScreenHidden() {
            screenShown = false
            stopTypingNow()
            sessionState.onThreadHidden(threadId)
        }

        override fun onCleared() {
            screenShown = false
            stopTypingNow()
            sessionState.onThreadHidden(threadId)
        }

        /**
         * Emits `message.read` for unread peer messages with a server id, while the screen is shown and the socket is
         * Connected. The connection drops emits in any other state, so an id is recorded in [emittedReadIds] only when
         * it was actually sent; pending ids go out after a reconnect (RESEARCH Pitfall 5). Never emits for a message
         * twice, and never from a hidden thread.
         */
        private fun emitPendingReads() {
            if (!screenShown || realtime.state.value != ConnectionState.Connected) return
            val ids = ThreadReducer.unreadPeerIds(_uiState.value.items, emittedReadIds)
            if (ids.isEmpty()) return
            realtime.emitMessageRead(threadId, ids)
            ids.mapTo(emittedReadIds) { it.lowercase() }
        }

        private fun onPeerTypingStart() {
            _uiState.update { it.copy(peerTyping = true) }
            peerTypingJob?.cancel()
            peerTypingJob =
                viewModelScope.launch {
                    delay(PEER_TYPING_TIMEOUT_MS)
                    _uiState.update { it.copy(peerTyping = false) }
                }
        }

        private fun clearPeerTyping() {
            peerTypingJob?.cancel()
            peerTypingJob = null
            _uiState.update { it.copy(peerTyping = false) }
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
            viewModelScope.launch {
                realtime.events.filterIsInstance<RealtimeEvent.TypingStart>().collect { event ->
                    if (isPeerTypingEvent(event.threadId, event.senderId)) onPeerTypingStart()
                }
            }
            viewModelScope.launch {
                realtime.events.filterIsInstance<RealtimeEvent.TypingStop>().collect { event ->
                    if (isPeerTypingEvent(event.threadId, event.senderId)) clearPeerTyping()
                }
            }
            // Catch-up after a reconnect: events missed while the socket was down are not replayed (Pitfall 5), so
            // reload page 1 and send the read receipts that were held back while the socket was down.
            viewModelScope.launch {
                var seenConnected = false
                realtime.state.collect { state ->
                    if (state == ConnectionState.Connected) {
                        if (seenConnected) reloadQuietly()
                        seenConnected = true
                        emitPendingReads()
                    }
                }
            }
        }

        private fun isPeerTypingEvent(
            eventThreadId: String,
            senderId: String,
        ): Boolean =
            eventThreadId.equals(threadId, ignoreCase = true) &&
                !senderId.equals(currentUser.currentUserId(), ignoreCase = true)

        private fun onMessageNew(event: RealtimeEvent.MessageNew) {
            val myId = currentUser.currentUserId()
            if (event.senderId.equals(myId, ignoreCase = true)) return
            val message = event.toThreadItem(myId)
            _uiState.update { it.copy(items = ThreadReducer.onIncoming(it.items, message, myId)) }
            clearPeerTyping()
            emitPendingReads()
        }

        /**
         * Loud while the first load has not finished (so a failure stays visible). Otherwise [LoadMode.Realtime]: the
         * messages and the unsent bubbles stay, and a failure raises `refreshFailed` for the snackbar with "Retry"
         * (CONTEXT D-05). The screen-shown reload stays [LoadMode.Quiet].
         */
        private fun reloadQuietly() {
            startLoad(if (_uiState.value.load is ThreadLoad.Loading) LoadMode.Loud else LoadMode.Realtime)
        }

        /**
         * Takes a one-time notice that belongs to this thread. A [ThreadNotice.Unblocked] notice also carries the
         * CHAT-UNBLOCK section 6 transition ("unblock 200 sets is_blocked = false locally", D-09), so the block state
         * becomes [BlockState.onUnblockSucceeded] at once instead of waiting for a `GET /chats` scan: an empty thread is
         * absent from that list (A4) and a scan may fail, and either case would leave the composer locked. A scan
         * already running was started before the unblock and could still answer "blocked", so it is restarted.
         */
        private fun takeNoticeIfForThisThread() {
            val pending = launchRequest.threadNotice.pending.value ?: return
            if (!pending.threadId.equals(threadId, ignoreCase = true)) return
            val notice = launchRequest.threadNotice.consume() ?: return
            if (!notice.threadId.equals(threadId, ignoreCase = true)) return
            _uiState.update {
                it.copy(
                    notice = notice,
                    block =
                        when (notice) {
                            is ThreadNotice.Unblocked -> it.block.onUnblockSucceeded()
                        },
                )
            }
            if (blockJob?.isActive == true) refreshBlockState()
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
                    // Pull to refresh lifts the "unavailable" banner, so a reinstated user can be messaged
                    // again without leaving the chat (iOS `ChattingViewModel.reload()`, phase 07 WR-02).
                    LoadMode.Pull -> it.copy(isRefreshing = true, recipientUnavailable = false)
                    LoadMode.Quiet, LoadMode.Realtime -> it
                }
            }
            loadJob =
                viewModelScope.launch {
                    try {
                        val payload = repository.messages(threadId, FIRST_PAGE)
                        val rows = payload.messages.map { it.toThreadItem(currentUser.currentUserId()) }
                        totalPages = payload.pagination?.totalPages ?: FIRST_PAGE
                        _uiState.update { it.withMergedPage(rows).copy(load = ThreadLoad.Loaded, isRefreshing = false) }
                        emitPendingReads()
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
                LoadMode.Pull, LoadMode.Realtime -> state.copy(refreshFailed = true, isRefreshing = false)
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

            /**
             * SavedStateHandle key of the unsent composer text (D-10): it survives Activity recreation and process death
             * for this thread's back stack entry, and is capped again when read. The app never writes it to disk, so it
             * does not outlive an app restart, and failed bubbles are not stored here at all.
             */
            const val KEY_COMPOSER = "chat_thread_composer"

            /** Composer limit in UTF-16 code units: the backend `content` max (D-04). */
            const val COMPOSER_MAX = CHAT_COMPOSER_MAX

            /** Quiet time after the last edit before `typing.stop` is emitted. */
            const val TYPING_IDLE_MS = 3_000L

            /** Safety net that clears the peer typing indicator when no `typing.stop` or message arrives. */
            const val PEER_TYPING_TIMEOUT_MS = 6_000L
            private const val ROLE_EXPERT = "expert"

            /** Statuses that mean "try again later", not "this message is refused" (iOS `retryableStatusCodes`). */
            private val RETRYABLE_STATUSES = setOf(408, 429)

            /**
             * Whether a failed send is a refusal: any HTTP 4xx except 408 and 429, which keep the bubble as Failed
             * because Retry can still succeed (the send is idempotent on `client_msg_id`). Mirrors iOS
             * `ChatThreadReducer.sendFailure(for:)`.
             */
            private fun isRefusal(status: Int): Boolean = status in 400..499 && status !in RETRYABLE_STATUSES
        }
    }
