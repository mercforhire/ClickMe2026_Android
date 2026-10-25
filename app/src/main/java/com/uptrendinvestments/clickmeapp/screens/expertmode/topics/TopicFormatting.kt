package com.uptrendinvestments.clickmeapp.screens.expertmode.topics

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.uptrendinvestments.clickmeapp.R

private const val MINUTES_PER_HOUR = 60

/**
 * Session duration text, ported from iOS `formatDuration`: "30 min", "1 hour", "1 hr 30 min", "2 hours".
 */
@Composable
fun topicDurationText(minutes: Int): String {
    val hours = minutes / MINUTES_PER_HOUR
    val mins = minutes % MINUTES_PER_HOUR
    return when {
        hours == 0 -> stringResource(R.string.topics_duration_minutes, mins)
        mins == 0 && hours == 1 -> stringResource(R.string.topics_duration_one_hour)
        mins == 0 -> stringResource(R.string.topics_duration_hours, hours)
        else -> stringResource(R.string.topics_duration_hours_minutes, hours, mins)
    }
}
