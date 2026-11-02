package com.uptrendinvestments.clickmeapp.managers

import com.uptrendinvestments.clickmeapp.di.ApplicationScope
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.JOINABLE_BOOKING_STATUSES
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.STATUS_COMPLETED
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * How far before a booking's start the client join window opens, and its minimum length after the start, in
 * seconds (D-02).
 */
const val JOIN_WINDOW_SECONDS = 3600L

/** Cadence of [JoinClock]; chosen under D-03 discretion. */
const val JOIN_CLOCK_TICK_MS = 30_000L

private const val MILLIS_PER_SECOND = 1000L

/**
 * Whether the Join button is enabled for a booking. Based on iOS `UpcomingBooking.isWithinJoinWindow`:
 * never for `pending_approval`; for `completed` while [end] is still in the future (someone may have hung up
 * early and want to resume); otherwise from [JOIN_WINDOW_SECONDS] before [start] until the later of
 * [start] + [JOIN_WINDOW_SECONDS] and [end], inclusive.
 *
 * Deliberate deviation from iOS (user decision on review finding WR-09, overriding the iOS-parity part of
 * D-02): iOS closes at [start] + 3600 s, which greys out Rejoin for the second half of any session longer
 * than an hour although CALL-END-SEMANTICS lets both parties rejoin for the whole window. iOS follow-up
 * logged.
 *
 * The server's join flag (the booking detail DTO field) is deliberately ignored because its window is too
 * tight (D-02). The backend join window (120 min before / after in code, 10 / 0 in docs, "tighten before
 * launch") still has the final say on `POST /join`, so this rule is a UX gate and never an authorization
 * decision (RESEARCH Pitfall 5, ASVS V4).
 *
 * The rule fails closed (Phase 16.5 D-03): only [JOINABLE_BOOKING_STATUSES] use the live window and
 * `completed` stays joinable until [end]; every other status, including `pending_approval`, `cancelled`,
 * `declined`, `missed`, `expired`, unknown and empty ones, is never joinable. `POST /bookings/:id/join` never
 * checks status (`call.controller.js`), and Payment History can open a terminal booking on Upcoming Booking
 * Details, so this client rule is the only status guard.
 */
fun isWithinJoinWindow(
    status: String,
    start: Instant,
    end: Instant,
    now: Instant,
): Boolean =
    when {
        status in JOINABLE_BOOKING_STATUSES -> isInLiveWindow(start, end, now)
        status == STATUS_COMPLETED -> end.isAfter(now)
        else -> false
    }

/**
 * Whether Upcoming Booking Details renders the Join card at all. A live booking outside its window keeps the
 * disabled card and its hint; a booking that will never happen (terminal, unknown, pending approval, or a
 * completed one whose [end] has passed) shows no card (Phase 16.5 D-03).
 */
fun showsJoinCard(
    status: String,
    end: Instant,
    now: Instant,
): Boolean = status in JOINABLE_BOOKING_STATUSES || (status == STATUS_COMPLETED && end.isAfter(now))

/** [now] in [start - JOIN_WINDOW_SECONDS, max(start + JOIN_WINDOW_SECONDS, end)], both edges inclusive. */
private fun isInLiveWindow(
    start: Instant,
    end: Instant,
    now: Instant,
): Boolean {
    val opens = start.minusSeconds(JOIN_WINDOW_SECONDS)
    val closes = maxOf(start.plusSeconds(JOIN_WINDOW_SECONDS), end)
    return !now.isBefore(opens) && !now.isAfter(closes)
}

/**
 * True exactly when [start] is within +-[JOIN_WINDOW_SECONDS] of [now] (iOS `isStartingNow`). Drives the
 * "Starting now" badge only; the Join button uses [isWithinJoinWindow].
 */
fun isStartingNow(
    start: Instant,
    now: Instant,
): Boolean = abs(start.toEpochMilli() - now.toEpochMilli()) <= JOIN_WINDOW_SECONDS * MILLIS_PER_SECOND

/**
 * App-scoped wall clock that ticks every [JOIN_CLOCK_TICK_MS] (D-03) so a visible Join button turns on
 * without leaving the screen. Join sites collect [now] with `collectAsStateWithLifecycle()`, so it only
 * ticks while a Join button is visible (`WhileSubscribed`).
 */
@Singleton
class JoinClock
    @Inject
    constructor(
        clock: Clock,
        @ApplicationScope scope: CoroutineScope,
    ) {
        val now: StateFlow<Instant> =
            flow {
                while (true) {
                    emit(clock.instant())
                    delay(JOIN_CLOCK_TICK_MS)
                }
            }.stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), clock.instant())

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
