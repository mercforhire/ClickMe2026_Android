package com.uptrendinvestments.clickmeapp.components.safety

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButtonDefaults
import com.uptrendinvestments.clickmeapp.model.enums.UserReportReason
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Test tags of the profile report sheet buttons. */
const val REPORT_SHEET_SUBMIT_TAG = "report_sheet_submit"
const val REPORT_SHEET_CANCEL_TAG = "report_sheet_cancel"

private val ButtonMinHeight = 56.dp
private val ButtonBorderWidth = 1.dp
private val SpinnerSize = 24.dp
private val SpinnerStroke = 2.dp
private const val STACK_BELOW_WIDTH_DP = 320
private const val STACK_AT_FONT_SCALE = 1.5f

/**
 * "Report {name}" bottom sheet for the Expert Profile (`ModalBottomSheet` swap for the iOS `ReportUserSheet`). The
 * reason, the details and the open state live in the caller so they survive rotation. [onDismiss] closes the sheet
 * (Cancel and every allowed dismissal).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportUserSheet(
    name: String,
    reason: UserReportReason?,
    details: String,
    submitting: Boolean,
    onSelectReason: (UserReportReason) -> Unit,
    onDetailsChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val inFlight by rememberUpdatedState(submitting)
    ModalBottomSheet(
        onDismissRequest = { if (!inFlight) onDismiss() },
        sheetState =
            rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
                confirmValueChange = { !inFlight || it != SheetValue.Hidden },
            ),
        shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        ReportUserSheetContent(
            name = name,
            reason = reason,
            details = details,
            submitting = submitting,
            onSelectReason = onSelectReason,
            onDetailsChange = onDetailsChange,
            onSubmit = onSubmit,
            onCancel = onDismiss,
        )
    }
}

/** Stateless sheet body, separate from the modal window so tests can render it directly. */
@Composable
fun ReportUserSheetContent(
    name: String,
    reason: UserReportReason?,
    details: String,
    submitting: Boolean,
    onSelectReason: (UserReportReason) -> Unit,
    onDetailsChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = stringResource(R.string.report_sheet_title, name),
                modifier = Modifier.fillMaxWidth().semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.report_sheet_subtitle, name),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        ReportReasonGroup(selected = reason, enabled = !submitting, onSelect = onSelectReason)
        ReportDetailsField(value = details, enabled = !submitting, onValueChange = onDetailsChange)
        val canSubmit = reason != null
        if (stacked()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                SubmitButton(canSubmit, submitting, onSubmit, Modifier.fillMaxWidth())
                CancelButton(submitting, onCancel, Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                CancelButton(submitting, onCancel, Modifier.weight(1f))
                SubmitButton(canSubmit, submitting, onSubmit, Modifier.weight(1f))
            }
        }
    }
}

/** Buttons stack (Submit first) below 320dp width or from font scale 1.5, like the request action footer. */
@Composable
private fun stacked(): Boolean =
    LocalConfiguration.current.screenWidthDp < STACK_BELOW_WIDTH_DP ||
        LocalDensity.current.fontScale >= STACK_AT_FONT_SCALE

@Composable
private fun CancelButton(
    submitting: Boolean,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onCancel,
        modifier = modifier.heightIn(min = ButtonMinHeight).testTag(REPORT_SHEET_CANCEL_TAG),
        enabled = !submitting,
        shape = CircleShape,
        border = BorderStroke(ButtonBorderWidth, MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
    ) {
        Text(text = stringResource(R.string.report_sheet_cancel), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SubmitButton(
    canSubmit: Boolean,
    submitting: Boolean,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val choose = stringResource(R.string.report_choose_reason)
    val submittingText = stringResource(R.string.report_submitting)
    PressScaleButton(
        onClick = onSubmit,
        modifier =
            modifier
                .heightIn(min = ButtonMinHeight)
                .testTag(REPORT_SHEET_SUBMIT_TAG)
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
