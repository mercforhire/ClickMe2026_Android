package com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation

import java.time.Instant

/**
 * What the expert's cancel confirmation tells them about the client's money. Port of iOS `ExpertCancelRefundLine`
 * (D-14, D-16, WR-03).
 */
sealed interface ExpertRefundPreview {
    /** No client-money claim: a free booking, nothing charged, a charge still waiting on card authentication, or unknown. */
    data object None : ExpertRefundPreview

    /** A live authorization: cancelling releases it and nothing was charged. */
    data class HoldReleased(
        val amountMajor: Double,
        val currency: String?,
    ) : ExpertRefundPreview

    /** A captured payment: cancelling refunds it in full. */
    data class FullRefund(
        val amountMajor: Double,
        val currency: String?,
    ) : ExpertRefundPreview
}

/**
 * The client-money line for the expert cancel sheet. An expert cancel always releases the hold or refunds in full
 * (backend `cancel` in `expertBooking.controller.js`, D-17 at 1d18aac), so no within-24-hours outcome exists. The
 * line may be shown only when the payload proves money is held or was taken:
 *
 * - [paymentActionRequired] true means a charge is waiting on the client's card authentication (D-14).
 * - [paymentNotCharged] true means no money is held on or taken from the card (D-16).
 * - A missing key (null) is unknown, so the rule fails closed.
 *
 * Only both keys exactly false gives a line: [ExpertRefundPreview.HoldReleased] while [authorizationExpiresAt] is set
 * (a live hold), otherwise [ExpertRefundPreview.FullRefund]. The amount is the list fee, not what the client paid (a
 * promo lowers it and the expert payload does not expose it), which is why the copy says "up to".
 */
fun expertRefundPreview(
    amountMajor: Double?,
    currency: String?,
    authorizationExpiresAt: Instant?,
    paymentActionRequired: Boolean?,
    paymentNotCharged: Boolean?,
): ExpertRefundPreview {
    if (amountMajor == null || amountMajor <= 0.0) return ExpertRefundPreview.None
    if (paymentActionRequired != false || paymentNotCharged != false) return ExpertRefundPreview.None
    return if (authorizationExpiresAt != null) {
        ExpertRefundPreview.HoldReleased(amountMajor, currency)
    } else {
        ExpertRefundPreview.FullRefund(amountMajor, currency)
    }
}
