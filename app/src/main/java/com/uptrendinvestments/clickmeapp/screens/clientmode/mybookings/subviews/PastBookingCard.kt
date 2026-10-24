package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.subviews

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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.BookingStatusBadge
import com.uptrendinvestments.clickmeapp.model.responses.BookingExpertDto
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingItem
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatShortDate
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.PastRow
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.formatSessionTimeRange
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.pastStatusDisplay
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** Test tag on a Past card. */
const val PAST_BOOKING_CARD_TAG = "my_bookings_past_card"

private val PREVIEW_START: Instant = Instant.parse("2026-10-10T18:00:00Z")

/**
 * A Past booking card (iOS `PastBookingCard`). The whole card opens Booking Summary (D-12). Private Note and Leave
 * Review are not on the card: they arrive with Phase 13 (D-16).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PastBookingCard(
    row: PastRow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    timeFormat: DateFormat = rememberDeviceTimeFormat(),
    locale: Locale = LocalConfiguration.current.locales[0],
) {
    val item = row.item
    val topicLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val topicValueColor = MaterialTheme.colorScheme.onSurface
    val topicLabel = stringResource(R.string.my_bookings_past_topic_label)
    val topicValue = item.topic ?: stringResource(R.string.explore_session_topic_fallback)
    val topicText =
        buildAnnotatedString {
            withStyle(SpanStyle(color = topicLabelColor)) { append(topicLabel) }
            append(" ")
            withStyle(SpanStyle(color = topicValueColor)) { append(topicValue) }
        }
    val whenText =
        stringResource(
            R.string.my_bookings_date_time,
            formatShortDate(item.startTime.atZone(zone).toLocalDate(), locale),
            formatSessionTimeRange(item.startTime, item.endTime, zone, timeFormat),
        )
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(PAST_BOOKING_CARD_TAG)
                .clickable(
                    onClickLabel = stringResource(R.string.my_bookings_view_summary),
                    role = Role.Button,
                    onClick = onClick,
                ),
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.expert.fullName ?: stringResource(R.string.explore_expert_name_fallback),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                BookingStatusBadge(status = row.status)
            }
            Text(
                text = topicText,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = whenText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.extras.onSurfaceMuted,
            )
        }
    }
}

private fun previewRow(status: String): PastRow =
    PastRow(
        item =
            ClientBookingItem(
                bookingId = "b1",
                expert = BookingExpertDto(id = "e1", fullName = "Ada Lovelace"),
                topic = "Algorithms",
                startTime = PREVIEW_START,
                endTime = PREVIEW_START.plusSeconds(3600),
                status = status,
            ),
        status = requireNotNull(pastStatusDisplay(status)),
    )

@Preview(name = "PastBookingCard")
@Composable
private fun PastBookingCardPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.padding(Spacing.md)) {
                listOf("completed", "cancelled", "missed").forEach { status ->
                    PastBookingCard(row = previewRow(status), onClick = {})
                }
            }
        }
    }
}
