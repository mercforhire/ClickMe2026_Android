package com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation

import com.uptrendinvestments.clickmeapp.model.responses.BookingDetailExpertDto
import com.uptrendinvestments.clickmeapp.model.responses.BookingDetailTopicDto
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingDetail
import com.uptrendinvestments.clickmeapp.model.responses.TopicPriceDto
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class RefundPreviewTest {
    private val now = Instant.parse("2026-10-22T16:00:00Z")

    private fun detail(
        isFree: Boolean = false,
        amount: Long? = 5000,
        currency: String? = "CAD",
        paymentStatus: String? = "captured",
        start: Instant = now.plus(Duration.ofDays(3)),
        hasPrice: Boolean = true,
    ) = ClientBookingDetail(
        bookingId = "b1",
        status = "confirmed",
        startTime = start,
        paymentStatus = paymentStatus,
        topic =
            BookingDetailTopicDto(
                id = "t1",
                title = "Algorithms",
                isFree = isFree,
                price = if (hasPrice) TopicPriceDto(amount = amount, currency = currency) else null,
            ),
        expert = BookingDetailExpertDto("e1", "Ada", null, null),
    )

    @Test
    fun captured_moreThan24hBeforeStart_isFullRefund() {
        val start = now.plus(Duration.ofHours(24)).plusSeconds(1)
        assertEquals(RefundPreview.Full(5000, "CAD"), refundPreview(detail(start = start), now))
    }

    @Test
    fun captured_exactly24hBeforeStart_isNoRefund() {
        val start = now.plus(Duration.ofHours(24))
        assertEquals(RefundPreview.NoRefundWithin24h, refundPreview(detail(start = start), now))
    }

    @Test
    fun captured_within24hOrAlreadyStarted_isNoRefund() {
        assertEquals(
            RefundPreview.NoRefundWithin24h,
            refundPreview(detail(start = now.plus(Duration.ofHours(2))), now),
        )
        assertEquals(
            RefundPreview.NoRefundWithin24h,
            refundPreview(detail(start = now.minus(Duration.ofHours(1))), now),
        )
    }

    @Test
    fun held_isHoldReleasedRegardlessOfStart() {
        assertEquals(RefundPreview.HoldReleased, refundPreview(detail(paymentStatus = "held"), now))
        assertEquals(
            RefundPreview.HoldReleased,
            refundPreview(detail(paymentStatus = "held", start = now.plus(Duration.ofHours(1))), now),
        )
    }

    @Test
    fun freeOrUnpaidStatuses_haveNoLine() {
        assertEquals(RefundPreview.None, refundPreview(detail(isFree = true, paymentStatus = "held"), now))
        assertEquals(RefundPreview.None, refundPreview(detail(isFree = true, paymentStatus = "captured"), now))
        assertEquals(RefundPreview.None, refundPreview(detail(paymentStatus = "none"), now))
        assertEquals(RefundPreview.None, refundPreview(detail(paymentStatus = "refunded"), now))
        assertEquals(RefundPreview.None, refundPreview(detail(paymentStatus = null), now))
    }

    @Test
    fun captured_withUnknownPrice_isPolicyFallback() {
        assertEquals(RefundPreview.PolicyFallback, refundPreview(detail(hasPrice = false), now))
        assertEquals(RefundPreview.PolicyFallback, refundPreview(detail(amount = null), now))
        assertEquals(RefundPreview.PolicyFallback, refundPreview(detail(currency = null), now))
    }
}
