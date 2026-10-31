package com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.subviews

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButtonDefaults
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RESCHEDULE_SUBMIT_TAG

private val ButtonMinHeight = 56.dp
private val ProgressSize = 24.dp

/**
 * "Submit Reschedule Request". Disabled until a new time is chosen (state description "Choose a new time first").
 * While [submitting] it keeps the enabled fill, shows a spinner in the label's place with the state description "In
 * progress" and ignores taps, so the footprint never changes (same pattern as the Make a Booking button).
 */
@Composable
fun RescheduleSubmitButton(
    enabled: Boolean,
    submitting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val disabledState = stringResource(R.string.reschedule_start_submit_disabled_a11y)
    val progressState = stringResource(R.string.a11y_in_progress)
    PressScaleButton(
        onClick = { if (!submitting) onClick() },
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = ButtonMinHeight)
                .testTag(RESCHEDULE_SUBMIT_TAG)
                .semantics {
                    when {
                        submitting -> stateDescription = progressState
                        !enabled -> stateDescription = disabledState
                    }
                },
        enabled = enabled || submitting,
        colors = PressScaleButtonDefaults.filledColors(),
    ) {
        if (submitting) {
            CircularProgressIndicator(
                modifier = Modifier.size(ProgressSize),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text = stringResource(R.string.reschedule_start_submit))
        }
    }
}
