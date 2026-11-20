package com.uptrendinvestments.clickmeapp.testing

import com.uptrendinvestments.clickmeapp.model.responses.ChatMessageItem
import com.uptrendinvestments.clickmeapp.model.responses.ChatMessagesPayload
import com.uptrendinvestments.clickmeapp.model.responses.ChatPartnerDto
import com.uptrendinvestments.clickmeapp.model.responses.ChatSendAckData
import com.uptrendinvestments.clickmeapp.model.responses.ChatThreadItem
import com.uptrendinvestments.clickmeapp.model.responses.ChatsPayload
import com.uptrendinvestments.clickmeapp.model.responses.InitiateChatData
import com.uptrendinvestments.clickmeapp.model.responses.MESSAGE_TYPE_EVENT
import com.uptrendinvestments.clickmeapp.model.responses.PaginationMeta
import com.uptrendinvestments.clickmeapp.model.responses.ReportUserData
import java.time.Instant

/** Chat DTO builders shared by every Phase 12 test. */
const val ME = "me"
const val PEER = "p1"

private val FIXTURE_TIME: Instant = Instant.parse("2026-11-20T10:00:00Z")

fun chatPartner(
    id: String = PEER,
    name: String? = "Ada Lovelace",
    avatarUrl: String? = null,
    roles: List<String> = listOf("expert"),
) = ChatPartnerDto(id = id, name = name, avatarUrl = avatarUrl, roles = roles)

fun chatThreadItem(
    threadId: String = "t1",
    partner: ChatPartnerDto = chatPartner(),
    lastMessage: String? = "Hi",
    lastMsgAt: Instant? = FIXTURE_TIME,
    unreadCount: Int = 0,
    isOnline: Boolean = false,
    isBlocked: Boolean = false,
    blockedByMe: Boolean = false,
) = ChatThreadItem(
    threadId = threadId,
    partner = partner,
    lastMessage = lastMessage,
    lastMsgAt = lastMsgAt,
    unreadCount = unreadCount,
    isOnline = isOnline,
    isBlocked = isBlocked,
    blockedByMe = blockedByMe,
)

fun chatsPayload(vararg threads: ChatThreadItem) = ChatsPayload(threads = threads.toList())

fun chatMessage(
    id: String = "m1",
    threadId: String = "t1",
    senderId: String? = PEER,
    content: String? = "Hello",
    createdAt: Instant = FIXTURE_TIME,
    status: String = "sent",
    clientMsgId: String? = null,
    type: String = "text",
) = ChatMessageItem(
    id = id,
    threadId = threadId,
    senderId = senderId,
    type = type,
    content = content,
    status = status,
    clientMsgId = clientMsgId,
    createdAt = createdAt,
)

/**
 * A system `event` row with a null sender. Null [bookingId], [topic], [startTime] and the reason parameters are left
 * out of `event_data`; [rescheduleDecline] is written as the boolean the backend sends, only when non-null.
 */
fun chatEvent(
    id: String = "e1",
    threadId: String = "t1",
    eventType: String = "BOOKING_CONFIRMED",
    bookingId: String? = "b1",
    topic: String? = "Algorithms",
    startTime: String? = "2026-11-21T15:00:00Z",
    content: String? = null,
    createdAt: Instant = FIXTURE_TIME,
    reasonText: String? = null,
    reasonBy: String? = null,
    declineReasonText: String? = null,
    declineReasonCode: String? = null,
    rescheduleDecline: Boolean? = null,
): ChatMessageItem {
    val data = mutableMapOf<String, Any?>()
    bookingId?.let { data["booking_id"] = it }
    topic?.let { data["topic_title"] = it }
    startTime?.let { data["start_time"] = it }
    reasonText?.let { data["reason_text"] = it }
    reasonBy?.let { data["reason_by"] = it }
    declineReasonText?.let { data["decline_reason_text"] = it }
    declineReasonCode?.let { data["decline_reason_code"] = it }
    rescheduleDecline?.let { data["reschedule_decline"] = it }
    return ChatMessageItem(
        id = id,
        threadId = threadId,
        senderId = null,
        type = MESSAGE_TYPE_EVENT,
        content = content,
        eventType = eventType,
        eventData = data,
        createdAt = createdAt,
    )
}

fun messagesPayload(
    messages: List<ChatMessageItem>,
    currentPage: Int = 1,
    totalPages: Int = 1,
) = ChatMessagesPayload(
    messages = messages,
    pagination = PaginationMeta(currentPage = currentPage, totalPages = totalPages),
)

fun sendAck(
    messageId: String = "m-server",
    timestamp: Instant = FIXTURE_TIME,
) = ChatSendAckData(messageId = messageId, status = "sent", timestamp = timestamp)

fun initiateData(threadId: String = "t1") = InitiateChatData(threadId = threadId, createdAt = FIXTURE_TIME)

fun reportData(reportId: String = "r1") = ReportUserData(reportId = reportId, status = "open", createdAt = FIXTURE_TIME)
