package com.uptrendinvestments.clickmeapp.managers

import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Port over the call foreground service. The service keeps the process alive and the microphone usable
 * while the app is backgrounded.
 */
interface CallForegroundController {
    /** Emits when the user taps the notification's End call action. */
    val hangUpRequests: Flow<Unit>

    /**
     * Starts the service and returns true only after it called `startForeground` with the microphone
     * type. Returns false when the platform refused to start it.
     */
    suspend fun startAndAwait(info: CallNotificationInfo): Boolean

    /** Stops the service. Idempotent. */
    fun stop()
}

/**
 * Display data for the ongoing-call notification. Deliberately has no token, channel or uid, so RTC
 * credentials cannot cross into the service intent (T-09-11).
 */
data class CallNotificationInfo(
    val bookingId: String,
    val peerName: String?,
    val scheduledStart: Instant,
)
