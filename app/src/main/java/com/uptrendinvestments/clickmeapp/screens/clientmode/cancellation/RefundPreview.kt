package com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation

import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingDetail
import java.time.Duration
import java.time.Instant

/** What the cancel confirmation tells the client about their money (D-06). */
sealed interface RefundPreview {
    /** Captured and cancelled early: the full topic price, in minor units, is refunded. */
    data class Full(
        val amountMinor: Long,
        val currency: String,
    ) : RefundPreview

    /** Captured and cancelled 24 hours or less before the start: no refund. */
    data object NoRefundWithin24h : RefundPreview

    /** Funds are only held (not captured): the hold is released. */
    data object HoldReleased : RefundPreview

    /** Captured but the price is unknown: show the generic policy text. */
    data object PolicyFallback : RefundPreview

    /** Free booking, or a payment status with nothing to promise: no refund line. */
    data object None : RefundPreview
}

/** Backend `refund.js`: a full refund only when the start is strictly more than this far away. */
val REFUND_FULL_WINDOW: Duration = Duration.ofHours(24)

/**
 * The refund preview for [detail] at [now] (D-06). Mirrors `refund.js`: a captured payment is refunded in full only
 * when the start is strictly more than 24 hours away, otherwise nothing. The server result stays authoritative (D-07).
 *
 * On the current backend every cancellable paid booking derives to `held` (manual capture after the session), so
 * [RefundPreview.Full] and [RefundPreview.NoRefundWithin24h] are dormant but correct if the backend captures earlier
 * (RESEARCH Finding 2; the dormant copy and the iOS "always full refund" promise are logged as follow-ups in plan
 * 08-15). Uses `topic.isFree`, never `price.isFree`.
 */
fun refundPreview(
    detail: ClientBookingDetail,
    now: Instant,
): RefundPreview {
    if (detail.topic.isFree) return RefundPreview.None
    return when (detail.paymentStatus) {
        "held" -> RefundPreview.HoldReleased
        "captured" -> {
            val amount = detail.topic.price?.amount
            val currency = detail.topic.price?.currency
            when {
                amount == null || currency == null -> RefundPreview.PolicyFallback
                Duration.between(now, detail.startTime) > REFUND_FULL_WINDOW -> RefundPreview.Full(amount, currency)
                else -> RefundPreview.NoRefundWithin24h
            }
        }
        else -> RefundPreview.None
    }
}
