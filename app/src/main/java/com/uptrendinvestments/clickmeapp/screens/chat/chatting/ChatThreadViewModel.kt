package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.managers.CurrentUserIdProvider
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.realtime.RealtimeConnection
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
        private var oldestLoadedPage = 1
        private var totalPages = 1

        init {
            startLoad(LoadMode.Loud)
        }

        /** Reloads page 1 after a failed first load. */
        fun retry() = startLoad(LoadMode.Loud)

        private fun startLoad(mode: LoadMode) {
            loadJob?.cancel()
            _uiState.update {
                when (mode) {
                    LoadMode.Loud -> it.copy(load = ThreadLoad.Loading)
                    LoadMode.Pull -> it.copy(isRefreshing = true)
                    LoadMode.Quiet -> it
                }
            }
            loadJob =
                viewModelScope.launch {
                    try {
                        val payload = repository.messages(threadId, FIRST_PAGE)
                        val page = payload.messages.map { it.toThreadItem(currentUser.currentUserId()) }
                        totalPages = payload.pagination?.totalPages ?: FIRST_PAGE
                        if (mode == LoadMode.Loud) oldestLoadedPage = FIRST_PAGE
                        _uiState.update { state ->
                            val items =
                                ThreadReducer.mergePage(
                                    if (mode ==
                                        LoadMode.Loud
                                    ) {
                                        emptyList()
                                    } else {
                                        state.items
                                    },
                                    page,
                                )
                            state.copy(
                                load = ThreadLoad.Loaded,
                                items = items,
                                hasMoreEarlier = oldestLoadedPage < totalPages,
                                isRefreshing = false,
                                peerId = state.peerId ?: firstPeerSenderId(items),
                            )
                        }
                    } catch (e: ApiException) {
                        _uiState.update {
                            when (mode) {
                                LoadMode.Loud ->
                                    it.copy(
                                        load = ThreadLoad.Failed(e.toSectionMessage()),
                                        isRefreshing = false,
                                    )
                                LoadMode.Pull -> it.copy(refreshFailed = true, isRefreshing = false)
                                LoadMode.Quiet -> it.copy(isRefreshing = false)
                            }
                        }
                    }
                }
        }

        companion object {
            const val FIRST_PAGE = 1
        }
    }
