package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.PriceText
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingTopicUi
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatShortDate
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatSlotTime
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import com.uptrendinvestments.clickmeapp.util.PriceDisplay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val LabelColumnWidth = 120.dp

/**
 * The five summary rows: Topic, Date & Time, Duration, Meeting Type and Price. Values that are not known yet show
 * "—". Date & Time is the short date only until [selectedSlotStart] is chosen, then the date plus the device-format
 * time in [zone]. Each row is a merged "{label} {value}" node; the group is not a live region.
 */
@Composable
fun MakeABookingSummarySection(
    topic: BookingTopicUi?,
    selectedDate: LocalDate,
    selectedSlotStart: Instant?,
    meetingType: MeetingType,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    timeFormat: java.text.DateFormat = rememberDeviceTimeFormat(),
) {
    val locale = LocalConfiguration.current.locales[0]
    val placeholder = stringResource(R.string.make_a_booking_summary_placeholder)
    val dateTime =
        if (selectedSlotStart == null) {
            formatShortDate(selectedDate, locale)
        } else {
            stringResource(
                R.string.make_a_booking_summary_date_time_value,
                formatShortDate(selectedSlotStart.atZone(zone).toLocalDate(), locale),
                formatSlotTime(selectedSlotStart, zone, timeFormat),
            )
        }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SummaryTextRow(stringResource(R.string.make_a_booking_summary_topic), topic?.title ?: placeholder)
            SummaryTextRow(stringResource(R.string.make_a_booking_summary_date_time), dateTime)
            SummaryTextRow(
                label = stringResource(R.string.make_a_booking_summary_duration),
                value = topic?.let { stringResource(R.string.make_a_booking_duration, it.durationMins) } ?: placeholder,
            )
            SummaryTextRow(
                label = stringResource(R.string.make_a_booking_summary_meeting_type),
                value = meetingTypeLabel(meetingType),
            )
            SummaryRow(label = stringResource(R.string.make_a_booking_summary_price)) {
                if (topic == null) {
                    SummaryValueText(placeholder)
                } else {
                    PriceText(display = topic.price, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun SummaryTextRow(
    label: String,
    value: String,
) {
    SummaryRow(label) { SummaryValueText(value) }
}

@Composable
private fun SummaryRow(
    label: String,
    value: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(LabelColumnWidth),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.extras.onSurfaceMuted,
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopEnd) { value() }
    }
}

@Composable
private fun SummaryValueText(value: String) {
    Text(
        text = value,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.End,
    )
}

@Preview(name = "MakeABookingSummarySection")
@Composable
private fun MakeABookingSummarySectionPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier.padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                MakeABookingSummarySection(
                    topic = BookingTopicUi("t1", "Intro call", 30, null, PriceDisplay.Free, true),
                    selectedDate = LocalDate.of(2026, 10, 24),
                    selectedSlotStart = Instant.parse("2026-10-24T18:00:00Z"),
                    meetingType = MeetingType.InAppVoice,
                )
                MakeABookingSummarySection(
                    topic = null,
                    selectedDate = LocalDate.of(2026, 10, 24),
                    selectedSlotStart = null,
                    meetingType = MeetingType.SkypeZoom,
                )
            }
        }
    }
}
