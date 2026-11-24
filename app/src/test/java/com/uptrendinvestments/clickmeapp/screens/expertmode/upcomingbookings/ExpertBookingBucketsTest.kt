package com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings

import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingItem
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingStatusLabel
import com.uptrendinvestments.clickmeapp.testing.expertBookingItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ExpertBookingBucketsTest {
    private val now = Instant.parse("2026-10-22T16:00:00Z")

    private fun item(
        id: String,
        status: String,
        start: Instant = now.plusSeconds(3600),
        end: Instant = start.plusSeconds(3600),
    ): ExpertBookingItem = expertBookingItem(id = id, statusCode = status, start = start, end = end)

    @Test
    fun pendingApprovalRowsAreExcludedFromUpcoming() {
        val upcoming = listOf(item("p", "pending_approval"), item("c", "confirmed"))
        val lists = buildExpertBookingLists(upcoming, emptyList(), now)
        assertEquals(listOf("c"), lists.upcoming.map { it.bookingId })
    }

    @Test
    fun aSessionEndingExactlyNowIsNotUpcoming() {
        val ended = item("edge", "confirmed", start = now.minusSeconds(3600), end = now)
        assertTrue(buildExpertBookingLists(listOf(ended), emptyList(), now).upcoming.isEmpty())
    }

    @Test
    fun aSessionEndingOneSecondFromNowIsUpcoming() {
        val running = item("run", "in_progress", start = now.minusSeconds(3599), end = now.plusSeconds(1))
        val lists = buildExpertBookingLists(listOf(running), emptyList(), now)
        assertEquals(listOf("run"), lists.upcoming.map { it.bookingId })
    }

    @Test
    fun aCompletedPastRowWithAFutureEndMovesToUpcomingAndOutOfPast() {
        val rejoinable = item("r", "completed", start = now.minusSeconds(1200), end = now.plusSeconds(600))
        val lists = buildExpertBookingLists(emptyList(), listOf(rejoinable), now)
        assertEquals(listOf("r"), lists.upcoming.map { it.bookingId })
        assertTrue(lists.past.isEmpty())
    }

    @Test
    fun aCompletedPastRowEndingExactlyNowStaysInPast() {
        val ended = item("done", "completed", start = now.minusSeconds(3600), end = now)
        val lists = buildExpertBookingLists(emptyList(), listOf(ended), now)
        assertTrue(lists.upcoming.isEmpty())
        assertEquals(listOf("done"), lists.past.map { it.item.bookingId })
        assertFalse(ended.isRejoinableCompleted(now))
    }

    @Test
    fun aCompletedPastRowInTheReconnectWindowStaysInPastEvenWithTheServerFlag() {
        val ended =
            expertBookingItem(
                id = "reconnect",
                statusCode = "completed",
                start = now.minusSeconds(5400),
                end = now.minusSeconds(1800),
                canJoin = true,
            )
        val lists = buildExpertBookingLists(emptyList(), listOf(ended), now)
        assertTrue(lists.upcoming.isEmpty())
        assertEquals(listOf("reconnect"), lists.past.map { it.item.bookingId })
        assertFalse(ended.isRejoinableCompleted(now))
    }

    @Test
    fun upcomingIsSortedByStartAscending() {
        val later = item("later", "confirmed", start = now.plusSeconds(7200))
        val sooner = item("sooner", "pending_reschedule", start = now.plusSeconds(600))
        val rejoin = item("rejoin", "completed", start = now.minusSeconds(900), end = now.plusSeconds(900))
        val lists = buildExpertBookingLists(listOf(later, sooner), listOf(rejoin), now)
        assertEquals(listOf("rejoin", "sooner", "later"), lists.upcoming.map { it.bookingId })
    }

    @Test
    fun pastIsSortedByStartDescendingWithLabels() {
        val past =
            listOf(
                item("completed", "completed", start = now.minusSeconds(10_000)),
                item("cancelled", "cancelled", start = now.minusSeconds(20_000)),
                item("declined", "declined", start = now.minusSeconds(30_000)),
                item("missed", "missed", start = now.minusSeconds(40_000)),
                item("expired", "expired", start = now.minusSeconds(50_000)),
            )
        val rows = buildExpertBookingLists(emptyList(), past, now).past
        assertEquals(listOf("completed", "cancelled", "declined", "missed", "expired"), rows.map { it.item.bookingId })
        assertEquals(
            listOf(
                BookingStatusLabel.Completed,
                BookingStatusLabel.Cancelled,
                BookingStatusLabel.Cancelled,
                BookingStatusLabel.Missed,
                BookingStatusLabel.Missed,
            ),
            rows.map { it.status.label },
        )
    }

    @Test
    fun anUnknownPastStatusIsDropped() {
        val unknown = item("weird", "brand_new", start = now.minusSeconds(7200))
        assertTrue(buildExpertBookingLists(emptyList(), listOf(unknown), now).past.isEmpty())
    }

    @Test
    fun aBookingInBothBucketsAppearsOnce() {
        val rejoinable = item("dup", "completed", start = now.minusSeconds(900), end = now.plusSeconds(900))
        val live = item("dup", "in_progress", start = now.minusSeconds(900), end = now.plusSeconds(900))
        val lists = buildExpertBookingLists(listOf(live), listOf(rejoinable), now)
        assertEquals(listOf("dup"), lists.upcoming.map { it.bookingId })
    }
}
