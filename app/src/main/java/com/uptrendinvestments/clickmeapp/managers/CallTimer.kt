package com.uptrendinvestments.clickmeapp.managers

import java.time.Instant

/** Remaining time at or under which the call enters the amber "ending soon" stretch (same as iOS). */
const val FINAL_STRETCH_SECONDS = 300L

/**
 * How long after the scheduled end `CallCenter` lets a call run before cutting it off (CALL-07). Replaces the iOS
 * five-minute TEMP test value.
 */
const val OVERTIME_CUTOFF_SECONDS = 3600L

/** A whole-minute and remaining-second split. Minutes are uncapped, there is no hours field. */
data class MinutesSeconds(
    val minutes: Long,
    val seconds: Long,
)

/** Splits [totalSeconds] by its absolute value; callers decide what the sign means. */
fun splitMinutesSeconds(totalSeconds: Long): MinutesSeconds {
    val abs = kotlin.math.abs(totalSeconds)
    return MinutesSeconds(abs / SECONDS_PER_MINUTE, abs % SECONDS_PER_MINUTE)
}

private const val SECONDS_PER_MINUTE = 60L

/**
 * Pure snapshot of the call timer at one instant. [elapsedSeconds] is negative before the scheduled start;
 * [remainingSeconds] is null when the scheduled end is unknown and is zero or negative once past it.
 */
data class CallTimerState(
    val elapsedSeconds: Long,
    val remainingSeconds: Long?,
) {
    val isBeforeStart: Boolean get() = elapsedSeconds < 0

    /** Between 1 second and [FINAL_STRETCH_SECONDS] left. Drives the ending-soon banner and chip. */
    val isInFinalStretch: Boolean get() = remainingSeconds != null && remainingSeconds in 1..FINAL_STRETCH_SECONDS

    /** The scheduled end has been reached; the call itself continues until the overtime cutoff. */
    val isPastScheduledEnd: Boolean get() = remainingSeconds != null && remainingSeconds <= 0

    val isWarning: Boolean get() = isInFinalStretch || isPastScheduledEnd

    /** The remaining time, only while in the final stretch, so the UI can hide the chip otherwise. */
    val remainingChip: MinutesSeconds?
        get() = remainingSeconds?.takeIf { isInFinalStretch }?.let(::splitMinutesSeconds)
}

/**
 * Builds the timer state at [now]. The timer is anchored to [scheduledStart], not to when the user joined, so both
 * participants see the same number (iOS `recomputeElapsed`). Division truncates toward zero, matching iOS
 * `Int(TimeInterval)`. Callers pass the injected `Clock`'s instant; this never reads the system clock.
 */
fun callTimerState(
    scheduledStart: Instant,
    scheduledEnd: Instant?,
    now: Instant,
): CallTimerState {
    val elapsed = (now.toEpochMilli() - scheduledStart.toEpochMilli()) / MILLIS_PER_SECOND
    val remaining = scheduledEnd?.let { (it.toEpochMilli() - now.toEpochMilli()) / MILLIS_PER_SECOND }
    return CallTimerState(elapsedSeconds = elapsed, remainingSeconds = remaining)
}

private const val MILLIS_PER_SECOND = 1000L

/**
 * The instant at which a call still running is ended locally: [scheduledEnd] plus [OVERTIME_CUTOFF_SECONDS] (CALL-07).
 * Enforced by `CallCenter` (plan 09-11), never by a UI ticker. 3600 s replaces the iOS five-minute TEMP value.
 */
fun overtimeCutoff(scheduledEnd: Instant): Instant = scheduledEnd.plusSeconds(OVERTIME_CUTOFF_SECONDS)
