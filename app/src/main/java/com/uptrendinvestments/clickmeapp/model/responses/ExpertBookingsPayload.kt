package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

/** Wire value of [RescheduleDto.initiatedBy] for a proposal the expert may accept or decline (D-12, D-18). */
const val RESCHEDULE_INITIATED_BY_CLIENT = "client"

/** `GET /expert/bookings` payload. [viewState] is informational; the client derives its own empty states. */
@JsonClass(generateAdapter = true)
data class ExpertBookingsPayload(
    @Json(name = "view_state") val viewState: String? = null,
    val bookings: List<ExpertBookingItem> = emptyList(),
    val pagination: PaginationMeta? = null,
)

/**
 * One expert booking row. Unlike the client feed, [status] is an object and the topic, times and money live under
 * [session]. [reschedule] is non-null only while the booking is `pending_reschedule` and carries the slim block
 * (`proposed_start`, `initiated_by`). The server's accent color is deliberately not declared; unknown keys are
 * ignored. [actions] is declared (`can_cancel`, `can_reschedule`, `can_join`) and defaults to all false when the key
 * is absent.
 * [privateNotePreview] is the expert's own note, cut to 140 UTF-16 units by the server (Phase 16.4, D-08).
 * User-written: show it, never log it; toString omits it.
 */
@JsonClass(generateAdapter = true)
data class ExpertBookingItem(
    @Json(name = "booking_id") val bookingId: String,
    val client: ExpertBookingClientDto,
    val session: ExpertBookingSessionDto,
    val status: ExpertStatusMeta,
    val reschedule: RescheduleDto? = null,
    val actions: ExpertBookingActionsDto = ExpertBookingActionsDto(),
    @Json(name = "private_note_preview") val privateNotePreview: String? = null,
) {
    val startTime: Instant get() = session.startTime
    val endTime: Instant get() = session.endTime
    val topic: String? get() = session.topic
    val statusCode: String get() = status.code

    override fun toString(): String = "ExpertBookingItem(bookingId=$bookingId, status=$statusCode)"
}

@JsonClass(generateAdapter = true)
data class ExpertBookingClientDto(
    val id: String,
    val name: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class ExpertBookingSessionDto(
    val topic: String? = null,
    @Json(name = "start_time") val startTime: Instant,
    @Json(name = "end_time") val endTime: Instant,
    @Json(name = "meeting_type") val meetingType: String? = null,
    @Json(name = "consultation_fee") val consultationFee: ConsultationFeeDto? = null,
    @Json(name = "earnings_breakdown") val earningsBreakdown: EarningsBreakdownDto? = null,
)

/**
 * Object-shaped booking status on the expert endpoints (RESEARCH C6). Branch on [code]. The server's hex accent is
 * deliberately not declared because colors come from the theme.
 */
@JsonClass(generateAdapter = true)
data class ExpertStatusMeta(
    val label: String? = null,
    val code: String,
)

/**
 * The session price. [amount] is in MAJOR units. The server [label] hard-codes a dollar sign whatever the currency,
 * so screens format [amount] with `Money.formatMajor` and use [label] only as a last resort (RESEARCH C5).
 */
@JsonClass(generateAdapter = true)
data class ConsultationFeeDto(
    val amount: Double? = null,
    val currency: String? = null,
    val label: String? = null,
) {
    /** A free session reports an amount of 0, so only a positive amount is paid. */
    val isPaid: Boolean get() = amount != null && amount > 0.0
}

/**
 * `GET /expert/bookings/{id}/details`. [paymentActionRequired] and [paymentNotCharged] were added by the backend at
 * 65bb2ed (live in 1d18aac) and iOS reads them the same way (`ExpertBookingDetail.swift`); a missing key means
 * unknown and the expert cancel line fails closed. The cancel, reschedule and join flags are declared on [actions]; the join flag
 * opens only the reconnect tier of `isWithinJoinWindow`. The message flag stays undeclared (messaging is a later
 * phase). [expertPrivateNote] is the expert's own full note (Phase 16.4, D-08). User-written: show it, never log
 * it; toString omits it.
 */
@JsonClass(generateAdapter = true)
data class ExpertBookingDetail(
    @Json(name = "booking_id") val bookingId: String,
    val status: ExpertStatusMeta,
    val reschedule: RescheduleDto? = null,
    @Json(name = "authorization_expires_at") val authorizationExpiresAt: Instant? = null,
    @Json(name = "payment_action_required") val paymentActionRequired: Boolean? = null,
    @Json(name = "payment_not_charged") val paymentNotCharged: Boolean? = null,
    val client: ExpertBookingClientDto,
    val session: ExpertBookingDetailSessionDto,
    @Json(name = "preparation_notes") val preparationNotes: PreparationNotesDto? = null,
    val actions: ExpertBookingActionsDto = ExpertBookingActionsDto(),
    @Json(name = "expert_private_note") val expertPrivateNote: String? = null,
) {
    val startTime: Instant get() = session.schedule.startTime
    val endTime: Instant get() = session.schedule.endTime

    /** Free when the fee is missing or zero. */
    val isPaid: Boolean get() = session.consultationFee?.isPaid == true

    override fun toString(): String = "ExpertBookingDetail(bookingId=$bookingId, status=${status.code})"
}

@JsonClass(generateAdapter = true)
data class ExpertBookingDetailSessionDto(
    @Json(name = "topic_title") val topicTitle: String? = null,
    @Json(name = "consultation_fee") val consultationFee: ConsultationFeeDto? = null,
    @Json(name = "earnings_breakdown") val earningsBreakdown: EarningsBreakdownDto? = null,
    val schedule: ScheduleDto,
    @Json(name = "meeting_type") val meetingType: String? = null,
)

@JsonClass(generateAdapter = true)
data class ScheduleDto(
    @Json(name = "start_time") val startTime: Instant,
    @Json(name = "end_time") val endTime: Instant,
)

/** The client's note to the expert. User-written: show it, never log it. */
@JsonClass(generateAdapter = true)
data class PreparationNotesDto(
    val text: String? = null,
)

/**
 * The cancel flag gates Cancel Booking (RESEARCH C8) and the reschedule flag gates the expert Reschedule button on
 * Expert Booking Summary and the expert upcoming card (Phase 16.2, locked). The server is the authority; a missing
 * key hides the button. The join flag feeds `isWithinJoinWindow` as its server flag (iOS
 * `ExpertBookingItem.Actions.canJoin`); a missing key closes the reconnect tier.
 */
@JsonClass(generateAdapter = true)
data class ExpertBookingActionsDto(
    @Json(name = "can_cancel") val canCancel: Boolean = false,
    @Json(name = "can_reschedule") val canReschedule: Boolean = false,
    @Json(name = "can_join") val canJoin: Boolean = false,
)
