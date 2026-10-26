package com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard

import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingItem
import com.uptrendinvestments.clickmeapp.model.responses.RESCHEDULE_INITIATED_BY_CLIENT
import com.uptrendinvestments.clickmeapp.model.responses.RescheduleDto
import com.uptrendinvestments.clickmeapp.testing.expertBookingItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class DashboardLogicTest {
    private val zone = ZoneId.of("America/Toronto")

    // 10:00 local on 2026-11-02 (EST). Local midnight today is 05:00Z; local midnight tomorrow is 05:00Z next day.
    private val clock = Clock.fixed(Instant.parse("2026-11-02T15:00:00Z"), zone)
    private val midnightToday = Instant.parse("2026-11-02T05:00:00Z")
    private val midnightTomorrow = Instant.parse("2026-11-03T05:00:00Z")

    private fun booking(
        id: String,
        start: Instant,
        status: String = "confirmed",
        reschedule: RescheduleDto? = null,
    ): ExpertBookingItem = expertBookingItem(id = id, statusCode = status, start = start, reschedule = reschedule)

    private fun ids(items: List<ExpertBookingItem>) = items.map { it.bookingId }

    @Test
    fun bookingStartingAtLocalMidnightTodayIsIncluded() {
        val result = listOf(booking("b1", midnightToday)).expertTodaysSessions(clock, zone)

        assertEquals(listOf("b1"), ids(result))
    }

    @Test
    fun bookingStartingAtLocalMidnightTomorrowIsExcluded() {
        val result = listOf(booking("b1", midnightTomorrow)).expertTodaysSessions(clock, zone)

        assertEquals(emptyList<String>(), ids(result))
    }

    @Test
    fun bookingStartedYesterdayAt2359LocalIsExcluded() {
        val result = listOf(booking("b1", midnightToday.minusSeconds(60))).expertTodaysSessions(clock, zone)

        assertEquals(emptyList<String>(), ids(result))
    }

    @Test
    fun pendingApprovalAndCancelledAreExcluded() {
        val start = midnightToday.plusSeconds(36_000)
        val result =
            listOf(
                booking("b1", start, "pending_approval"),
                booking("b2", start, "cancelled"),
            ).expertTodaysSessions(clock, zone)

        assertEquals(emptyList<String>(), ids(result))
    }

    @Test
    fun inProgressAndPendingRescheduleAreIncluded() {
        val start = midnightToday.plusSeconds(36_000)
        val result =
            listOf(
                booking("b1", start, "in_progress"),
                booking("b2", start.plusSeconds(60), "pending_reschedule"),
            ).expertTodaysSessions(clock, zone)

        assertEquals(listOf("b1", "b2"), ids(result))
    }

    @Test
    fun resultsAreSortedByStart() {
        val start = midnightToday.plusSeconds(36_000)
        val result =
            listOf(
                booking("late", start.plusSeconds(7200)),
                booking("early", start),
                booking("mid", start.plusSeconds(3600)),
            ).expertTodaysSessions(clock, zone)

        assertEquals(listOf("early", "mid", "late"), ids(result))
    }

    @Test
    fun aSessionThatAlreadyEndedEarlierTodayIsKept() {
        val result = listOf(booking("b1", midnightToday.plusSeconds(3600))).expertTodaysSessions(clock, zone)

        assertEquals(listOf("b1"), ids(result))
    }

    @Test
    fun rescheduleCountCountsOnlyClientProposalsOfPendingReschedules() {
        val start = midnightToday.plusSeconds(36_000)
        val byClient = RescheduleDto(initiatedBy = RESCHEDULE_INITIATED_BY_CLIENT)
        val byExpert = RescheduleDto(initiatedBy = "expert")
        val count =
            listOf(
                booking("b1", start, "pending_reschedule", byClient),
                booking("b2", start, "pending_reschedule", byClient),
                booking("b3", start, "pending_reschedule", byExpert),
                booking("b4", start, "pending_reschedule", null),
                booking("b5", start, "confirmed", byClient),
            ).clientRescheduleCount()

        assertEquals(2, count)
    }

    @Test
    fun firstNameIsTheTrimmedFirstWord() {
        assertEquals("Ada", dashboardFirstName("  Ada Marie ", null))
    }

    @Test
    fun firstNameFallsBackToTheProfileName() {
        assertEquals("Grace", dashboardFirstName(null, "Grace"))
        assertEquals("Grace", dashboardFirstName("   ", "Grace Hopper"))
    }

    @Test
    fun blankNamesGiveNull() {
        assertNull(dashboardFirstName("  ", ""))
        assertNull(dashboardFirstName(null, null))
    }
}
