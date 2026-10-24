@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.managers

import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.realtime.RealtimeEvent
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import java.time.Duration

class CallCenterPeerTest {
    @get:Rule
    val main = MainDispatcherRule()

    private fun TestScope.startedInApp(): CallCenterHarness {
        val h = CallCenterHarness(backgroundScope)
        h.api.joinCallHandler = { h.inAppJoinResponse() }
        h.center.startCall(h.request())
        runCurrent()
        return h
    }

    private fun CallCenterHarness.status() = center.session.value!!.status

    private fun TestScope.emit(
        h: CallCenterHarness,
        event: EngineEvent,
    ) {
        h.engine.emit(event)
        runCurrent()
    }

    @Test
    fun engineEvents_driveWaitingActiveAndBack() =
        runTest(main.testDispatcher) {
            val h = startedInApp()
            emit(h, EngineEvent.LocalJoined)
            assertEquals(CallStatus.WaitingForPeer, h.status())
            emit(h, EngineEvent.RemoteJoined(9))
            assertEquals(CallStatus.Active, h.status())
            emit(h, EngineEvent.RemoteOffline(9, 0))
            assertEquals(CallStatus.WaitingForPeer, h.status())
            emit(h, EngineEvent.RemoteJoined(9))
            assertEquals(CallStatus.Active, h.status())
        }

    @Test
    fun callEnded_forSameBooking_marksPeerAwayButNeverEndsTheCall() =
        runTest(main.testDispatcher) {
            val h = startedInApp()
            emit(h, EngineEvent.LocalJoined)
            emit(h, EngineEvent.RemoteJoined(9))
            h.realtime.emit(RealtimeEvent.CallEnded("b1", "completed"))
            runCurrent()
            assertEquals(CallStatus.WaitingForPeer, h.status())
            assertNotNull(h.center.session.value)
            assertEquals(0, h.engine.leaveCalls)
            assertEquals(0, h.api.endCallCalls)
            emit(h, EngineEvent.RemoteJoined(9))
            assertEquals(CallStatus.Active, h.status())
        }

    @Test
    fun callEnded_forAnotherBooking_isIgnored() =
        runTest(main.testDispatcher) {
            val h = startedInApp()
            emit(h, EngineEvent.LocalJoined)
            emit(h, EngineEvent.RemoteJoined(9))
            h.realtime.emit(RealtimeEvent.CallEnded("other", "completed"))
            runCurrent()
            assertEquals(CallStatus.Active, h.status())
        }

    @Test
    fun localFailed_endsStickily() =
        runTest(main.testDispatcher) {
            val h = startedInApp()
            emit(h, EngineEvent.LocalJoined)
            h.clock.advanceBy(Duration.ofSeconds(5))
            emit(h, EngineEvent.LocalFailed)
            val s = h.center.session.value!!
            assertEquals(CallStatus.Ended, s.status)
            assertEquals(h.clock.instant(), s.endedAt)
            emit(h, EngineEvent.RemoteJoined(9))
            assertEquals(CallStatus.Ended, h.status())
            assertNotNull(h.center.session.value)
        }

    @Test
    fun engineEvents_withNoSession_changeNothing() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            runCurrent()
            emit(h, EngineEvent.LocalJoined)
            emit(h, EngineEvent.RemoteJoined(9))
            assertEquals(null, h.center.session.value)
            assertEquals(0, h.api.confirmCallConnectionCalls)
        }

    @Test
    fun engineEvents_duringSkypeZoomSession_changeNothing() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            h.api.joinCallHandler = { h.externalJoinResponse("https://zoom.us/j/1") }
            h.center.startCall(h.request(meetingType = MeetingType.SkypeZoom))
            runCurrent()
            emit(h, EngineEvent.LocalJoined)
            emit(h, EngineEvent.RemoteJoined(9))
            assertEquals(CallStatus.Connecting, h.status())
            assertEquals(0, h.api.confirmCallConnectionCalls)
        }
}
