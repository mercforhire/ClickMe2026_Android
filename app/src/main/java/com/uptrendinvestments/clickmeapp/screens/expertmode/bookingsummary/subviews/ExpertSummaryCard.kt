package com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.BookingStatusBadge
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatShortDate
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.formatSessionTimeRange
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.EXPERT_SUMMARY_VIEW_PROFILE_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertSummaryDetails
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.SummaryFee
import com.uptrendinvestments.clickmeapp.screens.expertmode.clientDisplayName
import com.uptrendinvestments.clickmeapp.screens.expertmode.meetingTypeIcon
import com.uptrendinvestments.clickmeapp.screens.expertmode.meetingTypeLabel
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.topicDurationText
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import java.time.ZoneId
import java.util.Locale

private val ClientAvatarSize = 64.dp
private val MeetingIconSize = 20.dp
private val ProfileLinkMinHeight = 48.dp

/**
 * The expert Booking Summary card (UI-SPEC surface 6 block 2): a header with the client's avatar, name, short id and
 * status badge, an inset divider, then the Date, Time, Duration, Topic, Meeting Type and Consultation Fee rows.
 *
 * The client name and topic are untrusted server text and render as plain `Text`. There is no meeting-link row
 * (RESEARCH C11).
 *
 * The "View Profile" link under the client's name opens the read-only Client Profile through [onViewProfile] (iOS
 * `clientHeader`, Phase 16.4 D-13).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpertSummaryCard(
    details: ExpertSummaryDetails,
    onViewProfile: () -> Unit,
    zone: ZoneId,
    timeFormat: java.text.DateFormat,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(url = details.clientAvatarUrl, contentDescription = null, size = ClientAvatarSize)
                    Spacer(Modifier.width(Spacing.md))
                    Column {
                        Text(
                            text = clientDisplayName(details.clientName),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        TextButton(
                            onClick = onViewProfile,
                            contentPadding = PaddingValues(0.dp),
                            modifier =
                                Modifier
                                    .heightIn(
                                        min = ProfileLinkMinHeight,
                                    ).testTag(EXPERT_SUMMARY_VIEW_PROFILE_TAG),
                        ) {
                            Text(
                                text = stringResource(R.string.expert_summary_view_profile),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            text = stringResource(R.string.expert_summary_id, details.shortId),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.extras.onSurfaceMuted,
                        )
                    }
                }
                BookingStatusBadge(details.status)
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = Spacing.md),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                SummaryRow(stringResource(R.string.expert_summary_date)) {
                    SummaryValue(formatShortDate(details.start.atZone(zone).toLocalDate(), locale))
                }
                SummaryRow(stringResource(R.string.expert_summary_time)) {
                    SummaryValue(formatSessionTimeRange(details.start, details.end, zone, timeFormat))
                }
                SummaryRow(stringResource(R.string.expert_summary_duration)) {
                    SummaryValue(topicDurationText(details.durationMinutes))
                }
                details.topic?.let { topic ->
                    SummaryRow(stringResource(R.string.expert_summary_topic)) { SummaryValue(topic) }
                }
                SummaryRow(stringResource(R.string.expert_summary_meeting_type)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = meetingTypeIcon(details.meetingType),
                            contentDescription = null,
                            modifier = Modifier.size(MeetingIconSize),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.width(Spacing.sm))
                        SummaryValue(meetingTypeLabel(details.meetingType))
                    }
                }
                SummaryRow(stringResource(R.string.expert_summary_fee)) { FeeValue(details.fee) }
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: @Composable () -> Unit,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.extras.onSurfaceMuted,
        )
        value()
    }
}

@Composable
private fun SummaryValue(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun FeeValue(fee: SummaryFee) {
    val (text, color) =
        when (fee) {
            SummaryFee.Free -> stringResource(R.string.price_free) to MaterialTheme.colorScheme.primary
            is SummaryFee.Amount -> fee.text to MaterialTheme.colorScheme.onSurface
            is SummaryFee.ServerLabel -> fee.text to MaterialTheme.colorScheme.onSurface
        }
    Text(text = text, style = MaterialTheme.typography.titleMedium, color = color)
}
