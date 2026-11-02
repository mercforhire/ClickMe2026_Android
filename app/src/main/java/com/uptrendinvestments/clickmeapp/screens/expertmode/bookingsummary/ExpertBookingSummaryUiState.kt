package com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary

import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingDetail
import com.uptrendinvestments.clickmeapp.model.responses.RESCHEDULE_INITIATED_BY_CLIENT
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingStatusDisplay
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.JOINABLE_BOOKING_STATUSES
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleProposalUi
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.STATUS_COMPLETED
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.STATUS_PENDING_RESCHEDULE
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.summaryStatusDisplay
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.util.Money
import java.time.Duration
import java.time.Instant
import java.util.Locale

private const val SHORT_ID_LENGTH = 8

/** The backend's default currency, used only when a paid fee arrives with neither a currency nor a label (C5). */
private const val DEFAULT_CURRENCY = "CAD"

/** How the Consultation Fee row reads. The amount is formatted on the device, never taken from the server label. */
sealed interface SummaryFee {
    /** A free session, or no fee at all. */
    data object Free : SummaryFee

    /** A paid session, formatted from `consultation_fee.amount` and `currency` (JPY has no decimals). */
    data class Amount(
        val text: String,
    ) : SummaryFee

    /** Last resort for a paid session with no currency: the server's own label (RESEARCH C5). */
    data class ServerLabel(
        val text: String,
    ) : SummaryFee
}

/**
 * What the expert Booking Summary shows for one booking (EXP-08 / EXP-09).
 *
 * @property shortId the booking id without hyphens, first eight characters, upper-cased, without the `#`.
 * @property statusCode the server status code; Join and the join window branch on it.
 * @property preparationNotes the client's note to the expert, trimmed, or null when blank. User-written: show it, never
 *   log it.
 * @property canCancel the server's `actions.can_cancel`, the only gate for Cancel Booking (RESEARCH C8).
 * @property canReschedule the server's `actions.can_reschedule`, the only gate for the expert's Reschedule (Phase 16.2,
 *   locked: none of the client's 24 hour or awaiting-approval rules apply).
 * @property privateNote the expert's own full note, or null when there is none or it is blank (Phase 16.4, D-08).
 *   User-written: show it, never log it; toString omits it.
 * @property showsJoin Join is offered for joinable and rejoinable states (D-14); the join window only enables it.
 * @property proposal the pending reschedule shown as the first block (EXP-09), or null. Only a proposal made by the
 *   client is actionable for the expert; the card shows the expert's own as read-only (D-18).
 */
@Suppress("LongParameterList")
data class ExpertSummaryDetails(
    val bookingId: String,
    val shortId: String,
    val statusCode: String,
    val status: BookingStatusDisplay,
    val clientId: String,
    val clientName: String?,
    val clientAvatarUrl: String?,
    val topic: String?,
    val start: Instant,
    val end: Instant,
    val durationMinutes: Int,
    val meetingType: String?,
    val fee: SummaryFee,
    val preparationNotes: String?,
    val canCancel: Boolean,
    val canReschedule: Boolean,
    val showsJoin: Boolean,
    val proposal: RescheduleProposalUi? = null,
    val privateNote: String? = null,
) {
    override fun toString(): String = "ExpertSummaryDetails(bookingId=$bookingId, statusCode=$statusCode)"
}

/** Load state of the summary. */
sealed interface ExpertSummaryLoad {
    data object Loading : ExpertSummaryLoad

    data class Failed(
        val message: FieldError,
    ) : ExpertSummaryLoad

    /** The server no longer has this booking (404). Not an error: no Retry, the back arrow is the exit. */
    data object Unavailable : ExpertSummaryLoad

    data class Loaded(
        val details: ExpertSummaryDetails,
    ) : ExpertSummaryLoad
}

/**
 * UI state of the expert Booking Summary. 11-13 adds the reschedule block next to [load].
 *
 * @property showCallResumeNote the one-time "You were on a call" note, set for the booking a process kill interrupted
 *   (D-11, UI-SPEC A4).
 * @property isRefreshing a user Retry of a failed refresh is running; the content stays (D-05).
 * @property refreshFailed a realtime refetch or Retry failed with content showing; the screen shows the refresh-failed
 *   snackbar and consumes the flag.
 */
data class ExpertBookingSummaryUiState(
    val load: ExpertSummaryLoad = ExpertSummaryLoad.Loading,
    val showCallResumeNote: Boolean = false,
    val isRefreshing: Boolean = false,
    val refreshFailed: Boolean = false,
)

/** "A1B2C3D4" for "a1b2c3d4-e5f6-...": hyphens removed, first eight characters, upper-cased. */
fun expertShortBookingId(id: String): String = id.replace("-", "").take(SHORT_ID_LENGTH).uppercase(Locale.ROOT)

/**
 * Pure mapping of the expert booking detail to the display model. [now] comes from the injected `Clock`; it only
 * decides whether a `completed` booking is still rejoinable.
 */
fun ExpertBookingDetail.toSummaryDetails(
    now: Instant,
    locale: Locale,
): ExpertSummaryDetails {
    val code = status.code
    return ExpertSummaryDetails(
        bookingId = bookingId,
        shortId = expertShortBookingId(bookingId),
        statusCode = code,
        status = summaryStatusDisplay(code),
        clientId = client.id,
        clientName = client.name,
        clientAvatarUrl = client.avatarUrl,
        topic = session.topicTitle?.takeIf { it.isNotBlank() },
        start = startTime,
        end = endTime,
        durationMinutes = Duration.between(startTime, endTime).toMinutes().toInt(),
        meetingType = session.meetingType,
        fee = summaryFee(locale),
        preparationNotes = preparationNotes?.text?.trim()?.takeIf { it.isNotEmpty() },
        canCancel = actions.canCancel,
        canReschedule = actions.canReschedule,
        showsJoin = code in JOINABLE_BOOKING_STATUSES || (code == STATUS_COMPLETED && endTime.isAfter(now)),
        proposal = rescheduleProposal(),
        privateNote = expertPrivateNote?.takeIf { it.isNotBlank() },
    )
}

/**
 * The reschedule card's content, built only for a `pending_reschedule` booking with a proposed start. There is no
 * payment text for the expert (D-18), so the disclosure flags stay off.
 */
private fun ExpertBookingDetail.rescheduleProposal(): RescheduleProposalUi? {
    val proposed = reschedule?.proposedStart
    if (status.code != STATUS_PENDING_RESCHEDULE || reschedule == null || proposed == null) return null
    return RescheduleProposalUi(
        currentStart = startTime,
        proposedStart = proposed,
        proposedEnd = reschedule.proposedEnd,
        note = reschedule.note?.trim()?.takeIf { it.isNotEmpty() },
        initiatedByExpert = reschedule.initiatedBy != RESCHEDULE_INITIATED_BY_CLIENT,
        showsPaymentDisclosure = false,
        chargesSoon = false,
    )
}

private fun ExpertBookingDetail.summaryFee(locale: Locale): SummaryFee {
    val fee = session.consultationFee
    val amount = fee?.amount
    if (fee == null || amount == null || !fee.isPaid) return SummaryFee.Free
    val currency = fee.currency?.takeIf { it.isNotBlank() }
    return when {
        currency != null -> SummaryFee.Amount(Money.formatMajor(amount, currency, locale))
        !fee.label.isNullOrBlank() -> SummaryFee.ServerLabel(fee.label)
        else -> SummaryFee.Amount(Money.formatMajor(amount, DEFAULT_CURRENCY, locale))
    }
}
