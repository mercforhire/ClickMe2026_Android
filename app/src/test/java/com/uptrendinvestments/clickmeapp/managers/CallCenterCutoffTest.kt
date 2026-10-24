@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.managers

import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Duration
import java.time.Instant

class CallCenterCutoffTest {
    @get:Rule
    val main = MainDispatcherRule()

    /** The harness request ends 30 minutes after [T0]. */
    private val scheduledEnd: Instant = T0.plus(Duration.ofMinutes(30))

    private suspend fun TestScope.pollOnce(millis: Long = CallCenter.CUTOFF_POLL_MS) {
        advanceTimeBy(millis)
        runCurrent()
    }

    @Test
    fun justBeforeCutoff_callStaysActive() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            with(h) { startActiveInAppCall() }
            h.clock.instant = scheduledEnd.plusSeconds(3599)
            pollOnce()
            assertNotNull(h.center.session.value)
            assertEquals(0, h.api.endCallCalls)
        }

    @Test
    fun atCutoff_forceEndsOnceWithTimeLimitDialog() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            with(h) { startActiveInAppCall() }
            h.clock.instant = scheduledEnd.plusSeconds(3600)
            pollOnce()
            assertEquals(1, h.api.endCallCalls)
            assertNull(h.center.session.value)
            assertEquals(listOf<CallDialog>(CallDialog.TimeLimit), h.center.dialogs.value)
            pollOnce(120_000)
            assertEquals(1, h.api.endCallCalls)
            assertEquals(listOf<CallDialog>(CallDialog.TimeLimit), h.center.dialogs.value)
        }

    @Test
    fun fiveMinutesPastEnd_isNotTheCutoff() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            with(h) { startActiveInAppCall() }
            h.clock.instant = scheduledEnd.plusSeconds(301)
            pollOnce(60_000)
            assertNotNull(h.center.session.value)
            assertEquals(0, h.api.endCallCalls)
            assertTrue(
                h.center.dialogs.value
                    .isEmpty(),
            )
        }

    @Test
    fun nullScheduledEnd_isNeverForceEnded() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            h.api.joinCallHandler = { h.inAppJoinResponse() }
            h.center.startCall(h.request(scheduledEnd = null))
            runCurrent()
            h.clock.instant = scheduledEnd.plus(Duration.ofDays(2))
            pollOnce(120_000)
            assertNotNull(h.center.session.value)
            assertEquals(0, h.api.endCallCalls)
        }

    @Test
    fun userEndsBeforeCutoff_noTimeLimitDialog() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            with(h) { startActiveInAppCall() }
            h.center.endCall()
            runCurrent()
            h.clock.instant = scheduledEnd.plusSeconds(7200)
            pollOnce(120_000)
            assertEquals(1, h.api.endCallCalls)
            assertTrue(
                h.center.dialogs.value
                    .isEmpty(),
            )
        }

    @Test
    fun skypeZoomSession_isAlsoForceEnded() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            h.api.joinCallHandler = { h.externalJoinResponse("https://zoom.us/j/1") }
            h.center.startCall(h.request(meetingType = MeetingType.SkypeZoom))
            runCurrent()
            h.center.onExternalLinkOpened()
            runCurrent()
            h.clock.instant = scheduledEnd.plusSeconds(3600)
            pollOnce()
            assertEquals(1, h.api.endCallCalls)
            assertEquals(0, h.engine.leaveCalls)
            assertNull(h.center.session.value)
            assertEquals(listOf<CallDialog>(CallDialog.TimeLimit), h.center.dialogs.value)
        }
}
