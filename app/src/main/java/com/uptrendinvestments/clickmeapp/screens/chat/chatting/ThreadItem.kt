package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import java.time.Instant

/** Delivery state of one message bubble. Only the sender's own messages ever show [Sending] or [Failed]. */
enum class Delivery {
    Sending,
    Sent,
    Read,
    Failed,
    ;

    companion object {
        /** Maps the server's `status` string: "read" (any case) is [Read], anything else is [Sent]. */
        fun fromServerStatus(status: String?): Delivery = if (status.equals("read", ignoreCase = true)) Read else Sent
    }
}

/**
 * The kind of a system event row in a thread. [isTerminal] mirrors the iOS `isTerminal` routing (the booking is
 * over, so the card has no further action) and [isPositive] picks the accent colour in the UI-SPEC.
 */
enum class SystemEventKind(
    val wire: String?,
    val isTerminal: Boolean,
    val isPositive: Boolean,
) {
    BookingRequest("booking_request", false, true),
    BookingConfirmed("BOOKING_CONFIRMED", false, true),
    BookingDeclined("BOOKING_DECLINED", true, false),
    BookingCancelled("BOOKING_CANCELLED", true, false),
    BookingRescheduled("BOOKING_RESCHEDULED", false, true),
    BookingCompleted("BOOKING_COMPLETED", true, true),
    BookingExpired("BOOKING_EXPIRED", true, false),
    Unknown(null, true, false),
    ;

    companion object {
        /** Matches [raw] to a kind ignoring case; null or an unrecognised value is [Unknown]. */
        fun fromWire(raw: String?): SystemEventKind =
            entries.firstOrNull { it.wire != null && it.wire.equals(raw, ignoreCase = true) } ?: Unknown
    }
}

/** One row of a thread: a chat message or a system event. */
sealed interface ThreadItem {
    /** Stable LazyColumn key; an own message keeps its client_msg_id key across ack and reload. */
    val key: String

    /** The server's id; null while a local bubble is unsent. */
    val serverId: String?
    val createdAt: Instant

    /** A chat message. [mine] is the sender's own message, [clientMsgId] is set on messages this device sent. */
    data class Message(
        override val serverId: String?,
        val clientMsgId: String?,
        val senderId: String?,
        val text: String,
        override val createdAt: Instant,
        val mine: Boolean,
        val delivery: Delivery,
    ) : ThreadItem {
        override val key: String
            get() = clientMsgId?.let { "c:" + it.lowercase() } ?: ("s:" + serverId.orEmpty().lowercase())

        override fun toString(): String =
            "Message(serverId=$serverId, clientMsgId=$clientMsgId, senderId=$senderId, createdAt=$createdAt, " +
                "mine=$mine, delivery=$delivery)"
    }

    /** A booking-related system event row. */
    data class SystemEvent(
        override val serverId: String,
        val kind: SystemEventKind,
        val bookingId: String?,
        val topic: String?,
        val startTime: Instant?,
        val content: String?,
        override val createdAt: Instant,
    ) : ThreadItem {
        override val key: String
            get() = "s:" + serverId.lowercase()

        override fun toString(): String =
            "SystemEvent(serverId=$serverId, kind=$kind, bookingId=$bookingId, startTime=$startTime, " +
                "createdAt=$createdAt)"
    }
}
