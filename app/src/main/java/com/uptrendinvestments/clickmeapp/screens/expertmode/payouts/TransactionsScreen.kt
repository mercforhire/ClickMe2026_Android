package com.uptrendinvestments.clickmeapp.screens.expertmode.payouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.EmptyStateAction
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import java.time.ZoneId
import java.util.Locale

/** Test tag on the Activity list. */
const val TRANSACTIONS_LIST_TAG = "transactions_list"

private const val SKELETON_ROWS = 6
private const val SKELETON_ROW_HEIGHT_DP = 72

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
        onRowVisible = {},
        onRetry = viewModel::retry,
        onRetryPage = {},
        onRefresh = viewModel::retry,
        onRefreshFailureConsumed = {},
        modifier = modifier,
    )
}

/**
 * Stateless Activity list. [zone] and [locale] are parameters so tests pass fixed values. Layout: `md` horizontal and
 * top, `sm` between rows, `xl` plus the navigation inset at the bottom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList", "UNUSED_PARAMETER")
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
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { AuthTopAppBar(title = stringResource(R.string.payouts_activity_title), onBack = onBack) },
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
                TransactionsLoad.Loaded -> ActivityList(state = state, zone = zone, locale = locale)
            }
        }
    }
}

@Composable
private fun ActivityList(
    state: TransactionsUiState,
    zone: ZoneId,
    locale: Locale,
) {
    val navigationBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
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
            EarningsTransactionRow(item = row, zone = zone, locale = locale)
        }
    }
}

@Composable
private fun ActivitySkeleton() {
    SkeletonGroup(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.md, vertical = Spacing.md),
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
