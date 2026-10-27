package com.uptrendinvestments.clickmeapp.screens.chat

import com.uptrendinvestments.clickmeapp.screens.chat.chatting.ChatThreadKey
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import javax.inject.Inject
import javax.inject.Singleton

/** The person a Message control opens a chat with; every field except [id] seeds the thread's top bar. */
data class ChatPeer(
    val id: String,
    val name: String?,
    val avatarUrl: String?,
    val isExpert: Boolean?,
)

/** The outcome of [ChatLauncher.open]. */
sealed interface ChatOpenResult {
    /** The thread exists and [key] was posted to the hand-off slot. */
    data class Opened(
        val key: ChatThreadKey,
    ) : ChatOpenResult

    /** A request for the same peer is already in flight; nothing was started. */
    data object AlreadyOpening : ChatOpenResult

    /** The request failed; nothing was posted. */
    data class Failed(
        val error: FieldError,
    ) : ChatOpenResult
}

/**
 * The single "open chat thread" path for every Message control and the in-call control (D-13, D-14): initiate the
 * thread with the peer, then post a [ChatThreadKey] to [ChatLaunchRequest.thread], which switches to the Chats tab and
 * opens the thread. It never checks block state: a blocked thread still opens (D-12).
 */
@Singleton
class ChatLauncher
    @Inject
    constructor(
        private val repository: ChatsRepository,
        private val launchRequest: ChatLaunchRequest,
    ) {
        /** Initiates the thread with [peer] and hands it to the Chats tab. */
        suspend fun open(peer: ChatPeer): ChatOpenResult {
            val data = repository.initiate(peer.id)
            val key = ChatThreadKey(data.threadId, peer.id, peer.name, peer.avatarUrl, peer.isExpert)
            launchRequest.thread.post(key)
            return ChatOpenResult.Opened(key)
        }
    }
