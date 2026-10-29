package com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount.subviews.DeleteAccountBottomBar
import com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount.subviews.DeleteAccountCommentsField
import com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount.subviews.DeleteAccountPasswordField
import com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount.subviews.DeleteAccountReasonSection
import com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount.subviews.DeleteAccountWarningHeader
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/**
 * Port of iOS `DeleteAccountView` (PROF-04): the Warning form, then the Reason and password form. The Farewell is not
 * part of this screen: it is hosted at the app root ([AccountDeletionFarewellHost]) so the session teardown that
 * follows the deletion cannot remove it.
 *
 * Back is ignored while the request pair is in flight; otherwise it pops, except on the Reason form where it returns
 * to Warning.
 */
@Composable
fun DeleteAccountScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeleteAccountViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DeleteAccountContent(
        state = state,
        onBack = onBack,
        onContinue = viewModel::continueFromWarning,
        onBackToWarning = viewModel::backToWarning,
        onSelectReason = viewModel::selectReason,
        onFeedbackChange = viewModel::onFeedbackChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirm = viewModel::confirmDeletion,
        onDismissDialog = viewModel::dismissDialog,
        modifier = modifier,
    )
}

/** Stateless Delete Account screen: both forms under a "Delete Account" app bar with the pinned footer. */
@Composable
fun DeleteAccountContent(
    state: DeleteAccountUiState,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onBackToWarning: () -> Unit,
    onSelectReason: (DeleteAccountReason) -> Unit,
    onFeedbackChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Registered first so the in-flight swallow wins over the step handler below (the later handler runs first).
    BackHandler(enabled = state.step == DeleteStep.Reason && !state.isDeleting, onBack = onBackToWarning)
    BackHandler(enabled = state.isDeleting) {}

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            AuthTopAppBar(
                title = stringResource(R.string.delete_account_title),
                // Same as system Back: the Reason step steps back to the warning, the warning leaves the flow.
                onBack = if (state.step == DeleteStep.Reason) onBackToWarning else onBack,
                backEnabled = !state.isDeleting,
            )
        },
        bottomBar = {
            DeleteAccountBottomBar(
                step = state.step,
                canConfirm = state.canConfirm,
                isDeleting = state.isDeleting,
                onKeep = onBack,
                onContinue = onContinue,
                onConfirm = onConfirm,
            )
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = state.step,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val direction = if (forward) 1 else -1
                (
                    slideInHorizontally(tween(Motion.PUSH_POP_DURATION_MS)) { direction * it } +
                        fadeIn(tween(Motion.PUSH_POP_DURATION_MS))
                ) togetherWith
                    (
                        slideOutHorizontally(tween(Motion.PUSH_POP_DURATION_MS)) { -direction * it } +
                            fadeOut(tween(Motion.PUSH_POP_DURATION_MS))
                    )
            },
            label = "delete-account-step",
        ) { step ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.md, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                DeleteAccountWarningHeader()
                if (step == DeleteStep.Reason) {
                    DeleteAccountReasonSection(
                        selected = state.reason,
                        onSelect = onSelectReason,
                        enabled = !state.isDeleting,
                    )
                    DeleteAccountCommentsField(
                        value = state.feedback,
                        onValueChange = onFeedbackChange,
                        enabled = !state.isDeleting,
                    )
                    DeleteAccountPasswordField(
                        value = state.password,
                        onValueChange = onPasswordChange,
                        error = state.passwordError,
                        enabled = !state.isDeleting,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    state.dialog?.let { error ->
        AuthErrorDialog(
            message = error.asString(),
            onDismiss = onDismissDialog,
            title = stringResource(R.string.delete_account_error_title),
            confirmLabel = stringResource(R.string.delete_account_got_it),
        )
    }
}
