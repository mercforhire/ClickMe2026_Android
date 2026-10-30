package com.uptrendinvestments.clickmeapp.screens.expertmode.payouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ReceiptLong
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
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import java.time.ZoneId
import java.util.Locale

/** Test tag on the Activity list. */
const val TRANSACTIONS_LIST_TAG = "transactions_list"

private const val SKELETON_ROWS = 6
private const val SKELETON_ROW_HEIGHT_DP = 72
private const val SPINNER_SIZE_DP = 24
private const val FOOTER_PAGING_KEY = "paging"
private const val FOOTER_ERROR_KEY = "page_error"

/** Hilt-aware Activity screen (PAYOUT-03): the full paged earnings ledger behind "See all activity". */
@Composable
fun TransactionsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TransactionsContent(
        state = state,
        zone = ZoneId.systemDefault(),
        locale = LocalConfiguration.current.locales[0],
        onBack = onBack,
        onRowVisible = viewModel::onRowVisible,
        onRetry = viewModel::retry,
        onRetryPage = viewModel::retryPage,
        onRefresh = viewModel::refresh,
        onRefreshFailureConsumed = viewModel::consumeRefreshFailure,
        modifier = modifier,
    )
}

/**
 * Stateless Activity list. [zone] and [locale] are parameters so tests pass fixed values. Layout: `md` horizontal and
 * top, `sm` between rows, `xl` plus the navigation inset at the bottom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
fun TransactionsContent(
    state: TransactionsUiState,
    zone: ZoneId,
    locale: Locale,
    onBack: () -> Unit,
    onRowVisible: (Int) -> Unit,
    onRetry: () -> Unit,
    onRetryPage: () -> Unit,
    onRefresh: () -> Unit,
    onRefreshFailureConsumed: () -> Unit,
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
        topBar = { AuthTopAppBar(title = stringResource(R.string.payouts_activity_title), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            when (val load = state.load) {
                TransactionsLoad.Loading -> ActivitySkeleton()
                is TransactionsLoad.Failed ->
                    ScrollableCentered {
                        EmptyState(
                            icon = Icons.Outlined.ErrorOutline,
                            heading = stringResource(R.string.payouts_activity_error_title),
                            body = load.message.asString(),
                            action = EmptyStateAction(stringResource(R.string.payouts_activity_retry), onRetry),
                        )
                    }
                TransactionsLoad.Empty ->
                    ScrollableCentered {
                        EmptyState(
                            icon = Icons.Outlined.ReceiptLong,
                            heading = stringResource(R.string.payouts_activity_empty_title),
                            body = stringResource(R.string.payouts_activity_empty_body),
                        )
                    }
                TransactionsLoad.Loaded ->
                    ActivityList(
                        state = state,
                        zone = zone,
                        locale = locale,
                        onRowVisible = onRowVisible,
                        onRetryPage = onRetryPage,
                    )
            }
        }
    }
}

@Composable
private fun ActivityList(
    state: TransactionsUiState,
    zone: ZoneId,
    locale: Locale,
    onRowVisible: (Int) -> Unit,
    onRetryPage: () -> Unit,
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
        modifier = Modifier.fillMaxSize().testTag(TRANSACTIONS_LIST_TAG),
        contentPadding =
            PaddingValues(
                start = Spacing.md,
                end = Spacing.md,
                top = Spacing.md,
                bottom = Spacing.xl + navigationBottom,
            ),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(items = state.rows, key = { it.id }) { row ->
            EarningsTransactionRow(item = row, modifier = Modifier.contentMaxWidth(), zone = zone, locale = locale)
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
private fun LazyListState.lastVisibleIndex(): Int? {
    val visible = layoutInfo.visibleItemsInfo
    return visible.lastOrNull()?.index
}

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
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.dp,
        )
        Text(
            text = stringResource(R.string.payouts_activity_loading_more),
            style = MaterialTheme.typography.bodySmall,
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
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRetry) {
            Text(text = stringResource(R.string.payouts_activity_retry))
        }
    }
}

@Composable
private fun ActivitySkeleton() {
    SkeletonGroup(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier.fillMaxSize().contentMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.md),
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
