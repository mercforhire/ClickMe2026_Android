package com.uptrendinvestments.clickmeapp.screens.settings.accountsecurity.subviews

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.PasswordStrengthMeter
import com.uptrendinvestments.clickmeapp.components.ValidatedField
import com.uptrendinvestments.clickmeapp.components.buttons.AsyncSaveButton
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.components.buttonstyles.DestructiveOutlinedButton
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.screens.settings.accountsecurity.AccountSecurityUiState
import com.uptrendinvestments.clickmeapp.screens.settings.accountsecurity.PasswordField
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Test tags of the two Account Security buttons. */
const val ACCOUNT_SECURITY_UPDATE_TAG = "account_security_update"
const val ACCOUNT_SECURITY_DELETE_TAG = "account_security_delete"

private val ButtonMinHeight = 56.dp

/** The change-password block: heading, rule line, three masked fields with the strength meter, Update Password. */
@Composable
fun ChangePasswordCard(
    state: AccountSecurityUiState,
    onCurrentChange: (String) -> Unit,
    onNewChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val editable = state.saveState == SaveState.Idle
    val focus = remember { PasswordField.entries.associateWith { FocusRequester() } }
    // Focus moves to the first field that newly fails (a wrong current password, a validation message). Typing in a
    // field only clears its message, so it never steals focus back (UI-SPEC "Submit errors and in-flight", D-07).
    var shownErrors by remember { mutableStateOf(emptySet<PasswordField>()) }
    LaunchedEffect(state.fieldErrors) {
        val failing = state.fieldErrors.keys
        val fresh = PasswordField.entries.firstOrNull { it in failing && it !in shownErrors }
        shownErrors = failing
        fresh?.let { focus.getValue(it).requestFocus() }
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.account_security_change_password),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.account_security_rule),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.md),
        )
        FieldLabel(R.string.account_security_current)
        ValidatedField(
            value = state.current,
            onValueChange = onCurrentChange,
            modifier = Modifier.focusRequester(focus.getValue(PasswordField.Current)),
            placeholder = stringResource(R.string.account_security_current_hint),
            leadingIcon = Icons.Outlined.Lock,
            error = state.fieldErrors[PasswordField.Current]?.asString(),
            isSecure = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            enabled = editable,
        )
        FieldLabel(R.string.account_security_new)
        ValidatedField(
            value = state.new,
            onValueChange = onNewChange,
            modifier = Modifier.focusRequester(focus.getValue(PasswordField.New)),
            placeholder = stringResource(R.string.account_security_new_hint),
            leadingIcon = Icons.Outlined.Lock,
            error = state.fieldErrors[PasswordField.New]?.asString(),
            isSecure = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            enabled = editable,
        )
        AnimatedVisibility(
            visible = state.new.isNotEmpty(),
            enter =
                fadeIn(tween(Motion.STRENGTH_METER_VISIBILITY_MS)) +
                    expandVertically(tween(Motion.STRENGTH_METER_VISIBILITY_MS)),
            exit =
                fadeOut(tween(Motion.STRENGTH_METER_VISIBILITY_MS)) +
                    shrinkVertically(tween(Motion.STRENGTH_METER_VISIBILITY_MS)),
        ) {
            PasswordStrengthMeter(strength = state.strength, modifier = Modifier.padding(bottom = Spacing.md))
        }
        FieldLabel(R.string.account_security_confirm)
        ValidatedField(
            value = state.confirm,
            onValueChange = onConfirmChange,
            modifier = Modifier.focusRequester(focus.getValue(PasswordField.Confirm)),
            placeholder = stringResource(R.string.account_security_confirm_hint),
            leadingIcon = Icons.Outlined.Lock,
            error = state.fieldErrors[PasswordField.Confirm]?.asString(),
            isSecure = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            enabled = editable,
        )
        AsyncSaveButton(
            title = stringResource(R.string.account_security_update),
            state = state.saveState,
            onClick = onSubmit,
            modifier = Modifier.heightIn(min = ButtonMinHeight).testTag(ACCOUNT_SECURITY_UPDATE_TAG),
        )
    }
}

/** The Delete Account block: a bordered card whose destructive button opens the deletion flow (D-04). */
@Composable
fun DeleteAccountEntryCard(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = stringResource(R.string.account_security_delete_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.account_security_delete_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.md),
            )
            DestructiveOutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth().heightIn(min = ButtonMinHeight).testTag(ACCOUNT_SECURITY_DELETE_TAG),
            ) {
                Text(text = stringResource(R.string.account_security_delete_button))
            }
        }
    }
}

/** Caption above a field. The wrapper clears semantics so TalkBack reads the field's own placeholder once. */
@Composable
private fun FieldLabel(
    @StringRes labelId: Int,
) {
    Box(modifier = Modifier.padding(bottom = Spacing.sm).clearAndSetSemantics {}) {
        Text(
            text = stringResource(labelId),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
