package com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.BookingStatusBadge
import com.uptrendinvestments.clickmeapp.components.labelText
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingClientDto
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingItem
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingSessionDto
import com.uptrendinvestments.clickmeapp.model.responses.ExpertStatusMeta
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatShortDate
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.formatSessionTimeRange
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.pastStatusDisplay
import com.uptrendinvestments.clickmeapp.screens.expertmode.clientDisplayName
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertPastRow
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** Test tag on an expert Past card. */
const val EXPERT_PAST_CARD_TAG = "expert_past_card"

private val PREVIEW_START: Instant = Instant.parse("2026-10-10T18:00:00Z")

/**
 * An expert Past booking card: the Phase 8 `PastBookingCard` with the client in place of the expert (UI-SPEC surface 5).
 * The whole card opens the expert Booking Summary. There are no earnings on the Past card (UI-SPEC A15) and no Message,
 * Private Note or Review control (Phase 12 and 13).
 *
 * @param row the booking and the badge it shows (Completed, Cancelled or Missed).
 * @param onOpen opens the summary with origin Bookings.
 * @param zone zone the date and times are shown in; defaults to the device zone.
 * @param timeFormat the device time format so the 12 or 24 hour setting applies.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpertPastCard(
    row: ExpertPastRow,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    timeFormat: DateFormat = rememberDeviceTimeFormat(),
    locale: Locale = LocalConfiguration.current.locales[0],
) {
    val item = row.item
    val clientName = clientDisplayName(item.client.name)
    val topic = item.topic ?: stringResource(R.string.explore_session_topic_fallback)
    val topicLine = stringResource(R.string.expert_bookings_topic, topic)
    val topicLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val topicValueColor = MaterialTheme.colorScheme.onSurface
    val topicText =
        buildAnnotatedString {
            // The label is whatever precedes the topic in the localized "Topic: %1$s" template.
            val valueStart = topicLine.lastIndexOf(topic).coerceAtLeast(0)
            withStyle(SpanStyle(color = topicLabelColor)) { append(topicLine.substring(0, valueStart)) }
            withStyle(SpanStyle(color = topicValueColor)) { append(topicLine.substring(valueStart)) }
        }
    val date = formatShortDate(item.startTime.atZone(zone).toLocalDate(), locale)
    val timeRange = formatSessionTimeRange(item.startTime, item.endTime, zone, timeFormat)
    val whenText = stringResource(R.string.my_bookings_date_time, date, timeRange)
    val description =
        stringResource(R.string.expert_bookings_past_a11y, clientName, topic, date, timeRange, row.status.labelText())
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(EXPERT_PAST_CARD_TAG)
                .clickable(
                    onClickLabel = stringResource(R.string.expert_bookings_open_details),
                    role = Role.Button,
                    onClick = onOpen,
                ).semantics { contentDescription = description },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = clientName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                BookingStatusBadge(status = row.status)
            }
            Text(text = topicText, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = whenText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.extras.onSurfaceMuted,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
    }
}

private fun previewRow(status: String): ExpertPastRow =
    ExpertPastRow(
        item =
            ExpertBookingItem(
                bookingId = "b1",
                client = ExpertBookingClientDto(id = "c1", name = "Ada Lovelace"),
                session =
                    ExpertBookingSessionDto(
                        topic = "Algorithms",
                        startTime = PREVIEW_START,
                        endTime = PREVIEW_START.plusSeconds(3600),
                    ),
                status = ExpertStatusMeta(code = status),
            ),
        status = requireNotNull(pastStatusDisplay(status)),
    )

@Preview(name = "ExpertPastCard")
@Composable
private fun ExpertPastCardPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.padding(Spacing.md)) {
                listOf("completed", "declined", "expired").forEach { status ->
                    ExpertPastCard(row = previewRow(status), onOpen = {})
                }
            }
        }
    }
}
