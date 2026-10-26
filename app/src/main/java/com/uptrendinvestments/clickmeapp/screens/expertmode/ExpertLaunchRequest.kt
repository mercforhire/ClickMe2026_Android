package com.uptrendinvestments.clickmeapp.screens.expertmode

import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsLaunch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A memory-only, consume-on-read slot for one hand-off. A value that was never consumed is replaced by a later
 * [post], and [consume] returns the value once and clears it, so a hand-off applies exactly once.
 */
class OneShotSlot<T : Any> {
    private val _pending = MutableStateFlow<T?>(null)
    val pending: StateFlow<T?> = _pending.asStateFlow()

    /** Stores [value]; a value that was never consumed is replaced. */
    fun post(value: T) {
        _pending.value = value
    }

    /** Returns the pending value and clears it, or null when there is none. */
    fun consume(): T? = _pending.getAndUpdate { null }
}

/** A one-time message Incoming Requests shows after a decline. */
enum class IncomingRequestsNotice {
    Declined,
}

/** "Reload Incoming Requests" and optionally show [notice]. A launch always means reload. */
data class IncomingRequestsLaunch(
    val notice: IncomingRequestsNotice? = null,
)

/** "Reload the Dashboard" after Back to Home (D-05). */
data object DashboardLaunch

/**
 * One-shot hand-offs from a finishing expert flow to the screen that must react (a decline, Back to Home, a finished
 * cancel).
 *
 * It is a `@Singleton` and not a navigation result because the target ViewModel may not exist yet when the producer
 * finishes (the same reason as `BookingsLaunchRequest`). There is one slot per consumer so a screen never consumes
 * another screen's hand-off.
 *
 * Memory only: it carries no booking data beyond a formatted refund amount inside [BookingsLaunch].
 */
@Singleton
class ExpertLaunchRequest
    @Inject
    constructor() {
        val incomingRequests = OneShotSlot<IncomingRequestsLaunch>()
        val dashboard = OneShotSlot<DashboardLaunch>()
        val bookings = OneShotSlot<BookingsLaunch>()
    }
