package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings

import com.uptrendinvestments.clickmeapp.model.responses.BookingExpertDto
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class BookingBucketsTest {
    private val now = Instant.parse("2026-10-22T16:00:00Z")

    private fun item(
        id: String,
        status: String,
        start: String,
        end: String,
    ) = ClientBookingItem(
        bookingId = id,
        expert = BookingExpertDto("e1", "Ada", null),
        topic = "Topic",
        startTime = Instant.parse(start),
        endTime = Instant.parse(end),
        meetingType = "video",
        status = status,
    )

    @Test
    fun completedBookingStillRunningMovesToUpcoming() {
        val running = item("run", "completed", "2026-10-22T15:30:00Z", "2026-10-22T16:30:00Z")
        val done = item("done", "completed", "2026-10-22T14:00:00Z", "2026-10-22T15:59:00Z")
        val result = buildBookingLists(emptyList(), listOf(running, done), now)
        assertEquals(listOf("run"), result.upcoming.map { it.bookingId })
        assertEquals(listOf("done"), result.past.map { it.item.bookingId })
        val doneRow = result.past.single()
        assertEquals(BookingStatusLabel.Completed, doneRow.status.label)
    }

    @Test
    fun completedBookingEndingExactlyNowStaysInPast() {
        val ended = item("edge", "completed", "2026-10-22T15:00:00Z", "2026-10-22T16:00:00Z")
        val result = buildBookingLists(emptyList(), listOf(ended), now)
        assertTrue(result.upcoming.isEmpty())
        assertEquals(listOf("edge"), result.past.map { it.item.bookingId })
    }

    @Test
    fun completedBookingInsideTheReconnectWindowStaysInPast() {
        val ended = item("reconnect", "completed", "2026-10-22T14:30:00Z", "2026-10-22T15:30:00Z")
        val result = buildBookingLists(emptyList(), listOf(ended), now)
        assertTrue(result.upcoming.isEmpty())
        assertEquals(listOf("reconnect"), result.past.map { it.item.bookingId })
    }

    @Test
    fun pastStatusesMapToLabelsAndUnknownOnesAreDropped() {
        val statuses = listOf("completed", "cancelled", "declined", "missed", "expired", "rescheduled", "weird")
        val past =
            statuses.mapIndexed { i, s ->
                item(s, s, "2026-10-1${i}T10:00:00Z", "2026-10-1${i}T11:00:00Z")
            }
        val rows = buildBookingLists(emptyList(), past, now).past
        val labels = rows.associate { it.item.bookingId to it.status.label }
        assertEquals(
            mapOf(
                "completed" to BookingStatusLabel.Completed,
                "cancelled" to BookingStatusLabel.Cancelled,
                "declined" to BookingStatusLabel.Cancelled,
                "missed" to BookingStatusLabel.Missed,
                "expired" to BookingStatusLabel.Missed,
            ),
            labels,
        )
    }

    @Test
    fun upcomingKeepsEveryRowIncludingUnknownStatuses() {
        val statuses = listOf("pending_approval", "confirmed", "pending_reschedule", "in_progress", "brand_new")
        val upcoming =
            statuses.mapIndexed { i, s ->
                item(s, s, "2026-10-2${3 + i}T10:00:00Z", "2026-10-2${3 + i}T11:00:00Z")
            }
        val result = buildBookingLists(upcoming, emptyList(), now)
        assertEquals(upcoming.map { it.bookingId }, result.upcoming.map { it.bookingId })
        assertEquals("brand_new", result.upcoming.last().status)
    }

    @Test
    fun upcomingIsAscendingAndPastIsDescendingByStart() {
        val upcoming =
            listOf(
                item("u2", "confirmed", "2026-10-25T10:00:00Z", "2026-10-25T11:00:00Z"),
                item("u1", "confirmed", "2026-10-23T10:00:00Z", "2026-10-23T11:00:00Z"),
            )
        val rejoin = item("r", "completed", "2026-10-22T15:00:00Z", "2026-10-22T17:00:00Z")
        val past =
            listOf(
                item("p1", "cancelled", "2026-10-01T10:00:00Z", "2026-10-01T11:00:00Z"),
                item("p3", "completed", "2026-10-10T10:00:00Z", "2026-10-10T11:00:00Z"),
                item("p2", "missed", "2026-10-05T10:00:00Z", "2026-10-05T11:00:00Z"),
                rejoin,
            )
        val result = buildBookingLists(upcoming, past, now)
        assertEquals(listOf("r", "u1", "u2"), result.upcoming.map { it.bookingId })
        assertEquals(listOf("p3", "p2", "p1"), result.past.map { it.item.bookingId })
    }

    @Test
    fun duplicateBookingIdsAppearOncePerList() {
        val dupUp = item("d", "confirmed", "2026-10-23T10:00:00Z", "2026-10-23T11:00:00Z")
        val rejoinDup = item("d", "completed", "2026-10-22T15:00:00Z", "2026-10-22T17:00:00Z")
        val pastDup = item("x", "cancelled", "2026-10-01T10:00:00Z", "2026-10-01T11:00:00Z")
        val result = buildBookingLists(listOf(dupUp, dupUp), listOf(rejoinDup, pastDup, pastDup), now)
        assertEquals(listOf("d"), result.upcoming.map { it.bookingId })
        assertEquals(listOf("x"), result.past.map { it.item.bookingId })
    }

    @Test
    fun emptyInputsGiveEmpty() {
        assertEquals(BookingLists.EMPTY, buildBookingLists(emptyList(), emptyList(), now))
    }
}
