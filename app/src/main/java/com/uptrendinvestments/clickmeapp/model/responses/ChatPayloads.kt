package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

/** `chat_messages.type` of a system row (booking events). Callers branch on `type`, never on a null sender. */
const val MESSAGE_TYPE_EVENT = "event"

/** `chat_messages.status` of a message the recipient has read. */
const val MESSAGE_STATUS_READ = "read"

/** The other participant of a thread, as `GET /chats` returns it. */
@JsonClass(generateAdapter = true)
data class ChatPartnerDto(
    val id: String,
    val name: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    val roles: List<String> = emptyList(),
)

/**
 * One row of `GET /chats` (`chats.controller.js`). [blockedByMe] is absent on older responses and decodes as
 * false (CHAT-UNBLOCK section 9). [lastMessage] is user text, so [toString] prints ids and flags only.
 */
@JsonClass(generateAdapter = true)
data class ChatThreadItem(
    @Json(name = "thread_id") val threadId: String,
    val partner: ChatPartnerDto,
    @Json(name = "last_message") val lastMessage: String? = null,
    @Json(name = "last_msg_at") val lastMsgAt: Instant? = null,
    @Json(name = "unread_count") val unreadCount: Int = 0,
    @Json(name = "is_online") val isOnline: Boolean = false,
    @Json(name = "is_blocked") val isBlocked: Boolean = false,
    @Json(name = "blocked_by_me") val blockedByMe: Boolean = false,
) {
    override fun toString(): String =
        "ChatThreadItem(threadId=$threadId, partnerId=${partner.id}, unreadCount=$unreadCount, " +
            "isBlocked=$isBlocked, blockedByMe=$blockedByMe)"
}

/** `data` of `GET /chats`. */
@JsonClass(generateAdapter = true)
data class ChatsPayload(
    val threads: List<ChatThreadItem> = emptyList(),
    val pagination: PaginationMeta? = null,
)

/** The `sender` object embedded in a message row. */
@JsonClass(generateAdapter = true)
data class ChatMessageSenderDto(
    val id: String,
    val name: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
)

/**
 * One row of `GET /chats/{id}/messages` (`chats.controller.js`). `sender_id` is `ON DELETE SET NULL`, so
 * [senderId] and [sender] can be null on any row; callers branch on [type] (see [MESSAGE_TYPE_EVENT]).
 * [type], [status] and [eventType] stay strings so a new server value never fails decoding. [eventData] is
 * free-form (numbers decode as Double). [content] is user text, so [toString] prints ids, type and status only.
 */
@JsonClass(generateAdapter = true)
data class ChatMessageItem(
    val id: String,
    @Json(name = "thread_id") val threadId: String,
    @Json(name = "sender_id") val senderId: String? = null,
    val type: String,
    val content: String? = null,
    val status: String = "sent",
    @Json(name = "client_msg_id") val clientMsgId: String? = null,
    @Json(name = "event_type") val eventType: String? = null,
    @Json(name = "event_data") val eventData: Map<String, Any?>? = null,
    @Json(name = "created_at") val createdAt: Instant,
    val sender: ChatMessageSenderDto? = null,
) {
    override fun toString(): String = "ChatMessageItem(id=$id, type=$type, status=$status, eventType=$eventType)"
}

/** `data` of `GET /chats/{id}/messages`. */
@JsonClass(generateAdapter = true)
data class ChatMessagesPayload(
    val messages: List<ChatMessageItem> = emptyList(),
    val pagination: PaginationMeta? = null,
)

/** `data` of `POST /chats/{id}/send`. */
@JsonClass(generateAdapter = true)
data class ChatSendAckData(
    @Json(name = "message_id") val messageId: String,
    val status: String,
    val timestamp: Instant,
)

/** `data` of `POST /chats/initiate`. */
@JsonClass(generateAdapter = true)
data class InitiateChatData(
    @Json(name = "thread_id") val threadId: String,
    @Json(name = "created_at") val createdAt: Instant? = null,
)
