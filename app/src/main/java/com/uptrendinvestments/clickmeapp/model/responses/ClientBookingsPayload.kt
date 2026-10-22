package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

/** `GET /client/bookings` payload. */
@JsonClass(generateAdapter = true)
data class ClientBookingsPayload(
    val bookings: List<ClientBookingItem> = emptyList(),
    val pagination: PaginationMeta? = null,
)

/**
 * One client booking row. Phase 5 only needs today's upcoming sessions for the home screen. Phase 8
 * (My Bookings) extends this class with `reschedule` and `private_note_preview`; unknown keys are ignored
 * until then.
 */
@JsonClass(generateAdapter = true)
data class ClientBookingItem(
    @Json(name = "booking_id") val bookingId: String,
    val expert: BookingExpertDto,
    val topic: String? = null,
    @Json(name = "start_time") val startTime: Instant,
    @Json(name = "end_time") val endTime: Instant,
    @Json(name = "meeting_type") val meetingType: String? = null,
    val status: String,
)

@JsonClass(generateAdapter = true)
data class BookingExpertDto(
    val id: String,
    @Json(name = "full_name") val fullName: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
)
