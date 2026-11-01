package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.RefreshFailedSnackbarEffect
import com.uptrendinvestments.clickmeapp.components.chat.ChatEntryErrorDialog
import com.uptrendinvestments.clickmeapp.managers.isWithinJoinWindow
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingItem
import com.uptrendinvestments.clickmeapp.screens.chat.ChatEntryViewModel
import com.uptrendinvestments.clickmeapp.screens.chat.ChatPeer
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.subviews.MyBookingsEmptyState
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.subviews.MyBookingsError
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.subviews.MyBookingsSkeleton
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.subviews.PastBookingCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.subviews.UpcomingBookingCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.canClientStartReschedule
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.ReviewOpener
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.WriteReviewKey
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** Test tag on the My Bookings list (either tab). */
const val MY_BOOKINGS_LIST_TAG = "my_bookings_list"

private val TabMinHeight = 48.dp

/**
 * Hilt-aware My Bookings tab root (BOOK-05).
 *
 * D-10 reload triggers: the screen reports it was shown on resume and on every (re)entry to composition, and hidden on
 * pause and disposal, so the ViewModel can coalesce quiet reloads (a call while a current load runs is ignored, which
 * makes the first composition safe, RESEARCH Finding 6). Pull-to-refresh and realtime updates live in the ViewModel.
 */
@Composable
fun MyBookingsScreen(
    onOpenUpcoming: (String) -> Unit,
    onOpenPast: (String) -> Unit,
    onExploreExperts: () -> Unit,
    onLeaveReview: (WriteReviewKey) -> Unit,
    onReschedule: (bookingId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyBookingsViewModel = hiltViewModel(),
    chatEntry: ChatEntryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val joinNow by viewModel.joinNow.collectAsStateWithLifecycle()
    val reviewedBookingIds by viewModel.reviewedBookingIds.collectAsStateWithLifecycle()
    val entryState by chatEntry.state.collectAsStateWithLifecycle()
    ChatEntryErrorDialog(error = entryState.error, onDismiss = chatEntry::dismissError)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onScreenShown() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.onScreenHidden() }
    DisposableEffect(viewModel) { onDispose { viewModel.onScreenHidden() } }
    // The Nav3 entry may not receive ON_RESUME when it is re-shown; it leaves and re-enters composition instead.
    LaunchedEffect(Unit) { viewModel.onScreenShown() }

    MyBookingsContent(
        state = state,
        onSelectTab = viewModel::selectTab,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
        onOpenUpcoming = onOpenUpcoming,
        onOpenPast = onOpenPast,
        onExploreExperts = onExploreExperts,
        onNoticeConsumed = viewModel::consumeNotice,
        onRefreshFailureConsumed = viewModel::consumeRefreshFailure,
        joinNow = joinNow,
        onJoin = viewModel::join,
        modifier = modifier,
        onLeaveReview = onLeaveReview,
        reviewedBookingIds = reviewedBookingIds,
        onReviewThanksShown = viewModel::consumeReviewThanks,
        onReschedule = onReschedule,
        onRescheduleSentShown = viewModel::consumeRescheduleSent,
        messageBusyPeerId = entryState.busyPeerId,
        onMessage = { item ->
            chatEntry.open(ChatPeer(item.expert.id, item.expert.fullName, item.expert.avatarUrl, isExpert = true))
        },
    )
}

/**
 * Stateless My Bookings: app bar, full-width Upcoming / Past tabs, pull-to-refresh and the list or state of the
 * selected tab (iOS `MyBookingsView`).
 *
 * Both tabs load together (RESEARCH Finding 1), so the skeleton shows on the first load only and not per tab (this
 * replaces the UI-SPEC per-tab lazy load). States follow D-11: skeleton, error with Retry, per-tab empty copy. Cards
 * offer the D-12 actions only: tapping opens details or summary, Join Call is live within the join window (D-01) and
 * Upcoming cards carry Message (D-14, [messageBusyPeerId] is the peer being opened). Pull-to-refresh also works from
 * the empty and error states because they scroll. A failed pull and the post-cancel notice (D-07) show as snackbars.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingsContent(
    state: MyBookingsUiState,
    onSelectTab: (BookingsTab) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onOpenUpcoming: (String) -> Unit,
    onOpenPast: (String) -> Unit,
    onExploreExperts: () -> Unit,
    onNoticeConsumed: () -> Unit,
    onRefreshFailureConsumed: () -> Unit,
    joinNow: Instant,
    onJoin: (ClientBookingItem) -> Unit,
    modifier: Modifier = Modifier,
    messageBusyPeerId: String? = null,
    onMessage: (ClientBookingItem) -> Unit = {},
    onLeaveReview: (WriteReviewKey) -> Unit = {},
    reviewedBookingIds: Set<String> = emptySet(),
    onReviewThanksShown: () -> Unit = {},
    onReschedule: (String) -> Unit = {},
    onRescheduleSentShown: () -> Unit = {},
    zone: ZoneId = ZoneId.systemDefault(),
    timeFormat: DateFormat = rememberDeviceTimeFormat(),
    locale: Locale = LocalConfiguration.current.locales[0],
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val upcomingListState = rememberLazyListState()
    val pastListState = rememberLazyListState()
    val refreshFailedMessage = stringResource(R.string.my_bookings_refresh_failed)
    val reviewThanksMessage = stringResource(R.string.review_thanks_snackbar)
    val rescheduleSentMessage = stringResource(R.string.reschedule_start_sent)
    val noticeMessage =
        when (val notice = state.notice) {
            null -> null
            BookingsNotice.Cancelled -> stringResource(R.string.cancel_booking_result_cancelled)
            is BookingsNotice.CancelledWithRefund ->
                stringResource(R.string.cancel_booking_result_refunded, notice.amount)
        }

    LaunchedEffect(state.notice) {
        if (noticeMessage != null) {
            // Launched on the composition scope so consuming the notice does not cancel the Snackbar.
            scope.launch { snackbarHostState.showSnackbar(noticeMessage, duration = SnackbarDuration.Long) }
            onNoticeConsumed()
        }
    }
    LaunchedEffect(state.reviewThanks) {
        if (state.reviewThanks) {
            // D-19: Short, and launched on the composition scope so consuming the flag does not cancel it.
            scope.launch { snackbarHostState.showSnackbar(reviewThanksMessage, duration = SnackbarDuration.Short) }
            onReviewThanksShown()
        }
    }
    LaunchedEffect(state.rescheduleSent) {
        if (state.rescheduleSent) {
            // D-09: Short, launched on the composition scope so consuming the flag does not cancel it.
            scope.launch { snackbarHostState.showSnackbar(rescheduleSentMessage, duration = SnackbarDuration.Short) }
            onRescheduleSentShown()
        }
    }
    RefreshFailedSnackbarEffect(
        refreshFailed = state.refreshFailed,
        snackbarHostState = snackbarHostState,
        onRetry = onRefresh,
        onShown = onRefreshFailureConsumed,
        message = refreshFailedMessage,
    )

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.my_bookings_title), style = MaterialTheme.typography.titleLarge)
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                                            BookingsTab.Upcoming -> R.string.my_bookings_tab_upcoming
                                            BookingsTab.Past -> R.string.my_bookings_tab_past
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
                    label = "myBookingsTab",
                ) { tab ->
                    TabBody(
                        tab = tab,
                        state = state,
                        listState = if (tab == BookingsTab.Upcoming) upcomingListState else pastListState,
                        onRetry = onRetry,
                        onOpenUpcoming = onOpenUpcoming,
                        onOpenPast = onOpenPast,
                        onExploreExperts = onExploreExperts,
                        joinNow = joinNow,
                        onJoin = onJoin,
                        messageBusyPeerId = messageBusyPeerId,
                        onMessage = onMessage,
                        onLeaveReview = onLeaveReview,
                        reviewedBookingIds = reviewedBookingIds,
                        onReschedule = onReschedule,
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
private fun TabBody(
    tab: BookingsTab,
    state: MyBookingsUiState,
    listState: LazyListState,
    onRetry: () -> Unit,
    onOpenUpcoming: (String) -> Unit,
    onOpenPast: (String) -> Unit,
    onExploreExperts: () -> Unit,
    joinNow: Instant,
    onJoin: (ClientBookingItem) -> Unit,
    messageBusyPeerId: String?,
    onMessage: (ClientBookingItem) -> Unit,
    onLeaveReview: (WriteReviewKey) -> Unit,
    reviewedBookingIds: Set<String>,
    onReschedule: (String) -> Unit,
    zone: ZoneId,
    timeFormat: DateFormat,
    locale: Locale,
) {
    when (val load = state.load) {
        MyBookingsLoad.Loading -> MyBookingsSkeleton()
        is MyBookingsLoad.Failed ->
            ScrollableCentered { MyBookingsError(message = load.message, onRetry = onRetry) }
        MyBookingsLoad.Loaded -> {
            val isEmpty =
                when (tab) {
                    BookingsTab.Upcoming -> state.lists.upcoming.isEmpty()
                    BookingsTab.Past -> state.lists.past.isEmpty()
                }
            if (isEmpty) {
                ScrollableCentered { MyBookingsEmptyState(tab = tab, onExploreExperts = onExploreExperts) }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().testTag(MY_BOOKINGS_LIST_TAG),
                    contentPadding =
                        PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.lg, bottom = Spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    when (tab) {
                        BookingsTab.Upcoming ->
                            items(items = state.lists.upcoming, key = { it.bookingId }) { item ->
                                UpcomingBookingCard(
                                    item = item,
                                    onClick = { onOpenUpcoming(item.bookingId) },
                                    joinEnabled =
                                        isWithinJoinWindow(
                                            item.status,
                                            item.startTime,
                                            item.endTime,
                                            joinNow,
                                        ),
                                    onJoin = { onJoin(item) },
                                    modifier = Modifier.animateItem().contentMaxWidth(),
                                    messageBusy = messageBusyPeerId.equals(item.expert.id, ignoreCase = true),
                                    onMessage = { onMessage(item) },
                                    zone = zone,
                                    timeFormat = timeFormat,
                                    locale = locale,
                                    onReschedule =
                                        if (canClientStartReschedule(item.status, item.endTime, joinNow)) {
                                            { onReschedule(item.bookingId) }
                                        } else {
                                            null
                                        },
                                )
                            }
                        BookingsTab.Past ->
                            items(items = state.lists.past, key = { it.item.bookingId }) { row ->
                                PastBookingCard(
                                    row = row,
                                    onClick = { onOpenPast(row.item.bookingId) },
                                    modifier = Modifier.animateItem().contentMaxWidth(),
                                    onLeaveReview =
                                        if (row.status.raw == STATUS_COMPLETED &&
                                            row.item.bookingId !in reviewedBookingIds
                                        ) {
                                            {
                                                onLeaveReview(
                                                    WriteReviewKey(
                                                        bookingId = row.item.bookingId,
                                                        expertName = row.item.expert.fullName,
                                                        expertAvatarUrl = row.item.expert.avatarUrl,
                                                        opener = ReviewOpener.MyBookings,
                                                    ),
                                                )
                                            }
                                        } else {
                                            null
                                        },
                                    zone = zone,
                                    timeFormat = timeFormat,
                                    locale = locale,
                                )
                            }
                    }
                }
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
