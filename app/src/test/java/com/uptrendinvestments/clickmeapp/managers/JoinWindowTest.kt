package com.uptrendinvestments.clickmeapp.managers

import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.JOINABLE_BOOKING_STATUSES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class JoinWindowTest {
    private val now = Instant.parse("2026-10-24T12:00:00Z")

    private val enabledStatuses = listOf("confirmed", "in_progress", "completed")

    /** Test helper only: the production function has no default for [serverCanJoin]. */
    private fun within(
        status: String,
        startOffsetSeconds: Long,
        end: Instant = now.plusSeconds(startOffsetSeconds + 3600),
        serverCanJoin: Boolean? = null,
    ) = isWithinJoinWindow(status, now.plusSeconds(startOffsetSeconds), end, now, serverCanJoin)

    /** Join state for a session starting at [start] and ending at [end], evaluated at [at]. */
    private fun joinAt(
        status: String,
        start: Instant,
        end: Instant,
        at: Instant,
        serverCanJoin: Boolean? = null,
    ) = isWithinJoinWindow(status, start, end, at, serverCanJoin)

    @Test
    fun constants_matchTheBackend() {
        assertEquals(3600L, JOIN_LEAD_SECONDS)
        assertEquals(3600L, JOIN_MIN_FIRST_SPAN_SECONDS)
        assertEquals(3600L, JOIN_RECONNECT_GRACE_SECONDS)
        assertEquals(3600L, JOIN_WINDOW_SECONDS)
        assertEquals(30_000L, JOIN_CLOCK_TICK_MS)
    }

    @Test
    fun enabledStatuses_equalTheBackendJoinableSet() {
        assertEquals(setOf("confirmed", "in_progress", "completed"), JOIN_ENABLED_BOOKING_STATUSES)
        assertEquals(setOf("confirmed", "in_progress", "pending_reschedule"), JOINABLE_BOOKING_STATUSES)
    }

    @Test
    fun pendingReschedule_isNeverEnabled() {
        listOf(true, false, null).forEach { flag ->
            assertFalse("start - 600, flag $flag", within("pending_reschedule", 600, serverCanJoin = flag))
            assertFalse("at start, flag $flag", within("pending_reschedule", 0, serverCanJoin = flag))
            assertFalse(
                "at end, flag $flag",
                within("pending_reschedule", -3600, end = now, serverCanJoin = flag),
            )
        }
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
    fun opensExactlyOneHourBeforeStart() {
        enabledStatuses.forEach { status ->
            assertTrue(status, within(status, 3600))
            assertFalse(status, within(status, 3601))
        }
    }

    @Test
    fun serverCanJoin_neverOpensBeforeTheLead() {
        enabledStatuses.forEach { status ->
            assertFalse(status, within(status, 3601, serverCanJoin = true))
            assertFalse(status, within("completed", 10 * 3600, serverCanJoin = true))
        }
    }

    @Test
    fun firstJoin_closesAtTheLaterOfEndAndStartPlusOneHour() {
        val start = now
        enabledStatuses.forEach { status ->
            // Short session (30 min): the one-hour minimum span wins.
            val shortEnd = start.plusSeconds(1800)
            assertTrue(status, joinAt(status, start, shortEnd, start.plusSeconds(3600)))
            assertFalse(status, joinAt(status, start, shortEnd, start.plusSeconds(3601)))
            // Long session (2 h): the scheduled end wins.
            val longEnd = start.plusSeconds(7200)
            assertTrue(status, joinAt(status, start, longEnd, longEnd))
            assertFalse(status, joinAt(status, start, longEnd, longEnd.plusSeconds(1)))
        }
    }

    @Test
    fun firstJoinTier_ignoresTheServerFlag() {
        enabledStatuses.forEach { status ->
            assertTrue(status, within(status, 0, serverCanJoin = false))
            assertTrue(status, within(status, 0, serverCanJoin = null))
            assertTrue(status, within(status, 0, serverCanJoin = true))
        }
    }

    @Test
    fun reconnect_withServerCanJoinTrue_runsThroughEndPlusOneHour() {
        val start = now
        val end = start.plusSeconds(3600)
        enabledStatuses.forEach { status ->
            assertTrue(status, joinAt(status, start, end, end.plusSeconds(1800), serverCanJoin = true))
            assertTrue(status, joinAt(status, start, end, end.plusSeconds(3600), serverCanJoin = true))
            assertFalse(status, joinAt(status, start, end, end.plusSeconds(3601), serverCanJoin = true))
        }
    }

    @Test
    fun reconnect_isClosedWhenServerCanJoinIsFalseOrNull() {
        val start = now
        val end = start.plusSeconds(3600)
        enabledStatuses.forEach { status ->
            assertFalse(status, joinAt(status, start, end, end.plusSeconds(1800), serverCanJoin = false))
            assertFalse(status, joinAt(status, start, end, end.plusSeconds(1800), serverCanJoin = null))
            assertFalse(status, joinAt(status, start, end, end.plusSeconds(1), serverCanJoin = false))
            assertFalse(status, joinAt(status, start, end, end.plusSeconds(1), serverCanJoin = null))
        }
    }

    @Test
    fun completed_followsTheSameTiers() {
        // No longer enabled 10 h before the start.
        assertFalse(isWithinJoinWindow("completed", now.plusSeconds(10 * 3600), now.plusSeconds(11 * 3600), now, null))
        val start = now.minusSeconds(3 * 3600)
        val end = now.minusSeconds(1800)
        assertTrue(isWithinJoinWindow("completed", start, end, now, serverCanJoin = true))
        assertFalse(isWithinJoinWindow("completed", start, end, now, serverCanJoin = null))
        assertFalse(isWithinJoinWindow("completed", start, end, now, serverCanJoin = false))
    }

    @Test
    fun isStartingNow_isSymmetricAndInclusive() {
        assertTrue(isStartingNow(now.plusSeconds(3600), now))
        assertFalse(isStartingNow(now.plusSeconds(3601), now))
        assertTrue(isStartingNow(now.minusSeconds(3600), now))
        assertFalse(isStartingNow(now.minusSeconds(3601), now))
        assertTrue(isStartingNow(now, now))
    }

    // ---- WR-09 scenarios, now under the shared first-join tier ----

    @Test
    fun longSession_staysJoinableUntilItsEnd() {
        val start = now.minusSeconds(2 * 3600)
        enabledStatuses.forEach { status ->
            assertTrue(status, isWithinJoinWindow(status, start, now.plusSeconds(60), now, null))
            assertTrue(status, isWithinJoinWindow(status, start, now, now, null))
            assertFalse(status, isWithinJoinWindow(status, start, now.minusSeconds(1), now, null))
        }
        assertFalse(isWithinJoinWindow("pending_reschedule", start, now.plusSeconds(60), now, null))
    }

    @Test
    fun shortSession_stillGetsTheFullHourAfterStart() {
        val start = now.minusSeconds(3600)
        assertTrue(isWithinJoinWindow("in_progress", start, start.plusSeconds(1800), now, null))
        assertFalse(isWithinJoinWindow("in_progress", start.minusSeconds(1), start.plusSeconds(1800), now, null))
    }

    @Test
    fun longSession_opensNoEarlierThanAnHourBeforeStart() {
        val longEnd = now.plusSeconds(6 * 3600)
        enabledStatuses.forEach { status ->
            assertTrue(status, isWithinJoinWindow(status, now.plusSeconds(3600), longEnd, now, null))
            assertFalse(status, isWithinJoinWindow(status, now.plusSeconds(3601), longEnd, now, null))
        }
    }

    @Test
    fun pendingApproval_isNeverJoinableEvenForALongSession() {
        assertFalse(
            isWithinJoinWindow("pending_approval", now.minusSeconds(2 * 3600), now.plusSeconds(3600), now, true),
        )
    }

    // ---- Phase 16.5 D-03: the rule fails closed for terminal and unknown statuses ----

    private val neverJoinable = listOf("cancelled", "declined", "missed", "expired", "something_new", "")

    @Test
    fun terminalAndUnknownStatusesAreNeverEnabled() {
        (neverJoinable + "pending_approval").forEach { status ->
            assertFalse(status, within(status, 0, serverCanJoin = true))
            assertFalse(status, within(status, 600, serverCanJoin = true))
            assertFalse(status, within(status, 600))
        }
    }

    private val cardStart = now.plusSeconds(600)
    private val cardEnd = cardStart.plusSeconds(3600)

    private fun card(
        status: String,
        at: Instant,
        serverCanJoin: Boolean?,
        start: Instant = cardStart,
        end: Instant = cardEnd,
    ) = showsJoinCard(
        status = status,
        start = start,
        end = end,
        now = at,
        serverCanJoin = serverCanJoin,
    )

    @Test
    fun showsJoinCard_liveStatusesShowAtEveryInstant() {
        listOf("confirmed", "in_progress", "pending_reschedule").forEach { status ->
            listOf(true, false, null).forEach { flag ->
                listOf(
                    cardStart.minusSeconds(2 * 86_400),
                    cardStart,
                    cardEnd,
                    cardEnd.plusSeconds(86_400),
                ).forEach { at -> assertTrue("$status $at $flag", card(status, at, flag)) }
            }
        }
    }

    @Test
    fun showsJoinCard_completedShowsThroughTheFirstJoinEdgeWhateverTheFlag() {
        listOf(true, false, null).forEach { flag ->
            assertTrue("start - 3600, $flag", card("completed", cardStart.minusSeconds(3600), flag))
            assertTrue("end - 1, $flag", card("completed", cardEnd.minusSeconds(1), flag))
            assertTrue("end, $flag", card("completed", cardEnd, flag))
            assertFalse("start - 3601, $flag", card("completed", cardStart.minusSeconds(3601), flag))
        }
    }

    @Test
    fun showsJoinCard_completedPastTheEndShowsOnlyWithTheServerFlag() {
        listOf(1L, 3600L).forEach { offset ->
            val at = cardEnd.plusSeconds(offset)
            assertTrue("end + $offset, true", card("completed", at, true))
            assertFalse("end + $offset, false", card("completed", at, false))
            assertFalse("end + $offset, null", card("completed", at, null))
        }
        val closed = cardEnd.plusSeconds(3601)
        listOf(true, false, null).forEach { flag ->
            assertFalse("end + 3601, $flag", card("completed", closed, flag))
        }
    }

    @Test
    fun showsJoinCard_shortCompletedSessionShowsUntilStartPlusOneHour() {
        val end = cardStart.plusSeconds(1800)

        fun short(
            at: Instant,
            flag: Boolean?,
        ) = card("completed", at, flag, end = end)

        assertTrue(short(end.plusSeconds(1), null))
        assertTrue(short(cardStart.plusSeconds(3600), null))
        assertFalse(short(cardStart.plusSeconds(3601), null))
        assertFalse(short(cardStart.plusSeconds(3601), false))
        assertTrue(short(cardStart.plusSeconds(3601), true))
        assertTrue(short(end.plusSeconds(3600), true))
        assertFalse(short(end.plusSeconds(3601), true))
    }

    @Test
    fun showsJoinCard_completedVisibilityEqualsTheJoinWindow() {
        listOf(1800L, 3600L, 5400L).forEach { length ->
            val end = cardStart.plusSeconds(length)
            listOf(true, false, null).forEach { flag ->
                var offset = -3601L
                while (offset <= length + 3601L) {
                    val at = cardStart.plusSeconds(offset)
                    assertEquals(
                        "length $length offset $offset flag $flag",
                        isWithinJoinWindow("completed", cardStart, end, at, flag),
                        card("completed", at, flag, end = end),
                    )
                    offset++
                }
            }
        }
    }

    @Test
    fun showsJoinCard_terminalUnknownAndPendingApprovalNeverShow() {
        (neverJoinable + "pending_approval").forEach { status ->
            listOf(cardStart, cardEnd, cardEnd.plusSeconds(1800)).forEach { at ->
                assertFalse("$status $at", card(status, at, true))
            }
        }
    }
}
