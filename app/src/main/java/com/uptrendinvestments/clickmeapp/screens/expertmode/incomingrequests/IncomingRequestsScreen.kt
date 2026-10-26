package com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests

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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests.subviews.IncomingRequestCard
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.RequestDecisionKey
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import java.text.DateFormat
import java.time.ZoneId
import java.util.Locale

/** Test tag on the request list. */
const val INCOMING_REQUESTS_LIST_TAG = "incoming_requests_list"

/**
 * Hilt-aware Incoming Requests (EXP-06). Tapping a card opens Request Decision with the row's expiry and earnings
 * (UI-SPEC surface 2).
 */
@Composable
fun IncomingRequestsScreen(
    onBack: () -> Unit,
    onOpenRequest: (RequestDecisionKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IncomingRequestsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    IncomingRequestsContent(
        state = state,
        onBack = onBack,
        onOpenRequest = onOpenRequest,
        onRefresh = {},
        modifier = modifier,
    )
}

/** Stateless Incoming Requests: app bar, pull-to-refresh and the list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomingRequestsContent(
    state: IncomingRequestsUiState,
    onBack: () -> Unit,
    onOpenRequest: (RequestDecisionKey) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    timeFormat: DateFormat = rememberDeviceTimeFormat(),
    locale: Locale = LocalConfiguration.current.locales[0],
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { AuthTopAppBar(title = stringResource(R.string.incoming_requests_title), onBack = onBack) },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding()),
        ) {
            val rows = (state.load as? IncomingRequestsLoad.Loaded)?.rows.orEmpty()
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag(INCOMING_REQUESTS_LIST_TAG),
                contentPadding =
                    PaddingValues(
                        start = Spacing.md,
                        end = Spacing.md,
                        top = Spacing.lg,
                        bottom = Spacing.xl + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                    ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                items(items = rows, key = { it.item.requestId }) { row ->
                    IncomingRequestCard(
                        row = row,
                        onClick = { onOpenRequest(row.toDecisionKey()) },
                        modifier = Modifier.animateItem(),
                        zone = zone,
                        timeFormat = timeFormat,
                        locale = locale,
                    )
                }
            }
        }
    }
}
