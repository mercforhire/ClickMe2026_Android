package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Nav3 route to one chat thread. Every field except [threadId] is a display seed (no tokens, secrets or message text),
 * so the top bar renders before the first fetch (iOS `peerName`, `peerAvatarURL`).
 *
 * [peerId] comes from every entry point because all of them call initiate with it, so typing and View profile work
 * before the first peer message (RESEARCH Pitfall 6; a one-field deviation from the UI-SPEC key). [peerIsExpert] null
 * hides View profile (RESEARCH Open Question 3).
 *
 * It lives only on the Chats stack, so it cannot collide with a key on another stack.
 */
@Serializable
data class ChatThreadKey(
    val threadId: String,
    val peerId: String? = null,
    val peerName: String? = null,
    val peerAvatarUrl: String? = null,
    val peerIsExpert: Boolean? = null,
) : NavKey
