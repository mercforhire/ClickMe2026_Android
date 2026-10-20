package com.uptrendinvestments.clickmeapp.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.uptrendinvestments.clickmeapp.managers.LaunchDestination
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

private val LogoSize = 80.dp
private val LinkMinHeight = 48.dp

/**
 * Hilt-aware entry for the login screen (AUTH-01). [onForgotPassword] receives the current email so the
 * recovery flow can pre-fill it (D-07). [prefillEmail] is the Password Changed return hook (D-10).
 */
@Composable
fun LoginScreen(
    onLoggedIn: (LaunchDestination) -> Unit,
    onForgotPassword: (String) -> Unit,
    onSignUp: () -> Unit,
    prefillEmail: String? = null,
    onPrefillConsumed: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val focusRequest by viewModel.focusRequest.collectAsStateWithLifecycle()

    LaunchedEffect(destination) {
        destination?.let {
            onLoggedIn(it)
            viewModel.onDestinationConsumed()
        }
    }
    LaunchedEffect(prefillEmail) {
        if (prefillEmail != null) {
            viewModel.prefill(prefillEmail)
            onPrefillConsumed()
        }
    }

    LoginContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::submit,
        onForgotPassword = { onForgotPassword(state.email.trim()) },
        onSignUp = onSignUp,
        onDismissDialog = viewModel::dismissDialog,
        focusRequest = focusRequest,
        onFocusRequestConsumed = viewModel::onFocusRequestConsumed,
    )
}

/** Stateless login screen: header, email and password fields, Forgot Password link, Login button, Sign Up footer. */
@Composable
fun LoginContent(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
    onSignUp: () -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequest: LoginField? = null,
    onFocusRequestConsumed: () -> Unit = {},
) {
    val emailFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }

    LaunchedEffect(focusRequest) {
        when (focusRequest) {
            LoginField.Email -> emailFocus.requestFocus()
            LoginField.Password -> passwordFocus.requestFocus()
            null -> return@LaunchedEffect
        }
        onFocusRequestConsumed()
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        GreenGlowBlobLayer(Modifier.matchParentSize())
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.xxl, bottom = Spacing.lg),
        ) {
            LoginHeader()
            Spacer(Modifier.height(Spacing.lg))
            ValidatedField(
                value = state.email,
                onValueChange = onEmailChange,
                placeholder = stringResource(R.string.login_email_placeholder),
                leadingIcon = Icons.Outlined.Email,
                error = state.emailError?.asString(),
                modifier = Modifier.focusRequester(emailFocus),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() }),
                enabled = !state.isSubmitting,
                contentType = ContentType.EmailAddress,
            )
            ValidatedField(
                value = state.password,
                onValueChange = onPasswordChange,
                placeholder = stringResource(R.string.login_password_placeholder),
                leadingIcon = Icons.Outlined.Lock,
                error = state.passwordError?.asString(),
                modifier = Modifier.focusRequester(passwordFocus),
                isSecure = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                enabled = !state.isSubmitting,
            )
            AuthLink(
                text = stringResource(R.string.login_forgot_password),
                onClick = onForgotPassword,
                enabled = !state.isSubmitting,
                modifier = Modifier.align(Alignment.End),
            )
            Spacer(Modifier.height(Spacing.sm))
            AsyncSaveButton(
                title = stringResource(R.string.login_submit),
                state = if (state.isSubmitting) SaveState.Saving else SaveState.Idle,
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.lg))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.login_no_account),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.extras.onSurfaceMuted,
                )
                Spacer(Modifier.width(Spacing.xs))
                AuthLink(
                    text = stringResource(R.string.login_sign_up),
                    onClick = onSignUp,
                    enabled = !state.isSubmitting,
                )
            }
        }
    }

    state.dialogError?.let { AuthErrorDialog(message = it.asString(), onDismiss = onDismissDialog) }
}

@Composable
private fun LoginHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Image(
            painter = painterResource(R.drawable.clickme_mark),
            contentDescription = null,
            modifier = Modifier.size(LogoSize),
        )
        Text(
            text = stringResource(R.string.login_wordmark),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.login_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.extras.onSurfaceMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.xl),
        )
    }
}

/** Underlined text link in onSurface (UI-SPEC: not the accent green of iOS) with a 48 dp touch target. */
@Composable
private fun AuthLink(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = LinkMinHeight),
        colors =
            ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContentColor = MaterialTheme.extras.onSurfaceMuted,
            ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            textDecoration = TextDecoration.Underline,
        )
    }
}
