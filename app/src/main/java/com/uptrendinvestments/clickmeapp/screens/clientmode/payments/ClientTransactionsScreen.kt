package com.uptrendinvestments.clickmeapp.screens.clientmode.payments

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.EmptyStateAction
import com.uptrendinvestments.clickmeapp.components.RefreshFailedSnackbarEffect
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import java.time.ZoneId
import java.util.Locale

/** Test tag on the Payment History list. */
const val CLIENT_TRANSACTIONS_LIST_TAG = "client_transactions_list"

private const val SKELETON_ROWS = 6
private const val SKELETON_ROW_HEIGHT_DP = 72
private const val SPINNER_SIZE_DP = 24
private const val RETRY_MIN_HEIGHT_DP = 48
private const val FOOTER_PAGING_KEY = "paging"
private const val FOOTER_ERROR_KEY = "page_error"

/**
 * Hilt-aware Payment History screen (PARITY-AUDIT #4). A row tap reports the booking id through [onOpenBooking]; the
 * route that opens it is wired by the caller.
 */
@Composable
fun ClientTransactionsScreen(
    onBack: () -> Unit,
    onOpenBooking: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ClientTransactionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ClientTransactionsContent(
        state = state,
        zone = ZoneId.systemDefault(),
        locale = LocalConfiguration.current.locales[0],
        onBack = onBack,
        onRowVisible = viewModel::onRowVisible,
        onRetry = viewModel::retry,
        onRetryPage = viewModel::retryPage,
        onRefresh = viewModel::refresh,
        onRefreshFailureConsumed = viewModel::consumeRefreshFailure,
        onOpenBooking = onOpenBooking,
        modifier = modifier,
    )
}

/** Which body the screen shows. The cross-fade is keyed on this, never on the rows, so a page append never re-runs it. */
private enum class LoadKind { Loading, Failed, Empty, Loaded }

private fun ClientTransactionsLoad.kind(): LoadKind =
    when (this) {
        ClientTransactionsLoad.Loading -> LoadKind.Loading
        is ClientTransactionsLoad.Failed -> LoadKind.Failed
        ClientTransactionsLoad.Empty -> LoadKind.Empty
        ClientTransactionsLoad.Loaded -> LoadKind.Loaded
    }

/**
 * Stateless Payment History list. [zone] and [locale] are parameters so tests pass fixed values. Layout: `md`
 * horizontal and top, `sm` between rows, `xl` plus the navigation inset at the bottom. Follows the Phase 15 states
 * (D-04): a skeleton, not a spinner, for the first load; rows stay through a pull and through a failed pull.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
fun ClientTransactionsContent(
    state: ClientTransactionsUiState,
    zone: ZoneId,
    locale: Locale,
    onBack: () -> Unit,
    onRowVisible: (Int) -> Unit,
    onRetry: () -> Unit,
    onRetryPage: () -> Unit,
    onRefresh: () -> Unit,
    onRefreshFailureConsumed: () -> Unit,
    onOpenBooking: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    RefreshFailedSnackbarEffect(
        refreshFailed = state.refreshFailed,
        snackbarHostState = snackbarHostState,
        onRetry = onRefresh,
        onShown = onRefreshFailureConsumed,
    )

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { AuthTopAppBar(title = stringResource(R.string.payment_history_title), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            Crossfade(
                targetState = state.load.kind(),
                animationSpec = tween(Motion.CONTROL_STATE_DURATION_MS),
                label = "clientTransactionsLoad",
            ) { kind ->
                when (kind) {
                    LoadKind.Loading -> PaymentsSkeleton()
                    LoadKind.Failed ->
                        (state.load as? ClientTransactionsLoad.Failed)?.let { failed ->
                            ScrollableCentered {
                                EmptyState(
                                    icon = Icons.Outlined.ErrorOutline,
                                    heading = stringResource(R.string.payment_history_error_title),
                                    body = failed.message.asString(),
                                    action = EmptyStateAction(stringResource(R.string.section_retry), onRetry),
                                )
                            }
                        }
                    LoadKind.Empty ->
                        ScrollableCentered {
                            EmptyState(
                                icon = Icons.Outlined.CreditCard,
                                heading = stringResource(R.string.payment_history_empty_title),
                                body = stringResource(R.string.payment_history_empty_body),
                            )
                        }
                    LoadKind.Loaded ->
                        PaymentsList(
                            state = state,
                            zone = zone,
                            locale = locale,
                            onRowVisible = onRowVisible,
                            onRetryPage = onRetryPage,
                            onOpenBooking = onOpenBooking,
                        )
                }
            }
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun PaymentsList(
    state: ClientTransactionsUiState,
    zone: ZoneId,
    locale: Locale,
    onRowVisible: (Int) -> Unit,
    onRetryPage: () -> Unit,
    onOpenBooking: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val currentOnRowVisible by rememberUpdatedState(onRowVisible)
    LaunchedEffect(listState) {
        snapshotFlow { listState.lastVisibleIndex() }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { currentOnRowVisible(it) }
    }

    val navigationBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().testTag(CLIENT_TRANSACTIONS_LIST_TAG),
        contentPadding =
            PaddingValues(
                start = Spacing.md,
                end = Spacing.md,
                top = Spacing.md,
                bottom = Spacing.xl + navigationBottom,
            ),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(items = state.rows, key = { it.bookingId }) { row ->
            ClientTransactionRow(
                item = row,
                onClick = { onOpenBooking(row.bookingId) },
                modifier = Modifier.contentMaxWidth(),
                zone = zone,
                locale = locale,
            )
        }
        if (state.isPaging) {
            item(key = FOOTER_PAGING_KEY) { PagingFooter(Modifier.contentMaxWidth()) }
        }
        state.pageError?.let { error ->
            item(key = FOOTER_ERROR_KEY) {
                PageErrorFooter(error = error, onRetry = onRetryPage, modifier = Modifier.contentMaxWidth())
            }
        }
    }
}

/** The index of the last item on screen, or null before the first layout. Footer rows count, which only helps. */
private fun LazyListState.lastVisibleIndex(): Int? = layoutInfo.visibleItemsInfo.lastOrNull()?.index

/** The trailing "Loading more…" row while the next page is fetched. */
@Composable
private fun PagingFooter(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(SPINNER_SIZE_DP.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            strokeWidth = 2.dp,
        )
        Text(
            text = stringResource(R.string.payouts_activity_loading_more),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.extras.onSurfaceMuted,
        )
    }
}

/** The trailing error row of a failed later page; the loaded rows above it stay. */
@Composable
private fun PageErrorFooter(
    error: FieldError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = error.asString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.extras.onSurfaceMuted,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = RETRY_MIN_HEIGHT_DP.dp)) {
            Text(
                text = stringResource(R.string.section_retry),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun PaymentsSkeleton() {
    SkeletonGroup(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().contentMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            repeat(SKELETON_ROWS) {
                SkeletonBlock(
                    modifier = Modifier.fillMaxWidth().height(SKELETON_ROW_HEIGHT_DP.dp),
                    shape = MaterialTheme.shapes.medium,
                )
            }
        }
    }
}

/** Vertically scrollable, centered column so pull-to-refresh works from the empty and error states. */
@Composable
private fun ScrollableCentered(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}
