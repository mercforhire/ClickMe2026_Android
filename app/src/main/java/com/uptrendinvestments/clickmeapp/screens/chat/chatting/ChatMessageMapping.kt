package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import com.uptrendinvestments.clickmeapp.model.responses.ChatMessageItem
import com.uptrendinvestments.clickmeapp.model.responses.ChatThreadItem
import com.uptrendinvestments.clickmeapp.model.responses.MESSAGE_TYPE_EVENT
import com.uptrendinvestments.clickmeapp.realtime.RealtimeEvent
import com.uptrendinvestments.clickmeapp.screens.chat.ScannedBlock
import java.time.Instant

/**
 * Maps a REST row to a thread row. A row is a system event only when its `type` is `event`; a text row with a null
 * sender (deleted account) is still a message, so nothing here branches on a null sender (RESEARCH Backend Contract).
 * Text is passed through verbatim: no trimming and no parsing.
 */
fun ChatMessageItem.toThreadItem(myId: String?): ThreadItem {
    if (type.equals(MESSAGE_TYPE_EVENT, ignoreCase = true)) {
        val data = eventData.orEmpty()
        return ThreadItem.SystemEvent(
            serverId = id,
            kind = SystemEventKind.fromWire(eventType),
            bookingId = data["booking_id"] as? String,
            topic = (data["topic_title"] as? String)?.takeIf { it.isNotBlank() },
            startTime = (data["start_time"] as? String)?.let { runCatching { Instant.parse(it) }.getOrNull() },
            content = content,
            createdAt = createdAt,
        )
    }
    return ThreadItem.Message(
        serverId = id,
        clientMsgId = clientMsgId,
        senderId = senderId,
        text = content.orEmpty(),
        createdAt = createdAt,
        mine = senderId != null && senderId.equals(myId, ignoreCase = true),
        delivery = Delivery.fromServerStatus(status),
    )
}

/** Maps a `message.new` socket event to a message row. The server never echoes the sender, but [myId] is honoured. */
fun RealtimeEvent.MessageNew.toThreadItem(myId: String?): ThreadItem.Message =
    ThreadItem.Message(
        serverId = messageId,
        clientMsgId = null,
        senderId = senderId,
        text = content.orEmpty(),
        createdAt = timestamp,
        mine = senderId.equals(myId, ignoreCase = true),
        delivery = Delivery.fromServerStatus(status),
    )

/** The sender id of the first message that is not mine, or null (iOS latches the peer id the same way). */
fun firstPeerSenderId(items: List<ThreadItem>): String? =
    items.firstNotNullOfOrNull { item -> (item as? ThreadItem.Message)?.takeIf { !it.mine }?.senderId }

/** The block flags of a scanned `GET /chats` row. */
fun ChatThreadItem.toScannedBlock() = ScannedBlock(isBlocked = isBlocked, blockedByMe = blockedByMe)
