package com.uptrendinvestments.clickmeapp.screens.signup.verifyemail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.ValidatedField
import com.uptrendinvestments.clickmeapp.components.backgrounds.GreenGlowBlobLayer
import com.uptrendinvestments.clickmeapp.components.buttons.AsyncSaveButton
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

private val IconCircleSize = 96.dp
private val IconSize = 48.dp
private val ResendMinHeight = 48.dp

/**
 * Hilt-aware Verify Email step (iOS `SignupVerifyEmailView`). [onDone] is called once after a successful verify
 * (after the 600 ms "Verified!" hold) and when the user taps Done on the already-verified variant.
 */
@Composable
fun SignupVerifyEmailScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: SignupVerifyEmailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val done by viewModel.done.collectAsStateWithLifecycle()

    LaunchedEffect(done) {
        if (done) {
            onDone()
            viewModel.onDoneConsumed()
        }
    }

    SignupVerifyEmailContent(
        state = state,
        onSend = viewModel::send,
        onResend = viewModel::resend,
        onCodeChange = viewModel::onCodeChange,
        onVerify = viewModel::verify,
        onDone = onDone,
        onBack = onBack,
        onDismissDialog = viewModel::dismissDialog,
    )
}

/** Stateless Verify Email content: before-send, after-send (code field and resend) and verified variants. */
@Composable
fun SignupVerifyEmailContent(
    state: SignupVerifyEmailUiState,
    onSend: () -> Unit,
    onResend: () -> Unit,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Leaving while a send or verify is in flight would cancel viewModelScope and drop the result.
    val busy =
        state.verifyState != SaveState.Idle ||
            state.sendState == SaveState.Saving ||
            (state.sendState == SaveState.Saved && !state.codeSent)
    BackHandler(enabled = busy) {}

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        GreenGlowBlobLayer(Modifier.matchParentSize())
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                AuthTopAppBar(
                    title = stringResource(R.string.signup_verify_title),
                    onBack = onBack,
                    backEnabled = !busy,
                    containerColor = Color.Transparent,
                )
            },
        ) { innerPadding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Header(state)
                Spacer(Modifier.height(Spacing.xl))
                if (state.verified) {
                    PressScaleButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.signup_verify_done))
                    }
                } else {
                    SendAndCodeSection(state, onSend, onResend, onCodeChange, onVerify)
                }
            }
        }
    }

    state.dialogError?.let {
        AuthErrorDialog(
            message = it.asString(),
            onDismiss = onDismissDialog,
            title = stringResource(state.dialogTitleRes ?: R.string.auth_dialog_title),
        )
    }
}

@Composable
private fun Header(state: SignupVerifyEmailUiState) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.size(IconCircleSize).background(colorScheme.surfaceContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (state.verified) Icons.Outlined.MarkEmailRead else Icons.Outlined.MarkEmailUnread,
            contentDescription = null,
            modifier = Modifier.size(IconSize),
            tint = colorScheme.primary,
        )
    }
    Spacer(Modifier.height(Spacing.lg))
    Text(
        text =
            stringResource(
                if (state.verified) R.string.signup_verify_done_headline else R.string.signup_verify_headline,
            ),
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.headlineLarge,
        color = colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(Spacing.md))
    val message =
        when {
            state.verified -> R.string.signup_verify_done_message
            state.codeSent -> R.string.signup_verify_message_after
            else -> R.string.signup_verify_message_before
        }
    Text(
        text = stringResource(message),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.extras.onSurfaceMuted,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(Spacing.md))
    SelectionContainer {
        Text(
            text = state.email,
            style = MaterialTheme.typography.titleMedium,
            color = colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SendAndCodeSection(
    state: SignupVerifyEmailUiState,
    onSend: () -> Unit,
    onResend: () -> Unit,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
) {
    if (!state.codeSent) {
        AsyncSaveButton(
            title = stringResource(R.string.signup_verify_send),
            savedTitle = stringResource(R.string.signup_verify_sent),
            state = state.sendState,
            onClick = onSend,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    AnimatedVisibility(
        visible = state.codeSent,
        enter =
            fadeIn(tween(Motion.STRENGTH_METER_VISIBILITY_MS)) +
                expandVertically(tween(Motion.STRENGTH_METER_VISIBILITY_MS)),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val editable = state.verifyState == SaveState.Idle
            ValidatedField(
                value = state.code,
                onValueChange = onCodeChange,
                placeholder = stringResource(R.string.signup_verify_code_placeholder),
                leadingIcon = Icons.Outlined.Pin,
                error = state.codeError?.asString(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onVerify() }),
                enabled = editable,
                contentType = ContentType.SmsOtpCode,
            )
            Spacer(Modifier.height(Spacing.md))
            AsyncSaveButton(
                title = stringResource(R.string.signup_verify_verify),
                savedTitle = stringResource(R.string.signup_verify_verified_saved),
                state = state.verifyState,
                onClick = onVerify,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.sm))
            ResendButton(state, onResend)
        }
    }
}

@Composable
private fun ResendButton(
    state: SignupVerifyEmailUiState,
    onResend: () -> Unit,
) {
    val counting = state.resendSecondsRemaining > 0
    TextButton(
        onClick = onResend,
        enabled = !counting && !state.isResending,
        modifier = Modifier.heightIn(min = ResendMinHeight),
        colors =
            ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContentColor = MaterialTheme.extras.onSurfaceMuted,
            ),
    ) {
        Text(
            text =
                if (counting) {
                    stringResource(R.string.signup_verify_resend_in, state.resendSecondsRemaining)
                } else {
                    stringResource(R.string.signup_verify_resend)
                },
            style = MaterialTheme.typography.labelLarge,
            textDecoration = if (counting) TextDecoration.None else TextDecoration.Underline,
        )
    }
    if (!counting) {
        // Announced once when the countdown reaches zero; the per-second ticks are deliberately silent.
        Text(
            text = stringResource(R.string.signup_verify_resend_ready_a11y),
            modifier = Modifier.size(0.dp).semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}
