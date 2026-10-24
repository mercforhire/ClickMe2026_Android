package com.uptrendinvestments.clickmeapp.managers

import kotlinx.coroutines.flow.StateFlow

/**
 * Blocks actions that must not happen during a live call. Port of iOS `CallCenter.attempt` and
 * `attemptModifyBooking`: when blocked, the guard dialog is queued and the method returns false, and the
 * caller aborts.
 */
interface CallGuard {
    /** True while a call is live. */
    val isCallActive: StateFlow<Boolean>

    /** Returns true when [action] may proceed; false (with a queued dialog) during a call. */
    fun attempt(action: RestrictedAction): Boolean

    /**
     * Same as [attempt] but for modifying the booking that is in progress. Only blocks when [bookingId]
     * matches the live call; other bookings pass through.
     */
    fun attemptModifyBooking(
        bookingId: String,
        action: RestrictedAction,
    ): Boolean
}

/**
 * What Join sites depend on, so screen ViewModels never touch `CallCenter` internals. Starting a call
 * while one is live shows "You're already on a call" (D-12).
 */
fun interface CallStarter {
    fun startCall(request: CallStartRequest)
}
