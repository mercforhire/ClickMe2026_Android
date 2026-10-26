package com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation

import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingDetail
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.REFUND_FULL_WINDOW
import java.time.Duration
import java.time.Instant

/** What is known about the booking's payment. The expert detail exposes no payment status (RESEARCH C7). */
enum class PaymentState { None, Held, Captured }

/** What the expert's cancel confirmation tells them about the client's money (D-16 as resolved, Open Question 1). */
sealed interface ExpertRefundPreview {
    /** Captured and cancelled early: the client gets the full price, in major units. Dormant today. */
    data class ClientFullRefund(
        val amountMajor: Double,
        val currency: String,
    ) : ExpertRefundPreview

    /** Captured and cancelled 24 hours or less before the start: the client is not refunded. Dormant today. */
    data object ClientNoRefundWithin24h : ExpertRefundPreview

    /** Paid booking whose payment is only held: cancelling releases the hold. */
    data object HoldReleased : ExpertRefundPreview

    /** Captured but the price is unknown: show the generic policy text. Dormant today. */
    data object PolicyFallback : ExpertRefundPreview

    /** Free booking: no refund line. */
    data object None : ExpertRefundPreview
}

/**
 * The refund preview for a booking in [payment] state at [now] (D-16 as resolved by Open Question 1). Mirrors backend
 * `refund.js` for a captured payment: a full refund only when the start is strictly more than 24 hours away, otherwise
 * nothing. A held payment is always released. The server result stays authoritative.
 */
fun expertRefundPreview(
    payment: PaymentState,
    amountMajor: Double?,
    currency: String?,
    start: Instant,
    now: Instant,
): ExpertRefundPreview =
    when (payment) {
        PaymentState.None -> ExpertRefundPreview.None
        PaymentState.Held -> ExpertRefundPreview.HoldReleased
        PaymentState.Captured ->
            when {
                amountMajor == null || currency == null -> ExpertRefundPreview.PolicyFallback
                Duration.between(now, start) > REFUND_FULL_WINDOW ->
                    ExpertRefundPreview.ClientFullRefund(amountMajor, currency)
                else -> ExpertRefundPreview.ClientNoRefundWithin24h
            }
    }

/**
 * The payment state the preview is fed. Capture happens only after the session completes, so every cancellable paid
 * booking is a hold, and the expert detail exposes no payment status (RESEARCH C7, Open Question Resolution 1). The
 * [PaymentState.Captured] rows are dormant until the API exposes a status.
 */
fun ExpertBookingDetail.paymentState(): PaymentState = if (isPaid) PaymentState.Held else PaymentState.None
