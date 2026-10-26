package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

/**
 * `GET /expert/booking-requests` payload: the list endpoint returns every pending request in one response, so the
 * pending count is `requests.size`.
 */
@JsonClass(generateAdapter = true)
data class BookingRequestsPayload(
    val requests: List<BookingRequestItem> = emptyList(),
)

/**
 * One pending request row. [earnings] is MAJOR units and null for a free session; [earningsBreakdown] carries the
 * net figure to show. [expiresAt] drives the expiry label; the server flips a row to expired by cron, so the real
 * test is `expiresAt <= now`.
 */
@JsonClass(generateAdapter = true)
data class BookingRequestItem(
    @Json(name = "request_id") val requestId: String,
    val client: BookingRequestClientDto,
    val topic: String? = null,
    val earnings: Double? = null,
    val currency: String? = null,
    @Json(name = "earnings_breakdown") val earningsBreakdown: EarningsBreakdownDto? = null,
    @Json(name = "time_slot") val timeSlot: TimeSlotDto,
    @Json(name = "expires_at") val expiresAt: Instant? = null,
    val status: String? = null,
)

@JsonClass(generateAdapter = true)
data class BookingRequestClientDto(
    val id: String,
    @Json(name = "full_name") val fullName: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class TimeSlotDto(
    @Json(name = "start_time") val startTime: Instant,
    @Json(name = "end_time") val endTime: Instant,
)

/**
 * `GET /expert/booking-requests/{id}`. It carries no status and no expiry, so the caller passes `isExpired` in from
 * the tapped list row (RESEARCH C3).
 */
@JsonClass(generateAdapter = true)
data class BookingRequestDetail(
    @Json(name = "request_id") val requestId: String,
    val client: BookingRequestDetailClientDto,
    val session: BookingRequestSessionDto,
    val economics: BookingRequestEconomicsDto,
)

/** The detail endpoint names the client `name`, where the list uses `full_name`. */
@JsonClass(generateAdapter = true)
data class BookingRequestDetailClientDto(
    val id: String,
    val name: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
)

/** [clientMessage] is user-written free text: show it, never log it. */
@JsonClass(generateAdapter = true)
data class BookingRequestSessionDto(
    @Json(name = "start_time") val startTime: Instant,
    @Json(name = "end_time") val endTime: Instant,
    val topic: String? = null,
    @Json(name = "meeting_type") val meetingType: String? = null,
    @Json(name = "client_message") val clientMessage: String? = null,
)

/**
 * Only the currency and the breakdown are declared. The legacy earnings figure (always divided by 100 by the
 * server) is deliberately left out, because it is wrong for zero-decimal currencies (RESEARCH C4), so no screen
 * can show it.
 */
@JsonClass(generateAdapter = true)
data class BookingRequestEconomicsDto(
    val currency: String? = null,
    @Json(name = "earnings_breakdown") val earningsBreakdown: EarningsBreakdownDto? = null,
)

/** `POST /expert/booking-requests/{id}/accept` data. The server also returns a meeting link that is always null. */
@JsonClass(generateAdapter = true)
data class AcceptBookingRequestData(
    @Json(name = "booking_id") val bookingId: String,
    val status: String? = null,
    @Json(name = "confirmed_at") val confirmedAt: Instant? = null,
)

/** `POST /expert/booking-requests/{id}/decline` data. */
@JsonClass(generateAdapter = true)
data class DeclineBookingRequestData(
    @Json(name = "request_id") val requestId: String,
    val status: String? = null,
    @Json(name = "declined_at") val declinedAt: Instant? = null,
)
