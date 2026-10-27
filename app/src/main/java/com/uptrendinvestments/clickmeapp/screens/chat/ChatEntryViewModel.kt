package com.uptrendinvestments.clickmeapp.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.managers.CurrentUserIdProvider
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the Message controls on one screen show: which peer is being opened, and the last failure. */
data class ChatEntryState(
    val busyPeerId: String? = null,
    val error: FieldError? = null,
)

/**
 * Entry-scoped state shared by every Message control on a screen. It owns the in-flight request in
 * `viewModelScope`, so closing the screen cancels it and no thread is pushed.
 */
@HiltViewModel
class ChatEntryViewModel
    @Inject
    constructor(
        private val launcher: ChatLauncher,
        private val currentUser: CurrentUserIdProvider,
    ) : ViewModel() {
        private val _state = MutableStateFlow(ChatEntryState())
        val state: StateFlow<ChatEntryState> = _state.asStateFlow()

        /** True when [peerId] is the signed-in user, so the control is hidden (CANNOT_CHAT_WITH_SELF). */
        fun isSelf(peerId: String?): Boolean {
            val me = currentUser.currentUserId()
            return peerId != null && me != null && peerId.equals(me, ignoreCase = true)
        }

        /** Opens the chat with [peer]; [onOpened] runs once the thread was handed to the Chats tab. */
        fun open(
            peer: ChatPeer,
            onOpened: () -> Unit = {},
        ) {
            viewModelScope.launch {
                when (val result = launcher.open(peer)) {
                    is ChatOpenResult.Opened -> onOpened()
                    else -> Unit
                }
            }
        }

        fun dismissError() {
            _state.update { it.copy(error = null) }
        }
    }
