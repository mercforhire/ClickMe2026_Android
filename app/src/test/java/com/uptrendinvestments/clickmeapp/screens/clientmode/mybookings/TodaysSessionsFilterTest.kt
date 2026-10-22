package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings

import com.uptrendinvestments.clickmeapp.model.responses.BookingExpertDto
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingItem
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingsPayload
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

class TodaysSessionsFilterTest {
    private val toronto = ZoneId.of("America/Toronto")
    private val clock = MutableClock(Instant.parse("2026-10-21T16:00:00Z"), toronto)

    private fun booking(
        id: String,
        start: String,
        status: String = "confirmed",
        end: String? = null,
    ): ClientBookingItem {
        val s = Instant.parse(start)
        return ClientBookingItem(
            bookingId = id,
            expert = BookingExpertDto(id = "e-$id", fullName = "Expert $id"),
            topic = "Topic",
            startTime = s,
            endTime = end?.let { Instant.parse(it) } ?: s.plusSeconds(3600),
            meetingType = "video",
            status = status,
        )
    }

    @Test
    fun lateEveningLocalIsTodayAndJustAfterMidnightLocalIsNot() {
        val items =
            listOf(
                booking("late", "2026-10-22T03:30:00Z"),
                booking("tomorrow", "2026-10-22T04:30:00Z"),
            )
        assertEquals(listOf("late"), items.todaysSessions(clock, toronto).map { it.bookingId })
    }

    @Test
    fun keepsJoinableStatusesAndDropsTheRest() {
        val kept = listOf("confirmed", "in_progress", "pending_reschedule")
        val dropped =
            listOf("pending_approval", "completed", "cancelled", "declined", "missed", "expired")
        val items =
            (kept + dropped).mapIndexed { i, status ->
                booking(status, "2026-10-21T${14 + i / 2}:00:00Z", status)
            }
        val result = items.todaysSessions(clock, toronto).map { it.status }
        assertEquals(kept.toSet(), result.toSet())
        assertEquals(kept.size, result.size)
    }

    @Test
    fun sortsAscendingByStartTime() {
        val items =
            listOf(
                booking("c", "2026-10-21T20:00:00Z"),
                booking("a", "2026-10-21T13:00:00Z"),
                booking("b", "2026-10-21T15:00:00Z"),
            )
        assertEquals(listOf("a", "b", "c"), items.todaysSessions(clock, toronto).map { it.bookingId })
    }

    @Test
    fun keepsAConfirmedBookingThatAlreadyEndedToday() {
        val items = listOf(booking("past", "2026-10-21T13:00:00Z", end = "2026-10-21T14:00:00Z"))
        assertEquals(listOf("past"), items.todaysSessions(clock, toronto).map { it.bookingId })
    }

    private fun normalized(text: String) = text.replace(' ', ' ').replace(' ', ' ')

    @Test
    fun formatsTimeRangeForUsLocale() {
        val text =
            formatSessionTimeRange(
                Instant.parse("2026-10-21T15:00:00Z"),
                Instant.parse("2026-10-21T16:00:00Z"),
                ZoneOffset.UTC,
                Locale.US,
            )
        assertEquals("3:00 PM – 4:00 PM", normalized(text))
    }

    @Test
    fun formatsTimeRangeFor24HourLocale() {
        val text =
            formatSessionTimeRange(
                Instant.parse("2026-10-21T15:00:00Z"),
                Instant.parse("2026-10-21T16:00:00Z"),
                ZoneOffset.UTC,
                Locale.GERMANY,
            )
        assertEquals("15:00 – 16:00", normalized(text))
    }

    @Test
    fun repositoryListSendsUpcomingPageOneLimit20() =
        runTest {
            val api = FakeClickMeApi()
            var seenType: String? = null
            val payload = ClientBookingsPayload(bookings = listOf(booking("x", "2026-10-21T15:00:00Z")))
            api.clientBookingsHandler = {
                seenType = it
                payload
            }
            val result = ClientBookingsRepository(api).list(ClientBookingsRepository.TYPE_UPCOMING)
            assertEquals("upcoming", seenType)
            assertEquals(payload, result)
            assertEquals(1, api.clientBookingsCalls)
        }

    @Test
    fun repositoryMapsIoExceptionToNetwork() =
        runTest {
            val api = FakeClickMeApi()
            api.clientBookingsHandler = { throw IOException("offline") }
            try {
                ClientBookingsRepository(api).list(ClientBookingsRepository.TYPE_UPCOMING)
                fail("expected ApiException.Network")
            } catch (e: ApiException) {
                assertTrue(e is ApiException.Network)
            }
        }
}
