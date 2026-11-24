package com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/** Mirrors iOS `ExpertCancelRefundLineTests` case by case. */
class ExpertRefundPreviewTest {
    private val hold = Instant.parse("2026-10-24T12:00:00Z")

    @Suppress("LongParameterList")
    private fun preview(
        amount: Double? = 75.0,
        currency: String? = "USD",
        expires: Instant? = null,
        actionRequired: Boolean? = false,
        notCharged: Boolean? = false,
    ) = expertRefundPreview(
        amountMajor = amount,
        currency = currency,
        authorizationExpiresAt = expires,
        paymentActionRequired = actionRequired,
        paymentNotCharged = notCharged,
    )

    @Test
    fun freeBookingHasNoLineWhateverTheKeysRead() {
        listOf(0.0, null).forEach { amount ->
            listOf(true, false, null).forEach { a ->
                listOf(true, false, null).forEach { n ->
                    assertEquals(
                        ExpertRefundPreview.None,
                        preview(amount = amount, expires = hold, actionRequired = a, notCharged = n),
                    )
                }
            }
        }
    }

    @Test
    fun eitherKeyMissingShowsNoLine() {
        listOf(hold, null).forEach { expires ->
            assertEquals(ExpertRefundPreview.None, preview(expires = expires, actionRequired = null))
            assertEquals(ExpertRefundPreview.None, preview(expires = expires, notCharged = null))
            assertEquals(
                ExpertRefundPreview.None,
                preview(expires = expires, actionRequired = null, notCharged = null),
            )
        }
    }

    @Test
    fun cardAuthenticationPendingShowsNoLine() {
        assertEquals(ExpertRefundPreview.None, preview(expires = hold, actionRequired = true))
        assertEquals(ExpertRefundPreview.None, preview(actionRequired = true))
    }

    @Test
    fun notChargedShowsNoLine() {
        assertEquals(ExpertRefundPreview.None, preview(expires = hold, notCharged = true))
        assertEquals(ExpertRefundPreview.None, preview(notCharged = true))
    }

    @Test
    fun liveHoldReadsAsReleased() {
        assertEquals(ExpertRefundPreview.HoldReleased(75.0, "USD"), preview(expires = hold))
    }

    @Test
    fun paidWithoutAHoldReadsAsFullRefund() {
        assertEquals(ExpertRefundPreview.FullRefund(75.0, "USD"), preview(expires = null))
    }
}
