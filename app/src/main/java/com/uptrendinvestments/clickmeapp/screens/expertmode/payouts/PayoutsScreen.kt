package com.uptrendinvestments.clickmeapp.screens.expertmode.payouts

import android.net.Uri
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
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
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
import com.uptrendinvestments.clickmeapp.components.ProfileMenuRow
import com.uptrendinvestments.clickmeapp.components.RefreshFailedSnackbarEffect
import com.uptrendinvestments.clickmeapp.components.SectionAppear
import com.uptrendinvestments.clickmeapp.components.SectionHeader
import com.uptrendinvestments.clickmeapp.components.web.rememberCustomTabLauncher
import com.uptrendinvestments.clickmeapp.model.responses.EarningsTransactionItem
import com.uptrendinvestments.clickmeapp.model.responses.PayoutSummaryData
import com.uptrendinvestments.clickmeapp.navigation.SettingsOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.payouts.subviews.PayoutBalanceHero
import com.uptrendinvestments.clickmeapp.screens.expertmode.payouts.subviews.PayoutEligiblePill
import com.uptrendinvestments.clickmeapp.screens.expertmode.payouts.subviews.PayoutMetaCard
import com.uptrendinvestments.clickmeapp.screens.expertmode.payouts.subviews.PayoutStatusCard
import com.uptrendinvestments.clickmeapp.screens.expertmode.payouts.subviews.PayoutStatusVariant
import com.uptrendinvestments.clickmeapp.screens.expertmode.payouts.subviews.PayoutsSkeleton
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import kotlinx.coroutines.launch

/** Test tag of the "See all activity" action in the Recent activity header. */
const val PAYOUTS_SEE_ALL_TAG = "payouts_see_all"

/** Test tag of the Tax & Payout Info row. */
const val PAYOUTS_TAX_ROW_TAG = "payouts_tax_row"

/**
 * Hilt-aware expert Payouts screen (PAYOUT-01..03). [onBack] pops it. [origin] is where the user came from and is
 * handed back to [onOpenActivity] and [onOpenTaxProfile] so the screens they open can return to the same shell. The
 * view model reloads quietly whenever the screen is shown again, which also picks up a Stripe setup finished in the
 * browser.
 */
@Composable
fun PayoutsScreen(
    origin: SettingsOrigin,
    onBack: () -> Unit,
    onOpenActivity: (SettingsOrigin) -> Unit,
    onOpenTaxProfile: (SettingsOrigin) -> Unit,
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
        origin = origin,
        onBack = onBack,
        onRetry = viewModel::retry,
        onRefresh = viewModel::refresh,
        onRefreshFailureShown = viewModel::consumeRefreshFailure,
        onStartPayoutSetup = viewModel::startPayoutSetup,
        onOnboardingUrl = viewModel::consumeOnboardingUrl,
        onDismissOnboardingError = viewModel::dismissOnboardingError,
        onOpenActivity = onOpenActivity,
        onOpenTaxProfile = onOpenTaxProfile,
        modifier = modifier,
    )
}

/** Stateless Payouts screen: app bar, then the cards for the current [PayoutsContent] in a scrolling column. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayoutsScreenContent(
    state: PayoutsUiState,
    origin: SettingsOrigin,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onRefreshFailureShown: () -> Unit,
    onStartPayoutSetup: () -> Unit,
    onOnboardingUrl: () -> Unit,
    onDismissOnboardingError: () -> Unit,
    onOpenActivity: (SettingsOrigin) -> Unit,
    onOpenTaxProfile: (SettingsOrigin) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val linkFailedMessage = stringResource(R.string.payouts_link_failed)
    val openTab =
        rememberCustomTabLauncher {
            scope.launch { snackbarHostState.showSnackbar(linkFailedMessage) }
        }
    // T-14-24: the single-use Stripe link is read once, cleared from the state right away, then opened.
    LaunchedEffect(state.onboardingUrl) {
        val url = state.onboardingUrl ?: return@LaunchedEffect
        onOnboardingUrl()
        openTab(Uri.parse(url))
    }
    state.onboardingError?.let { error ->
        AuthErrorDialog(
            title = stringResource(R.string.payouts_setup_error_title),
            message = error.asString(),
            onDismiss = onDismissOnboardingError,
            confirmLabel = stringResource(R.string.payouts_got_it),
        )
    }
    RefreshFailedSnackbarEffect(
        refreshFailed = state.refreshFailed,
        snackbarHostState = snackbarHostState,
        onRetry = onRefresh,
        onShown = onRefreshFailureShown,
    )
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
                                .contentMaxWidth()
                                .padding(horizontal = Spacing.md)
                                .padding(top = Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                    ) {
                        SectionAppear {
                            PayoutsBody(
                                state = state,
                                onStartPayoutSetup = onStartPayoutSetup,
                                onOpenActivity = { onOpenActivity(origin) },
                                onOpenTaxProfile = { onOpenTaxProfile(origin) },
                            )
                        }
                        Spacer(Modifier.fillMaxWidth().height(Spacing.xl))
                    }
                }
        }
    }
}

/**
 * The cards for the current content: setup shows only the setup card; hold and ready show their status card then the
 * meta card; a balance shows the hero, the Eligible pill when applicable, the meta card and the schedule note. Recent
 * activity follows (never in setup) and the Tax & Payout Info row is always last.
 */
@Composable
private fun PayoutsBody(
    state: PayoutsUiState,
    onStartPayoutSetup: () -> Unit,
    onOpenActivity: () -> Unit,
    onOpenTaxProfile: () -> Unit,
) {
    val content = state.content
    if (content == null) return
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        PayoutsCards(content, state.summary, state.isMintingLink, onStartPayoutSetup)
        if (content !is PayoutsContent.SetupRequired) {
            RecentActivity(state.recentRows, state.hasMoreActivity, onOpenActivity)
        }
        ProfileMenuRow(
            title = stringResource(R.string.payouts_tax_row),
            icon = Icons.Outlined.Description,
            onClick = onOpenTaxProfile,
            modifier = Modifier.testTag(PAYOUTS_TAX_ROW_TAG),
        )
    }
}

@Composable
private fun PayoutsCards(
    content: PayoutsContent,
    summary: PayoutSummaryData?,
    isMinting: Boolean,
    onStartPayoutSetup: () -> Unit,
) {
    when (content) {
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

/** The latest earnings (PAYOUT-03): [previewRows] as the shared ledger row, or nothing when there are no rows. */
@Composable
private fun RecentActivity(
    rows: List<EarningsTransactionItem>,
    hasMore: Boolean,
    onSeeAll: () -> Unit,
) {
    if (rows.isEmpty()) return
    val seeAll = shouldShowSeeAll(rows, hasMore)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionHeader(
            title = stringResource(R.string.payouts_recent_activity),
            actionLabel = if (seeAll) stringResource(R.string.payouts_see_all) else null,
            onAction = if (seeAll) onSeeAll else null,
            actionModifier = Modifier.testTag(PAYOUTS_SEE_ALL_TAG),
        )
        previewRows(rows).forEach { EarningsTransactionRow(item = it) }
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
