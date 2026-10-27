package com.uptrendinvestments.clickmeapp.screens.chat.reportandblock

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.UserReportReason
import com.uptrendinvestments.clickmeapp.screens.chat.reportandblock.subviews.ReportCard
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

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
    ReportAndBlockContent(
        state = state,
        onBack = onBack,
        onSelectReason = viewModel::selectReason,
        onDetailsChange = viewModel::onDetailsChange,
        onSubmitReport = viewModel::submitReport,
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
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            AuthTopAppBar(
                title = stringResource(R.string.report_block_title),
                onBack = onBack,
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
                    .padding(
                        start = Spacing.md,
                        end = Spacing.md,
                        top = Spacing.lg,
                        bottom = Spacing.xl + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                    ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
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
}
