package com.uptrendinvestments.clickmeapp.screens.chat.conversations

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.managers.CurrentUserIdProvider
import com.uptrendinvestments.clickmeapp.model.responses.ChatThreadItem
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.realtime.ConnectionState
import com.uptrendinvestments.clickmeapp.realtime.RealtimeConnection
import com.uptrendinvestments.clickmeapp.realtime.RealtimeEvent
import com.uptrendinvestments.clickmeapp.screens.chat.ChatLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.chat.ChatSessionState
import com.uptrendinvestments.clickmeapp.screens.chat.ChatsRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Conversations list state machine (CHAT-01). Port of iOS `ChatConversationsViewModel`, with the Phase 8 My Bookings
 * load machine as the Android template.
 *
 * `GET /chats?page=1&limit=50`, no paging (D-17). A load is Loud (first load and Retry: shows the skeleton and a
 * failure message), Pull (pull-to-refresh: shows the indicator and flags a failure for the snackbar) or Quiet
 * (silent: keeps the rows and swallows a failure). Quiet reloads never return to the skeleton (D-17). A Realtime
 * load (reconnect, a message for an unknown thread) keeps the rows like Quiet but flags a failure, so the screen offers
 * "Retry" (CONTEXT D-05); only the tab-return reload stays silent.
 *
 * Realtime (D-17): `message.new` moves the thread to the top, `user.status` flips the presence dot, and a reconnect
 * reloads (Realtime). The row badge shows [displayUnread] of the server count and a session-local count of peer
 * messages that arrived while the thread was not open (A1; the backend does not maintain `unread_count`, Pitfall 1).
 * Blocked threads look like any other row (D-19), and a block leaves a one-time "Blocked {name}" notice (D-09).
 * Rows are keyed by thread id, so a page or an event for a listed thread updates it in place (Pitfall 10).
 */
@HiltViewModel
class ConversationsViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val repository: ChatsRepository,
        private val realtime: RealtimeConnection,
        private val chatSessionState: ChatSessionState,
        private val currentUser: CurrentUserIdProvider,
        private val launchRequest: ChatLaunchRequest,
    ) : ViewModel() {
        private enum class LoadMode {
            /** Shows the skeleton and a failure message. */
            Loud,

            /** Silent: keeps the previous rows and swallows a failure. */
            Quiet,

            /** Pull-to-refresh: shows the refresh indicator and flags a failure. */
            Pull,

            /** A reload the socket asked for (reconnect, unknown thread): keeps the rows like Quiet, flags a failure. */
            Realtime,
        }

        private val _uiState =
            MutableStateFlow(ConversationsUiState(query = savedStateHandle.get<String>(KEY_QUERY).orEmpty()))
        val uiState: StateFlow<ConversationsUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null
        private var loadIsCurrent = false

        /** Peer messages that arrived while a thread was not open, by lower-cased thread id. Survives reloads. */
        private val localUnread = mutableMapOf<String, Int>()

        init {
            // D-09: a notice posted before this ViewModel existed is applied before the first load.
            launchRequest.conversationsNotice.consume()?.let { notice -> _uiState.update { it.copy(notice = notice) } }
            startLoad(LoadMode.Loud)
            // A notice posted while this ViewModel lives (the list is still on the stack under the thread).
            viewModelScope.launch {
                launchRequest.conversationsNotice.pending.filterNotNull().collect {
                    launchRequest.conversationsNotice.consume()?.let { notice ->
                        _uiState.update { state -> state.copy(notice = notice) }
                    }
                }
            }
            // The payloads are user data: they are applied to rows and never logged (T-12-14). A burst of events for
            // unknown threads ends with one latest-wins reload, because startLoad cancels the one in flight (T-12-15).
            viewModelScope.launch {
                realtime.events.filterIsInstance<RealtimeEvent.MessageNew>().collect(::onMessageNew)
            }
            viewModelScope.launch {
                realtime.events.filterIsInstance<RealtimeEvent.UserStatus>().collect(::onUserStatus)
            }
            // The socket is down while the app is in the background, so events are missed: reload when it comes back
            // (RESEARCH Pattern 5). The first emission is the current state and only seeds `previous`.
            viewModelScope.launch {
                var previous: ConnectionState? = null
                realtime.state.collect { current ->
                    val before = previous
                    previous = current
                    if (before != null && before != ConnectionState.Connected && current == ConnectionState.Connected) {
                        startLoad(realtimeOrLoud())
                    }
                }
            }
            // Opening a thread reads it: its session-local count restarts at 0.
            viewModelScope.launch {
                chatSessionState.openThreadId.filterNotNull().collect(::clearLocalUnread)
            }
        }

        fun retry() = startLoad(LoadMode.Loud)

        fun refresh() = startLoad(LoadMode.Pull)

        /**
         * The screen, or the Chats tab, is shown again (D-17). A call while a current load runs does nothing, so first
         * composition never double-loads; a Loaded list reloads quietly.
         */
        fun onScreenShown() {
            if (loadJob?.isActive == true && loadIsCurrent) return
            startLoad(if (_uiState.value.load is ConversationsLoad.Loaded) LoadMode.Quiet else LoadMode.Loud)
        }

        /**
         * The screen was hidden or disposed (`ON_PAUSE`, disposal; Nav3 may not deliver ON_RESUME to a covered entry).
         * A load started before this may predate a change made while hidden, so the next [onScreenShown] restarts it.
         */
        fun onScreenHidden() {
            loadIsCurrent = false
        }

        /** Sets the search text (D-18). Survives rotation and process death. */
        fun onQueryChange(query: String) {
            savedStateHandle[KEY_QUERY] = query
            _uiState.update { it.copy(query = query) }
        }

        /** The user opened [row]: its session-local unread count is read now. */
        fun onOpen(row: ConversationUi) {
            clearLocalUnread(row.threadId)
        }

        fun consumeNotice() {
            _uiState.update { it.copy(notice = null) }
        }

        fun consumeRefreshFailure() {
            _uiState.update { it.copy(refreshFailed = false) }
        }

        /** Realtime when the rows are loaded, else Loud: a rows-keeping load over the skeleton would swallow its failure. */
        private fun realtimeOrLoud(): LoadMode =
            if (_uiState.value.load is ConversationsLoad.Loaded) LoadMode.Realtime else LoadMode.Loud

        /**
         * `message.new` (iOS `handleMessageNew`): the listed thread takes the new preview and time and moves to the
         * top; an unknown thread (a first message from a new peer) triggers a quiet reload (`silentReload`). The
         * session-local unread count goes up for a peer's message unless that thread is open (A1); a message from
         * the current user never counts.
         */
        private fun onMessageNew(event: RealtimeEvent.MessageNew) {
            val fromMe = currentUser.currentUserId()?.equals(event.senderId, ignoreCase = true) == true
            val isOpen = chatSessionState.openThreadId.value?.equals(event.threadId, ignoreCase = true) == true
            val countsAsUnread = !fromMe && !isOpen
            val key = event.threadId.lowercase()
            if (countsAsUnread) localUnread[key] = (localUnread[key] ?: 0) + 1
            val known = _uiState.value.conversations.any { it.threadId.equals(event.threadId, ignoreCase = true) }
            if (!known) {
                startLoad(realtimeOrLoud())
                return
            }
            _uiState.update { state ->
                val index = state.conversations.indexOfFirst { it.threadId.equals(event.threadId, ignoreCase = true) }
                if (index < 0) return@update state
                val existing = state.conversations[index]
                val updated =
                    existing.copy(
                        lastMessage = event.content ?: existing.lastMessage,
                        lastMsgAt = event.timestamp,
                        localUnread = localUnread[key] ?: 0,
                    )
                state.copy(conversations = listOf(updated) + state.conversations.filterIndexed { i, _ -> i != index })
            }
        }

        /** `user.status` (iOS `handleUserStatus`): flips the presence dot of rows whose partner matches. */
        private fun onUserStatus(event: RealtimeEvent.UserStatus) {
            _uiState.update { state ->
                state.copy(
                    conversations =
                        state.conversations.map {
                            if (it.peerId.equals(event.userId, ignoreCase = true)) {
                                it.copy(isOnline = event.isOnline)
                            } else {
                                it
                            }
                        },
                )
            }
        }

        private fun clearLocalUnread(threadId: String) {
            if (localUnread.remove(threadId.lowercase()) == null) return
            _uiState.update { state ->
                state.copy(
                    conversations =
                        state.conversations.map {
                            if (it.threadId.equals(threadId, ignoreCase = true)) it.copy(localUnread = 0) else it
                        },
                )
            }
        }

        private fun startLoad(mode: LoadMode) {
            loadJob?.cancel()
            loadIsCurrent = true
            _uiState.update {
                when (mode) {
                    LoadMode.Loud -> it.copy(load = ConversationsLoad.Loading)
                    LoadMode.Pull -> it.copy(isRefreshing = true)
                    LoadMode.Quiet, LoadMode.Realtime -> it
                }
            }
            loadJob =
                viewModelScope.launch {
                    try {
                        val rows = repository.threads().threads
                        _uiState.update {
                            it.copy(
                                conversations = buildRows(rows),
                                load = ConversationsLoad.Loaded,
                                isRefreshing = false,
                            )
                        }
                    } catch (e: ApiException) {
                        _uiState.update {
                            when {
                                mode == LoadMode.Loud || it.load is ConversationsLoad.Loading ->
                                    it.copy(load = ConversationsLoad.Failed(e.toSectionMessage()), isRefreshing = false)
                                mode == LoadMode.Pull || mode == LoadMode.Realtime ->
                                    it.copy(refreshFailed = true, isRefreshing = false)
                                else -> it.copy(isRefreshing = false)
                            }
                        }
                    }
                }
        }

        private fun buildRows(items: List<ChatThreadItem>) =
            sortConversations(
                items
                    .distinctBy { it.threadId.lowercase() }
                    .map { it.toConversationUi(localUnread[it.threadId.lowercase()] ?: 0) },
            )

        companion object {
            const val KEY_QUERY = "conversations_query"
        }
    }
