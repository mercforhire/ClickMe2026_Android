package com.uptrendinvestments.clickmeapp.screens.chat.reportandblock.subviews

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButtonDefaults
import com.uptrendinvestments.clickmeapp.components.safety.ReportDetailsField
import com.uptrendinvestments.clickmeapp.components.safety.ReportReasonGroup
import com.uptrendinvestments.clickmeapp.model.enums.UserReportReason
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Test tag of the Submit report button. */
const val REPORT_SUBMIT_TAG = "report_submit"

/** Test tag of the "Thanks, we'll review this." row. */
const val REPORT_THANKS_TAG = "report_thanks"

private val ButtonMinHeight = 56.dp
private val SpinnerSize = 24.dp
private val SpinnerStroke = 2.dp
private val CardBorderWidth = 1.dp

/**
 * The Report card (UI-SPEC surface 3): headline, body, the six-reason group, the optional details and Submit report.
 * After the server accepted the report the button becomes the "Thanks" row and the inputs lock; [showSubmit] is false
 * on a self-thread, where no action is offered.
 */
@Composable
fun ReportCard(
    reason: UserReportReason?,
    details: String,
    submitting: Boolean,
    submitted: Boolean,
    showSubmit: Boolean,
    onSelectReason: (UserReportReason) -> Unit,
    onDetailsChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val inputsEnabled = !submitting && !submitted
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(CardBorderWidth, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = stringResource(R.string.report_card_title),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.report_card_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ReportReasonGroup(selected = reason, enabled = inputsEnabled, onSelect = onSelectReason)
            ReportDetailsField(value = details, enabled = inputsEnabled, onValueChange = onDetailsChange)
            if (showSubmit) {
                SubmitArea(
                    canSubmit = reason != null,
                    submitting = submitting,
                    submitted = submitted,
                    onSubmit = onSubmit,
                )
            }
        }
    }
}

@Composable
private fun SubmitArea(
    canSubmit: Boolean,
    submitting: Boolean,
    submitted: Boolean,
    onSubmit: () -> Unit,
) {
    Crossfade(
        targetState = submitted,
        animationSpec = tween(Motion.FIELD_ERROR_DURATION_MS),
        label = "reportSubmitArea",
    ) { done ->
        if (done) {
            ThanksRow()
        } else {
            SubmitButton(canSubmit = canSubmit, submitting = submitting, onSubmit = onSubmit)
        }
    }
}

@Composable
private fun ThanksRow() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = ButtonMinHeight)
                .testTag(REPORT_THANKS_TAG)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            modifier = Modifier.size(SpinnerSize),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.report_thanks),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SubmitButton(
    canSubmit: Boolean,
    submitting: Boolean,
    onSubmit: () -> Unit,
) {
    val choose = stringResource(R.string.report_choose_reason)
    val submittingText = stringResource(R.string.report_submitting)
    PressScaleButton(
        onClick = onSubmit,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = ButtonMinHeight)
                .testTag(REPORT_SUBMIT_TAG)
                .semantics {
                    if (submitting) {
                        stateDescription = submittingText
                    } else if (!canSubmit) {
                        stateDescription = choose
                    }
                },
        enabled = canSubmit && !submitting,
        colors = PressScaleButtonDefaults.filledColors(),
    ) {
        if (submitting) {
            CircularProgressIndicator(
                modifier = Modifier.size(SpinnerSize),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = SpinnerStroke,
            )
        } else {
            Text(text = stringResource(R.string.report_submit), style = MaterialTheme.typography.labelLarge)
        }
    }
}
