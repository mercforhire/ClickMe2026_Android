package com.uptrendinvestments.clickmeapp.managers

import com.uptrendinvestments.clickmeapp.di.ApplicationScope
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.JOINABLE_BOOKING_STATUSES
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.STATUS_COMPLETED
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.STATUS_CONFIRMED
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.STATUS_IN_PROGRESS
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
 * Half-width of the "Starting now" badge, in seconds: [isStartingNow] is true within +-this of the start. It no
 * longer drives the Join button, which uses [JOIN_LEAD_SECONDS], [JOIN_MIN_FIRST_SPAN_SECONDS] and
 * [JOIN_RECONNECT_GRACE_SECONDS].
 */
const val JOIN_WINDOW_SECONDS = 3600L

/**
 * How long before a booking's start Join opens, in seconds. Backend `JOIN_LEAD_MS` (`src/services/agora.js`); iOS
 * `JoinWindow.lead`.
 */
const val JOIN_LEAD_SECONDS = 3600L

/**
 * Minimum length of the first-join window after the start, in seconds, whatever the scheduled end. Backend
 * `FIRST_JOIN_MIN_SPAN_MS`; iOS `JoinWindow.minFirstJoinSpan`.
 */
const val JOIN_MIN_FIRST_SPAN_SECONDS = 3600L

/**
 * How long after the scheduled end a party who already joined may reconnect, in seconds. Backend `JOIN_GRACE_MS`;
 * iOS `JoinWindow.reconnectGrace`.
 */
const val JOIN_RECONNECT_GRACE_SECONDS = 3600L

/** Cadence of [JoinClock]; chosen under D-03 discretion. */
const val JOIN_CLOCK_TICK_MS = 30_000L

private const val MILLIS_PER_SECOND = 1000L

/**
 * The statuses whose Join can be enabled: `confirmed`, `in_progress` and `completed`. It equals the backend
 * `JOINABLE_BOOKING_STATUSES` at 1d18aac (`src/utils/bookingStatusSets.js`) and iOS `JoinWindow.joinableStatuses`.
 *
 * It is deliberately separate from the Android membership set [JOINABLE_BOOKING_STATUSES] in `TodaysSessions.kt`.
 * That set has the same name as the backend one but different members: it keeps `pending_reschedule`, so its strips
 * and cards still render a disabled Join (iOS 02-14, user decision 2026-11-10). This set decides enabling only.
 */
val JOIN_ENABLED_BOOKING_STATUSES: Set<String> = setOf(STATUS_CONFIRMED, STATUS_IN_PROGRESS, STATUS_COMPLETED)

/**
 * Whether the Join button is enabled for a booking. Three tiers, every edge inclusive:
 * 1. Only [JOIN_ENABLED_BOOKING_STATUSES] can be enabled. Terminal, unknown, empty and `pending_approval`
 *    statuses fail closed, and so does `pending_reschedule`, which shows a disabled Join.
 * 2. Join opens [JOIN_LEAD_SECONDS] before [start] and, for a first join, stays enabled through the later of
 *    [end] and [start] + [JOIN_MIN_FIRST_SPAN_SECONDS], whatever [serverCanJoin] says.
 * 3. After that edge, a reconnect stays enabled through [end] + [JOIN_RECONNECT_GRACE_SECONDS] only when
 *    [serverCanJoin] is true.
 *
 * [serverCanJoin] is the payload's `can_join` when it carries one (client booking detail, expert `actions`) and
 * null otherwise (client home feed, client bookings list). Null and false both close the reconnect tier. The flag
 * is never consulted before the start lead or inside the first-join tier.
 *
 * Sources: iOS `ClickMe2026/Utils/JoinWindow.swift` (iOS plans 02-04 and 02-14), and backend `evaluateJoinWindow`
 * (`src/services/agora.js`) and `JOINABLE_BOOKING_STATUSES` (`src/utils/bookingStatusSets.js`) at 1d18aac. This
 * replaces the Phase 9 D-02 rule, which ignored the flag, enabled `pending_reschedule` and enabled `completed`
 * only until [end]. The old WR-09 deviation from iOS is gone because iOS and the backend now close the first
 * join at the later of [end] and [start] + 60 min as well.
 *
 * The result is a display gate and never an authorization. The server keeps the final say on
 * `POST /bookings/:id/join`, which since 1d18aac also rejects non-joinable statuses with 403 SESSION_NOT_ACTIVE
 * (`call.controller.js`).
 */
fun isWithinJoinWindow(
    status: String,
    start: Instant,
    end: Instant,
    now: Instant,
    serverCanJoin: Boolean?,
): Boolean {
    if (status !in JOIN_ENABLED_BOOKING_STATUSES) return false
    if (now.isBefore(start.minusSeconds(JOIN_LEAD_SECONDS))) return false
    val firstJoinCloses = maxOf(end, start.plusSeconds(JOIN_MIN_FIRST_SPAN_SECONDS))
    if (!now.isAfter(firstJoinCloses)) return true
    return serverCanJoin == true && !now.isAfter(end.plusSeconds(JOIN_RECONNECT_GRACE_SECONDS))
}

/**
 * Whether a booking detail screen renders the Join button at all. It is the shared visibility rule for client
 * Upcoming Booking Details and the expert Booking Summary, and it equals iOS `ExpertJoinVisibility.shows`
 * (`ClickMe2026/Utils/ExpertJoinVisibility.swift`): [JOINABLE_BOOKING_STATUSES] (iOS `alwaysShownStatuses`) show
 * at every instant, with the button disabled outside the window, and every other status shows only while
 * [isWithinJoinWindow] is true.
 *
 * A completed booking therefore shows Join exactly while it can be enabled, including the server-approved
 * reconnect tier through [end] + [JOIN_RECONNECT_GRACE_SECONDS] (quick 261123-rdc, the follow-up to 261123-qgu).
 * A visible completed Join is never disabled, and it leaves at the window edge once the caller re-evaluates it
 * against the live [JoinClock].
 *
 * Client deviation: iOS client details shows the card for every status but `pending_approval`
 * (`JoinWindow.showsJoinButton`), disabled where it cannot be used. Android keeps Phase 16.5 D-03 instead, so a
 * booking that can no longer be joined (terminal, unknown, pending approval, or completed past its window)
 * shows no card.
 */
fun showsJoinCard(
    status: String,
    start: Instant,
    end: Instant,
    now: Instant,
    serverCanJoin: Boolean?,
): Boolean =
    status in JOINABLE_BOOKING_STATUSES ||
        isWithinJoinWindow(
            status = status,
            start = start,
            end = end,
            now = now,
            serverCanJoin = serverCanJoin,
        )

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
