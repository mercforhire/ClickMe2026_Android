package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings

import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingDetail
import com.uptrendinvestments.clickmeapp.model.responses.RESCHEDULE_INITIATED_BY_EXPERT
import java.time.Duration
import java.time.Instant

/** A proposal starting sooner than this may be charged sooner than the original authorization window allows. */
val CHARGES_SOON_WINDOW: Duration = Duration.ofHours(48)

/**
 * Which extra lines the accept dialog adds. Informational only: it never gates accept (RESEARCH Anti-Patterns; the
 * backend withdrew authorization-window gating in RESCHEDULE-PAYMENT-CONVERSION.md).
 *
 * @property showsPaymentDisclosure the proposed session ends after the card hold expires.
 * @property chargesSoon the proposed session starts under 48 hours from now. The card appends this caveat only when
 *   [showsPaymentDisclosure] is true (UI-SPEC surface 3).
 */
data class RescheduleDisclosure(
    val showsPaymentDisclosure: Boolean,
    val chargesSoon: Boolean,
)

/**
 * Port of iOS `BookingSummaryViewModel.requiresAcceptRescheduleDisclosure` and `acceptRescheduleChargesSoon`.
 * [now] comes from the injected `Clock`.
 */
fun rescheduleDisclosure(
    proposedStart: Instant,
    proposedEnd: Instant?,
    authorizationExpiresAt: Instant?,
    now: Instant,
): RescheduleDisclosure =
    RescheduleDisclosure(
        showsPaymentDisclosure =
            authorizationExpiresAt != null && proposedEnd != null && proposedEnd.isAfter(authorizationExpiresAt),
        chargesSoon = Duration.between(now, proposedStart) < CHARGES_SOON_WINDOW,
    )

/**
 * The shared reschedule card model (D-01), or null when this booking has no pending proposal to show: the status
 * must be `pending_reschedule` and the proposal must carry a start. The card is actionable only when the expert
 * proposed it (D-03); otherwise it is the read-only "Awaiting expert's response" card.
 */
fun ClientBookingDetail.toRescheduleProposalUi(now: Instant): RescheduleProposalUi? {
    if (status != STATUS_PENDING_RESCHEDULE) return null
    val proposal = reschedule ?: return null
    val proposedStart = proposal.proposedStart ?: return null
    val disclosure = rescheduleDisclosure(proposedStart, proposal.proposedEnd, authorizationExpiresAt, now)
    return RescheduleProposalUi(
        currentStart = startTime,
        proposedStart = proposedStart,
        proposedEnd = proposal.proposedEnd,
        note = proposal.note?.trim()?.takeIf { it.isNotEmpty() },
        initiatedByExpert = proposal.initiatedBy == RESCHEDULE_INITIATED_BY_EXPERT,
        showsPaymentDisclosure = disclosure.showsPaymentDisclosure,
        chargesSoon = disclosure.chargesSoon,
    )
}
