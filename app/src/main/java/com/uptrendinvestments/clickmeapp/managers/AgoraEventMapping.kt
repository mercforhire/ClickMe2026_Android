package com.uptrendinvestments.clickmeapp.managers

import io.agora.rtc2.Constants

/**
 * Pure mapping of Agora callbacks to [EngineEvent]s, kept free of the SDK runtime so it is JVM-testable
 * (the [Constants] values are compile-time ints, so no native library loads).
 *
 * The iOS `uid == 0` "local disconnected" mapping never fires on a real callback and is replaced by this
 * connection-state mapping. RECONNECTING and connection lost are left to the SDK's own retry (about 20
 * minutes before FAILED). [EngineEvent.LocalFailed] yields the sticky Ended status; the user hangs up and
 * rejoins, which mints a fresh token (RESEARCH Pitfall 5).
 *
 * @param isLeaving true while we are tearing the channel down ourselves, so the resulting DISCONNECTED /
 * FAILED transitions are not reported as failures.
 * @return the event to publish, or null when the transition changes nothing.
 */
internal fun mapConnectionStateChanged(
    state: Int,
    reason: Int,
    isLeaving: Boolean,
): EngineEvent? =
    when (state) {
        Constants.CONNECTION_STATE_CONNECTED -> EngineEvent.LocalJoined
        Constants.CONNECTION_STATE_FAILED -> if (isLeaving) null else EngineEvent.LocalFailed
        Constants.CONNECTION_STATE_DISCONNECTED ->
            if (!isLeaving && reason in TERMINAL_DISCONNECT_REASONS) EngineEvent.LocalFailed else null
        else -> null
    }

/** A remote user left (quit, dropped, or became audience, which cannot occur in the COMMUNICATION profile). */
internal fun mapUserOffline(
    uid: Int,
    reason: Int,
): EngineEvent = EngineEvent.RemoteOffline(uid, reason)

private val TERMINAL_DISCONNECT_REASONS =
    setOf(
        Constants.CONNECTION_CHANGED_BANNED_BY_SERVER,
        Constants.CONNECTION_CHANGED_INVALID_TOKEN,
        Constants.CONNECTION_CHANGED_TOKEN_EXPIRED,
    )
