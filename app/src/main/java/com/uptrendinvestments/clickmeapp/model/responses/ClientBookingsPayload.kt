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
 * One client booking row (My Bookings, Explore Today's Sessions). [reschedule] is non-null only while the booking
 * is `pending_reschedule` and carries the slim block. [privateNotePreview] is the client's own note, cut to 140 UTF-16
 * units by the server (Phase 16.4, D-08). User-written: show it, never log it; toString omits it. Unknown keys are
 * ignored.
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
    val reschedule: RescheduleDto? = null,
    @Json(name = "private_note_preview") val privateNotePreview: String? = null,
) {
    override fun toString(): String = "ClientBookingItem(bookingId=$bookingId, status=$status)"
}

/** Wire value of [RescheduleDto.initiatedBy] for a proposal the client may accept or decline (D-03). */
const val RESCHEDULE_INITIATED_BY_EXPERT = "expert"

/**
 * One DTO for the slim list block (`proposed_start`, `initiated_by`) and the full detail block (all six keys),
 * since the two share names (RESEARCH Pitfall 5). [proposedStart] is nullable on purpose so one malformed row can
 * never fail the whole list decode; the UI shows the proposal only when it is present. Any [initiatedBy] other
 * than [RESCHEDULE_INITIATED_BY_EXPERT] (including null) is not actionable (D-03; the server answers 403).
 */
@JsonClass(generateAdapter = true)
data class RescheduleDto(
    @Json(name = "proposed_start") val proposedStart: Instant? = null,
    @Json(name = "proposed_end") val proposedEnd: Instant? = null,
    @Json(name = "proposed_timezone") val proposedTimezone: String? = null,
    @Json(name = "initiated_by") val initiatedBy: String? = null,
    val note: String? = null,
    @Json(name = "created_at") val createdAt: Instant? = null,
)

@JsonClass(generateAdapter = true)
data class BookingExpertDto(
    val id: String,
    @Json(name = "full_name") val fullName: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
)
