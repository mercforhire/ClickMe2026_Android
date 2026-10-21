package com.uptrendinvestments.clickmeapp.screens.login.setnewpassword

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.PasswordStrengthMeter
import com.uptrendinvestments.clickmeapp.components.ValidatedField
import com.uptrendinvestments.clickmeapp.components.buttons.AsyncSaveButton
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

private val BadgeSize = 24.dp

/**
 * Hilt-aware Set New Password step (iOS `LoginResetPasswordView`). [email] comes from Forgot Password.
 * [onPasswordChanged] is called once with the email after the 600 ms "Password Updated!" hold.
 */
@Composable
fun SetNewPasswordScreen(
    email: String,
    onPasswordChanged: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: SetNewPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val passwordChanged by viewModel.passwordChanged.collectAsStateWithLifecycle()
    val focusRequest by viewModel.focusRequest.collectAsStateWithLifecycle()

    LaunchedEffect(email) { viewModel.start(email) }
    LaunchedEffect(passwordChanged) {
        passwordChanged?.let {
            onPasswordChanged(it)
            viewModel.onNavigated()
        }
    }

    SetNewPasswordContent(
        state = state,
        onCodeChange = viewModel::onCodeChange,
        onNewPasswordChange = viewModel::onNewPasswordChange,
        onConfirmChange = viewModel::onConfirmChange,
        onSubmit = viewModel::submit,
        onBack = onBack,
        onDismissDialog = viewModel::dismissDialog,
        focusRequest = focusRequest,
        onFocusRequestConsumed = viewModel::onFocusRequestConsumed,
    )
}

/** Stateless Set New Password form: scrolling fields with the submit button pinned above the keyboard. */
@Composable
fun SetNewPasswordContent(
    state: SetNewPasswordUiState,
    onCodeChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequest: ResetField? = null,
    onFocusRequestConsumed: () -> Unit = {},
) {
    val codeFocus = remember { FocusRequester() }
    val newPasswordFocus = remember { FocusRequester() }
    val confirmFocus = remember { FocusRequester() }
    LaunchedEffect(focusRequest) {
        when (focusRequest) {
            ResetField.Code -> codeFocus.requestFocus()
            ResetField.NewPassword -> newPasswordFocus.requestFocus()
            ResetField.Confirm -> confirmFocus.requestFocus()
            null -> return@LaunchedEffect
        }
        onFocusRequestConsumed()
    }

    val editable = state.saveState == SaveState.Idle
    // Leaving while the reset is in flight or in the "Saved" hold would cancel viewModelScope and drop the
    // success: the password is changed and the code used, but Password Changed never shows. Swallow Back.
    BackHandler(enabled = !editable) {}

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            AuthTopAppBar(title = stringResource(R.string.reset_title), onBack = onBack, backEnabled = editable)
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .imePadding(),
        ) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.lg),
            ) {
                FieldLabel(R.string.reset_code_label)
                ValidatedField(
                    value = state.code,
                    onValueChange = onCodeChange,
                    placeholder = stringResource(R.string.reset_code_placeholder),
                    leadingIcon = Icons.Outlined.Pin,
                    error = state.codeError?.asString(),
                    modifier = Modifier.focusRequester(codeFocus),
                    keyboardOptions =
                        KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    enabled = editable,
                    contentType = ContentType.SmsOtpCode,
                )
                FieldLabel(R.string.reset_new_password_label)
                ValidatedField(
                    value = state.newPassword,
                    onValueChange = onNewPasswordChange,
                    placeholder = stringResource(R.string.reset_new_password_placeholder),
                    leadingIcon = Icons.Outlined.Lock,
                    error = state.newPasswordError?.asString(),
                    modifier = Modifier.focusRequester(newPasswordFocus),
                    isSecure = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    enabled = editable,
                )
                AnimatedVisibility(
                    visible = state.newPassword.isNotEmpty(),
                    enter =
                        fadeIn(tween(Motion.STRENGTH_METER_VISIBILITY_MS)) +
                            expandVertically(tween(Motion.STRENGTH_METER_VISIBILITY_MS)),
                    exit =
                        fadeOut(tween(Motion.STRENGTH_METER_VISIBILITY_MS)) +
                            shrinkVertically(tween(Motion.STRENGTH_METER_VISIBILITY_MS)),
                ) {
                    PasswordStrengthMeter(
                        strength = state.strength,
                        modifier = Modifier.padding(bottom = Spacing.md),
                    )
                }
                FieldLabel(R.string.reset_confirm_label)
                ValidatedField(
                    value = state.confirmPassword,
                    onValueChange = onConfirmChange,
                    placeholder = stringResource(R.string.reset_confirm_placeholder),
                    leadingIcon = Icons.Outlined.Lock,
                    error = state.confirmError?.asString(),
                    modifier = Modifier.focusRequester(confirmFocus),
                    isSecure = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                    enabled = editable,
                    trailingBadge =
                        if (state.passwordsMatch) {
                            {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = stringResource(R.string.a11y_passwords_match),
                                    modifier = Modifier.size(BadgeSize),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        } else {
                            null
                        },
                )
                Spacer(Modifier.height(Spacing.xl))
            }
            AsyncSaveButton(
                title = stringResource(R.string.reset_submit),
                savedTitle = stringResource(R.string.reset_saved),
                state = state.saveState,
                onClick = onSubmit,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.lg),
            )
        }
    }

    state.dialogError?.let { AuthErrorDialog(message = it.asString(), onDismiss = onDismissDialog) }
}

/** Field caption shown above each input. */
@Composable
private fun FieldLabel(
    @StringRes labelId: Int,
) {
    // The wrapper clears semantics so TalkBack skips the caption; the field's own placeholder names it.
    Box(modifier = Modifier.padding(bottom = Spacing.sm).clearAndSetSemantics {}) {
        Text(
            text = stringResource(labelId),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
