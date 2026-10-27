package com.uptrendinvestments.clickmeapp.screens.chat

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.network.ApiException
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

private const val STATUS_NOT_FOUND = 404

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
        private val inFlight = mutableSetOf<String>()

        /**
         * Initiates the thread with [peer] and hands it to the Chats tab. At most one initiate per peer id (ignoring
         * case) runs at a time; a second call returns [ChatOpenResult.AlreadyOpening] without a request. Cancelling
         * the caller releases the peer and posts nothing.
         */
        suspend fun open(peer: ChatPeer): ChatOpenResult {
            val id = peer.id.lowercase()
            if (!synchronized(inFlight) { inFlight.add(id) }) return ChatOpenResult.AlreadyOpening
            try {
                val data = repository.initiate(peer.id)
                val key = ChatThreadKey(data.threadId, peer.id, peer.name, peer.avatarUrl, peer.isExpert)
                launchRequest.thread.post(key)
                return ChatOpenResult.Opened(key)
            } catch (e: ApiException) {
                return ChatOpenResult.Failed(e.toChatEntryError())
            } finally {
                synchronized(inFlight) { inFlight.remove(id) }
            }
        }
    }

/**
 * The message the entry error dialog shows for a failed initiate (D-15): a 404 is "This user is unavailable.", other
 * server failures show the server message (this also covers 422 CANNOT_CHAT_WITH_SELF, which the hidden self controls
 * make unreachable), and connectivity and decoding failures show the shared copy. Never exception text (T-12-12).
 */
fun ApiException.toChatEntryError(): FieldError =
    when (this) {
        is ApiException.Http ->
            when {
                status == STATUS_NOT_FOUND -> FieldError.Res(R.string.chat_entry_error_unavailable)
                else -> userMessage?.let { FieldError.Text(it) } ?: FieldError.Res(R.string.login_error_unknown)
            }
        is ApiException.Network -> FieldError.Res(R.string.login_error_network)
        is ApiException.Decoding -> FieldError.Res(R.string.login_error_unknown)
    }
