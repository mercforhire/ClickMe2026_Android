package com.uptrendinvestments.clickmeapp.screens.chat.conversations

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.managers.CurrentUserIdProvider
import com.uptrendinvestments.clickmeapp.model.responses.ChatThreadItem
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.realtime.RealtimeConnection
import com.uptrendinvestments.clickmeapp.screens.chat.ChatLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.chat.ChatSessionState
import com.uptrendinvestments.clickmeapp.screens.chat.ChatsRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Conversations list state machine (CHAT-01). Port of iOS `ChatConversationsViewModel`, with the Phase 8 My Bookings
 * load machine as the Android template.
 *
 * `GET /chats?page=1&limit=50`, no paging (D-17). A load is Loud (first load and Retry: shows the skeleton and a
 * failure message), Pull (pull-to-refresh: shows the indicator and flags a failure for the snackbar) or Quiet
 * (silent: keeps the rows and swallows a failure). Quiet reloads never return to the skeleton (D-17).
 */
@HiltViewModel
class ConversationsViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val repository: ChatsRepository,
        @Suppress("UnusedPrivateProperty") private val realtime: RealtimeConnection,
        @Suppress("UnusedPrivateProperty") private val chatSessionState: ChatSessionState,
        @Suppress("UnusedPrivateProperty") private val currentUser: CurrentUserIdProvider,
        @Suppress("UnusedPrivateProperty") private val launchRequest: ChatLaunchRequest,
    ) : ViewModel() {
        private enum class LoadMode {
            /** Shows the skeleton and a failure message. */
            Loud,

            /** Silent: keeps the previous rows and swallows a failure. */
            Quiet,

            /** Pull-to-refresh: shows the refresh indicator and flags a failure. */
            Pull,
        }

        private val _uiState =
            MutableStateFlow(ConversationsUiState(query = savedStateHandle.get<String>(KEY_QUERY).orEmpty()))
        val uiState: StateFlow<ConversationsUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null
        private var loadIsCurrent = false

        /** Peer messages that arrived while a thread was not open, by lower-cased thread id. Survives reloads. */
        private val localUnread = mutableMapOf<String, Int>()

        init {
            startLoad(LoadMode.Loud)
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

        fun consumeRefreshFailure() {
            _uiState.update { it.copy(refreshFailed = false) }
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
                    LoadMode.Quiet -> it
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
                                mode == LoadMode.Pull -> it.copy(refreshFailed = true, isRefreshing = false)
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
