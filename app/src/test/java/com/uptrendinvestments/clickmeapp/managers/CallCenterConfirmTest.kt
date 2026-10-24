@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.managers

import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.model.responses.ConfirmConnectionData
import com.uptrendinvestments.clickmeapp.model.responses.JoinCallResponse
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class CallCenterConfirmTest {
    @get:Rule
    val main = MainDispatcherRule()

    private fun TestScope.startedInApp(): CallCenterHarness {
        val h = CallCenterHarness(backgroundScope)
        h.api.joinCallHandler = { h.inAppJoinResponse() }
        h.center.startCall(h.request())
        runCurrent()
        return h
    }

    private fun TestScope.startedExternal(url: String? = "https://zoom.us/j/1"): CallCenterHarness {
        val h = CallCenterHarness(backgroundScope)
        h.api.joinCallHandler = { h.externalJoinResponse(url) }
        h.center.startCall(h.request(meetingType = MeetingType.SkypeZoom))
        runCurrent()
        return h
    }

    private fun TestScope.emit(
        h: CallCenterHarness,
        event: EngineEvent,
    ) {
        h.engine.emit(event)
        runCurrent()
    }

    @Test
    fun firstActive_sendsExactlyOneTruncatedConfirm() =
        runTest(main.testDispatcher) {
            val h = startedInApp()
            emit(h, EngineEvent.LocalJoined)
            emit(h, EngineEvent.RemoteJoined(9))
            val (bookingId, body) = h.api.confirmCallConnectionRequests.single()
            assertEquals("b1", bookingId)
            assertEquals("connected", body.connectionStatus)
            assertEquals("in_app_voice", body.method)
            assertEquals("2026-10-24T14:00:00Z", body.timestamp)
            assertTrue(
                h.center.session.value!!
                    .confirmSent,
            )
        }

    @Test
    fun activeFlap_doesNotConfirmTwice() =
        runTest(main.testDispatcher) {
            val h = startedInApp()
            emit(h, EngineEvent.LocalJoined)
            emit(h, EngineEvent.RemoteJoined(9))
            emit(h, EngineEvent.RemoteOffline(9, 0))
            emit(h, EngineEvent.RemoteJoined(9))
            assertEquals(1, h.api.confirmCallConnectionCalls)
        }

    @Test
    fun confirmConflictOrNetworkError_isIgnored() =
        runTest(main.testDispatcher) {
            for (failure in listOf<Throwable?>(IOException("offline"), null)) {
                val h = startedInApp()
                h.api.confirmCallConnectionHandler = { _, _ ->
                    throw failure ?: h.api.httpError(409, """{"status":"error","message":"already"}""")
                }
                emit(h, EngineEvent.LocalJoined)
                emit(h, EngineEvent.RemoteJoined(9))
                assertEquals(1, h.api.confirmCallConnectionCalls)
                assertEquals(
                    CallStatus.Active,
                    h.center.session.value!!
                        .status,
                )
                assertTrue(
                    h.center.dialogs.value
                        .isEmpty(),
                )
                assertNotNull(h.center.session.value)
            }
        }

    @Test
    fun externalLinkOpened_confirmsOnceAndMinimizes() =
        runTest(main.testDispatcher) {
            val h = startedExternal()
            h.center.onExternalLinkOpened()
            runCurrent()
            val (_, body) = h.api.confirmCallConnectionRequests.single()
            assertEquals("skype_zoom", body.method)
            assertTrue(
                h.center.session.value!!
                    .hasOpenedExternalLink,
            )
            assertTrue(h.center.isMinimized.value)
            h.center.expand()
            h.center.onExternalLinkOpened()
            runCurrent()
            assertEquals(1, h.api.confirmCallConnectionCalls)
        }

    @Test
    fun externalLinkOpened_whileJoinInFlight_doesNothing() =
        runTest(main.testDispatcher) {
            val h = CallCenterHarness(backgroundScope)
            val gate = CompletableDeferred<JoinCallResponse>()
            h.api.joinCallHandler = { gate.await() }
            h.center.startCall(h.request(meetingType = MeetingType.SkypeZoom))
            runCurrent()
            h.center.onExternalLinkOpened()
            runCurrent()
            assertEquals(0, h.api.confirmCallConnectionCalls)
            assertFalse(
                h.center.session.value!!
                    .hasOpenedExternalLink,
            )
            assertFalse(h.center.isMinimized.value)
        }

    @Test
    fun toggleMute_changesFlagOnlyWhenEngineAccepts() =
        runTest(main.testDispatcher) {
            val h = startedInApp()
            h.center.toggleMute()
            assertTrue(
                h.center.session.value!!
                    .isMuted,
            )
            assertEquals(listOf(true), h.engine.muteCalls)
            h.engine.muteResult = false
            h.center.toggleMute()
            assertTrue(
                h.center.session.value!!
                    .isMuted,
            )
            assertEquals(listOf(true, false), h.engine.muteCalls)
        }

    @Test
    fun toggleSpeaker_startsOnAndTurnsOff() =
        runTest(main.testDispatcher) {
            val h = startedInApp()
            assertTrue(
                h.center.session.value!!
                    .isSpeakerOn,
            )
            h.center.toggleSpeaker()
            assertFalse(
                h.center.session.value!!
                    .isSpeakerOn,
            )
            assertEquals(listOf(false), h.engine.speakerCalls)
        }

    @Test
    fun controls_areNoOpsForSkypeZoom() =
        runTest(main.testDispatcher) {
            val h = startedExternal()
            h.center.toggleMute()
            h.center.toggleSpeaker()
            assertEquals(0, h.engine.muteCalls.size)
            assertEquals(0, h.engine.speakerCalls.size)
        }

    @Test
    fun failedConfirm_isRetriedOnNextActive_andSucceedsExactlyOnce() =
        runTest(main.testDispatcher) {
            val h = startedInApp()
            var fail = true
            h.api.confirmCallConnectionHandler = { _, _ ->
                if (fail) throw IOException("offline")
                ConfirmConnectionData()
            }
            emit(h, EngineEvent.LocalJoined)
            emit(h, EngineEvent.RemoteJoined(9))
            assertEquals(1, h.api.confirmCallConnectionCalls)
            assertFalse(
                h.center.session.value!!
                    .confirmSent,
            )
            fail = false
            emit(h, EngineEvent.RemoteOffline(9, 0))
            emit(h, EngineEvent.RemoteJoined(9))
            assertEquals(2, h.api.confirmCallConnectionCalls)
            assertTrue(
                h.center.session.value!!
                    .confirmSent,
            )
            emit(h, EngineEvent.RemoteOffline(9, 0))
            emit(h, EngineEvent.RemoteJoined(9))
            assertEquals(2, h.api.confirmCallConnectionCalls)
        }

    @Test
    fun confirmInFlight_isNotDuplicated_andFlagIsSetOnSuccess() =
        runTest(main.testDispatcher) {
            val h = startedInApp()
            val gate = CompletableDeferred<ConfirmConnectionData>()
            h.api.confirmCallConnectionHandler = { _, _ -> gate.await() }
            emit(h, EngineEvent.LocalJoined)
            emit(h, EngineEvent.RemoteJoined(9))
            emit(h, EngineEvent.RemoteOffline(9, 0))
            emit(h, EngineEvent.RemoteJoined(9))
            assertEquals(1, h.api.confirmCallConnectionCalls)
            assertFalse(
                h.center.session.value!!
                    .confirmSent,
            )
            gate.complete(ConfirmConnectionData())
            runCurrent()
            assertTrue(
                h.center.session.value!!
                    .confirmSent,
            )
            assertEquals(1, h.api.confirmCallConnectionCalls)
        }

    @Test
    fun failedExternalConfirm_isRetriedOnTheNextOpen() =
        runTest(main.testDispatcher) {
            val h = startedExternal()
            var fail = true
            h.api.confirmCallConnectionHandler = { _, _ ->
                if (fail) throw IOException("offline")
                ConfirmConnectionData()
            }
            h.center.onExternalLinkOpened()
            runCurrent()
            fail = false
            h.center.onExternalLinkOpened()
            runCurrent()
            assertEquals(2, h.api.confirmCallConnectionCalls)
            assertTrue(
                h.center.session.value!!
                    .confirmSent,
            )
        }
}
