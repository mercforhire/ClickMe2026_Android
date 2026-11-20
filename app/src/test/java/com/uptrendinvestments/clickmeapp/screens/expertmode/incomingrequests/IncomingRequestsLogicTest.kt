package com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests

import com.uptrendinvestments.clickmeapp.model.responses.FEE_SOURCE_UNAVAILABLE
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.RequestDecisionKey
import com.uptrendinvestments.clickmeapp.testing.bookingRequestItem
import com.uptrendinvestments.clickmeapp.testing.breakdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.util.Locale

class IncomingRequestsLogicTest {
    private val now = Instant.parse("2026-10-22T16:00:00Z")
    private val start = now.plus(Duration.ofDays(2))

    private fun row(
        id: String,
        expiresIn: Duration?,
        status: String? = "pending_approval",
    ) = bookingRequestItem(id = id, start = start, expiresAt = expiresIn?.let { now.plus(it) }, status = status)

    @Test
    fun statusExpiredIsExpiredEvenWithAFutureExpiry() {
        assertTrue(row("r1", Duration.ofHours(3), status = "expired").isExpiredAt(now))
    }

    @Test
    fun expiryEqualToNowIsExpired() {
        assertTrue(row("r1", Duration.ZERO).isExpiredAt(now))
    }

    @Test
    fun aNullExpiryOnAPendingRowIsPending() {
        assertFalse(row("r1", null).isExpiredAt(now))
    }

    @Test
    fun oneSecondLeftIsPendingWithZeroHours() {
        val rows = buildIncomingRequestRows(listOf(row("r1", Duration.ofSeconds(1))), now)
        assertFalse(rows.single().isExpired)
        assertEquals(0L, rows.single().hoursLeft)
    }

    @Test
    fun fiveHoursFiftyNineMinutesCountsWholeHours() {
        val rows = buildIncomingRequestRows(listOf(row("r1", Duration.ofMinutes(5 * 60 + 59L))), now)
        assertEquals(5L, rows.single().hoursLeft)
    }

    @Test
    fun anExpiredRowHasNoHoursLeft() {
        val rows = buildIncomingRequestRows(listOf(row("r1", Duration.ofHours(-2))), now)
        assertTrue(rows.single().isExpired)
        assertNull(rows.single().hoursLeft)
    }

    @Test
    fun pendingRowsSortBySoonestExpiryThenNullExpiryThenExpiredLast() {
        val items =
            listOf(
                row("expired-late", Duration.ofHours(-1)),
                row("later", Duration.ofHours(20)),
                row("no-expiry", null),
                row("soon", Duration.ofHours(2)),
                row("expired-early", Duration.ofHours(-30)),
                row("flagged", Duration.ofHours(1), status = "expired"),
            )

        val ids = buildIncomingRequestRows(items, now).map { it.item.requestId }

        assertEquals(listOf("soon", "later", "no-expiry", "expired-early", "expired-late", "flagged"), ids)
    }

    @Test
    fun netAmountIsFormattedInMajorUnits() {
        val item = bookingRequestItem(earnings = 75.0, breakdown = breakdown(net = 63.75, currency = "USD"))
        assertEquals(RequestCardEarnings.Net("US\$63.75"), requestCardEarnings(item, Locale.US))
    }

    @Test
    fun aZeroDecimalCurrencyHasNoDecimals() {
        val item =
            bookingRequestItem(
                earnings = 10_000.0,
                currency = "JPY",
                breakdown = breakdown(gross = 10_000.0, fee = 2_500.0, net = 7_500.0, currency = "JPY"),
            )
        assertEquals(RequestCardEarnings.Net("¥7,500"), requestCardEarnings(item, Locale.US))
    }

    @Test
    fun aFreeRequestIsFree() {
        assertEquals(
            RequestCardEarnings.Free,
            requestCardEarnings(bookingRequestItem(earnings = null, breakdown = null)),
        )
        val zeroBreakdown = breakdown(gross = 0.0, fee = 0.0, net = 0.0, pct = 0.0)
        assertEquals(
            RequestCardEarnings.Free,
            requestCardEarnings(bookingRequestItem(earnings = 0.0, breakdown = zeroBreakdown)),
        )
    }

    @Test
    fun aPaidRequestWithoutANetAmountIsPayoutPendingAndNeverShowsGross() {
        val unavailable =
            breakdown(feeSource = FEE_SOURCE_UNAVAILABLE, gross = 75.0, fee = null, net = null, pct = null)
        val item = bookingRequestItem(earnings = 75.0, breakdown = unavailable)
        assertEquals(RequestCardEarnings.PayoutPending, requestCardEarnings(item, Locale.US))
    }

    @Test
    fun aPaidRequestWithNoBreakdownIsPayoutPending() {
        val item = bookingRequestItem(earnings = 75.0, breakdown = null)
        assertEquals(RequestCardEarnings.PayoutPending, requestCardEarnings(item, Locale.US))
    }

    @Test
    fun theDecisionKeyCarriesTheRowFields() {
        val item = bookingRequestItem(id = "r9", start = start, expiresAt = now.plusSeconds(60), earnings = 75.0)
        val key = buildIncomingRequestRows(listOf(item), now).single().toDecisionKey()
        assertEquals(
            RequestDecisionKey(
                requestId = "r9",
                isExpired = false,
                expiresAtMillis = now.plusSeconds(60).toEpochMilli(),
                listEarningsMajor = 75.0,
                listCurrency = "USD",
            ),
            key,
        )
    }

    @Test
    fun theDecisionKeyTakesTheCurrencyFromTheBreakdownWhenTheRowHasNone() {
        val item = bookingRequestItem(currency = null, breakdown = breakdown(currency = "CAD"))
        val key = IncomingRequestRow(item, isExpired = true, hoursLeft = null).toDecisionKey()
        assertEquals("CAD", key.listCurrency)
        assertTrue(key.isExpired)
    }
}
