package com.uptrendinvestments.clickmeapp.screens.clientmode.payments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth
import java.time.ZoneId
import java.util.Locale

/** Test tag on the Payment History list. */
const val CLIENT_TRANSACTIONS_LIST_TAG = "client_transactions_list"

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

/**
 * Stateless Payment History list. [zone] and [locale] are parameters so tests pass fixed values. Layout: `md`
 * horizontal and top, `sm` between rows, `xl` plus the navigation inset at the bottom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList", "UNUSED_PARAMETER")
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
            if (state.load == ClientTransactionsLoad.Loaded) {
                val navigationBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                LazyColumn(
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
                }
            }
        }
    }
}
