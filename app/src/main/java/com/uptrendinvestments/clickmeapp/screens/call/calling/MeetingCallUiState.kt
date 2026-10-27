package com.uptrendinvestments.clickmeapp.screens.call.calling

import com.uptrendinvestments.clickmeapp.managers.CallSession
import com.uptrendinvestments.clickmeapp.managers.CallStatus
import com.uptrendinvestments.clickmeapp.managers.CallTimerState
import com.uptrendinvestments.clickmeapp.managers.callTimerState
import com.uptrendinvestments.clickmeapp.model.enums.ConnectionType
import java.time.Instant

/** Everything the expanded call screen renders. A pure projection of [CallSession] and the clock. */
data class MeetingCallUiState(
    val connectionType: ConnectionType,
    val peerName: String?,
    val peerAvatarUrl: String?,
    val myAvatarUrl: String?,
    val topic: String?,
    val scheduledStart: Instant,
    val scheduledEnd: Instant?,
    val status: CallStatus,
    val timer: CallTimerState,
    val isMuted: Boolean,
    val isSpeakerOn: Boolean,
    val isEndingCall: Boolean,
    val externalJoinUrl: String?,
    val provider: ExternalMeetingProvider,
    val hasOpenedExternalLink: Boolean,
    /** The counterpart's user id; null until the session knows it, which keeps the Chat control disabled. */
    val peerId: String? = null,
)

/**
 * Maps the live session at [now]. Once the call is Ended the timer freezes at the moment it ended (iOS stops the
 * ticker on end); otherwise it follows the shared clock, anchored to the scheduled start.
 */
fun CallSession.toMeetingCallUiState(
    now: Instant,
    myAvatarUrl: String?,
): MeetingCallUiState {
    val timerNow = if (status == CallStatus.Ended) endedAt ?: now else now
    return MeetingCallUiState(
        connectionType = connectionType,
        peerName = peerName,
        peerAvatarUrl = peerAvatarUrl,
        myAvatarUrl = myAvatarUrl,
        topic = topic,
        scheduledStart = scheduledStart,
        scheduledEnd = scheduledEnd,
        status = status,
        timer = callTimerState(scheduledStart, scheduledEnd, timerNow),
        isMuted = isMuted,
        isSpeakerOn = isSpeakerOn,
        isEndingCall = isEndingCall,
        externalJoinUrl = externalJoinUrl,
        provider = ExternalMeetingProvider.detect(externalJoinUrl),
        hasOpenedExternalLink = hasOpenedExternalLink,
        peerId = peerId,
    )
}
