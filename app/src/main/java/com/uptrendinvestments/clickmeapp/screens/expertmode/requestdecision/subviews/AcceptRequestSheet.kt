package com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButtonDefaults
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.capNotes
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.ACCEPT_MESSAGE_MAX_CHARS
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.NOTE_COUNTER_FROM
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

/** Test tags of the Accept sheet. */
const val ACCEPT_MESSAGE_FIELD_TAG = "accept_message_field"
const val ACCEPT_SHEET_CONFIRM_TAG = "accept_sheet_confirm"
const val ACCEPT_SHEET_CANCEL_TAG = "accept_sheet_cancel"

private val ButtonMinHeight = 56.dp
private val FieldMinHeight = 112.dp
private val SpinnerSize = 24.dp
private val SpinnerStroke = 2.dp
private const val FIELD_MAX_LINES = 6

/**
 * "Confirm Booking" sheet (platform swap of the iOS sheet to `ModalBottomSheet`, CLAUDE.md). While [isSubmitting] the
 * sheet cannot be dismissed by swipe, scrim or Back (T-11-36); [onDismiss] is how Cancel and every allowed dismissal
 * close it. The message text lives in the caller so it survives rotation and a failed accept.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcceptRequestSheet(
    message: String,
    onMessageChange: (String) -> Unit,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val submitting by rememberUpdatedState(isSubmitting)
    ModalBottomSheet(
        onDismissRequest = { if (!submitting) onDismiss() },
        sheetState =
            rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
                confirmValueChange = { !submitting || it != SheetValue.Hidden },
            ),
        shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        AcceptRequestSheetContent(
            message = message,
            onMessageChange = onMessageChange,
            isSubmitting = isSubmitting,
            onCancel = onDismiss,
            onConfirm = onConfirm,
        )
    }
}

/** Stateless sheet body, separate from the modal window so tests can render it directly. */
@Composable
fun AcceptRequestSheetContent(
    message: String,
    onMessageChange: (String) -> Unit,
    isSubmitting: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = stringResource(R.string.accept_sheet_title),
            modifier = Modifier.fillMaxWidth().semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        DecisionNoteField(
            label = stringResource(R.string.accept_sheet_label),
            placeholder = stringResource(R.string.accept_sheet_placeholder),
            value = message,
            maxChars = ACCEPT_MESSAGE_MAX_CHARS,
            counterRes = R.string.accept_sheet_counter,
            onValueChange = onMessageChange,
            testTag = ACCEPT_MESSAGE_FIELD_TAG,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f).heightIn(min = ButtonMinHeight).testTag(ACCEPT_SHEET_CANCEL_TAG),
                enabled = !isSubmitting,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
            ) {
                Text(text = stringResource(R.string.accept_sheet_cancel), style = MaterialTheme.typography.labelLarge)
            }
            val accepting = stringResource(R.string.accept_sheet_accepting)
            PressScaleButton(
                onClick = onConfirm,
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = ButtonMinHeight)
                        .testTag(ACCEPT_SHEET_CONFIRM_TAG)
                        .semantics { if (isSubmitting) stateDescription = accepting },
                enabled = !isSubmitting,
                colors = PressScaleButtonDefaults.filledColors(),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(SpinnerSize),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = SpinnerStroke,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.accept_sheet_confirm),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

/**
 * The multi-line text box of the Accept and Decline sheets: [label] above, a field capped at [maxChars] on input
 * (UTF-16 length, never splitting a surrogate pair), and a "{n}/1000" counter from [NOTE_COUNTER_FROM] characters.
 */
@Composable
internal fun DecisionNoteField(
    label: String,
    placeholder: String,
    value: String,
    maxChars: Int,
    @StringRes counterRes: Int,
    onValueChange: (String) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.extras.onSurfaceMuted,
        )
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(capNotes(it, maxChars)) },
            modifier = Modifier.fillMaxWidth().heightIn(min = FieldMinHeight).testTag(testTag),
            textStyle = MaterialTheme.typography.bodyLarge,
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.extras.onSurfaceMuted,
                )
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            maxLines = FIELD_MAX_LINES,
            shape = MaterialTheme.shapes.medium,
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colorScheme.onSurface,
                    unfocusedTextColor = colorScheme.onSurface,
                    focusedContainerColor = colorScheme.surfaceContainer,
                    unfocusedContainerColor = colorScheme.surfaceContainer,
                    focusedBorderColor = colorScheme.primary,
                    unfocusedBorderColor = colorScheme.outlineVariant,
                    cursorColor = colorScheme.primary,
                ),
        )
        if (value.length >= NOTE_COUNTER_FROM) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Text(
                    text = stringResource(counterRes, value.length),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.extras.onSurfaceMuted,
                )
            }
        }
    }
}
