package com.uptrendinvestments.clickmeapp.screens.chat.reportandblock

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Nav3 route to Report & Block for one chat thread. [peerId] is the backend user id the report and block calls use;
 * [peerName] is a display seed for the confirmation copy. No tokens or message text. It lives only on the Chats
 * stack, so it cannot collide with a key on another stack.
 */
@Serializable
data class ChatReportBlockKey(
    val threadId: String,
    val peerId: String? = null,
    val peerName: String? = null,
) : NavKey
