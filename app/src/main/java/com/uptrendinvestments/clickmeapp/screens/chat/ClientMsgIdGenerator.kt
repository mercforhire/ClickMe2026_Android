package com.uptrendinvestments.clickmeapp.screens.chat

/**
 * Makes the client_msg_id for one send: a UUID per message. A retry reuses the failed bubble's id (D-01), so the
 * server can match its row to the bubble. Tests pass a counter.
 */
fun interface ClientMsgIdGenerator {
    fun next(): String
}
