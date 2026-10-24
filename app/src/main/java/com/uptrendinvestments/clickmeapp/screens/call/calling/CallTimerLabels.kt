package com.uptrendinvestments.clickmeapp.screens.call.calling

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.CallTimerState
import com.uptrendinvestments.clickmeapp.managers.splitMinutesSeconds

/** `Starts in M:SS` before the scheduled start, `MM:SS` after it (minutes keep growing past 59). */
@Composable
fun callTimerText(state: CallTimerState): String {
    val parts = splitMinutesSeconds(state.elapsedSeconds)
    return if (state.isBeforeStart) {
        stringResource(R.string.call_timer_starts_in, parts.minutes, parts.seconds)
    } else {
        stringResource(R.string.call_timer_elapsed, parts.minutes, parts.seconds)
    }
}

/** `M:SS left` only inside the final stretch, otherwise null so the chip is hidden. */
@Composable
fun callRemainingChipText(state: CallTimerState): String? {
    val chip = state.remainingChip ?: return null
    return stringResource(R.string.call_timer_remaining_chip, chip.minutes, chip.seconds)
}

/**
 * Spoken form of the ticking timer ("2 minutes 5 seconds elapsed"). The timer is not a live region, so this is read
 * only when TalkBack focuses it.
 */
@Composable
fun callTimerSpokenDescription(state: CallTimerState): String {
    val parts = splitMinutesSeconds(state.elapsedSeconds)
    val minutes = pluralStringResource(R.plurals.call_timer_a11y_minutes, parts.minutes.toPluralCount(), parts.minutes)
    val seconds = pluralStringResource(R.plurals.call_timer_a11y_seconds, parts.seconds.toPluralCount(), parts.seconds)
    return if (state.isBeforeStart) {
        stringResource(R.string.call_timer_a11y_starts_in, minutes, seconds)
    } else {
        stringResource(R.string.call_timer_a11y_elapsed, minutes, seconds)
    }
}

/** Plural selection takes an Int; a call is far below Int range in minutes, so clamp rather than overflow. */
private fun Long.toPluralCount(): Int = coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
