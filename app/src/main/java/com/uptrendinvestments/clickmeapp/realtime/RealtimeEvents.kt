package com.uptrendinvestments.clickmeapp.realtime

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

/** Wire names of the socket events (backend SOCKET-CONTRACT.md section 4). */
object RealtimeEventNames {
    const val MESSAGE_NEW = "message.new"
    const val MESSAGE_READ = "message.read"
    const val USER_STATUS = "user.status"
    const val TYPING_START = "typing.start"
    const val TYPING_STOP = "typing.stop"
    const val BOOKING_UPDATE = "booking.update"
    const val CALL_ENDED = "call.ended"

    /** Every server-to-client event the app listens for. */
    val INBOUND: List<String> =
        listOf(
            MESSAGE_NEW,
            MESSAGE_READ,
            USER_STATUS,
            TYPING_START,
            TYPING_STOP,
            BOOKING_UPDATE,
            CALL_ENDED,
        )
}

/**
 * Typed server-to-client socket events.
 *
 * Ids, `type` and `status` stay [String]: Moshi has no UUID adapter, and a closed enum for booking
 * status (as on iOS) would drop values such as `rescheduled`. Callers compare against raw strings.
 */
sealed interface RealtimeEvent {
    @JsonClass(generateAdapter = true)
    data class MessageNew(
        @Json(name = "message_id") val messageId: String,
        @Json(name = "thread_id") val threadId: String,
        @Json(name = "sender_id") val senderId: String,
        val type: String,
        val content: String? = null,
        val timestamp: Instant,
        val status: String,
    ) : RealtimeEvent {
        /** Chat content is user data: only ids are printed. */
        override fun toString(): String = "MessageNew(messageId=$messageId, threadId=$threadId)"
    }

    @JsonClass(generateAdapter = true)
    data class MessageRead(
        @Json(name = "thread_id") val threadId: String,
        @Json(name = "message_ids") val messageIds: List<String>,
        @Json(name = "read_by") val readBy: String,
    ) : RealtimeEvent

    @JsonClass(generateAdapter = true)
    data class UserStatus(
        @Json(name = "user_id") val userId: String,
        val status: String,
    ) : RealtimeEvent {
        val isOnline: Boolean get() = status == "online"
    }

    data class TypingStart(
        val threadId: String,
        val senderId: String,
    ) : RealtimeEvent

    data class TypingStop(
        val threadId: String,
        val senderId: String,
    ) : RealtimeEvent

    /**
     * `reviewee_id` and `review_eligible` arrive on the completed variant, `actions_invalidated` on the
     * expert's expired variant; all are absent otherwise.
     */
    @JsonClass(generateAdapter = true)
    data class BookingUpdate(
        @Json(name = "booking_id") val bookingId: String,
        val status: String,
        @Json(name = "reviewee_id") val revieweeId: String? = null,
        @Json(name = "actions_invalidated") val actionsInvalidated: Boolean? = null,
        @Json(name = "review_eligible") val reviewEligible: Boolean? = null,
    ) : RealtimeEvent

    /** `end_reason` is `completed`, `dropped` or `manual_exit` when present. */
    @JsonClass(generateAdapter = true)
    data class CallEnded(
        @Json(name = "booking_id") val bookingId: String,
        @Json(name = "end_reason") val endReason: String? = null,
    ) : RealtimeEvent
}

/** Wire shape shared by `typing.start` and `typing.stop`. */
@JsonClass(generateAdapter = true)
internal data class TypingPayload(
    @Json(name = "thread_id") val threadId: String,
    @Json(name = "sender_id") val senderId: String,
)
