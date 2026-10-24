@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.managers

import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.model.requests.EndCallRequest
import com.uptrendinvestments.clickmeapp.model.responses.ConfirmConnectionData
import com.uptrendinvestments.clickmeapp.model.responses.EndCallData
import com.uptrendinvestments.clickmeapp.model.responses.JoinCallResponse
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Duration

class CallCenterEndTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val startLog = listOf("permission", "foreground.start", "engine.join")

    @Test
    fun endCall_afterConfirm_leavesThenEndsThenDestroys() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            with(h) { startActiveInAppCall() }
            h.clock.advanceBy(Duration.ofSeconds(125))
            h.center.endCall()
            runCurrent()
            assertEquals(
                startLog + listOf("engine.leave", "end", "engine.destroy", "foreground.stop"),
                h.log.toList(),
            )
            assertEquals(listOf("b1" to EndCallRequest(125, "completed")), h.api.endCallRequests)
            assertNull(h.center.session.value)
            assertFalse(h.center.isMinimized.value)
            assertTrue(
                h.center.dialogs.value
                    .isEmpty(),
            )
            assertFalse(h.center.isCallActive.value)
        }

    @Test
    fun endCall_withoutConfirm_isManualExit() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            with(h) { startActiveInAppCall(confirm = false) }
            h.clock.advanceBy(Duration.ofSeconds(40))
            h.center.endCall()
            runCurrent()
            assertEquals(listOf("b1" to EndCallRequest(40, "manual_exit")), h.api.endCallRequests)
            assertNull(h.center.session.value)
        }

    @Test
    fun endCall_atJoinInstant_reportsAtLeastOneSecond() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            with(h) { startActiveInAppCall() }
            h.center.endCall()
            runCurrent()
            assertEquals(
                1,
                h.api.endCallRequests
                    .single()
                    .second.actualDuration,
            )
        }

    @Test
    fun endCall_whileJoinInFlight_cancelsStartAndSendsNoEnd() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            val gate = CompletableDeferred<JoinCallResponse>()
            h.api.joinCallHandler = { gate.await() }
            h.center.startCall(h.request())
            runCurrent()
            h.center.endCall()
            runCurrent()
            gate.complete(h.inAppJoinResponse())
            runCurrent()
            // No leg started, so no fabricated 1 s /end (WR-02).
            assertTrue(h.api.endCallRequests.isEmpty())
            assertNull(h.center.session.value)
            assertTrue(h.engine.joins.isEmpty())
            assertEquals(listOf("foreground.stop"), h.log.toList())
            assertEquals(0, h.permission.calls)
        }

    @Test
    fun endCall_whileMicPromptOpen_sendsNoEnd() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            h.api.joinCallHandler = { h.inAppJoinResponse() }
            h.permission.gate = CompletableDeferred()
            h.center.startCall(h.request())
            runCurrent()
            assertEquals(1, h.permission.calls)
            h.clock.advanceBy(Duration.ofSeconds(30))
            h.center.endCall()
            runCurrent()
            assertEquals(0, h.api.endCallCalls)
            assertTrue(h.engine.joins.isEmpty())
            assertEquals(0, h.engine.leaveCalls)
            assertEquals(0, h.engine.destroyCalls)
            assertNull(h.center.session.value)
            assertTrue(
                h.center.dialogs.value
                    .isEmpty(),
            )
        }

    @Test
    fun endCall_whileServiceStarting_sendsNoEnd() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            h.api.joinCallHandler = { h.inAppJoinResponse() }
            h.foreground.startGate = CompletableDeferred()
            h.center.startCall(h.request())
            runCurrent()
            h.center.endCall()
            runCurrent()
            assertEquals(0, h.api.endCallCalls)
            assertTrue(h.engine.joins.isEmpty())
            assertNull(h.center.session.value)
            assertEquals(1, h.foreground.stopCalls)
        }

    @Test
    fun endCall_whileEndInFlight_isEndingAndNotRepeated() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            with(h) { startActiveInAppCall() }
            val gate = CompletableDeferred<EndCallData>()
            h.api.endCallHandler = { _, _ -> gate.await() }
            h.center.endCall()
            runCurrent()
            val session = h.center.session.value!!
            assertTrue(session.isEndingCall)
            assertEquals(CallStatus.Ended, session.status)
            h.center.endCall()
            runCurrent()
            assertEquals(1, h.api.endCallCalls)
            gate.complete(EndCallData())
            runCurrent()
            assertEquals(1, h.api.endCallCalls)
            assertNull(h.center.session.value)
        }

    @Test
    fun endCall_ignoresHttpAndNetworkErrors() =
        runTest(main.testDispatcher) {
            for (failure in listOf<Throwable?>(IOException("offline"), null)) {
                val h = CallCenterHarness(backgroundScope)
                with(h) { startActiveInAppCall() }
                h.api.endCallHandler = { _, _ ->
                    throw failure ?: h.api.httpError(409, """{"status":"error","message":"not in progress"}""")
                }
                h.center.endCall()
                runCurrent()
                assertEquals(1, h.api.endCallCalls)
                assertNull(h.center.session.value)
                assertEquals(1, h.engine.destroyCalls)
                assertEquals(1, h.foreground.stopCalls)
                assertTrue(
                    h.center.dialogs.value
                        .isEmpty(),
                )
            }
        }

    @Test
    fun endCall_neverReturningEnd_timesOut() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            with(h) { startActiveInAppCall() }
            h.api.endCallHandler = { _, _ -> awaitCancellation() }
            h.center.endCall()
            runCurrent()
            assertNotNull(h.center.session.value)
            advanceTimeBy(CallCenter.END_REQUEST_TIMEOUT_MS - 1)
            runCurrent()
            assertNotNull(h.center.session.value)
            advanceTimeBy(1)
            runCurrent()
            assertNull(h.center.session.value)
            assertEquals(1, h.engine.destroyCalls)
            assertEquals(1, h.foreground.stopCalls)
        }

    @Test
    fun endCall_leaveThatHangs_isBounded() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            with(h) { startActiveInAppCall() }
            h.engine.leaveGate = CompletableDeferred()
            h.center.endCall()
            runCurrent()
            assertEquals(0, h.api.endCallCalls)
            advanceTimeBy(CallCenter.LEAVE_STEP_TIMEOUT_MS)
            runCurrent()
            assertEquals(1, h.api.endCallCalls)
            assertNull(h.center.session.value)
        }

    @Test
    fun endCall_skypeZoom_sendsEndWithoutTouchingEngine() =
        runTest(main.testDispatcher) {
            for (opened in listOf(true, false)) {
                val h = CallCenterHarness(backgroundScope)
                h.api.joinCallHandler = { h.externalJoinResponse("https://zoom.us/j/1") }
                h.center.startCall(h.request(meetingType = MeetingType.SkypeZoom))
                runCurrent()
                if (opened) {
                    h.center.onExternalLinkOpened()
                    runCurrent()
                }
                h.clock.advanceBy(Duration.ofSeconds(60))
                h.center.endCall()
                runCurrent()
                val expected = if (opened) "completed" else "manual_exit"
                assertEquals(listOf("b1" to EndCallRequest(60, expected)), h.api.endCallRequests)
                assertEquals(0, h.engine.leaveCalls)
                assertEquals(0, h.engine.destroyCalls)
                assertEquals(1, h.foreground.stopCalls)
                assertNull(h.center.session.value)
            }
        }

    @Test
    fun notificationHangUp_runsTheSameFlow() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            with(h) { startActiveInAppCall() }
            h.foreground.emitHangUp()
            runCurrent()
            assertEquals(1, h.api.endCallCalls)
            assertNull(h.center.session.value)
        }

    @Test
    fun rejoin_afterEnd_confirmsAgain() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            with(h) { startActiveInAppCall() }
            h.center.endCall()
            runCurrent()
            assertEquals(1, h.api.confirmCallConnectionCalls)
            with(h) { startActiveInAppCall() }
            assertEquals(2, h.api.joinCallCalls)
            assertEquals(2, h.api.confirmCallConnectionCalls)
            assertEquals(
                CallStatus.Active,
                h.center.session.value!!
                    .status,
            )
        }

    @Test
    fun clearSessionData_duringCall_tearsDownLocallyWithoutEnd() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            with(h) { startActiveInAppCall() }
            h.center.clearSessionData()
            assertEquals(0, h.api.endCallCalls)
            assertEquals(1, h.engine.leaveCalls)
            assertEquals(1, h.engine.destroyCalls)
            assertEquals(1, h.foreground.stopCalls)
            assertNull(h.center.session.value)
            assertTrue(
                h.center.dialogs.value
                    .isEmpty(),
            )
        }

    @Test
    fun clearSessionData_withoutCall_onlyClearsDialogs() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            h.api.joinCallHandler = { throw IOException("offline") }
            h.center.startCall(h.request())
            runCurrent()
            assertEquals(1, h.center.dialogs.value.size)
            val stopsBefore = h.foreground.stopCalls
            h.center.clearSessionData()
            assertTrue(
                h.center.dialogs.value
                    .isEmpty(),
            )
            assertEquals(0, h.engine.destroyCalls)
            assertEquals(stopsBefore, h.foreground.stopCalls)
            assertEquals(0, h.api.endCallCalls)
        }

    @Test
    fun clearSessionData_whileEndInFlight_leavesNoDialogAndNoSession() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            with(h) { startActiveInAppCall() }
            h.api.endCallHandler = { _, _ -> awaitCancellation() }
            h.center.endCall()
            runCurrent()
            h.center.clearSessionData()
            assertNull(h.center.session.value)
            assertTrue(
                h.center.dialogs.value
                    .isEmpty(),
            )
            assertEquals(1, h.engine.destroyCalls)
        }

    @Test
    fun endCall_waitsForInFlightConfirm_thenSendsCompleted() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            val gate = CompletableDeferred<ConfirmConnectionData>()
            h.api.confirmCallConnectionHandler = { _, _ ->
                gate.await().also { synchronized(h.log) { h.log += "confirm" } }
            }
            with(h) { startActiveInAppCall() }
            h.clock.advanceBy(Duration.ofSeconds(10))
            h.center.endCall()
            runCurrent()
            // /end must not overtake the PATCH that moves the booking to in_progress (WR-03).
            assertEquals(0, h.api.endCallCalls)
            gate.complete(ConfirmConnectionData())
            runCurrent()
            assertEquals(listOf("b1" to EndCallRequest(10, "completed")), h.api.endCallRequests)
            assertEquals(
                startLog + listOf("engine.leave", "confirm", "end", "engine.destroy", "foreground.stop"),
                h.log.toList(),
            )
            assertNull(h.center.session.value)
        }

    @Test
    fun endCall_afterFailedConfirm_isManualExit() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            h.api.confirmCallConnectionHandler = { _, _ -> throw IOException("offline") }
            with(h) { startActiveInAppCall() }
            assertEquals(1, h.api.confirmCallConnectionCalls)
            h.clock.advanceBy(Duration.ofSeconds(20))
            h.center.endCall()
            runCurrent()
            assertEquals(listOf("b1" to EndCallRequest(20, "manual_exit")), h.api.endCallRequests)
        }

    @Test
    fun endCall_confirmThatHangs_isBoundedAndManualExit() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            h.api.confirmCallConnectionHandler = { _, _ -> awaitCancellation() }
            with(h) { startActiveInAppCall() }
            h.center.endCall()
            runCurrent()
            advanceTimeBy(CallCenter.CONFIRM_SETTLE_TIMEOUT_MS - 1)
            runCurrent()
            assertEquals(0, h.api.endCallCalls)
            advanceTimeBy(1)
            runCurrent()
            assertEquals(
                "manual_exit",
                h.api.endCallRequests
                    .single()
                    .second.endReason,
            )
            assertNull(h.center.session.value)
        }

    @Test
    fun rejoin_whileOldConfirmHangs_confirmsTheNewSession() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            h.api.confirmCallConnectionHandler = { _, _ -> awaitCancellation() }
            with(h) { startActiveInAppCall() }
            h.center.endCall()
            runCurrent()
            advanceTimeBy(CallCenter.CONFIRM_SETTLE_TIMEOUT_MS)
            runCurrent()
            assertNull(h.center.session.value)
            h.api.confirmCallConnectionHandler = { _, _ -> ConfirmConnectionData() }
            with(h) { startActiveInAppCall() }
            assertEquals(2, h.api.confirmCallConnectionCalls)
            assertTrue(
                h.center.session.value!!
                    .confirmSent,
            )
        }
}
