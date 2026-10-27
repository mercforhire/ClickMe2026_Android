package com.uptrendinvestments.clickmeapp.model.requests

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// The backend zod schemas for chat and moderation are `.strict()` and `.optional()` rejects JSON null, so every
// request DTO here carries only schema keys and nullable fields default to null. The app Moshi omits nulls; never
// turn on null serialization for these types (RESEARCH Backend Contract, chat.schema.js, moderation.schema.js).

/**
 * `POST /chats/initiate` body. The backend schema is `{peer_id}`; the deprecated `expert_id` key is never sent
 * (iOS still sends it, the contract does not).
 */
@JsonClass(generateAdapter = true)
data class InitiateChatRequest(
    @Json(name = "peer_id") val peerId: String,
)

/**
 * `POST /chats/{id}/send` body. Exactly the keys `type`, `content` and `client_msg_id`: no attachments key.
 * [content] is user text, so [toString] prints the type and client message id only.
 */
@JsonClass(generateAdapter = true)
data class SendMessageRequest(
    val type: String,
    val content: String,
    @Json(name = "client_msg_id") val clientMsgId: String,
) {
    override fun toString(): String = "SendMessageRequest(type=$type, clientMsgId=$clientMsgId)"
}

/**
 * `POST /chats/{id}/actions` body. [action] is `block`, `unblock` or `report`; [reason] (a
 * [com.uptrendinvestments.clickmeapp.model.enums.UserReportReason] wire value) and [details] are only sent for
 * `report`. [details] is user text, so [toString] prints the action and reason only.
 */
@JsonClass(generateAdapter = true)
data class ChatActionRequest(
    val action: String,
    val reason: String? = null,
    val details: String? = null,
) {
    override fun toString(): String = "ChatActionRequest(action=$action, reason=$reason)"
}

/**
 * `POST /users/{id}/report` body: [reason] is a `UserReportReason` wire value, [details] optional (at most 1000
 * characters server side). [details] is user text, so [toString] prints the reason only.
 */
@JsonClass(generateAdapter = true)
data class ReportUserRequest(
    val reason: String,
    val details: String? = null,
) {
    override fun toString(): String = "ReportUserRequest(reason=$reason)"
}
