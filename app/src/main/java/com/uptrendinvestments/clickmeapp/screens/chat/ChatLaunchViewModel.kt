package com.uptrendinvestments.clickmeapp.screens.chat

import androidx.lifecycle.ViewModel
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.ChatThreadKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Exposes the "open this thread" hand-off to the Hilt-aware home screens, which pass it into the Hilt-free shells
 * (RESEARCH Pattern 3). The shell consumes the slot once it has navigated.
 */
@HiltViewModel
class ChatLaunchViewModel
    @Inject
    constructor(
        private val launchRequest: ChatLaunchRequest,
    ) : ViewModel() {
        val pendingThread: StateFlow<ChatThreadKey?> = launchRequest.thread.pending

        /** Returns the pending thread and clears it, or null when there is none. */
        fun consumeThread(): ChatThreadKey? = launchRequest.thread.consume()
    }
