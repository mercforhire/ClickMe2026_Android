package com.uptrendinvestments.clickmeapp.screens.chat

import com.uptrendinvestments.clickmeapp.screens.chat.chatting.ChatThreadKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.OneShotSlot
import javax.inject.Inject
import javax.inject.Singleton

/** A one-time message the Conversations list shows after a block. */
sealed interface ConversationsNotice {
    /** The peer was blocked; [peerName] may be unknown. */
    data class Blocked(
        val peerName: String?,
    ) : ConversationsNotice
}

/** A one-time message a thread shows when it appears. */
sealed interface ThreadNotice {
    /** The thread the notice belongs to, so another thread never consumes it. */
    val threadId: String

    /** The peer was unblocked; [peerName] may be unknown. */
    data class Unblocked(
        override val threadId: String,
        val peerName: String?,
    ) : ThreadNotice
}

/**
 * One-shot hand-offs for the chat feature: "open this thread" ([thread]), and the notices a finished Report & Block or
 * unblock leaves for the screen behind it ([conversationsNotice], [threadNotice]).
 *
 * It is a `@Singleton` and not a navigation result because the in-call Chat control lives in `CallOverlayHost`, above
 * both shells, and the target ViewModel may not exist yet when the producer finishes (precedent: `ExpertLaunchRequest`,
 * `BookingsLaunchRequest`). There is one slot per consumer so a screen never consumes another screen's hand-off.
 *
 * Memory only: it carries a thread key and display names, never tokens or message text.
 */
@Singleton
class ChatLaunchRequest
    @Inject
    constructor() {
        val thread = OneShotSlot<ChatThreadKey>()
        val conversationsNotice = OneShotSlot<ConversationsNotice>()
        val threadNotice = OneShotSlot<ThreadNotice>()
    }
