package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class ThreadRowsTest {
    private fun message(
        id: String,
        at: String,
        mine: Boolean = false,
        sender: String = if (mine) "me" else "peer",
    ) = ThreadItem.Message(
        serverId = id,
        clientMsgId = null,
        senderId = sender,
        text = "text $id",
        createdAt = Instant.parse(at),
        mine = mine,
        delivery = Delivery.Sent,
    )

    private fun event(
        id: String,
        at: String,
    ) = ThreadItem.SystemEvent(
        serverId = id,
        kind = SystemEventKind.BookingConfirmed,
        bookingId = "b1",
        topic = "Algorithms",
        startTime = null,
        content = null,
        createdAt = Instant.parse(at),
    )

    private fun entry(row: ThreadRow) = row as ThreadRow.Entry

    @Test
    fun emptyItemsGiveNoRows() {
        assertTrue(buildThreadRows(emptyList(), ZoneOffset.UTC).isEmpty())
    }

    @Test
    fun headersPrecedeEachLocalDay() {
        val rows =
            buildThreadRows(
                listOf(
                    message("1", "2026-10-26T09:00:00Z"),
                    message("2", "2026-10-26T18:00:00Z"),
                    message("3", "2026-10-27T09:00:00Z"),
                ),
                ZoneOffset.UTC,
            )

        assertEquals(5, rows.size)
        assertEquals(ThreadRow.DateHeader(LocalDate.parse("2026-10-26")), rows[0])
        assertTrue(rows[1] is ThreadRow.Entry)
        assertTrue(rows[2] is ThreadRow.Entry)
        assertEquals(ThreadRow.DateHeader(LocalDate.parse("2026-10-27")), rows[3])
        assertTrue(rows[4] is ThreadRow.Entry)
    }

    @Test
    fun dayChangeFollowsTheLocalZone() {
        val zone: ZoneId = ZoneId.of("America/Toronto")
        val rows =
            buildThreadRows(
                listOf(
                    // 22:00 and 23:30 local on Oct 26, 00:30 local on Oct 27 (UTC-4).
                    message("1", "2026-10-27T02:00:00Z"),
                    message("2", "2026-10-27T03:30:00Z"),
                    message("3", "2026-10-27T04:30:00Z"),
                ),
                zone,
            )

        val headers = rows.filterIsInstance<ThreadRow.DateHeader>()
        assertEquals(listOf(LocalDate.parse("2026-10-26"), LocalDate.parse("2026-10-27")), headers.map { it.date })
        assertTrue(rows[3] is ThreadRow.DateHeader)
    }

    @Test
    fun sameSenderInTheSameMinuteGroups() {
        val rows =
            buildThreadRows(
                listOf(
                    message("1", "2026-10-27T09:00:05Z"),
                    message("2", "2026-10-27T09:00:40Z"),
                    message("3", "2026-10-27T09:00:50Z"),
                ),
                ZoneOffset.UTC,
            ).filterIsInstance<ThreadRow.Entry>()

        assertEquals(listOf(false, true, true), rows.map { it.tightAbove })
        assertEquals(listOf(false, false, true), rows.map { it.showAvatar })
    }

    @Test
    fun senderChangeOrNewMinuteIsNotTight() {
        val rows =
            buildThreadRows(
                listOf(
                    message("1", "2026-10-27T09:00:05Z"),
                    message("2", "2026-10-27T09:00:20Z", mine = true),
                    message("3", "2026-10-27T09:01:05Z", mine = true),
                    message("4", "2026-10-27T09:01:10Z"),
                ),
                ZoneOffset.UTC,
            ).filterIsInstance<ThreadRow.Entry>()

        assertEquals(listOf(false, false, false, false), rows.map { it.tightAbove })
        // Own messages never show an avatar; both incoming messages end their run.
        assertEquals(listOf(true, false, false, true), rows.map { it.showAvatar })
    }

    @Test
    fun systemEventBreaksARun() {
        val rows =
            buildThreadRows(
                listOf(
                    message("1", "2026-10-27T09:00:05Z"),
                    event("e1", "2026-10-27T09:00:10Z"),
                    message("2", "2026-10-27T09:00:20Z"),
                ),
                ZoneOffset.UTC,
            ).filterIsInstance<ThreadRow.Entry>()

        assertFalse(rows[1].tightAbove)
        assertFalse(rows[2].tightAbove)
        assertTrue(rows[0].showAvatar)
        assertFalse(rows[1].showAvatar)
        assertTrue(rows[2].showAvatar)
        assertEquals("s:e1", entry(rows[1]).key)
    }
}
