package com.uptrendinvestments.clickmeapp.screens.settings.accountsecurity

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.screens.settings.accountsecurity.subviews.ChangePasswordCard
import com.uptrendinvestments.clickmeapp.screens.settings.accountsecurity.subviews.DeleteAccountEntryCard
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth

/**
 * Hilt-aware Account Security screen (iOS `AccountSecurityView`). [onOpenDeleteAccount] runs only when the call
 * guard allows the action; the view model never navigates.
 */
@Composable
fun AccountSecurityScreen(
    onBack: () -> Unit,
    onOpenDeleteAccount: () -> Unit,
    viewModel: AccountSecurityViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AccountSecurityContent(
        state = state,
        onCurrentChange = viewModel::onCurrentChange,
        onNewChange = viewModel::onNewChange,
        onConfirmChange = viewModel::onConfirmChange,
        onSubmit = viewModel::submit,
        onDeleteAccount = { if (viewModel.requestDeleteAccount()) onOpenDeleteAccount() },
        onDismissDialog = viewModel::dismissDialog,
        onBack = onBack,
    )
}

/** Stateless Account Security content: the change-password card, the Delete Account entry card and the dialog. */
@Composable
fun AccountSecurityContent(
    state: AccountSecurityUiState,
    onCurrentChange: (String) -> Unit,
    onNewChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDeleteAccount: () -> Unit,
    onDismissDialog: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val editable = state.saveState == SaveState.Idle
    // Leaving while the change is in flight would drop the confirmation of a password that did change.
    BackHandler(enabled = !editable) {}

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            AuthTopAppBar(
                title = stringResource(R.string.account_security_title),
                onBack = onBack,
                backEnabled = editable,
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
                    .contentMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            ChangePasswordCard(
                state = state,
                onCurrentChange = onCurrentChange,
                onNewChange = onNewChange,
                onConfirmChange = onConfirmChange,
                onSubmit = onSubmit,
            )
            DeleteAccountEntryCard(onDelete = onDeleteAccount)
        }
    }

    when (val dialog = state.dialog) {
        AccountSecurityDialog.Success ->
            AuthErrorDialog(
                title = stringResource(R.string.account_security_success_title),
                message = stringResource(R.string.account_security_success_body),
                confirmLabel = stringResource(R.string.account_security_got_it),
                onDismiss = onDismissDialog,
            )
        is AccountSecurityDialog.Error ->
            AuthErrorDialog(
                title = stringResource(dialog.title),
                message = dialog.message.asString(),
                confirmLabel = stringResource(R.string.account_security_got_it),
                onDismiss = onDismissDialog,
            )
        null -> Unit
    }
}
