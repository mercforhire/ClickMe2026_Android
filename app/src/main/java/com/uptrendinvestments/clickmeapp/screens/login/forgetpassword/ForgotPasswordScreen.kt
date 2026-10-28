package com.uptrendinvestments.clickmeapp.screens.login.forgetpassword

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.ValidatedField
import com.uptrendinvestments.clickmeapp.components.buttons.AsyncSaveButton
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

private val BackIconSize = 20.dp
private val BackButtonMinHeight = 48.dp

/**
 * Hilt-aware "Reset Password" entry. [initialEmail] is the Login email; [onCodeSent] receives the trimmed email
 * once the request was accepted and the 600 ms "Code Sent!" hold is over.
 */
@Composable
fun ForgotPasswordScreen(
    initialEmail: String,
    onCodeSent: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navigateToReset by viewModel.navigateToReset.collectAsStateWithLifecycle()
    val focusEmail by viewModel.focusEmail.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.start(initialEmail) }

    LaunchedEffect(navigateToReset) {
        navigateToReset?.let {
            onCodeSent(it)
            viewModel.onNavigated()
        }
    }

    ForgotPasswordContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onSend = viewModel::send,
        onBack = onBack,
        onDismissDialog = viewModel::dismissDialog,
        focusEmail = focusEmail,
        onFocusConsumed = viewModel::onFocusConsumed,
    )
}

/** Stateless Reset Password screen (UI-SPEC Screen 2). */
@Composable
fun ForgotPasswordContent(
    state: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
    focusEmail: Boolean = false,
    onFocusConsumed: () -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusEmail) {
        if (focusEmail) {
            focusRequester.requestFocus()
            onFocusConsumed()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { AuthTopAppBar(title = stringResource(R.string.forgot_title), onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.lg),
        ) {
            Text(
                text = stringResource(R.string.forgot_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.extras.onSurfaceMuted,
                modifier = Modifier.padding(top = Spacing.lg),
            )
            Spacer(Modifier.height(Spacing.xl))
            // The field announces itself through its placeholder, so TalkBack skips this visible label.
            Text(
                text = stringResource(R.string.forgot_email_label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clearAndSetSemantics {},
            )
            Spacer(Modifier.height(Spacing.sm))
            ValidatedField(
                value = state.email,
                onValueChange = onEmailChange,
                placeholder = stringResource(R.string.login_email_placeholder),
                leadingIcon = Icons.Outlined.Email,
                error = state.emailError?.asString(),
                modifier = Modifier.focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSend() }),
                enabled = state.saveState != SaveState.Saving,
                contentType = ContentType.EmailAddress,
            )
            Spacer(Modifier.height(Spacing.sm))
            AsyncSaveButton(
                title = stringResource(R.string.forgot_send_reset_code),
                savedTitle = stringResource(R.string.forgot_code_sent),
                state = state.saveState,
                onClick = onSend,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.lg))
            Text(
                text = stringResource(R.string.forgot_info),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.extras.onSurfaceMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md),
            )
            Spacer(Modifier.height(Spacing.xl))
            TextButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = BackButtonMinHeight),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(BackIconSize),
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = stringResource(R.string.forgot_back_to_login),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Spacer(Modifier.height(Spacing.xl))
        }
    }

    state.dialogError?.let { error ->
        AuthErrorDialog(message = error.asString(), onDismiss = onDismissDialog)
    }
}
