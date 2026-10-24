package com.uptrendinvestments.clickmeapp.managers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class CallTimerTest {
    private val start = Instant.parse("2026-10-24T10:00:00Z")
    private val end = Instant.parse("2026-10-24T10:30:00Z")

    private fun at(time: String): Instant = Instant.parse("2026-10-24T${time}Z")

    @Test
    fun beforeStartIsNegativeElapsedAndBeforeStart() {
        val state = callTimerState(start, end, at("09:58:30"))
        assertEquals(-90L, state.elapsedSeconds)
        assertTrue(state.isBeforeStart)
    }

    @Test
    fun subSecondBeforeStartTruncatesTowardZero() {
        val state = callTimerState(start, end, Instant.parse("2026-10-24T09:59:59.500Z"))
        assertEquals(0L, state.elapsedSeconds)
        assertFalse(state.isBeforeStart)
    }

    @Test
    fun afterStartElapsedCountsUp() {
        assertEquals(125L, callTimerState(start, end, at("10:02:05")).elapsedSeconds)
    }

    @Test
    fun minutesGrowPastFiftyNineWithoutAnHoursField() {
        val state = callTimerState(start, null, at("11:15:12"))
        assertEquals(4512L, state.elapsedSeconds)
        assertEquals(MinutesSeconds(75, 12), splitMinutesSeconds(state.elapsedSeconds))
    }

    @Test
    fun finalStretchStartsAtFiveMinutesRemaining() {
        val state = callTimerState(start, end, at("10:25:00"))
        assertEquals(300L, state.remainingSeconds)
        assertTrue(state.isInFinalStretch)
        assertFalse(state.isPastScheduledEnd)
        assertTrue(state.isWarning)
        assertEquals(MinutesSeconds(5, 0), state.remainingChip)
    }

    @Test
    fun justOutsideTheFinalStretchHasNoChip() {
        val state = callTimerState(start, end, at("10:24:59"))
        assertEquals(301L, state.remainingSeconds)
        assertFalse(state.isInFinalStretch)
        assertFalse(state.isWarning)
        assertNull(state.remainingChip)
    }

    @Test
    fun lastSecondIsStillFinalStretch() {
        val state = callTimerState(start, end, at("10:29:59"))
        assertEquals(1L, state.remainingSeconds)
        assertTrue(state.isInFinalStretch)
        assertEquals(MinutesSeconds(0, 1), state.remainingChip)
    }

    @Test
    fun atScheduledEndIsPastEndNotFinalStretch() {
        val state = callTimerState(start, end, at("10:30:00"))
        assertEquals(0L, state.remainingSeconds)
        assertFalse(state.isInFinalStretch)
        assertTrue(state.isPastScheduledEnd)
        assertTrue(state.isWarning)
        assertNull(state.remainingChip)
    }

    @Test
    fun unknownEndHasNoRemainingAndNoFlags() {
        val state = callTimerState(start, null, at("10:28:00"))
        assertNull(state.remainingSeconds)
        assertFalse(state.isInFinalStretch)
        assertFalse(state.isPastScheduledEnd)
        assertFalse(state.isWarning)
        assertNull(state.remainingChip)
    }

    @Test
    fun overtimeCutoffIsScheduledEndPlusThreeThousandSixHundredSeconds() {
        assertEquals(3600L, OVERTIME_CUTOFF_SECONDS)
        assertEquals(at("11:30:00"), overtimeCutoff(end))
    }

    @Test
    fun overtimeCutoffIsNotTheIosTempFiveMinuteValue() {
        assertNotEquals(at("10:35:00"), overtimeCutoff(end))
    }

    @Test
    fun finalStretchConstantIsFiveMinutes() {
        assertEquals(300L, FINAL_STRETCH_SECONDS)
    }

    @Test
    fun splitMinutesSecondsSplitsAndUsesAbsoluteValue() {
        assertEquals(MinutesSeconds(2, 5), splitMinutesSeconds(125))
        assertEquals(MinutesSeconds(1, 30), splitMinutesSeconds(90))
        assertEquals(MinutesSeconds(1, 30), splitMinutesSeconds(-90))
        assertEquals(MinutesSeconds(0, 0), splitMinutesSeconds(0))
    }
}
