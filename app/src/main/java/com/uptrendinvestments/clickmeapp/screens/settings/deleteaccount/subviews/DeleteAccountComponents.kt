package com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.ValidatedField
import com.uptrendinvestments.clickmeapp.components.buttonstyles.DestructiveOutlinedButton
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount.DeleteAccountReason
import com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount.DeleteStep
import com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount.FEEDBACK_COUNTER_FROM
import com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount.FEEDBACK_MAX
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

/** Test tag on "Keep my account" (both forms). */
const val DELETE_KEEP_TAG = "delete_keep"

/** Test tag on the Warning form's "Delete Account". */
const val DELETE_CONTINUE_TAG = "delete_continue"

/** Test tag on the Reason form's final "Delete Account". */
const val DELETE_CONFIRM_TAG = "delete_confirm"

/** Test tag on the comments field. */
const val DELETE_COMMENTS_TAG = "delete_comments"

/** Test tag prefix of a reason row; the wire value follows. */
const val DELETE_REASON_TAG_PREFIX = "delete_reason_"

private val WarningIconSize = 32.dp
private val ButtonMinHeight = 56.dp
private val SpinnerSize = 24.dp
private val SpinnerStroke = 2.dp
private val RowMinHeight = 56.dp
private val CommentsMinHeight = 120.dp
private const val DISABLED_ALPHA = 0.38f
private const val COMMENTS_MIN_LINES = 4
private const val COMMENTS_MAX_LINES = 8

/** Left-aligned warning shared by both forms: icon, headline that wraps, and the permanence notice. */
@Composable
fun DeleteAccountWarningHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = null,
            modifier = Modifier.size(WarningIconSize),
            tint = MaterialTheme.colorScheme.tertiary,
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = stringResource(R.string.delete_account_headline),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.delete_account_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * "Help us improve" with the four reason rows, none preselected. The rows form a `selectableGroup` of radio buttons;
 * the whole row is the target (56dp). Disabled while the deletion request is in flight.
 */
@Composable
fun DeleteAccountReasonSection(
    selected: DeleteAccountReason?,
    onSelect: (DeleteAccountReason) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.delete_account_help_us),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.delete_account_reason_caption),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.sm))
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            DeleteAccountReason.entries.forEach { reason ->
                ReasonRow(
                    reason = reason,
                    selected = reason == selected,
                    enabled = enabled,
                    onSelect = { onSelect(reason) },
                )
            }
        }
    }
}

@Composable
private fun ReasonRow(
    reason: DeleteAccountReason,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    val border =
        if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = RowMinHeight)
                .testTag(DELETE_REASON_TAG_PREFIX + reason.wire)
                .background(MaterialTheme.colorScheme.surfaceContainerLow, shape)
                .border(border, shape)
                .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect)
                .padding(horizontal = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Spacer(Modifier.size(Spacing.md))
        Text(
            text = stringResource(reason.label),
            modifier = Modifier.padding(vertical = Spacing.sm),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Optional multi-line comments. The ViewModel clamps to [FEEDBACK_MAX], so this only shows [value]. The counter
 * appears from [FEEDBACK_COUNTER_FROM] and turns `tertiary` at the cap.
 */
@Composable
fun DeleteAccountCommentsField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.delete_account_comments_label),
            style = MaterialTheme.typography.labelLarge,
            color = colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.sm))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = CommentsMinHeight).testTag(DELETE_COMMENTS_TAG),
            enabled = enabled,
            textStyle = MaterialTheme.typography.bodyLarge,
            placeholder = {
                Text(
                    text = stringResource(R.string.delete_account_comments_placeholder),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            keyboardOptions =
                KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default),
            minLines = COMMENTS_MIN_LINES,
            maxLines = COMMENTS_MAX_LINES,
            shape = MaterialTheme.shapes.medium,
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colorScheme.onSurface,
                    unfocusedTextColor = colorScheme.onSurface,
                    disabledTextColor = colorScheme.onSurface.copy(alpha = DISABLED_ALPHA),
                    focusedContainerColor = colorScheme.surfaceContainer,
                    unfocusedContainerColor = colorScheme.surfaceContainer,
                    disabledContainerColor = colorScheme.surfaceContainer,
                    focusedBorderColor = colorScheme.primary,
                    unfocusedBorderColor = colorScheme.outline,
                    disabledBorderColor = colorScheme.outline.copy(alpha = DISABLED_ALPHA),
                    cursorColor = colorScheme.primary,
                    focusedPlaceholderColor = MaterialTheme.extras.onSurfaceMuted,
                    unfocusedPlaceholderColor = MaterialTheme.extras.onSurfaceMuted,
                    disabledPlaceholderColor = MaterialTheme.extras.onSurfaceMuted.copy(alpha = DISABLED_ALPHA),
                ),
        )
        if (value.length >= FEEDBACK_COUNTER_FROM) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs), contentAlignment = Alignment.CenterEnd) {
                Text(
                    text = stringResource(R.string.delete_account_comments_counter, value.length, FEEDBACK_MAX),
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (value.length >= FEEDBACK_MAX) colorScheme.tertiary else MaterialTheme.extras.onSurfaceMuted,
                )
            }
        }
    }
}

/** The masked "Confirm your password" field with its inline error. */
@Composable
fun DeleteAccountPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    error: FieldError?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.delete_account_password_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.sm))
        ValidatedField(
            value = value,
            onValueChange = onValueChange,
            placeholder = stringResource(R.string.delete_account_password_placeholder),
            leadingIcon = Icons.Outlined.Lock,
            error = error?.asString(),
            isSecure = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )
    }
}

/**
 * Pinned footer shared by both forms: `surface` with a top hairline, above the navigation bar and the keyboard.
 *
 * Warning: filled "Keep my account" over the outlined "Delete Account". Reason: the outlined "Delete Account"
 * (enabled by [canConfirm], a spinner while [isDeleting]) over "Keep my account". Buttons grow with the font scale.
 */
@Composable
fun DeleteAccountBottomBar(
    step: DeleteStep,
    canConfirm: Boolean,
    isDeleting: Boolean,
    onKeep: () -> Unit,
    onContinue: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .navigationBarsPadding()
                .imePadding(),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            val keep: @Composable () -> Unit = {
                PressScaleButton(
                    onClick = onKeep,
                    modifier = Modifier.fillMaxWidth().heightIn(min = ButtonMinHeight).testTag(DELETE_KEEP_TAG),
                    enabled = !isDeleting,
                ) {
                    Text(stringResource(R.string.delete_account_keep))
                }
            }
            when (step) {
                DeleteStep.Warning -> {
                    keep()
                    DestructiveOutlinedButton(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth().heightIn(min = ButtonMinHeight).testTag(DELETE_CONTINUE_TAG),
                    ) {
                        Text(stringResource(R.string.delete_account_delete))
                    }
                }

                DeleteStep.Reason -> {
                    DestructiveOutlinedButton(
                        onClick = onConfirm,
                        modifier = Modifier.fillMaxWidth().heightIn(min = ButtonMinHeight).testTag(DELETE_CONFIRM_TAG),
                        enabled = canConfirm,
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(SpinnerSize),
                                color = MaterialTheme.colorScheme.error,
                                strokeWidth = SpinnerStroke,
                            )
                        } else {
                            Text(stringResource(R.string.delete_account_delete))
                        }
                    }
                    keep()
                }
            }
        }
    }
}
