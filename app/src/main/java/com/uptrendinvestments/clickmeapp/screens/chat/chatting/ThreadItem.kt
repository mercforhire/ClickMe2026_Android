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
    /**
     * Stable, unique LazyColumn key. An own message keeps its client_msg_id key across ack and reload; every other row
     * is keyed by its server id (IN-02).
     */
    val key: String

    /** The server's id; null while a local bubble is unsent. */
    val serverId: String?
    val createdAt: Instant

    /**
     * A chat message. [mine] is the sender's own message, [clientMsgId] is set on messages this device sent.
     *
     * The key uses the client_msg_id only for [mine] rows. The backend guarantees client_msg_id uniqueness only per
     * (thread, sender), and a peer can read my ids from the message list and reuse one, so a peer row keyed by it
     * would duplicate my key and crash the LazyColumn (IN-02). Peer rows use the server id, which is also stable
     * when a reload adds a client_msg_id to a row that arrived over the socket without one.
     */
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
            get() =
                if (mine && clientMsgId != null) {
                    "c:" + clientMsgId.lowercase()
                } else {
                    "s:" + serverId.orEmpty().lowercase()
                }

        override fun toString(): String =
            "Message(serverId=$serverId, clientMsgId=$clientMsgId, senderId=$senderId, createdAt=$createdAt, " +
                "mine=$mine, delivery=$delivery)"
    }

    /**
     * A booking-related system event row.
     *
     * [reasonCaption] and [reasonCodeLabel] carry another user's free text (the cancel, decline or reschedule reason),
     * so they are plain strings for a plain `Text` and are left out of [toString] to keep them out of logs.
     * [isRescheduleDecline] is the backend's `reschedule_decline` marker on a `BOOKING_DECLINED` row.
     */
    data class SystemEvent(
        override val serverId: String,
        val kind: SystemEventKind,
        val bookingId: String?,
        val topic: String?,
        val startTime: Instant?,
        val content: String?,
        override val createdAt: Instant,
        val reasonCaption: String? = null,
        val reasonCodeLabel: String? = null,
        val isRescheduleDecline: Boolean = false,
    ) : ThreadItem {
        override val key: String
            get() = "s:" + serverId.lowercase()

        /** Whether the booking behind this row is over. A reschedule decline leaves the booking active. */
        val isTerminal: Boolean
            get() = kind.isTerminal && !isRescheduleDecline

        override fun toString(): String =
            "SystemEvent(serverId=$serverId, kind=$kind, bookingId=$bookingId, startTime=$startTime, " +
                "createdAt=$createdAt, isRescheduleDecline=$isRescheduleDecline)"
    }
}
