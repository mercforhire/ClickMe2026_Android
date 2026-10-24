@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.managers

import com.uptrendinvestments.clickmeapp.model.enums.ConnectionType
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.model.responses.JoinCallResponse
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class CallCenterStartTest {
    @get:Rule
    val main = MainDispatcherRule()

    private fun assertTornDownWithoutEnd(h: CallCenterHarness) {
        assertNull(h.center.session.value)
        assertFalse(h.center.isCallActive.value)
        assertEquals(0, h.api.endCallCalls)
    }

    @Test
    fun startCall_createsConnectingSessionBeforeJoinReturns() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            val gate = CompletableDeferred<JoinCallResponse>()
            h.api.joinCallHandler = { gate.await() }
            h.center.startCall(h.request(meetingType = MeetingType.SkypeZoom))
            runCurrent()
            val s = h.center.session.value!!
            assertEquals(CallStatus.Connecting, s.status)
            assertFalse(h.center.isMinimized.value)
            assertTrue(h.center.isCallActive.value)
            assertEquals(ConnectionType.SkypeZoom, s.connectionType)
        }

    @Test
    fun inApp_success_runsPermissionThenForegroundThenEngine() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            h.api.joinCallHandler = { h.inAppJoinResponse() }
            h.center.startCall(h.request())
            runCurrent()
            assertEquals(listOf("permission", "foreground.start", "engine.join"), h.log)
            assertEquals(listOf(Triple("tok", "chan", 7)), h.engine.joins)
            val info = h.foreground.starts.single()
            assertEquals("b1", info.bookingId)
            assertEquals("Pat", info.peerName)
            assertEquals(T0, info.scheduledStart)
            assertEquals(
                T0,
                h.center.session.value!!
                    .joinStartedAt,
            )
            assertEquals(0, h.api.endCallCalls)
        }

    @Test
    fun responseConnectionType_overridesTheHint() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            h.api.joinCallHandler = { h.inAppJoinResponse() }
            h.center.startCall(h.request(meetingType = MeetingType.SkypeZoom))
            runCurrent()
            assertEquals(
                ConnectionType.InAppVoice,
                h.center.session.value!!
                    .connectionType,
            )
            assertEquals(1, h.engine.joins.size)
        }

    @Test
    fun inApp_withoutAgoraConfig_failsWithNotSetUp() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            h.api.joinCallHandler = { h.inAppJoinResponse().copy(agoraConfig = null) }
            h.center.startCall(h.request())
            runCurrent()
            assertTornDownWithoutEnd(h)
            assertEquals(
                listOf<CallDialog>(CallDialog.JoinError(JoinErrorReason.NotSetUpForInAppVoice)),
                h.center.dialogs.value,
            )
            assertEquals(0, h.permission.calls)
            assertEquals(0, h.foreground.starts.size)
        }

    @Test
    fun skypeZoom_withUrl_setsUrlAndNeverTouchesMicOrAgora() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            h.api.joinCallHandler = { h.externalJoinResponse("https://zoom.us/j/1") }
            h.center.startCall(h.request(meetingType = MeetingType.SkypeZoom))
            runCurrent()
            val s = h.center.session.value!!
            assertEquals("https://zoom.us/j/1", s.externalJoinUrl)
            assertEquals(T0, s.joinStartedAt)
            assertEquals(CallStatus.Connecting, s.status)
            assertEquals(0, h.permission.calls)
            assertEquals(0, h.foreground.starts.size)
            assertEquals(0, h.engine.joins.size)
        }

    @Test
    fun skypeZoom_withNullOrBlankUrl_failsWithMissingLink() =
        runTest(main.testDispatcher) {
            for (url in listOf(null, "  ")) {
                val h = CallCenterHarness(backgroundScope)
                h.api.joinCallHandler = { h.externalJoinResponse(url) }
                h.center.startCall(h.request(meetingType = MeetingType.SkypeZoom))
                runCurrent()
                assertTornDownWithoutEnd(h)
                assertEquals(
                    listOf<CallDialog>(CallDialog.JoinError(JoinErrorReason.MissingMeetingLink)),
                    h.center.dialogs.value,
                )
            }
        }

    @Test
    fun serverRefusal_showsServerMessage() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            h.api.joinCallHandler = {
                throw h.api.httpError(403, """{"status":"error","message":"Session join window is not active."}""")
            }
            h.center.startCall(h.request())
            runCurrent()
            assertTornDownWithoutEnd(h)
            assertEquals(
                listOf<CallDialog>(
                    CallDialog.JoinError(JoinErrorReason.Server("Session join window is not active.")),
                ),
                h.center.dialogs.value,
            )
            assertEquals(0, h.foreground.starts.size)
            assertEquals(0, h.engine.joins.size)
        }

    @Test
    fun networkFailure_showsNetworkError() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            h.api.joinCallHandler = { throw IOException("offline") }
            h.center.startCall(h.request())
            runCurrent()
            assertTornDownWithoutEnd(h)
            assertEquals(
                listOf<CallDialog>(CallDialog.JoinError(JoinErrorReason.Network)),
                h.center.dialogs.value,
            )
        }

    @Test
    fun unknownConnectionType_showsUnknownError() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            h.api.joinCallHandler = { h.inAppJoinResponse().copy(connectionType = "video") }
            h.center.startCall(h.request())
            runCurrent()
            assertTornDownWithoutEnd(h)
            assertEquals(
                listOf<CallDialog>(CallDialog.JoinError(JoinErrorReason.Unknown)),
                h.center.dialogs.value,
            )
            assertEquals(0, h.foreground.starts.size)
        }

    @Test
    fun foregroundStartFailure_neverCreatesTheEngine() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            h.api.joinCallHandler = { h.inAppJoinResponse() }
            h.foreground.startResult = false
            h.center.startCall(h.request())
            runCurrent()
            assertTornDownWithoutEnd(h)
            assertEquals(listOf<CallDialog>(CallDialog.JoinError(JoinErrorReason.Unknown)), h.center.dialogs.value)
            assertEquals(0, h.engine.joins.size)
            assertTrue(h.foreground.stopCalls >= 1)
        }

    @Test
    fun backgroundedAfterPermission_failsWithoutStartingForeground() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            h.api.joinCallHandler = { h.inAppJoinResponse() }
            val answer = CompletableDeferred<MicPermissionResult>()
            h.permission.gate = answer
            h.center.startCall(h.request())
            runCurrent()
            h.processForeground.isForeground.value = false
            answer.complete(MicPermissionResult.Granted)
            runCurrent()
            assertTornDownWithoutEnd(h)
            assertEquals(listOf<CallDialog>(CallDialog.JoinError(JoinErrorReason.Unknown)), h.center.dialogs.value)
            assertEquals(0, h.foreground.starts.size)
        }

    @Test
    fun engineJoinRejected_destroysEngineAndStopsService() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            h.api.joinCallHandler = { h.inAppJoinResponse() }
            h.engine.joinResult = false
            h.center.startCall(h.request())
            runCurrent()
            assertTornDownWithoutEnd(h)
            assertEquals(listOf<CallDialog>(CallDialog.JoinError(JoinErrorReason.Unknown)), h.center.dialogs.value)
            assertEquals(1, h.engine.destroyCalls)
            assertTrue(h.foreground.stopCalls >= 1)
        }
}
