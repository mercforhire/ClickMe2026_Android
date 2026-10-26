package com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation

import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingDetail

/** What is known about the booking's payment. The expert detail exposes no payment status (RESEARCH C7). */
enum class PaymentState { None, Held }

/** What the expert's cancel confirmation tells them about the client's money (D-16 as resolved, Open Question 1). */
sealed interface ExpertRefundPreview {
    /** Paid booking: the client's payment is only held, and cancelling releases the hold. */
    data object HoldReleased : ExpertRefundPreview

    /** Free booking: no refund line. */
    data object None : ExpertRefundPreview
}

/** The preview for a [payment] state. */
fun expertRefundPreview(payment: PaymentState): ExpertRefundPreview =
    when (payment) {
        PaymentState.None -> ExpertRefundPreview.None
        PaymentState.Held -> ExpertRefundPreview.HoldReleased
    }

/**
 * Capture happens only after the session completes, so every cancellable paid booking is a hold; the expert detail
 * exposes no payment status (RESEARCH C7, Open Question Resolution 1).
 */
fun ExpertBookingDetail.paymentState(): PaymentState = if (isPaid) PaymentState.Held else PaymentState.None
