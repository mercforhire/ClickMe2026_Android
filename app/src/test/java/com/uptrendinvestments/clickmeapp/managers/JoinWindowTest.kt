package com.uptrendinvestments.clickmeapp.managers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class JoinWindowTest {
    private val now = Instant.parse("2026-10-24T12:00:00Z")
    private val farEnd = now.plusSeconds(10 * 3600)

    private fun within(
        status: String,
        startOffsetSeconds: Long,
        end: Instant = now.plusSeconds(startOffsetSeconds + 3600),
    ) = isWithinJoinWindow(status, now.plusSeconds(startOffsetSeconds), end, now)

    @Test
    fun constants_matchDecisions() {
        assertEquals(3600L, JOIN_WINDOW_SECONDS)
        assertEquals(30_000L, JOIN_CLOCK_TICK_MS)
    }

    @Test
    fun pendingApproval_isNeverJoinable() {
        assertFalse(within("pending_approval", 0))
        assertFalse(within("pending_approval", 3600))
    }

    @Test
    fun confirmed_boundariesAreInclusive() {
        assertTrue(within("confirmed", 3600))
        assertFalse(within("confirmed", 3601))
        assertTrue(within("confirmed", -3600))
        assertFalse(within("confirmed", -3601))
        assertTrue(within("confirmed", 0))
    }

    @Test
    fun inProgressAndPendingReschedule_followTheSameWindow() {
        listOf("in_progress", "pending_reschedule").forEach { status ->
            assertTrue(status, within(status, 3600))
            assertFalse(status, within(status, 3601))
            assertTrue(status, within(status, -3600))
            assertFalse(status, within(status, -3601))
        }
    }

    @Test
    fun completed_isJoinableWhileEndIsInTheFuture() {
        val start = now.minusSeconds(3 * 3600)
        assertTrue(isWithinJoinWindow("completed", start, now.plusSeconds(60), now))
        assertFalse(isWithinJoinWindow("completed", start, now.minusSeconds(1), now))
        assertFalse(isWithinJoinWindow("completed", start, now, now))
    }

    @Test
    fun completed_ignoresTheStartWindow() {
        assertTrue(isWithinJoinWindow("completed", now.plusSeconds(10 * 3600), farEnd, now))
    }

    @Test
    fun isStartingNow_isSymmetricAndInclusive() {
        assertTrue(isStartingNow(now.plusSeconds(3600), now))
        assertFalse(isStartingNow(now.plusSeconds(3601), now))
        assertTrue(isStartingNow(now.minusSeconds(3600), now))
        assertFalse(isStartingNow(now.minusSeconds(3601), now))
        assertTrue(isStartingNow(now, now))
    }
}
