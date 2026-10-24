package com.uptrendinvestments.clickmeapp.managers

import com.uptrendinvestments.clickmeapp.testing.MutableClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class JoinClockTest {
    private val start = Instant.parse("2026-10-24T12:00:00Z")

    @Test
    fun now_startsAtTheClockInstant() =
        runTest {
            val clock = MutableClock(start)
            val joinClock = JoinClock(clock, backgroundScope)

            assertEquals(start, joinClock.now.value)
        }

    @Test
    fun now_ticksEvery30SecondsWhileCollected() =
        runTest {
            val clock = MutableClock(start)
            val joinClock = JoinClock(clock, backgroundScope)
            joinClock.now.onEach { }.launchIn(backgroundScope)
            runCurrent()
            assertEquals(start, joinClock.now.value)

            clock.advanceBy(Duration.ofSeconds(30))
            advanceTimeBy(JOIN_CLOCK_TICK_MS + 1)
            runCurrent()

            assertEquals(start.plusSeconds(30), joinClock.now.value)
        }

    @Test
    fun now_doesNotTickWithoutACollector() =
        runTest {
            val clock = MutableClock(start)
            val joinClock = JoinClock(clock, backgroundScope)

            clock.advanceBy(Duration.ofSeconds(60))
            advanceTimeBy(JOIN_CLOCK_TICK_MS * 4)
            runCurrent()

            assertEquals(start, joinClock.now.value)
        }
}
