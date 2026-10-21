package com.uptrendinvestments.clickmeapp.screens.signup.initial

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.PasswordStrengthMeter
import com.uptrendinvestments.clickmeapp.components.ValidatedField
import com.uptrendinvestments.clickmeapp.components.backgrounds.GreenGlowBlobLayer
import com.uptrendinvestments.clickmeapp.components.buttons.AsyncSaveButton
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.managers.LaunchDestination
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

/**
 * Hilt-aware signup Initial step (SIGN-01, iOS `SignupInitialView`). [onSignedUp] receives
 * [LaunchDestination.SignupSetup] once, after the account exists; the caller swaps the root route.
 */
@Composable
fun SignupInitialScreen(
    onSignedUp: (LaunchDestination) -> Unit,
    onBack: () -> Unit,
    viewModel: SignupInitialViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val focusRequest by viewModel.focusRequest.collectAsStateWithLifecycle()

    LaunchedEffect(destination) {
        destination?.let {
            onSignedUp(it)
            viewModel.onDestinationConsumed()
        }
    }

    SignupInitialContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::submit,
        onBack = onBack,
        onDismissDialog = viewModel::dismissDialog,
        focusRequest = focusRequest,
        onFocusRequestConsumed = viewModel::onFocusRequestConsumed,
    )
}

/** Stateless Create Account form: headline, email, password with strength meter and rule hint, Continue. */
@Composable
fun SignupInitialContent(
    state: SignupInitialUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequest: SignupField? = null,
    onFocusRequestConsumed: () -> Unit = {},
) {
    val emailFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }

    LaunchedEffect(focusRequest) {
        when (focusRequest) {
            SignupField.Email -> emailFocus.requestFocus()
            SignupField.Password -> passwordFocus.requestFocus()
            null -> return@LaunchedEffect
        }
        onFocusRequestConsumed()
    }

    // WR-05: system and predictive Back are swallowed while the account is being created, like the top-bar arrow.
    // Popping the entry would cancel the signup after the server may already have created the account.
    BackHandler(enabled = state.isSubmitting) {}

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        GreenGlowBlobLayer(Modifier.matchParentSize())
        Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
            AuthTopAppBar(
                title = "",
                onBack = onBack,
                backEnabled = !state.isSubmitting,
                containerColor = Color.Transparent,
            )
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.xxl, bottom = Spacing.xl),
            ) {
                Text(
                    text = stringResource(R.string.signup_initial_headline),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().semantics { heading() },
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = stringResource(R.string.signup_initial_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.extras.onSurfaceMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Spacing.xl))
                ValidatedField(
                    value = state.email,
                    onValueChange = onEmailChange,
                    placeholder = stringResource(R.string.signup_initial_email_placeholder),
                    leadingIcon = Icons.Outlined.Email,
                    error = state.emailError?.asString(),
                    modifier = Modifier.focusRequester(emailFocus),
                    keyboardOptions =
                        KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() }),
                    enabled = !state.isSubmitting,
                    contentType = ContentType.EmailAddress,
                )
                ValidatedField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    placeholder = stringResource(R.string.signup_initial_password_placeholder),
                    leadingIcon = Icons.Outlined.Lock,
                    error = state.passwordError?.asString(),
                    modifier = Modifier.focusRequester(passwordFocus),
                    isSecure = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                    enabled = !state.isSubmitting,
                    contentType = ContentType.NewPassword,
                )
                AnimatedVisibility(
                    visible = state.password.isNotEmpty(),
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
                Text(
                    text = stringResource(R.string.signup_initial_password_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (state.passwordError != null) {
                            MaterialTheme.extras.destructive
                        } else {
                            MaterialTheme.extras.onSurfaceMuted
                        },
                )
                Spacer(Modifier.height(Spacing.lg))
                AsyncSaveButton(
                    title = stringResource(R.string.signup_initial_continue),
                    state = if (state.isSubmitting) SaveState.Saving else SaveState.Idle,
                    onClick = onSubmit,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    state.dialogError?.let {
        AuthErrorDialog(
            message = it.asString(),
            onDismiss = onDismissDialog,
            title = stringResource(R.string.signup_initial_error_title),
        )
    }
}
