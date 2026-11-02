package com.uptrendinvestments.clickmeapp.screens.clientmode.payments

import com.uptrendinvestments.clickmeapp.testing.clientTransaction
import com.uptrendinvestments.clickmeapp.util.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class ClientTransactionLogicTest {
    private fun labelOf(status: ClientPaymentStatus): String =
        RuntimeEnvironment.getApplication().getString(status.label)

    @Test
    fun everyDocumentedStatusHasItsLabel() {
        val expected =
            listOf(
                Triple("captured", ClientPaymentStatus.Paid, "Paid"),
                Triple("requires_capture", ClientPaymentStatus.AuthorizedNotCharged, "Authorized (not yet charged)"),
                Triple("scheduled", ClientPaymentStatus.Scheduled, "Scheduled"),
                Triple("processing", ClientPaymentStatus.Processing, "Processing"),
                Triple("requires_payment_method", ClientPaymentStatus.PaymentNeeded, "Payment needed"),
                Triple("refunded", ClientPaymentStatus.Refunded, "Refunded"),
                Triple("partially_refunded", ClientPaymentStatus.PartiallyRefunded, "Partially refunded"),
                Triple("failed", ClientPaymentStatus.PaymentFailed, "Payment failed"),
                Triple("canceled", ClientPaymentStatus.Canceled, "Canceled"),
                Triple("auth_expired", ClientPaymentStatus.AuthExpired, "Authorization expired"),
            )
        expected.forEach { (raw, status, label) ->
            assertEquals(raw, status, clientPaymentStatusOf(raw, 7500L))
            assertEquals(raw, label, labelOf(status))
        }
        assertEquals("Processed", labelOf(ClientPaymentStatus.Processed))
        assertEquals("No payment", labelOf(ClientPaymentStatus.NoPayment))
        assertEquals("—", labelOf(ClientPaymentStatus.Unknown))
    }

    @Test
    fun noneWithAnAmountIsProcessed() {
        assertEquals(ClientPaymentStatus.Processed, clientPaymentStatusOf("none", 5000L))
        assertEquals(ClientPaymentStatus.NoPayment, clientPaymentStatusOf("none", 0L))
        assertEquals(ClientPaymentStatus.NoPayment, clientPaymentStatusOf("none", null))
    }

    @Test
    fun unknownStatusIsUnknownAndNeverThrows() {
        assertEquals(ClientPaymentStatus.Unknown, clientPaymentStatusOf("something_new", 100L))
        assertEquals(ClientPaymentStatus.Unknown, clientPaymentStatusOf("", 100L))
    }

    @Test
    fun spellingMatters() {
        assertEquals(ClientPaymentStatus.Canceled, clientPaymentStatusOf("canceled", null))
        assertEquals(ClientPaymentStatus.Unknown, clientPaymentStatusOf("cancelled", null))
    }

    @Test
    fun tintsMatchTheUiSpecTable() {
        val expected =
            mapOf(
                ClientPaymentStatus.Paid to ClientPaymentPillTint.Primary,
                ClientPaymentStatus.AuthorizedNotCharged to ClientPaymentPillTint.Tertiary,
                ClientPaymentStatus.Scheduled to ClientPaymentPillTint.Tertiary,
                ClientPaymentStatus.Processing to ClientPaymentPillTint.Tertiary,
                ClientPaymentStatus.PaymentNeeded to ClientPaymentPillTint.Tertiary,
                ClientPaymentStatus.PaymentFailed to ClientPaymentPillTint.Error,
                ClientPaymentStatus.Canceled to ClientPaymentPillTint.Error,
                ClientPaymentStatus.AuthExpired to ClientPaymentPillTint.Error,
                ClientPaymentStatus.Refunded to ClientPaymentPillTint.Neutral,
                ClientPaymentStatus.PartiallyRefunded to ClientPaymentPillTint.Neutral,
                ClientPaymentStatus.Processed to ClientPaymentPillTint.Neutral,
                ClientPaymentStatus.NoPayment to ClientPaymentPillTint.Neutral,
                ClientPaymentStatus.Unknown to ClientPaymentPillTint.Neutral,
            )
        assertEquals(ClientPaymentStatus.entries.toSet(), expected.keys)
        expected.forEach { (status, tint) -> assertEquals(status.name, tint, status.tint) }
    }

    @Test
    fun minorUnitsUseTheCurrencysOwnDecimals() {
        val cad = clientAmountText(7500L, "CAD", Locale.US)
        assertEquals(Money.format(7500L, "CAD", Locale.US), cad)
        assertTrue(cad!!.contains("75.00"))

        val jpy = clientAmountText(7500L, "JPY", Locale.US)!!
        assertTrue(jpy, jpy.contains("7,500"))
        assertFalse(jpy, jpy.contains("."))
    }

    @Test
    fun nullAmountHasNoTextAndZeroDoes() {
        assertNull(clientAmountText(null, "CAD", Locale.US))
        val zero = clientAmountText(0L, "CAD", Locale.US)
        assertEquals(Money.format(0L, "CAD", Locale.US), zero)
        assertTrue(zero!!.contains("0.00"))
    }

    @Test
    fun partialRefund() {
        assertTrue(isPartialRefund(clientTransaction(amount = 7500L, refunded = 2500L)))
        assertFalse(isPartialRefund(clientTransaction(amount = 7500L, refunded = 7500L)))
        assertFalse(isPartialRefund(clientTransaction(amount = 7500L, refunded = 0L)))
        assertFalse(isPartialRefund(clientTransaction(amount = null, refunded = 2500L)))
        assertFalse(isPartialRefund(clientTransaction(amount = 0L, refunded = 2500L)))
    }

    @Test
    fun rowDatePrefersTheSessionDate() {
        val withSession =
            clientTransaction(
                sessionDate = Instant.parse("2026-08-17T14:30:00Z"),
                createdAt = Instant.parse("2026-08-10T09:00:00Z"),
            )
        assertEquals("Aug 17, 2026", clientRowDate(withSession, ZoneOffset.UTC, Locale.US))

        val orphan = withSession.copy(sessionDate = null)
        assertEquals("Aug 10, 2026", clientRowDate(orphan, ZoneOffset.UTC, Locale.US))
    }

    @Test
    fun amountIsMutedOnlyWhenFullyRefunded() {
        assertTrue(amountIsMuted("refunded"))
        assertFalse(amountIsMuted("partially_refunded"))
        assertFalse(amountIsMuted("captured"))
    }
}
