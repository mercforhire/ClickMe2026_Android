package com.uptrendinvestments.clickmeapp.managers

import com.uptrendinvestments.clickmeapp.model.enums.ConnectionType
import com.uptrendinvestments.clickmeapp.model.enums.EndReason
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.network.ApiException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException
import java.time.Instant

class CallSessionTest {
    private val t0 = Instant.parse("2026-10-24T10:00:00Z")
    private val t1 = Instant.parse("2026-10-24T10:05:00Z")
    private val t2 = Instant.parse("2026-10-24T10:10:00Z")

    private fun session(status: CallStatus = CallStatus.Connecting) =
        CallSession(
            id = 1L,
            bookingId = "b1",
            peerId = "p1",
            peerName = "Sam",
            peerAvatarUrl = null,
            topic = null,
            scheduledStart = t0,
            scheduledEnd = null,
            connectionType = ConnectionType.InAppVoice,
            status = status,
        )

    private fun http(
        status: Int,
        message: String?,
    ) = ApiException.Http(
        status = status,
        code = null,
        rawCode = null,
        serverMessage = message,
        fieldErrors = emptyList(),
        detail = null,
    )

    @Test
    fun deriveCallStatus_coversAllNinePairs() {
        val d = ConnState.Disconnected
        val w = ConnState.Waiting
        val r = ConnState.Ready
        val expected =
            mapOf(
                (d to d) to CallStatus.Ended,
                (d to w) to CallStatus.Ended,
                (d to r) to CallStatus.Ended,
                (w to d) to CallStatus.WaitingForPeer,
                (r to d) to CallStatus.WaitingForPeer,
                (r to r) to CallStatus.Active,
                (r to w) to CallStatus.WaitingForPeer,
                (w to w) to CallStatus.Connecting,
                (w to r) to CallStatus.Connecting,
            )
        assertEquals(9, expected.size)
        expected.forEach { (pair, status) ->
            assertEquals("${pair.first}/${pair.second}", status, deriveCallStatus(pair.first, pair.second))
        }
    }

    @Test
    fun withConnStates_readyReady_becomesActive() {
        val result = session().withConnStates(ConnState.Ready, ConnState.Ready, t1)
        assertEquals(CallStatus.Active, result.status)
        assertEquals(ConnState.Ready, result.mine)
        assertEquals(ConnState.Ready, result.remote)
        assertNull(result.endedAt)
    }

    @Test
    fun withConnStates_endedIsSticky() {
        val ended = session().withConnStates(ConnState.Disconnected, ConnState.Ready, t1)
        val after = ended.withConnStates(ConnState.Ready, ConnState.Ready, t2)
        assertEquals(CallStatus.Ended, after.status)
        assertEquals(t1, after.endedAt)
    }

    @Test
    fun withConnStates_stampsEndedAtOnFirstEnded() {
        val result = session().withConnStates(ConnState.Disconnected, ConnState.Waiting, t1)
        assertEquals(CallStatus.Ended, result.status)
        assertEquals(t1, result.endedAt)
    }

    @Test
    fun withConnStates_remoteDisconnectThenRejoin_restoresActive() {
        val active = session().withConnStates(ConnState.Ready, ConnState.Ready, t0)
        val waiting = active.withConnStates(ConnState.Ready, ConnState.Disconnected, t1)
        assertEquals(CallStatus.WaitingForPeer, waiting.status)
        assertEquals(CallStatus.Active, waiting.withConnStates(ConnState.Ready, ConnState.Ready, t2).status)
    }

    @Test
    fun initialConnectionType_followsMeetingTypeHint() {
        fun request(meetingType: MeetingType?) = CallStartRequest("b1", null, null, null, null, t0, null, meetingType)

        assertEquals(ConnectionType.SkypeZoom, request(MeetingType.SkypeZoom).initialConnectionType())
        assertEquals(ConnectionType.InAppVoice, request(MeetingType.InAppVoice).initialConnectionType())
        assertEquals(ConnectionType.InAppVoice, request(null).initialConnectionType())
    }

    @Test
    fun enums_wireValues() {
        assertEquals(ConnectionType.SkypeZoom, ConnectionType.fromWire("skype_zoom"))
        assertEquals(ConnectionType.InAppVoice, ConnectionType.fromWire("in_app_voice"))
        assertNull(ConnectionType.fromWire("video"))
        assertNull(ConnectionType.fromWire(null))
        assertEquals("manual_exit", EndReason.ManualExit.wire)
        assertEquals(EndReason.Dropped, EndReason.fromWire("dropped"))
    }

    @Test
    fun toJoinErrorReason_mapsApiExceptions() {
        val message = "Session join window is not active."
        assertEquals(JoinErrorReason.Server(message), http(403, message).toJoinErrorReason())
        assertEquals(JoinErrorReason.Unknown, http(500, null).toJoinErrorReason())
        assertEquals(JoinErrorReason.Network, ApiException.Network(IOException("x")).toJoinErrorReason())
        assertEquals(JoinErrorReason.Unknown, ApiException.Decoding(RuntimeException("x")).toJoinErrorReason())
    }

    @Test
    fun defaults_speakerOnAndNotMuted() {
        val s = session()
        assertEquals(true, s.isSpeakerOn)
        assertEquals(false, s.isMuted)
    }
}
