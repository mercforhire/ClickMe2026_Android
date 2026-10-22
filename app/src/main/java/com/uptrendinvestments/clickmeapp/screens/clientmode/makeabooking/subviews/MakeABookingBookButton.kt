package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButtonDefaults
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookDisabledReason
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

const val BOOK_NOW_TAG = "make_a_booking_book_now"

private val ButtonMinHeight = 56.dp
private val ProgressSize = 24.dp

/**
 * Book Now. Enabled only when [disabledReason] is null. While [isSubmitting] the button keeps its enabled fill, shows
 * a progress indicator instead of the label and ignores taps (D-11). A paid topic ([BookDisabledReason.PaidTopic])
 * disables the button and shows the "coming soon" note below it (D-01).
 *
 * Phase 7 replaces the PaidTopic branch (the note) with the payment entry point in the same footprint.
 */
@Composable
fun MakeABookingBookButton(
    disabledReason: BookDisabledReason?,
    isSubmitting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state =
        when {
            isSubmitting -> stringResource(R.string.make_a_booking_state_booking)
            disabledReason == BookDisabledReason.NoSlot -> stringResource(R.string.make_a_booking_state_no_slot)
            disabledReason == BookDisabledReason.PaidTopic -> stringResource(R.string.make_a_booking_state_paid)
            disabledReason == BookDisabledReason.NoTopic -> stringResource(R.string.make_a_booking_state_no_topic)
            else -> null
        }
    Column(modifier = modifier.fillMaxWidth()) {
        PressScaleButton(
            onClick = { if (!isSubmitting) onClick() },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = ButtonMinHeight)
                    .testTag(BOOK_NOW_TAG)
                    .semantics { if (state != null) stateDescription = state },
            enabled = disabledReason == null,
            colors = PressScaleButtonDefaults.filledColors(),
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(ProgressSize),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(text = stringResource(R.string.make_a_booking_book_now))
            }
        }
        if (disabledReason == BookDisabledReason.PaidTopic) {
            Text(
                text = stringResource(R.string.make_a_booking_paid_note),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.md)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.extras.onSurfaceMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(name = "MakeABookingBookButton")
@Composable
private fun MakeABookingBookButtonPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier.padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MakeABookingBookButton(disabledReason = null, isSubmitting = false, onClick = {})
                MakeABookingBookButton(disabledReason = null, isSubmitting = true, onClick = {})
                MakeABookingBookButton(disabledReason = BookDisabledReason.NoSlot, isSubmitting = false, onClick = {})
                MakeABookingBookButton(
                    disabledReason = BookDisabledReason.PaidTopic,
                    isSubmitting = false,
                    onClick = {},
                )
            }
        }
    }
}
