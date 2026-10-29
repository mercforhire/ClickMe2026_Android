package com.uptrendinvestments.clickmeapp.screens.expertmode.payouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.EmptyStateAction
import com.uptrendinvestments.clickmeapp.components.SectionAppear
import com.uptrendinvestments.clickmeapp.model.responses.PayoutSummaryData
import com.uptrendinvestments.clickmeapp.screens.expertmode.payouts.subviews.PayoutBalanceHero
import com.uptrendinvestments.clickmeapp.screens.expertmode.payouts.subviews.PayoutEligiblePill
import com.uptrendinvestments.clickmeapp.screens.expertmode.payouts.subviews.PayoutMetaCard
import com.uptrendinvestments.clickmeapp.screens.expertmode.payouts.subviews.PayoutStatusCard
import com.uptrendinvestments.clickmeapp.screens.expertmode.payouts.subviews.PayoutStatusVariant
import com.uptrendinvestments.clickmeapp.screens.expertmode.payouts.subviews.PayoutsSkeleton
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import kotlinx.coroutines.launch

/**
 * Hilt-aware expert Payouts screen (PAYOUT-01). [onBack] pops it. The view model reloads quietly whenever the screen
 * is shown again, which also picks up a Stripe setup finished in the browser.
 */
@Composable
fun PayoutsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PayoutsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // The Nav3 entry may not receive ON_RESUME when it is re-shown, so it also reports from first composition; the
    // view model ignores the second call while a load runs.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onScreenShown() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.onScreenHidden() }
    DisposableEffect(viewModel) { onDispose { viewModel.onScreenHidden() } }
    LaunchedEffect(Unit) { viewModel.onScreenShown() }
    PayoutsScreenContent(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onRefresh = viewModel::refresh,
        onRefreshFailureShown = viewModel::consumeRefreshFailure,
        onStartPayoutSetup = {},
        modifier = modifier,
    )
}

/** Stateless Payouts screen: app bar, then the cards for the current [PayoutsContent] in a scrolling column. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayoutsScreenContent(
    state: PayoutsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onRefreshFailureShown: () -> Unit,
    onStartPayoutSetup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val refreshFailedMessage = stringResource(R.string.payouts_refresh_failed)
    LaunchedEffect(state.refreshFailed) {
        if (state.refreshFailed) {
            // Launched on the composition scope so consuming the flag does not cancel the Snackbar.
            scope.launch { snackbarHostState.showSnackbar(refreshFailedMessage, duration = SnackbarDuration.Short) }
            onRefreshFailureShown()
        }
    }
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { AuthTopAppBar(title = stringResource(R.string.payouts_title), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (val load = state.load) {
            PayoutsLoad.Loading -> PayoutsSkeleton(modifier = Modifier.padding(innerPadding))
            is PayoutsLoad.Failed ->
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.padding(innerPadding),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .semantics { liveRegion = LiveRegionMode.Polite },
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        EmptyState(
                            icon = Icons.Outlined.ErrorOutline,
                            heading = stringResource(R.string.payouts_error_title),
                            body = load.message.asString(),
                            action = EmptyStateAction(stringResource(R.string.expert_retry), onRetry),
                        )
                    }
                }
            PayoutsLoad.Loaded ->
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.padding(innerPadding),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = Spacing.md)
                                .padding(top = Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                    ) {
                        SectionAppear {
                            PayoutsBody(
                                content = state.content,
                                summary = state.summary,
                                isMinting = state.isMintingLink,
                                onStartPayoutSetup = onStartPayoutSetup,
                            )
                        }
                        Spacer(Modifier.fillMaxWidth().height(Spacing.xl))
                    }
                }
        }
    }
}

/**
 * The cards for [content]: setup shows only the setup card; hold and ready show their status card then the meta card;
 * a balance shows the hero, the Eligible pill when applicable, the meta card and the schedule note.
 */
@Composable
private fun PayoutsBody(
    content: PayoutsContent?,
    summary: PayoutSummaryData?,
    isMinting: Boolean,
    onStartPayoutSetup: () -> Unit,
) {
    when (content) {
        null -> Unit
        is PayoutsContent.SetupRequired ->
            PayoutStatusCard(PayoutStatusVariant.Setup, onSetUp = onStartPayoutSetup, isMinting = isMinting)
        is PayoutsContent.OnHold ->
            StatusThenMeta(PayoutStatusVariant.Hold(content.heldCount), summary)
        PayoutsContent.ReadyNoEarnings -> StatusThenMeta(PayoutStatusVariant.Ready, summary)
        is PayoutsContent.Loaded ->
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                PayoutBalanceHero(summary = content.summary)
                if (content.eligible) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        PayoutEligiblePill()
                    }
                }
                PayoutMetaCard(summary = content.summary)
                Text(
                    text = stringResource(R.string.payouts_schedule_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.extras.onSurfaceMuted,
                )
            }
    }
}

@Composable
private fun StatusThenMeta(
    variant: PayoutStatusVariant,
    summary: PayoutSummaryData?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        PayoutStatusCard(variant, onSetUp = {}, isMinting = false)
        PayoutMetaCard(summary = summary)
    }
}
