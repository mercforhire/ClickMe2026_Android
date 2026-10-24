package com.uptrendinvestments.clickmeapp.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingStatusDisplay
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingStatusLabel
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.StatusTone
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.humanizeStatus
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.pastStatusDisplay
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

const val BOOKING_STATUS_BADGE_TAG = "booking_status_badge"

/** Scheme colour for a [StatusTone]. Shared by the My Bookings pills and the badge. */
@Composable
fun StatusTone.color(): Color =
    when (this) {
        StatusTone.Primary -> MaterialTheme.colorScheme.primary
        StatusTone.Tertiary -> MaterialTheme.colorScheme.tertiary
        StatusTone.Error -> MaterialTheme.colorScheme.error
        StatusTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    }

/** The visible wording of a status; [BookingStatusLabel.Other] shows the humanized raw server string. */
@Composable
fun BookingStatusDisplay.labelText(): String =
    when (label) {
        BookingStatusLabel.Completed -> stringResource(R.string.booking_status_completed)
        BookingStatusLabel.Cancelled -> stringResource(R.string.booking_status_cancelled)
        BookingStatusLabel.Missed -> stringResource(R.string.booking_status_missed)
        BookingStatusLabel.Confirmed -> stringResource(R.string.booking_status_confirmed)
        BookingStatusLabel.PendingApproval -> stringResource(R.string.booking_status_pending_approval)
        BookingStatusLabel.NewTimeProposed -> stringResource(R.string.booking_status_new_time_proposed)
        BookingStatusLabel.InProgress -> stringResource(R.string.booking_status_in_progress)
        BookingStatusLabel.Other -> humanizeStatus(raw)
    }

/**
 * Status pill. The tone colours the outline and text, and the wording carries the meaning, so a status is never shown
 * by colour alone (UI-SPEC accessibility).
 */
@Composable
fun BookingStatusBadge(
    status: BookingStatusDisplay,
    modifier: Modifier = Modifier,
) {
    val tone = status.tone.color()
    Surface(
        modifier = modifier.testTag(BOOKING_STATUS_BADGE_TAG),
        shape = CircleShape,
        color = tone.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, tone.copy(alpha = 0.6f)),
    ) {
        Text(
            text = status.labelText(),
            style = MaterialTheme.typography.labelMedium,
            color = tone,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        )
    }
}

@Preview(name = "BookingStatusBadge", showBackground = false)
@Composable
private fun BookingStatusBadgePreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                listOf("completed", "cancelled", "missed").forEach { raw ->
                    pastStatusDisplay(raw)?.let { BookingStatusBadge(status = it) }
                }
            }
        }
    }
}
