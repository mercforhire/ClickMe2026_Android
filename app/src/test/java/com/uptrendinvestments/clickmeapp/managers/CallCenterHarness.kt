package com.uptrendinvestments.clickmeapp.managers

import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.model.responses.AgoraConfig
import com.uptrendinvestments.clickmeapp.model.responses.ConfirmConnectionData
import com.uptrendinvestments.clickmeapp.model.responses.EndCallData
import com.uptrendinvestments.clickmeapp.model.responses.ExternalConfig
import com.uptrendinvestments.clickmeapp.model.responses.JoinCallResponse
import com.uptrendinvestments.clickmeapp.testing.FakeCallEngine
import com.uptrendinvestments.clickmeapp.testing.FakeCallForegroundController
import com.uptrendinvestments.clickmeapp.testing.FakeCallPermissionGate
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakeProcessForeground
import com.uptrendinvestments.clickmeapp.testing.FakeRealtimeConnection
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import java.time.Duration
import java.time.Instant

/** Wall-clock start used by every CallCenter test. Sub-second part checks the timestamp truncation. */
val T0: Instant = Instant.parse("2026-10-24T14:00:00.750Z")

/**
 * Shared builder for the CallCenter tests: a [CallCenter] wired to the Wave 1 fakes, with one shared [log]
 * recording the cross-fake call order ("permission", "foreground.start", "engine.join", ...).
 *
 * Pass the test's `backgroundScope` as [scope]; the center's collectors run for the whole test.
 */
class CallCenterHarness(
    scope: CoroutineScope,
    foregroundInitially: Boolean = true,
) {
    val log = mutableListOf<String>()
    val api = FakeClickMeApi()
    val realtime = FakeRealtimeConnection()
    val engine = FakeCallEngine(log)
    val foreground = FakeCallForegroundController(log)
    val permission = FakeCallPermissionGate(log)
    val processForeground = FakeProcessForeground(foregroundInitially)
    val clock = MutableClock(T0)
    val center = CallCenter(api, realtime, engine, foreground, permission, processForeground, clock, scope)

    init {
        api.confirmCallConnectionHandler = { _, _ -> ConfirmConnectionData() }
        api.endCallHandler = { _, _ ->
            synchronized(log) { log += "end" }
            EndCallData()
        }
    }

    /**
     * Starts an in-app call and drives it to LocalJoined, plus RemoteJoined (so Active and the confirm
     * PATCH) when [confirm] is true.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun TestScope.startActiveInAppCall(confirm: Boolean = true) {
        api.joinCallHandler = { inAppJoinResponse() }
        center.startCall(request())
        runCurrent()
        engine.emit(EngineEvent.LocalJoined)
        runCurrent()
        if (confirm) {
            engine.emit(EngineEvent.RemoteJoined(9))
            runCurrent()
        }
    }

    fun inAppJoinResponse(
        bookingId: String = "b1",
        token: String = "tok",
        channel: String = "chan",
        uid: Int = 7,
    ) = JoinCallResponse(
        bookingId = bookingId,
        connectionType = "in_app_voice",
        agoraConfig = AgoraConfig(rtcToken = token, uid = uid, channelName = channel),
    )

    fun externalJoinResponse(
        url: String?,
        bookingId: String = "b1",
    ) = JoinCallResponse(
        bookingId = bookingId,
        connectionType = "skype_zoom",
        externalConfig = ExternalConfig(joinUrl = url),
    )

    fun request(
        bookingId: String = "b1",
        meetingType: MeetingType? = MeetingType.InAppVoice,
        scheduledStart: Instant = T0,
        scheduledEnd: Instant? = T0.plus(Duration.ofMinutes(30)),
    ) = CallStartRequest(
        bookingId = bookingId,
        peerId = "p1",
        peerName = "Pat",
        peerAvatarUrl = null,
        topic = "Career",
        scheduledStart = scheduledStart,
        scheduledEnd = scheduledEnd,
        meetingType = meetingType,
    )
}
