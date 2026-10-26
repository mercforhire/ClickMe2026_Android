package com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard

import android.text.format.DateFormat
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.EmptyStateAction
import com.uptrendinvestments.clickmeapp.components.avatar.TopBarAvatarButton
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingItem
import com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.subviews.DashboardHero
import com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.subviews.PendingRequestsBanner
import com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.subviews.RescheduleRequestsBanner
import com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.subviews.TodaysSessionCard
import com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.subviews.TodaysSessionsEmpty
import com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.subviews.TodaysSessionsHeader
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId

/**
 * Hilt-aware expert Dashboard tab root (EXP-05). Navigation is reported through callbacks so the route owns the
 * navigator: [onOpenRequests] opens Incoming Requests, [onOpenBookingsUpcoming] selects the Bookings tab on Upcoming
 * (the screen first posts the hand-off the Bookings tab consumes, D-12) and [onOpenBooking] opens a booking Summary.
 */
@Composable
fun ExpertDashboardScreen(
    avatarUrl: String?,
    onOpenModeSwitch: () -> Unit,
    onOpenRequests: () -> Unit,
    onOpenBookingsUpcoming: () -> Unit,
    onOpenBooking: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpertDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val now by viewModel.joinNow.collectAsStateWithLifecycle()
    ExpertDashboardContent(
        state = state,
        now = now,
        avatarUrl = avatarUrl,
        onOpenModeSwitch = onOpenModeSwitch,
        onOpenRequests = onOpenRequests,
        onOpenBookingsUpcoming = {
            viewModel.openBookingsUpcoming()
            onOpenBookingsUpcoming()
        },
        onOpenBooking = onOpenBooking,
        onJoin = viewModel::join,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

/**
 * Stateless Dashboard tab: app bar with the avatar action, then the hero and the banners in a scrolling column. The
 * failed state keeps the same scrolling column so pull-to-refresh can still work.
 */
@Composable
fun ExpertDashboardContent(
    state: ExpertDashboardUiState,
    now: Instant,
    avatarUrl: String?,
    onOpenModeSwitch: () -> Unit,
    onOpenRequests: () -> Unit,
    onOpenBookingsUpcoming: () -> Unit,
    onOpenBooking: (String) -> Unit,
    onJoin: (ExpertBookingItem) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val timeFormat = DateFormat.getTimeFormat(LocalContext.current)
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            AuthTopAppBar(
                title = stringResource(R.string.tab_dashboard),
                onBack = null,
                actions = { TopBarAvatarButton(avatarUrl, onClick = onOpenModeSwitch) },
            )
        },
    ) { innerPadding ->
        when (val load = state.load) {
            DashboardLoad.Loading -> Unit
            is DashboardLoad.Failed ->
                Column(
                    modifier =
                        Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    EmptyState(
                        icon = Icons.Outlined.ErrorOutline,
                        heading = stringResource(R.string.dashboard_error_heading),
                        body = load.message.asString(),
                        action = EmptyStateAction(stringResource(R.string.expert_retry), onRetry),
                    )
                }
            DashboardLoad.Loaded ->
                Column(
                    modifier =
                        Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = Spacing.md)
                            .padding(top = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                ) {
                    DashboardHero(firstName = state.firstName)
                    Column {
                        PendingRequestsBanner(count = state.pendingCount, onClick = onOpenRequests)
                        RescheduleRequestsBanner(count = state.rescheduleCount, onClick = onOpenBookingsUpcoming)
                    }
                    Column {
                        TodaysSessionsHeader(onViewCalendar = onOpenBookingsUpcoming)
                        Spacer(Modifier.height(Spacing.sm))
                        if (state.todaysSessions.isEmpty()) {
                            TodaysSessionsEmpty()
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                                state.todaysSessions.forEach { item ->
                                    TodaysSessionCard(
                                        item = item,
                                        now = now,
                                        onOpen = { onOpenBooking(item.bookingId) },
                                        onJoin = { onJoin(item) },
                                        zone = zone,
                                        timeFormat = timeFormat,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.fillMaxWidth().height(Spacing.xl))
                }
        }
    }
}
