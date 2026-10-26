package com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventBusy
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatShortDate
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.formatSessionTimeRange
import com.uptrendinvestments.clickmeapp.screens.expertmode.clientDisplayName
import com.uptrendinvestments.clickmeapp.screens.expertmode.meetingTypeIcon
import com.uptrendinvestments.clickmeapp.screens.expertmode.meetingTypeLabel
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** Test tag on the expired banner. */
const val REQUEST_EXPIRED_BANNER_TAG = "request_expired_banner"

private val BannerIcon = 24.dp
private val HeaderAvatar = 72.dp
private val RowIcon = 20.dp
private val CardBorderWidth = 1.dp
private const val BANNER_FILL_ALPHA = 0.10f
private const val BANNER_BORDER_ALPHA = 0.45f
private const val NAME_MAX_LINES = 2

/** "This request has expired" above the details card (C3). The screen shows no accept or decline controls then. */
@Composable
fun RequestExpiredBanner(modifier: Modifier = Modifier) {
    val tertiary = MaterialTheme.colorScheme.tertiary
    Surface(
        modifier = modifier.fillMaxWidth().testTag(REQUEST_EXPIRED_BANNER_TAG),
        shape = MaterialTheme.shapes.medium,
        color = tertiary.copy(alpha = BANNER_FILL_ALPHA),
        border = BorderStroke(CardBorderWidth, tertiary.copy(alpha = BANNER_BORDER_ALPHA)),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                imageVector = Icons.Outlined.EventBusy,
                contentDescription = null,
                modifier = Modifier.size(BannerIcon),
                tint = tertiary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = stringResource(R.string.request_decision_expired_title),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.request_decision_expired_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The client's avatar and name. No View Profile control (Phase 12). */
@Composable
fun RequestClientHeader(
    name: String?,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Avatar(url = avatarUrl, contentDescription = null, size = HeaderAvatar)
        Text(
            text = clientDisplayName(name),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = NAME_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Date and time, topic and meeting type in one card, rows separated by dividers. A blank topic hides its row. */
@Composable
fun RequestDetailsCard(
    start: Instant,
    end: Instant,
    topic: String?,
    meetingType: String?,
    zone: ZoneId,
    timeFormat: java.text.DateFormat,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val whenText =
        stringResource(
            R.string.request_decision_date_time_value,
            formatShortDate(start.atZone(zone).toLocalDate(), locale),
            formatSessionTimeRange(start, end, zone, timeFormat),
        )
    RequestCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            DetailRow(
                label = stringResource(R.string.request_decision_date_time),
                value = whenText,
                icon = Icons.Outlined.CalendarMonth,
            )
            if (topic != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DetailRow(label = stringResource(R.string.request_decision_topic), value = topic, icon = null)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            DetailRow(
                label = stringResource(R.string.request_decision_meeting_type),
                value = meetingTypeLabel(meetingType),
                icon = meetingTypeIcon(meetingType),
            )
        }
    }
}

/** The client's message, only shown when it is non-blank. Plain selectable text: no links, no HTML. */
@Composable
fun RequestMessageCard(
    message: String,
    modifier: Modifier = Modifier,
) {
    RequestCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = stringResource(R.string.request_decision_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.extras.onSurfaceMuted,
            )
            SelectionContainer {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun RequestCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(CardBorderWidth, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) { content() }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    icon: ImageVector?,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.extras.onSurfaceMuted,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(RowIcon),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(Spacing.sm))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
