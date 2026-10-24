package com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.CancellationReason
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

/** Test tag prefix on each reason row; the suffix is the [CancellationReason] entry name. */
const val CANCEL_REASON_ROW_TAG_PREFIX = "cancel_reason_"

/** Test tag on the optional comment field. */
const val CANCEL_COMMENT_TAG = "cancel_comment"

private val RowMinHeight = 56.dp
private val CommentMinHeight = 112.dp
private val ButtonMinHeight = 56.dp
private val ArrowSize = 24.dp

/** The text shown for this reason. It always equals [CancellationReason.wireLabel], the value sent to the server (D-05). */
@Composable
fun CancellationReason.label(): String =
    stringResource(
        when (this) {
            CancellationReason.ChangeOfPlans -> R.string.cancel_booking_reason_change_of_plans
            CancellationReason.FoundAnotherExpert -> R.string.cancel_booking_reason_found_another_expert
            CancellationReason.ScheduleConflict -> R.string.cancel_booking_reason_schedule_conflict
            CancellationReason.PersonalEmergency -> R.string.cancel_booking_reason_personal_emergency
            CancellationReason.Other -> R.string.cancel_booking_reason_other
        },
    )

/**
 * Step 1 of the cancel flow (D-08): why the client is cancelling. A radio list of the five iOS reasons, an optional
 * comment (the screen's ViewModel caps it at 450 characters) and Continue.
 *
 * Stateless: [reason] and [comment] are the whole values and every edit goes to the callbacks. Focus moves to the
 * headline when the step appears so TalkBack announces the new step (UI-SPEC Accessibility).
 */
@Composable
fun CancellationReasonStep(
    reason: CancellationReason,
    comment: String,
    onSelectReason: (CancellationReason) -> Unit,
    onCommentChange: (String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val headlineFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { headlineFocus.requestFocus() }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.md, end = Spacing.md, top = Spacing.lg, bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = stringResource(R.string.cancel_booking_headline),
                modifier =
                    Modifier
                        .focusRequester(headlineFocus)
                        .focusable()
                        .semantics { heading() },
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.cancel_booking_subhead),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ReasonCard(selected = reason, onSelect = onSelectReason)
        CommentField(comment = comment, onChange = onCommentChange)
        PressScaleButton(
            onClick = {
                keyboard?.hide()
                onContinue()
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = ButtonMinHeight),
        ) {
            Text(stringResource(R.string.cancel_booking_continue))
            Spacer(Modifier.width(Spacing.sm))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(ArrowSize),
            )
        }
    }
}

@Composable
private fun ReasonCard(
    selected: CancellationReason,
    onSelect: (CancellationReason) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.selectableGroup()) {
            CancellationReason.entries.forEachIndexed { index, entry ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = Spacing.md),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
                ReasonRow(entry = entry, isSelected = entry == selected, onSelect = onSelect)
            }
        }
    }
}

@Composable
private fun ReasonRow(
    entry: CancellationReason,
    isSelected: Boolean,
    onSelect: (CancellationReason) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = RowMinHeight)
                .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(entry) })
                .padding(horizontal = Spacing.md)
                .testTag(CANCEL_REASON_ROW_TAG_PREFIX + entry.name),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = entry.label(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        RadioButton(
            selected = isSelected,
            onClick = null,
            colors =
                RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.outline,
                ),
        )
    }
}

@Composable
private fun CommentField(
    comment: String,
    onChange: (String) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = stringResource(R.string.cancel_booking_comment_label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.extras.onSurfaceMuted,
        )
        OutlinedTextField(
            value = comment,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = CommentMinHeight).testTag(CANCEL_COMMENT_TAG),
            textStyle = MaterialTheme.typography.bodyLarge,
            placeholder = {
                Text(
                    text = stringResource(R.string.cancel_booking_comment_placeholder),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.extras.onSurfaceMuted,
                )
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            minLines = 4,
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
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Text(
                text = stringResource(R.string.cancel_booking_comment_counter, comment.length),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.extras.onSurfaceMuted,
            )
        }
    }
}

@Preview(name = "CancellationReasonStep")
@Composable
private fun CancellationReasonStepPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            CancellationReasonStep(
                reason = CancellationReason.ChangeOfPlans,
                comment = "",
                onSelectReason = {},
                onCommentChange = {},
                onContinue = {},
            )
        }
    }
}
