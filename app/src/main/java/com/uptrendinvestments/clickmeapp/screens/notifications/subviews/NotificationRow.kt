package com.uptrendinvestments.clickmeapp.screens.notifications.subviews

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.notifications.NotificationRowModel
import com.uptrendinvestments.clickmeapp.screens.notifications.NotificationTime
import com.uptrendinvestments.clickmeapp.screens.notifications.parseNotificationBody
import com.uptrendinvestments.clickmeapp.screens.notifications.stripBoldMarkers
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

/** Test tag on the body text of a row. */
const val NOTIFICATION_BODY_TAG = "notification_body"

/** Test tag on the category tile of a row. */
const val NOTIFICATION_TILE_TAG = "notification_tile"

/** Backend categories that get the primary-tinted tile (iOS `iconIsPrimary`). */
private val interactiveCategories = setOf("booking", "session", "message", "review")

/** True for the four interactive categories, which use the primary-tinted tile. */
internal fun isInteractiveCategory(category: String): Boolean = category in interactiveCategories

/**
 * One notification: category tile, title, body and a relative timestamp. Rows never truncate. [onClick] is null for an
 * inert row (no destination), which gets no click modifier at all: no ripple, no role, no dead tap.
 */
@Composable
fun NotificationRow(
    row: NotificationRowModel,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val timeLabel = notificationTimeLabel(row.time)
    val plainBody = stripBoldMarkers(row.body)
    val description =
        if (plainBody.isBlank()) {
            stringResource(R.string.notifications_row_description_no_body, row.title, timeLabel)
        } else {
            stringResource(R.string.notifications_row_description, row.title, plainBody, timeLabel)
        }
    val clickLabel = stringResource(R.string.notifications_row_click_label)
    val tappable =
        if (onClick == null) {
            Modifier
        } else {
            Modifier
                .clickable(onClickLabel = clickLabel, role = Role.Button, onClick = onClick)
                .semantics(mergeDescendants = true) { contentDescription = description }
        }
    Surface(
        modifier = modifier.fillMaxWidth().testTag(NOTIFICATION_ROW_TAG).then(tappable),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.heightIn(min = 48.dp).padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            NotificationIconTile(row.category)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = row.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (row.body.isNotBlank()) NotificationBodyText(row.body)
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
 * The 48 dp circular category tile. The four interactive categories use a 12% primary fill with a primary glyph; the
 * rest (payout, account, anything unknown) are neutral. Decorative: no content description.
 */
@Composable
fun NotificationIconTile(
    category: String,
    modifier: Modifier = Modifier,
) {
    val interactive = isInteractiveCategory(category)
    val colors = MaterialTheme.colorScheme
    val tileModifier =
        if (interactive) {
            Modifier.background(colors.primary.copy(alpha = TILE_FILL_ALPHA), CircleShape)
        } else {
            Modifier
                .background(colors.surfaceContainerHigh, CircleShape)
                .border(1.dp, colors.outlineVariant, CircleShape)
        }
    Box(
        modifier = modifier.size(48.dp).testTag(NOTIFICATION_TILE_TAG).then(tileModifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = categoryIcon(category),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (interactive) colors.primary else colors.onSurfaceVariant,
        )
    }
}

private const val TILE_FILL_ALPHA = 0.12f

internal fun categoryIcon(category: String) =
    when (category) {
        "booking" -> Icons.Outlined.CalendarMonth
        "session" -> Icons.Outlined.Timer
        "message" -> Icons.Outlined.ChatBubbleOutline
        "review" -> Icons.Outlined.StarBorder
        "payout" -> Icons.Outlined.Payments
        "account" -> Icons.Outlined.Shield
        else -> Icons.Outlined.NotificationsNone
    }

/**
 * The row body with `**bold**` spans in weight 600 and `onSurface` (never accent, UI-SPEC A8). Any other markup, and
 * an unmatched marker, is shown literally by [parseNotificationBody].
 */
@Composable
fun NotificationBodyText(
    raw: String,
    modifier: Modifier = Modifier,
) {
    val boldColor = MaterialTheme.colorScheme.onSurface
    val text: AnnotatedString =
        remember(raw, boldColor) {
            buildAnnotatedString {
                parseNotificationBody(raw).forEach { span ->
                    if (span.bold) {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = boldColor)) { append(span.text) }
                    } else {
                        append(span.text)
                    }
                }
            }
        }
    Text(
        text = text,
        modifier = modifier.testTag(NOTIFICATION_BODY_TAG),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
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
                        body = "Your session with **Ada** is confirmed.",
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
