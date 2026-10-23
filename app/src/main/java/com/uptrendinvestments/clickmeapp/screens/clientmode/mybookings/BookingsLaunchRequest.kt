package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import javax.inject.Inject
import javax.inject.Singleton

/** The My Bookings tabs. */
enum class BookingsTab { Upcoming, Past }

/** A one-time message My Bookings shows after a cancel. */
sealed interface BookingsNotice {
    /** The booking was cancelled; no money moved. */
    data object Cancelled : BookingsNotice

    /** The booking was cancelled and [amount] is being refunded. Already formatted by the Cancel ViewModel (D-07). */
    data class CancelledWithRefund(
        val amount: String,
    ) : BookingsNotice
}

/** "Select [tab] and show [notice]" for My Bookings. */
data class BookingsLaunch(
    val tab: BookingsTab,
    val notice: BookingsNotice? = null,
)

/**
 * One-shot hand-off from a finished cancel to My Bookings (D-07).
 *
 * It is a `@Singleton` and not a navigation result because both origins of a cancel, the Bookings stack and the
 * Explore stack, must behave the same after `resetAndSelect`, and My Bookings' ViewModel may not exist yet when the
 * cancel completes (RESEARCH Pattern 3, Open Question 4).
 *
 * Memory only, and consumed on read, so a launch applies exactly once. It carries no booking data, only a tab and a
 * formatted amount.
 */
@Singleton
class BookingsLaunchRequest
    @Inject
    constructor() {
        private val _pending = MutableStateFlow<BookingsLaunch?>(null)
        val pending: StateFlow<BookingsLaunch?> = _pending.asStateFlow()

        /** Stores [launch]; a launch that was never consumed is replaced. */
        fun post(launch: BookingsLaunch) {
            _pending.value = launch
        }

        /** Returns the pending launch and clears it, or null when there is none. */
        fun consume(): BookingsLaunch? = _pending.getAndUpdate { null }
    }
