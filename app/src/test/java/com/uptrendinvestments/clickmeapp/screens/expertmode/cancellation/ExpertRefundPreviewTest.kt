package com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation

import com.uptrendinvestments.clickmeapp.testing.expertBookingDetail
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ExpertRefundPreviewTest {
    private val now = Instant.parse("2026-10-23T12:00:00Z")

    private fun preview(
        payment: PaymentState,
        amount: Double? = 75.0,
        currency: String? = "USD",
        start: Instant = now.plusSeconds(HOURS_48),
    ) = expertRefundPreview(payment, amount, currency, start, now)

    @Test
    fun nonePaymentShowsNothing() {
        assertEquals(ExpertRefundPreview.None, preview(PaymentState.None))
    }

    @Test
    fun heldPaymentReleasesTheHoldWhateverTheTime() {
        assertEquals(ExpertRefundPreview.HoldReleased, preview(PaymentState.Held, start = now.plusSeconds(HOURS_48)))
        assertEquals(ExpertRefundPreview.HoldReleased, preview(PaymentState.Held, start = now.plusSeconds(60)))
        assertEquals(ExpertRefundPreview.HoldReleased, preview(PaymentState.Held, start = now.minusSeconds(60)))
    }

    @Test
    fun capturedMoreThan24HoursAheadRefundsInFull() {
        val result = preview(PaymentState.Captured, start = now.plusSeconds(HOURS_24 + 1))

        assertEquals(ExpertRefundPreview.ClientFullRefund(75.0, "USD"), result)
    }

    @Test
    fun capturedExactly24HoursAheadIsNotRefunded() {
        val result = preview(PaymentState.Captured, start = now.plusSeconds(HOURS_24))

        assertEquals(ExpertRefundPreview.ClientNoRefundWithin24h, result)
    }

    @Test
    fun capturedWithinTheLast24HoursIsNotRefunded() {
        assertEquals(
            ExpertRefundPreview.ClientNoRefundWithin24h,
            preview(PaymentState.Captured, start = now.plusSeconds(HOURS_24 - 1)),
        )
    }

    @Test
    fun capturedWithoutAnAmountFallsBackToPolicy() {
        assertEquals(ExpertRefundPreview.PolicyFallback, preview(PaymentState.Captured, amount = null))
    }

    @Test
    fun capturedWithoutACurrencyFallsBackToPolicy() {
        assertEquals(ExpertRefundPreview.PolicyFallback, preview(PaymentState.Captured, currency = null))
    }

    @Test
    fun aPaidDetailIsHeld() {
        assertEquals(PaymentState.Held, expertBookingDetail(fee = 75.0).paymentState())
    }

    @Test
    fun aZeroFeeDetailIsNone() {
        assertEquals(PaymentState.None, expertBookingDetail(fee = 0.0).paymentState())
    }

    @Test
    fun aDetailWithoutAFeeIsNone() {
        assertEquals(PaymentState.None, expertBookingDetail(fee = null).paymentState())
    }

    private companion object {
        const val HOURS_24 = 24L * 3600
        const val HOURS_48 = 48L * 3600
    }
}
