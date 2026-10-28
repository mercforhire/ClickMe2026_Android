package com.uptrendinvestments.clickmeapp.screens.notifications

import com.uptrendinvestments.clickmeapp.testing.notificationItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
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
}
