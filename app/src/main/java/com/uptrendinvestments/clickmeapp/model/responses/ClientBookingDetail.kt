package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

/**
 * `GET /client/bookings/{id}` payload. Returned by `mybookings/ClientBookingsRepository.detail`; used by Request
 * Sent, Upcoming Booking Details, Cancellation and Booking Summary.
 *
 * [paymentStatus] is the server's derived display vocabulary `none | held | captured | refunded`. [privateNote] is
 * the client's own full note (Phase 16.4, D-08). User-written: show it, never log it; toString omits it.
 * [authorizationExpiresAt] is informational only, used for the accept disclosure.
 */
@JsonClass(generateAdapter = true)
data class ClientBookingDetail(
    @Json(name = "booking_id") val bookingId: String,
    val status: String,
    @Json(name = "start_time") val startTime: Instant,
    @Json(name = "end_time") val endTime: Instant? = null,
    @Json(name = "meeting_type") val meetingType: String? = null,
    @Json(name = "expert_timezone") val expertTimezone: String? = null,
    @Json(name = "payment_status") val paymentStatus: String? = null,
    val topic: BookingDetailTopicDto,
    val expert: BookingDetailExpertDto,
    @Json(name = "client_notes") val clientNotes: String? = null,
    @Json(name = "authorization_expires_at") val authorizationExpiresAt: Instant? = null,
    @Json(name = "join_link") val joinLink: String? = null,
    @Json(name = "can_join") val canJoin: Boolean = false,
    val reschedule: RescheduleDto? = null,
    @Json(name = "private_note") val privateNote: String? = null,
) {
    override fun toString(): String = "ClientBookingDetail(bookingId=$bookingId, status=$status)"
}

/**
 * Topic block of the booking detail. [isFree] is the topic-level flag (a sibling of `price`), not
 * `price.is_free` (see the iOS `ClientBookingDetail.swift` comment).
 */
@JsonClass(generateAdapter = true)
data class BookingDetailTopicDto(
    val id: String? = null,
    val title: String = "",
    @Json(name = "duration_mins") val durationMins: Int? = null,
    @Json(name = "is_free") val isFree: Boolean = false,
    val price: TopicPriceDto? = null,
)

@JsonClass(generateAdapter = true)
data class BookingDetailExpertDto(
    val id: String? = null,
    @Json(name = "full_name") val fullName: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    val title: String? = null,
)
