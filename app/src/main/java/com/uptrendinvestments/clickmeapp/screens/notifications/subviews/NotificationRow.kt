package com.uptrendinvestments.clickmeapp.screens.notifications.subviews

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.notifications.NotificationRowModel
import com.uptrendinvestments.clickmeapp.screens.notifications.NotificationTime
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Test tag on every notification row. */
const val NOTIFICATION_ROW_TAG = "notification_row"

/**
 * One notification: title, body and a relative timestamp. Rows never truncate. [onClick] is null for an inert row (no
 * destination), which gets no click modifier at all.
 */
@Composable
fun NotificationRow(
    row: NotificationRowModel,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val timeLabel = notificationTimeLabel(row.time)
    Surface(
        modifier = modifier.testTag(NOTIFICATION_ROW_TAG),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = row.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (row.body.isNotBlank()) {
                    Text(
                        text = row.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.extras.onSurfaceMuted,
                )
            }
        }
    }
}

/**
 * The relative timestamp of a row: "Just now", "{n} min ago", "{n} hr ago", "Yesterday, {time}" or "{MMM d}, {time}",
 * with the time in the device 12/24 hour format and the date pattern of the device locale.
 */
@Composable
fun notificationTimeLabel(time: NotificationTime): String {
    val timeFormat = rememberDeviceTimeFormat()
    val locale = LocalConfiguration.current.locales[0]
    return when (time) {
        NotificationTime.JustNow -> stringResource(R.string.notifications_time_just_now)
        is NotificationTime.MinutesAgo ->
            pluralStringResource(R.plurals.notifications_time_minutes, time.minutes, time.minutes)
        is NotificationTime.HoursAgo ->
            pluralStringResource(R.plurals.notifications_time_hours, time.hours, time.hours)
        is NotificationTime.Yesterday ->
            stringResource(
                R.string.notifications_time_yesterday,
                remember(time, timeFormat) { formatTime(timeFormat, time.time) },
            )
        is NotificationTime.Earlier ->
            stringResource(
                R.string.notifications_time_date,
                remember(time, locale) { formatDate(locale, time.date) },
                remember(time, timeFormat) { formatTime(timeFormat, time.time) },
            )
    }
}

/** [time] in the device time format. The wall-clock time is formatted in UTC so no zone or DST shift applies. */
private fun formatTime(
    timeFormat: java.text.DateFormat,
    time: LocalTime,
): String {
    val format = (timeFormat.clone() as java.text.DateFormat).apply { timeZone = TimeZone.getTimeZone("UTC") }
    val instant: Instant = LocalDate.of(2000, 1, 1).atTime(time).toInstant(ZoneOffset.UTC)
    return format.format(Date.from(instant))
}

private fun formatDate(
    locale: Locale,
    date: LocalDate,
): String = date.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMd"), locale))

@Preview
@Composable
private fun NotificationRowPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            NotificationRow(
                row =
                    NotificationRowModel(
                        id = "1",
                        category = "booking",
                        title = "Booking confirmed",
                        body = "Your session with Ada is confirmed.",
                        createdAt = Instant.EPOCH,
                        time = NotificationTime.MinutesAgo(2),
                        destination = null,
                    ),
                onClick = null,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
