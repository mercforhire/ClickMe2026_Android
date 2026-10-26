package com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.EmptyStateAction
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingItem
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsTab
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.subviews.ExpertBookingsSkeleton
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.subviews.ExpertPastCard
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.subviews.ExpertUpcomingCard
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** Test tag on the expert Bookings list (either tab). */
const val EXPERT_BOOKINGS_LIST_TAG = "expert_bookings_list"

/** Test tag on the scrollable centered column that holds the empty and error states. */
const val EXPERT_BOOKINGS_STATE_TAG = "expert_bookings_state"

private val TabMinHeight = 48.dp

/**
 * Hilt-aware expert Bookings tab root (EXP-08).
 *
 * @param onOpenBooking opens the expert Booking Summary for a booking id.
 * @param onUpdateAvailability opens Availability from the empty Upcoming state.
 */
@Composable
fun ExpertBookingsScreen(
    onOpenBooking: (String) -> Unit,
    onUpdateAvailability: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpertBookingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val joinNow by viewModel.joinNow.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onScreenShown() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.onScreenHidden() }
    DisposableEffect(viewModel) { onDispose { viewModel.onScreenHidden() } }
    // The Nav3 entry may not receive ON_RESUME when it is re-shown; it leaves and re-enters composition instead.
    LaunchedEffect(Unit) { viewModel.onScreenShown() }

    ExpertBookingsContent(
        state = state,
        joinNow = joinNow,
        onSelectTab = viewModel::selectTab,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
        onOpenBooking = onOpenBooking,
        onJoin = viewModel::join,
        onUpdateAvailability = onUpdateAvailability,
        onNoticeConsumed = viewModel::consumeNotice,
        onRefreshFailureConsumed = viewModel::consumeRefreshFailure,
        modifier = modifier,
    )
}

/**
 * Stateless expert Bookings tab: app bar, full-width Upcoming / Past tabs, pull-to-refresh and the list of the
 * selected tab (UI-SPEC surface 5). Cards offer the D-14 and D-15 actions only: tapping opens the summary and Join is
 * live within the join window.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList", "UNUSED_PARAMETER")
@Composable
fun ExpertBookingsContent(
    state: ExpertBookingsUiState,
    joinNow: Instant,
    onSelectTab: (BookingsTab) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onOpenBooking: (String) -> Unit,
    onJoin: (ExpertBookingItem) -> Unit,
    onUpdateAvailability: () -> Unit,
    onNoticeConsumed: () -> Unit,
    onRefreshFailureConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    timeFormat: DateFormat = rememberDeviceTimeFormat(),
    locale: Locale = LocalConfiguration.current.locales[0],
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { AuthTopAppBar(title = stringResource(R.string.expert_bookings_title), onBack = null) },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            PrimaryTabRow(
                selectedTabIndex = state.selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                BookingsTab.entries.forEach { tab ->
                    val selected = tab == state.selectedTab
                    Tab(
                        selected = selected,
                        onClick = { onSelectTab(tab) },
                        modifier = Modifier.heightIn(min = TabMinHeight),
                        text = {
                            Text(
                                text =
                                    stringResource(
                                        when (tab) {
                                            BookingsTab.Upcoming -> R.string.expert_bookings_tab_upcoming
                                            BookingsTab.Past -> R.string.expert_bookings_tab_past
                                        },
                                    ),
                                style =
                                    if (selected) {
                                        MaterialTheme.typography.titleMedium
                                    } else {
                                        MaterialTheme.typography.bodyLarge
                                    },
                                color =
                                    if (selected) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                            )
                        },
                    )
                }
            }
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                Crossfade(
                    targetState = state.selectedTab,
                    animationSpec = tween(Motion.CONTROL_STATE_DURATION_MS),
                    label = "expertBookingsTab",
                ) { tab ->
                    ExpertBookingsTabBody(
                        tab = tab,
                        state = state,
                        joinNow = joinNow,
                        onRetry = onRetry,
                        onSelectTab = onSelectTab,
                        onUpdateAvailability = onUpdateAvailability,
                        onOpenBooking = onOpenBooking,
                        onJoin = onJoin,
                        zone = zone,
                        timeFormat = timeFormat,
                        locale = locale,
                    )
                }
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun ExpertBookingsTabBody(
    tab: BookingsTab,
    state: ExpertBookingsUiState,
    joinNow: Instant,
    onRetry: () -> Unit,
    onSelectTab: (BookingsTab) -> Unit,
    onUpdateAvailability: () -> Unit,
    onOpenBooking: (String) -> Unit,
    onJoin: (ExpertBookingItem) -> Unit,
    zone: ZoneId,
    timeFormat: DateFormat,
    locale: Locale,
) {
    when (val load = state.load) {
        ExpertBookingsLoad.Loading -> ExpertBookingsSkeleton(past = tab == BookingsTab.Past)
        is ExpertBookingsLoad.Failed -> ScrollableCentered { ExpertBookingsError(load.message, onRetry) }
        ExpertBookingsLoad.Loaded -> {
            val isEmpty =
                when (tab) {
                    BookingsTab.Upcoming -> state.lists.upcoming.isEmpty()
                    BookingsTab.Past -> state.lists.past.isEmpty()
                }
            if (isEmpty) {
                ScrollableCentered {
                    ExpertBookingsEmptyState(tab, onSelectTab, onUpdateAvailability)
                }
            } else {
                ExpertBookingsList(
                    tab = tab,
                    state = state,
                    joinNow = joinNow,
                    onOpenBooking = onOpenBooking,
                    onJoin = onJoin,
                    zone = zone,
                    timeFormat = timeFormat,
                    locale = locale,
                )
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun ExpertBookingsList(
    tab: BookingsTab,
    state: ExpertBookingsUiState,
    joinNow: Instant,
    onOpenBooking: (String) -> Unit,
    onJoin: (ExpertBookingItem) -> Unit,
    zone: ZoneId,
    timeFormat: DateFormat,
    locale: Locale,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag(EXPERT_BOOKINGS_LIST_TAG),
        contentPadding =
            PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.lg, bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        when (tab) {
            BookingsTab.Upcoming ->
                items(items = state.lists.upcoming, key = { it.bookingId }) { item ->
                    ExpertUpcomingCard(
                        item = item,
                        now = joinNow,
                        onOpen = { onOpenBooking(item.bookingId) },
                        onJoin = { onJoin(item) },
                        modifier = Modifier.animateItem(),
                        zone = zone,
                        timeFormat = timeFormat,
                        locale = locale,
                    )
                }
            BookingsTab.Past ->
                items(items = state.lists.past, key = { it.item.bookingId }) { row ->
                    ExpertPastCard(
                        row = row,
                        onOpen = { onOpenBooking(row.item.bookingId) },
                        modifier = Modifier.animateItem(),
                        zone = zone,
                        timeFormat = timeFormat,
                        locale = locale,
                    )
                }
        }
    }
}

/** Empty copy of a tab. Upcoming also offers the availability and Past shortcuts (UI-SPEC A3). */
@Composable
private fun ExpertBookingsEmptyState(
    tab: BookingsTab,
    onSelectTab: (BookingsTab) -> Unit,
    onUpdateAvailability: () -> Unit,
) {
    when (tab) {
        BookingsTab.Upcoming -> {
            EmptyState(
                icon = Icons.Outlined.CalendarMonth,
                heading = stringResource(R.string.expert_bookings_empty_upcoming_heading),
                body = stringResource(R.string.expert_bookings_empty_upcoming_body),
                action =
                    EmptyStateAction(
                        label = stringResource(R.string.expert_bookings_update_availability),
                        onClick = onUpdateAvailability,
                    ),
            )
            TextButton(
                onClick = { onSelectTab(BookingsTab.Past) },
                modifier = Modifier.padding(top = Spacing.sm).heightIn(min = TabMinHeight),
            ) {
                Text(
                    text = stringResource(R.string.expert_bookings_view_past),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        BookingsTab.Past ->
            EmptyState(
                icon = Icons.Outlined.History,
                heading = stringResource(R.string.expert_bookings_empty_past_heading),
                body = stringResource(R.string.expert_bookings_empty_past_body),
            )
    }
}

/** First-load failure with Retry: the server message, or the connection message for a network error. */
@Composable
private fun ExpertBookingsError(
    message: FieldError,
    onRetry: () -> Unit,
) {
    EmptyState(
        icon = Icons.Outlined.ErrorOutline,
        heading = stringResource(R.string.expert_bookings_error_heading),
        body = message.asString(),
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        action = EmptyStateAction(stringResource(R.string.expert_retry), onRetry),
    )
}

/** Vertically scrollable, centered column so pull-to-refresh works from the error state. */
@Composable
private fun ScrollableCentered(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().testTag(EXPERT_BOOKINGS_STATE_TAG).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}
