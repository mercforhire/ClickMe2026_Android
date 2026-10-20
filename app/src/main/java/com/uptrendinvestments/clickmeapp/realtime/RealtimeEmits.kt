package com.uptrendinvestments.clickmeapp.realtime

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.uptrendinvestments.clickmeapp.network.MoshiProvider

@JsonClass(generateAdapter = true)
internal data class TypingEmit(
    @Json(name = "thread_id") val threadId: String,
    @Json(name = "recipient_id") val recipientId: String,
)

@JsonClass(generateAdapter = true)
internal data class MessageReadEmit(
    @Json(name = "thread_id") val threadId: String,
    @Json(name = "message_ids") val messageIds: List<String>,
)

/**
 * Client-to-server payload builders. Ids are lowercased (the backend stores UUIDs lowercase) and the
 * JSON is produced by Moshi, never by concatenation. The socket service converts the string to a
 * `JSONObject` at the library boundary. Sending a message stays REST and there is no room join, so
 * nothing else is emitted.
 */
object RealtimeEmits {
    private val typingAdapter = MoshiProvider.moshi.adapter(TypingEmit::class.java)
    private val messageReadAdapter = MoshiProvider.moshi.adapter(MessageReadEmit::class.java)

    fun typingPayload(
        threadId: String,
        recipientId: String,
    ): String = typingAdapter.toJson(TypingEmit(threadId.lowercase(), recipientId.lowercase()))

    /** Null for an empty list: the server ignores an empty array, and iOS guards the same way. */
    fun messageReadPayload(
        threadId: String,
        messageIds: List<String>,
    ): String? =
        if (messageIds.isEmpty()) {
            null
        } else {
            messageReadAdapter.toJson(MessageReadEmit(threadId.lowercase(), messageIds.map { it.lowercase() }))
        }
}
