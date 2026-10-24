package com.uptrendinvestments.clickmeapp.managers

import com.uptrendinvestments.clickmeapp.model.enums.ConnectionType
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import java.time.Instant

/** What the call screen renders. Mirrors iOS `MeetingCallViewModel.CallStatus`. */
enum class CallStatus {
    Connecting,
    WaitingForPeer,
    Active,
    Ended,
}

/** Connection state of one side of the call. */
enum class ConnState {
    Waiting,
    Ready,
    Disconnected,
}

/**
 * Combines the local and remote connection states into a [CallStatus]. Port of iOS `wireAgoraStateSync`;
 * the branch order matters: local Disconnected first, then remote Disconnected, then (Ready, Ready), then
 * local Ready, else Connecting.
 */
fun deriveCallStatus(
    mine: ConnState,
    remote: ConnState,
): CallStatus =
    when {
        mine == ConnState.Disconnected -> CallStatus.Ended
        remote == ConnState.Disconnected -> CallStatus.WaitingForPeer
        mine == ConnState.Ready && remote == ConnState.Ready -> CallStatus.Active
        mine == ConnState.Ready -> CallStatus.WaitingForPeer
        else -> CallStatus.Connecting
    }

/** Everything a Join site hands to `CallStarter.startCall`. */
data class CallStartRequest(
    val bookingId: String,
    val peerId: String?,
    val peerName: String?,
    val peerAvatarUrl: String?,
    val topic: String?,
    val scheduledStart: Instant,
    val scheduledEnd: Instant?,
    val meetingType: MeetingType?,
)

/**
 * Client-side hint so the call screen can pick its surface before `POST /join` returns. The server
 * response stays the source of truth (iOS hint rule).
 */
fun CallStartRequest.initialConnectionType(): ConnectionType =
    if (meetingType == MeetingType.SkypeZoom) ConnectionType.SkypeZoom else ConnectionType.InAppVoice

/**
 * The live call. Immutable; `CallCenter` replaces it on each change.
 *
 * [id] is a per-start counter so coroutines that resume after an await can detect a session that was
 * replaced or cleared. `call.ended` from the peer is modelled as `remote = Disconnected`, not a separate
 * status (RESEARCH Pattern 2), so a later remote join restores [CallStatus.Active].
 *
 * [confirmSent] turns true only when `PATCH confirm-connection` succeeded for this session (WR-03).
 */
data class CallSession(
    val id: Long,
    val bookingId: String,
    val peerId: String?,
    val peerName: String?,
    val peerAvatarUrl: String?,
    val topic: String?,
    val scheduledStart: Instant,
    val scheduledEnd: Instant?,
    val connectionType: ConnectionType,
    val mine: ConnState = ConnState.Waiting,
    val remote: ConnState = ConnState.Waiting,
    val status: CallStatus = CallStatus.Connecting,
    val externalJoinUrl: String? = null,
    val hasOpenedExternalLink: Boolean = false,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val isEndingCall: Boolean = false,
    val confirmSent: Boolean = false,
    val joinStartedAt: Instant? = null,
    val endedAt: Instant? = null,
)

/**
 * Stores the new connection states and re-derives [CallSession.status]. Ended is sticky (iOS
 * `guard callStatus != .ended`): once ended, nothing changes. [CallSession.endedAt] is stamped once, when
 * Ended is first derived.
 */
fun CallSession.withConnStates(
    mine: ConnState,
    remote: ConnState,
    now: Instant,
): CallSession {
    if (status == CallStatus.Ended) return this
    val derived = deriveCallStatus(mine, remote)
    return copy(
        mine = mine,
        remote = remote,
        status = derived,
        endedAt = if (derived == CallStatus.Ended) now else endedAt,
    )
}
