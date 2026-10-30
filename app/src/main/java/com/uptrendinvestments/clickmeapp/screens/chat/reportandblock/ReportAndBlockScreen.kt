package com.uptrendinvestments.clickmeapp.screens.chat.reportandblock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.safety.BlockUserDialog
import com.uptrendinvestments.clickmeapp.model.enums.UserReportReason
import com.uptrendinvestments.clickmeapp.screens.chat.reportandblock.subviews.BlockCard
import com.uptrendinvestments.clickmeapp.screens.chat.reportandblock.subviews.ReportCard
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth

/**
 * Report & Block screen for one chat thread (UI-SPEC surface 3). Navigation callbacks: [onBack] for the app-bar
 * arrow and system Back, [onBlocked] once a block succeeded (leave to Conversations), [onUnblocked] once an unblock
 * succeeded (return to the thread).
 */
@Composable
fun ReportAndBlockScreen(
    viewModel: ReportAndBlockViewModel,
    onBack: () -> Unit,
    onBlocked: () -> Unit,
    onUnblocked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnBlocked by rememberUpdatedState(onBlocked)
    val currentOnUnblocked by rememberUpdatedState(onUnblocked)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ReportAndBlockEvent.Blocked -> currentOnBlocked()
                ReportAndBlockEvent.Unblocked -> currentOnUnblocked()
            }
        }
    }
    ReportAndBlockContent(
        state = state,
        onBack = onBack,
        onSelectReason = viewModel::selectReason,
        onDetailsChange = viewModel::onDetailsChange,
        onSubmitReport = viewModel::submitReport,
        onRequestBlock = viewModel::requestBlock,
        onDismissBlockConfirm = viewModel::dismissBlockConfirm,
        onConfirmBlock = viewModel::confirmBlock,
        onUnblock = viewModel::unblock,
        onDismissDialog = viewModel::dismissDialog,
        modifier = modifier,
    )
}

/** Stateless Report & Block body so tests can render any state. */
@Composable
fun ReportAndBlockContent(
    state: ReportAndBlockUiState,
    onBack: () -> Unit,
    onSelectReason: (UserReportReason) -> Unit,
    onDetailsChange: (String) -> Unit,
    onSubmitReport: () -> Unit,
    onRequestBlock: () -> Unit,
    onDismissBlockConfirm: () -> Unit,
    onConfirmBlock: () -> Unit,
    onUnblock: () -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Back is locked while Block, Unblock or Submit runs (predictive back included), so a request is never abandoned.
    BackHandler(enabled = state.inFlight) {}
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            AuthTopAppBar(
                title = stringResource(R.string.report_block_title),
                onBack = onBack,
                backEnabled = !state.inFlight,
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .contentMaxWidth()
                    .padding(
                        start = Spacing.md,
                        end = Spacing.md,
                        top = Spacing.lg,
                        bottom = Spacing.xl + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                    ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            BlockCard(
                block = state.block,
                peerName = state.peerName,
                blockInFlight = state.blockInFlight,
                unblockInFlight = state.unblockInFlight,
                showActions = !state.isSelfThread,
                onBlock = onRequestBlock,
                onUnblock = onUnblock,
            )
            ReportCard(
                reason = state.reason,
                details = state.details,
                submitting = state.reportSubmitting,
                submitted = state.reportSubmitted,
                showSubmit = !state.isSelfThread,
                onSelectReason = onSelectReason,
                onDetailsChange = onDetailsChange,
                onSubmit = onSubmitReport,
            )
        }
    }
    state.dialog?.let { dialog ->
        AuthErrorDialog(
            message = dialog.message.asString(),
            title = stringResource(dialog.titleRes),
            onDismiss = onDismissDialog,
        )
    }
    if (state.showBlockConfirm) {
        BlockUserDialog(
            name = state.peerName.orEmpty(),
            inFlight = state.blockInFlight,
            onConfirm = onConfirmBlock,
            onDismiss = onDismissBlockConfirm,
        )
    }
}
