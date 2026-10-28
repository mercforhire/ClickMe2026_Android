package com.uptrendinvestments.clickmeapp.screens.notifications

import com.uptrendinvestments.clickmeapp.testing.notificationItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class NotificationsGroupingTest {
    private val zone = ZoneId.of("America/Toronto")
    private val now = Instant.parse("2026-10-28T19:00:00Z")

    private fun row(
        id: String,
        createdAt: String,
    ) = notificationItem(id = id, createdAt = Instant.parse(createdAt)).toRowModel(now, zone)

    @Test
    fun sectionsInFixedOrderWithServerOrderInside() {
        val rows =
            listOf(
                row("t1", "2026-10-28T18:00:00Z"),
                row("y1", "2026-10-28T02:00:00Z"),
                row("t2", "2026-10-28T14:00:00Z"),
                row("e1", "2026-10-20T14:00:00Z"),
                row("y2", "2026-10-27T20:00:00Z"),
            )
        val sections = groupNotifications(rows, now, zone)
        assertEquals(
            listOf(NotificationDay.Today, NotificationDay.Yesterday, NotificationDay.Earlier),
            sections.map { it.day },
        )
        assertEquals(listOf("t1", "t2"), sections[0].rows.map { it.id })
        assertEquals(listOf("y1", "y2"), sections[1].rows.map { it.id })
    }

    @Test
    fun emptySectionsOmitted() {
        val onlyEarlier = groupNotifications(listOf(row("e1", "2026-10-01T14:00:00Z")), now, zone)
        assertEquals(listOf(NotificationDay.Earlier), onlyEarlier.map { it.day })
        assertEquals(emptyList<NotificationSection>(), groupNotifications(emptyList(), now, zone))
    }

    private fun dayOf(
        createdAt: String,
        at: Instant = now,
    ): NotificationDay =
        groupNotifications(
            listOf(notificationItem(createdAt = Instant.parse(createdAt)).toRowModel(at, zone)),
            at,
            zone,
        ).single().day

    @Test
    fun midnightBoundaries() {
        assertEquals(NotificationDay.Today, dayOf("2026-10-28T04:00:00Z"))
        assertEquals(NotificationDay.Yesterday, dayOf("2026-10-28T03:59:59Z"))
        assertEquals(NotificationDay.Yesterday, dayOf("2026-10-27T04:00:00Z"))
        assertEquals(NotificationDay.Earlier, dayOf("2026-10-27T03:59:59Z"))
    }

    @Test
    fun dstDayIsStillYesterday() {
        val afterFallBack = Instant.parse("2026-11-02T15:00:00Z")
        assertEquals(NotificationDay.Yesterday, dayOf("2026-11-01T04:30:00Z", afterFallBack))
        assertEquals(NotificationDay.Yesterday, dayOf("2026-11-02T04:30:00Z", afterFallBack))
        assertEquals(NotificationDay.Today, dayOf("2026-11-02T05:00:00Z", afterFallBack))
    }

    @Test
    fun equalTimestampsKeepServerOrder() {
        val rows = listOf("c", "a", "b").map { row(it, "2026-10-28T14:00:00Z") }
        val sections = groupNotifications(rows, now, zone)
        assertEquals(listOf("c", "a", "b"), sections.single().rows.map { it.id })
    }

    private fun timeOf(createdAt: String) = notificationTime(Instant.parse(createdAt), now, zone)

    @Test
    fun minutesAndJustNowLabels() {
        assertEquals(NotificationTime.JustNow, timeOf("2026-10-28T18:59:30Z"))
        assertEquals(NotificationTime.MinutesAgo(1), timeOf("2026-10-28T18:59:00Z"))
        assertEquals(NotificationTime.MinutesAgo(5), timeOf("2026-10-28T18:55:00Z"))
        assertEquals(NotificationTime.MinutesAgo(59), timeOf("2026-10-28T18:00:01Z"))
    }

    @Test
    fun hoursYesterdayAndEarlierLabels() {
        assertEquals(NotificationTime.HoursAgo(1), timeOf("2026-10-28T18:00:00Z"))
        assertEquals(NotificationTime.HoursAgo(3), timeOf("2026-10-28T16:00:00Z"))
        assertEquals(NotificationTime.Yesterday(LocalTime.of(20, 15)), timeOf("2026-10-28T00:15:00Z"))
        assertEquals(
            NotificationTime.Earlier(LocalDate.of(2026, 10, 23), LocalTime.of(9, 5)),
            timeOf("2026-10-23T13:05:00Z"),
        )
    }

    @Test
    fun futureCreatedAtCountsAsJustNow() {
        assertEquals(NotificationTime.JustNow, timeOf("2026-10-28T19:02:00Z"))
        assertEquals(NotificationTime.JustNow, timeOf("2026-11-05T19:00:00Z"))
    }

    @Test
    fun rowModelCarriesTheTimeLabel() {
        assertEquals(NotificationTime.HoursAgo(3), row("a", "2026-10-28T16:00:00Z").time)
    }
}
