package com.uptrendinvestments.clickmeapp.screens.clientmode.requestsent.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatShortDate
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatSlotTime
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.clientmode.requestsent.BookingRequestSentDetails
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import java.time.ZoneId

private const val EXPERT_NAME_MAX_LINES = 2

/**
 * "Booking Details" card: Expert, Topic, Date & Time and the "Awaiting Approval" status (D-14). No payment
 * lifecycle card: Phase 7 adds it for paid bookings.
 */
@Composable
fun BookingRequestSentDetailsCard(
    details: BookingRequestSentDetails,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    timeFormat: java.text.DateFormat = rememberDeviceTimeFormat(),
) {
    val locale = LocalConfiguration.current.locales[0]
    val labelExpert = stringResource(R.string.request_sent_label_expert)
    val labelTopic = stringResource(R.string.request_sent_label_topic)
    val labelDateTime = stringResource(R.string.request_sent_label_date_time)
    val labelStatus = stringResource(R.string.request_sent_label_status)
    val statusText = stringResource(R.string.request_sent_status_pending)
    val expertName = details.expertName ?: labelExpert
    val dateTime =
        stringResource(
            R.string.request_sent_date_time_value,
            formatShortDate(details.start.atZone(zone).toLocalDate(), locale),
            formatSlotTime(details.start, zone, timeFormat),
            formatSlotTime(details.end, zone, timeFormat),
        )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            Text(
                text = stringResource(R.string.request_sent_details_header),
                modifier = Modifier.padding(Spacing.md),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.extras.onSurfaceMuted,
            )
            RowDivider()
            DetailRow(
                a11y = stringResource(R.string.request_sent_row_a11y, labelExpert, expertName),
            ) {
                Avatar(url = details.expertAvatarUrl, contentDescription = null, size = 40.dp)
                Spacer(Modifier.width(Spacing.md))
                LabeledValue(label = labelExpert, modifier = Modifier.weight(1f)) {
                    Text(
                        text = expertName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = EXPERT_NAME_MAX_LINES,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            RowDivider()
            DetailRow(
                a11y = stringResource(R.string.request_sent_row_a11y, labelTopic, details.topicTitle),
            ) {
                LabeledValue(label = labelTopic, modifier = Modifier.weight(1f)) {
                    Text(
                        text = details.topicTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.width(Spacing.md))
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    DecorativeIcon(Icons.Outlined.Layers, MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            RowDivider()
            DetailRow(
                a11y = stringResource(R.string.request_sent_row_a11y, labelDateTime, dateTime),
            ) {
                LabeledValue(label = labelDateTime, modifier = Modifier.weight(1f)) {
                    Text(
                        text = dateTime,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.width(Spacing.md))
                DecorativeIcon(Icons.Outlined.CalendarMonth, MaterialTheme.extras.onSurfaceMuted)
            }
            RowDivider()
            DetailRow(
                a11y = stringResource(R.string.request_sent_row_a11y, labelStatus, statusText),
            ) {
                Text(
                    text = labelStatus,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.extras.onSurfaceMuted,
                )
                StatusPill(text = statusText)
            }
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = Spacing.md),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun DetailRow(
    a11y: String,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
                .semantics(mergeDescendants = true) { contentDescription = a11y },
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun LabeledValue(
    label: String,
    modifier: Modifier = Modifier,
    value: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.extras.onSurfaceMuted,
        )
        Spacer(Modifier.height(Spacing.xs))
        value()
    }
}

@Composable
private fun DecorativeIcon(
    imageVector: ImageVector,
    tint: Color,
) {
    Icon(imageVector = imageVector, contentDescription = null, modifier = Modifier.size(24.dp), tint = tint)
}

@Composable
private fun StatusPill(text: String) {
    Row(
        modifier =
            Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
