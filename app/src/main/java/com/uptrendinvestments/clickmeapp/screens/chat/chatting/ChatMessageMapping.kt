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
        val kind = SystemEventKind.fromWire(eventType)
        val isRescheduleDecline = kind == SystemEventKind.BookingDeclined && data["reschedule_decline"] == true
        val isRequestDecline = kind == SystemEventKind.BookingDeclined && !isRescheduleDecline
        return ThreadItem.SystemEvent(
            serverId = id,
            kind = kind,
            bookingId = data["booking_id"] as? String,
            topic = (data["topic_title"] as? String)?.takeIf { it.isNotBlank() },
            startTime = (data["start_time"] as? String)?.let { runCatching { Instant.parse(it) }.getOrNull() },
            content = content,
            createdAt = createdAt,
            reasonCaption = reasonCaption(kind, data, isRequestDecline),
            reasonCodeLabel = if (isRequestDecline) declineCodeLabel(data["decline_reason_code"] as? String) else null,
            isRescheduleDecline = isRescheduleDecline,
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

private const val ROLE_CLIENT = "client"
private const val ROLE_EXPERT = "expert"

/** The decline-code labels of iOS `DeclineNote.reasonLine(for:)`: same codes, same wording. */
private val DECLINE_CODE_LABELS =
    mapOf(
        "UNAVAILABLE" to "Not available at that time",
        "OUTSIDE_EXPERTISE" to "Outside their expertise",
        "OUT_OF_SCOPE" to "Outside their expertise",
        "SCHEDULE_CONFLICT" to "Schedule conflict",
        "CONFLICT" to "Schedule conflict",
        "PRICING" to "Pricing mismatch",
        "OTHER" to "Other reason",
    )

/** The label for a decline code, or null for a missing or unknown one. The raw code is never shown. */
private fun declineCodeLabel(code: String?): String? = code?.let { DECLINE_CODE_LABELS[it] }

private fun trimmedText(value: Any?): String? = (value as? String)?.trim()?.takeIf { it.isNotEmpty() }

/**
 * The line under a cancel, decline or reschedule-proposal row that says who gave which reason, or null when there is
 * no reason to show (iOS `ChatItemMapper.reasonCaption`). The label comes from `reason_by`, never from the text. A
 * request decline carries no `reason_by`, so it is labelled as the expert's and may fall back to the older
 * `decline_reason_text`. The text is another user's free text: it is kept out of logs and `toString`.
 */
private fun reasonCaption(
    kind: SystemEventKind,
    data: Map<String, Any?>,
    isRequestDecline: Boolean,
): String? {
    val noun =
        when (kind) {
            SystemEventKind.BookingCancelled, SystemEventKind.BookingDeclined -> "Reason"
            SystemEventKind.BookingRescheduled -> "Note"
            else -> return null
        }
    val text =
        trimmedText(data["reason_text"])
            ?: if (isRequestDecline) trimmedText(data["decline_reason_text"]) else null
    if (text == null) return null
    val role =
        when (trimmedText(data["reason_by"])?.lowercase()) {
            ROLE_CLIENT -> ROLE_CLIENT
            ROLE_EXPERT -> ROLE_EXPERT
            else -> if (isRequestDecline) ROLE_EXPERT else null
        }
    val label = if (role == null) noun else "$noun from the $role"
    return "$label: $text"
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
