package com.uptrendinvestments.clickmeapp.managers

import com.uptrendinvestments.clickmeapp.di.ApplicationScope
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.STATUS_COMPLETED
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.STATUS_PENDING_APPROVAL
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

/** Half-width of the client join window around a booking's start, in seconds (iOS parity, D-02). */
const val JOIN_WINDOW_SECONDS = 3600L

/** Cadence of [JoinClock]; chosen under D-03 discretion. */
const val JOIN_CLOCK_TICK_MS = 30_000L

private const val MILLIS_PER_SECOND = 1000L

/**
 * Whether the Join button is enabled for a booking. Port of iOS `UpcomingBooking.isWithinJoinWindow`:
 * never for `pending_approval`; for `completed` while [end] is still in the future (someone may have hung up
 * early and want to resume); otherwise within +-[JOIN_WINDOW_SECONDS] of [start], inclusive.
 *
 * The server's `can_join` (`ClientBookingDetail.canJoin`) is deliberately ignored because its window is too
 * tight (D-02). The backend join window (120 min before / after in code, 10 / 0 in docs, "tighten before
 * launch") still has the final say on `POST /join`, so this rule is a UX gate and never an authorization
 * decision (RESEARCH Pitfall 5, ASVS V4).
 */
fun isWithinJoinWindow(
    status: String,
    start: Instant,
    end: Instant,
    now: Instant,
): Boolean =
    when (status) {
        STATUS_PENDING_APPROVAL -> false
        STATUS_COMPLETED -> end.isAfter(now)
        else -> isStartingNow(start, now)
    }

/** True exactly when [start] is within +-[JOIN_WINDOW_SECONDS] of [now] (iOS `isStartingNow`). */
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
