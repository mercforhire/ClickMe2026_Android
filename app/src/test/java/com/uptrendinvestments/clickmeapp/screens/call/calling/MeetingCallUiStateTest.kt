package com.uptrendinvestments.clickmeapp.screens.call.calling

import com.uptrendinvestments.clickmeapp.managers.CallSession
import com.uptrendinvestments.clickmeapp.managers.CallStatus
import com.uptrendinvestments.clickmeapp.model.enums.ConnectionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MeetingCallUiStateTest {
    private val start = Instant.parse("2026-10-24T10:00:00Z")
    private val end = start.plusSeconds(1800)

    private fun session(
        connectionType: ConnectionType = ConnectionType.InAppVoice,
        status: CallStatus = CallStatus.Connecting,
        externalJoinUrl: String? = null,
        hasOpenedExternalLink: Boolean = false,
        endedAt: Instant? = null,
    ) = CallSession(
        id = 1,
        bookingId = "b1",
        peerId = "p1",
        peerName = "Alex",
        peerAvatarUrl = "https://img/peer.png",
        topic = "Career advice",
        scheduledStart = start,
        scheduledEnd = end,
        connectionType = connectionType,
        status = status,
        externalJoinUrl = externalJoinUrl,
        hasOpenedExternalLink = hasOpenedExternalLink,
        isMuted = true,
        isSpeakerOn = false,
        isEndingCall = true,
        endedAt = endedAt,
    )

    @Test
    fun connectingInAppSessionMapsTimerStatusProviderAndMyAvatar() {
        val state = session().toMeetingCallUiState(start.plusSeconds(125), "https://img/me.png")
        assertEquals(125L, state.timer.elapsedSeconds)
        assertEquals(1800L - 125L, state.timer.remainingSeconds)
        assertEquals(CallStatus.Connecting, state.status)
        assertEquals(ExternalMeetingProvider.Unknown, state.provider)
        assertEquals("https://img/me.png", state.myAvatarUrl)
        assertEquals(ConnectionType.InAppVoice, state.connectionType)
    }

    @Test
    fun endedSessionFreezesTheTimerAtEndedAt() {
        val ended = session(status = CallStatus.Ended, endedAt = start.plusSeconds(60))
        val state = ended.toMeetingCallUiState(start.plusSeconds(600), null)
        assertEquals(60L, state.timer.elapsedSeconds)
    }

    @Test
    fun endedSessionWithoutEndedAtFallsBackToNow() {
        val state = session(status = CallStatus.Ended).toMeetingCallUiState(start.plusSeconds(90), null)
        assertEquals(90L, state.timer.elapsedSeconds)
    }

    @Test
    fun nonEndedSessionIgnoresEndedAt() {
        val state = session(endedAt = start.plusSeconds(60)).toMeetingCallUiState(start.plusSeconds(600), null)
        assertEquals(600L, state.timer.elapsedSeconds)
    }

    @Test
    fun externalSessionDetectsProviderAndKeepsOpenedFlag() {
        val s =
            session(
                connectionType = ConnectionType.SkypeZoom,
                externalJoinUrl = "https://meet.google.com/x",
                hasOpenedExternalLink = true,
            )
        val state = s.toMeetingCallUiState(start, null)
        assertEquals(ExternalMeetingProvider.GoogleMeet, state.provider)
        assertTrue(state.hasOpenedExternalLink)
        assertEquals("https://meet.google.com/x", state.externalJoinUrl)
        assertNull(state.myAvatarUrl)
    }

    @Test
    fun externalSessionWithoutUrlIsUnknownAndNotOpened() {
        val state = session(connectionType = ConnectionType.SkypeZoom).toMeetingCallUiState(start, null)
        assertEquals(ExternalMeetingProvider.Unknown, state.provider)
        assertFalse(state.hasOpenedExternalLink)
    }

    @Test
    fun plainFieldsCopyThroughUnchanged() {
        val state = session().toMeetingCallUiState(start, null)
        assertTrue(state.isMuted)
        assertFalse(state.isSpeakerOn)
        assertTrue(state.isEndingCall)
        assertEquals("Alex", state.peerName)
        assertEquals("https://img/peer.png", state.peerAvatarUrl)
        assertEquals("Career advice", state.topic)
        assertEquals(start, state.scheduledStart)
        assertEquals(end, state.scheduledEnd)
    }
}
