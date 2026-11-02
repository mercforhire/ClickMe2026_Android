package com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile

import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingItem
import com.uptrendinvestments.clickmeapp.testing.breakdown
import com.uptrendinvestments.clickmeapp.testing.expertBookingItem
import com.uptrendinvestments.clickmeapp.util.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class ClientHistoryTest {
    private val locale = Locale.US

    private fun row(item: ExpertBookingItem) = ClientHistoryRow(item, clientHistoryBucket(item.statusCode))

    private fun amount(
        statusCode: String,
        item: ExpertBookingItem = expertBookingItem(statusCode = statusCode),
    ) = row(item.copy(status = item.status.copy(code = statusCode))).amountText(locale)

    @Test
    fun everyStatusMapsToItsBucket() {
        val expected =
            mapOf(
                "pending_approval" to ClientHistoryBucket.Upcoming,
                "confirmed" to ClientHistoryBucket.Upcoming,
                "pending_reschedule" to ClientHistoryBucket.Upcoming,
                "in_progress" to ClientHistoryBucket.Upcoming,
                "completed" to ClientHistoryBucket.Completed,
                "cancelled" to ClientHistoryBucket.Cancelled,
                "declined" to ClientHistoryBucket.Cancelled,
                "missed" to ClientHistoryBucket.Cancelled,
                "expired" to ClientHistoryBucket.Cancelled,
            )
        expected.forEach { (code, bucket) -> assertEquals(code, bucket, clientHistoryBucket(code)) }
    }

    @Test
    fun anUnknownStatusIsUpcomingWithNoAmount() {
        assertEquals(ClientHistoryBucket.Upcoming, clientHistoryBucket("rescheduled_new"))
        assertNull(amount("rescheduled_new"))
    }

    @Test
    fun upcomingAndCompletedShowTheNetAmount() {
        val expected = Money.formatMajor(63.75, "USD", locale)
        listOf("pending_approval", "confirmed", "pending_reschedule", "in_progress", "completed").forEach {
            assertEquals(it, expected, amount(it))
        }
    }

    @Test
    fun cancelledBucketNeverShowsAnAmount() {
        listOf("cancelled", "declined", "missed", "expired").forEach {
            assertNull(it, amount(it))
        }
    }

    @Test
    fun noNetAmountShowsNothing() {
        assertNull(amount("completed", expertBookingItem(breakdown = null)))
        assertNull(amount("completed", expertBookingItem(breakdown = breakdown(net = null))))
        assertNull(amount("completed", expertBookingItem(breakdown = breakdown(currency = null), fee = null)))
    }

    @Test
    fun aMissingBreakdownCurrencyFallsBackToTheFeeCurrency() {
        val item = expertBookingItem(breakdown = breakdown(currency = null), currency = "CAD")
        assertEquals(Money.formatMajor(63.75, "CAD", locale), amount("completed", item))
    }

    @Test
    fun grossNeverStandsInForNet() {
        assertNull(amount("completed", expertBookingItem(breakdown = breakdown(gross = 75.0, net = null))))
    }

    @Test
    fun zeroNetIsTruthful() {
        val item = expertBookingItem(breakdown = breakdown(gross = 0.0, fee = 0.0, net = 0.0))
        assertEquals(Money.formatMajor(0.0, "USD", locale), amount("completed", item))
    }

    @Test
    fun rowsFollowTheBucketOfTheirStatus() {
        val history =
            buildClientHistory(
                "c1",
                upcoming = listOf(expertBookingItem(id = "a", statusCode = "confirmed")),
                past = listOf(expertBookingItem(id = "b", statusCode = "declined")),
            )
        assertEquals(
            setOf("a" to ClientHistoryBucket.Upcoming, "b" to ClientHistoryBucket.Cancelled),
            history.map { it.item.bookingId to it.bucket }.toSet(),
        )
    }

    @Test
    fun aDuplicateBookingIdAppearsOnce() {
        val dup = expertBookingItem(id = "a")
        assertEquals(1, buildClientHistory("c1", listOf(dup), listOf(dup)).size)
    }
}
