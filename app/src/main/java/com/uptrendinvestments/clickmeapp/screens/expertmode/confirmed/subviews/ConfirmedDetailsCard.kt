package com.uptrendinvestments.clickmeapp.screens.expertmode.confirmed.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButtonDefaults
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatShortDate
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatSlotTime
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.expertmode.clientDisplayName
import com.uptrendinvestments.clickmeapp.screens.expertmode.confirmed.BookingConfirmedKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.meetingTypeIcon
import com.uptrendinvestments.clickmeapp.screens.expertmode.meetingTypeLabel
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.topicDurationText
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** Test tag on the Add to Calendar button. */
internal const val CONFIRMED_ADD_CALENDAR_TAG = "confirmed_add_calendar"

/** Test tag on the Back to Home button. */
internal const val CONFIRMED_BACK_HOME_TAG = "confirmed_back_home"

private const val MILLIS_PER_MINUTE = 60_000L
private val DetailIconSize = 20.dp
private val AvatarSize = 40.dp
private val ActionMinHeight = 56.dp
private val ActionIconSize = 24.dp

/**
 * "Booking Confirmed!" heading and the body naming the client (iOS named the expert, Android names the client because
 * the viewer is the expert). The client name is untrusted text: it is a format argument, rendered as plain text. The
 * block is a polite live region so the result is announced.
 */
@Composable
fun ConfirmedHeadline(
    clientName: String?,
    start: Instant,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    timeFormat: java.text.DateFormat = rememberDeviceTimeFormat(),
    locale: Locale = Locale.getDefault(),
) {
    val body =
        stringResource(
            R.string.booking_confirmed_body,
            clientDisplayName(clientName),
            formatShortDate(start.atZone(zone).toLocalDate(), locale),
            formatSlotTime(start, zone, timeFormat),
        )
    Column(
        modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.booking_confirmed_headline),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Client, topic, "{date}, {time} ({duration})" and meeting type, all from [key]; nothing is fetched. */
@Composable
fun ConfirmedDetailsCard(
    key: BookingConfirmedKey,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    timeFormat: java.text.DateFormat = rememberDeviceTimeFormat(),
    locale: Locale = Locale.getDefault(),
) {
    val start = Instant.ofEpochMilli(key.startMillis)
    val minutes = ((key.endMillis - key.startMillis) / MILLIS_PER_MINUTE).toInt()
    val whenText =
        stringResource(
            R.string.booking_confirmed_when,
            formatShortDate(start.atZone(zone).toLocalDate(), locale),
            formatSlotTime(start, zone, timeFormat),
            topicDurationText(minutes),
        )
    val topic = key.topic?.trim()?.takeIf { it.isNotEmpty() }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(url = key.clientAvatarUrl, contentDescription = null, size = AvatarSize)
                Spacer(Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = clientDisplayName(key.clientName),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (topic != null) {
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            text = topic,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.extras.onSurfaceMuted,
                        )
                    }
                }
            }
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            DetailRow(icon = Icons.Outlined.CalendarMonth, text = whenText)
            DetailRow(icon = meetingTypeIcon(key.meetingType), text = meetingTypeLabel(key.meetingType))
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    text: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(DetailIconSize),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** "Add to Calendar" (filled) and "Back to Home" (tonal). [enabled] is false until the entry animation ends. */
@Composable
fun ConfirmedActions(
    enabled: Boolean,
    onAddToCalendar: () -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        PressScaleButton(
            onClick = onAddToCalendar,
            modifier = Modifier.fillMaxWidth().heightIn(min = ActionMinHeight).testTag(CONFIRMED_ADD_CALENDAR_TAG),
            enabled = enabled,
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(ActionIconSize),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(stringResource(R.string.booking_confirmed_add_to_calendar))
        }
        PressScaleButton(
            onClick = onBackToHome,
            modifier = Modifier.fillMaxWidth().heightIn(min = ActionMinHeight).testTag(CONFIRMED_BACK_HOME_TAG),
            enabled = enabled,
            colors = PressScaleButtonDefaults.tonalColors(),
        ) { Text(stringResource(R.string.booking_confirmed_back_home)) }
    }
}
