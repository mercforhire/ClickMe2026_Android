package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButtonDefaults
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookCtaAction
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookDisabledReason
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

const val BOOK_NOW_TAG = "make_a_booking_book_now"

private val ButtonMinHeight = 56.dp
private val ProgressSize = 24.dp

/**
 * The booking CTA (D-08). Enabled only when [disabledReason] is null. The label cross-fades between "Book Now" and
 * "Add a Payment Method" ([ctaAction]) inside the same footprint, so the button never changes size. While
 * [isSubmitting] the button keeps its enabled fill, shows a progress indicator instead of the label and ignores
 * taps (D-11). Each disabled reason, and the in-flight state ("In progress"), is announced through the button's
 * `stateDescription`.
 */
@Composable
fun MakeABookingBookButton(
    disabledReason: BookDisabledReason?,
    ctaAction: BookCtaAction,
    isSubmitting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state =
        when {
            isSubmitting -> stringResource(R.string.a11y_in_progress)
            disabledReason == BookDisabledReason.NoSlot -> stringResource(R.string.make_a_booking_state_no_slot)
            disabledReason == BookDisabledReason.CheckingPaymentMethods ->
                stringResource(R.string.make_a_booking_state_checking_payment_methods)
            disabledReason == BookDisabledReason.NoTopic -> stringResource(R.string.make_a_booking_state_no_topic)
            else -> null
        }
    PressScaleButton(
        onClick = { if (!isSubmitting) onClick() },
        modifier =
            modifier
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
            Crossfade(
                targetState = ctaAction,
                animationSpec = tween(Motion.CONTROL_STATE_DURATION_MS),
                label = "bookCtaLabel",
            ) { action ->
                Text(
                    text =
                        stringResource(
                            when (action) {
                                BookCtaAction.Book -> R.string.make_a_booking_book_now
                                BookCtaAction.AddPaymentMethod -> R.string.make_a_booking_add_payment_method
                            },
                        ),
                )
            }
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
                MakeABookingBookButton(
                    disabledReason = null,
                    ctaAction = BookCtaAction.Book,
                    isSubmitting = false,
                    onClick = {},
                )
                MakeABookingBookButton(
                    disabledReason = null,
                    ctaAction = BookCtaAction.AddPaymentMethod,
                    isSubmitting = false,
                    onClick = {},
                )
                MakeABookingBookButton(
                    disabledReason = null,
                    ctaAction = BookCtaAction.Book,
                    isSubmitting = true,
                    onClick = {},
                )
                MakeABookingBookButton(
                    disabledReason = BookDisabledReason.NoSlot,
                    ctaAction = BookCtaAction.Book,
                    isSubmitting = false,
                    onClick = {},
                )
                MakeABookingBookButton(
                    disabledReason = BookDisabledReason.CheckingPaymentMethods,
                    ctaAction = BookCtaAction.Book,
                    isSubmitting = false,
                    onClick = {},
                )
            }
        }
    }
}
