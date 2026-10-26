package com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.managers.CallStartRequest
import com.uptrendinvestments.clickmeapp.managers.CallStarter
import com.uptrendinvestments.clickmeapp.managers.JoinClock
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestItem
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingItem
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingsPayload
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.realtime.RealtimeConnection
import com.uptrendinvestments.clickmeapp.realtime.RealtimeEvent
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsLaunch
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsTab
import com.uptrendinvestments.clickmeapp.screens.expertmode.ExpertLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests.BookingRequestsRepository
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository.Companion.TYPE_UPCOMING
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/**
 * Expert Dashboard workload state machine (EXP-05). Port of the booking and request half of iOS
 * `ExpertDashboardViewModel`, with the pending count taken from the list size (RESEARCH C2: the list endpoint has no
 * pagination block, so the iOS total is always the item count).
 *
 * The load fetches upcoming bookings (limit 50) and the pending requests in parallel. Both are hard: a failure of
 * either gives [DashboardLoad.Failed] (D-13).
 *
 * Refresh triggers (D-05, D-13): pull-to-refresh ([refresh], flags a failure for the snackbar); a quiet reload when
 * the tab is shown again ([onScreenShown]); a quiet reload on every realtime `booking.update`; and a quiet reload
 * when "Back to Home" posts [ExpertLaunchRequest.dashboard] so the pending count drops. Quiet reloads never show
 * Loading and a failed one keeps the previous data. Every start cancels the load in flight, so a burst of events or
 * a pull during a quiet reload ends with one latest-wins result and never two overlapping fetch pairs (Pitfall 7).
 *
 * Nothing is logged: the realtime payload is never read and the lists hold client names.
 */
@HiltViewModel
class ExpertDashboardViewModel
    @Inject
    constructor(
        private val bookings: ExpertBookingsRepository,
        private val requests: BookingRequestsRepository,
        private val userManager: UserManager,
        private val realtime: RealtimeConnection,
        private val clock: Clock,
        private val launchRequest: ExpertLaunchRequest,
        private val callStarter: CallStarter,
        joinClock: JoinClock,
    ) : ViewModel() {
        private enum class LoadMode {
            /** Shows the skeleton and a failure message. */
            Loud,

            /** Silent: keeps the previous data and swallows a failure. */
            Quiet,

            /** Pull-to-refresh: shows the refresh indicator and flags a failure. */
            Pull,
        }

        private val _uiState = MutableStateFlow(ExpertDashboardUiState())
        val uiState: StateFlow<ExpertDashboardUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null
        private var loadIsCurrent = false

        /** Shared 30 s wall clock that turns a visible Join button on without leaving the screen (Phase 9 D-03). */
        val joinNow: StateFlow<Instant> = joinClock.now

        init {
            // D-13: any booking.update refetches. The payload is never read; startLoad cancels the load in flight, so a
            // burst of events ends with one latest-wins result.
            viewModelScope.launch {
                realtime.events.filterIsInstance<RealtimeEvent.BookingUpdate>().collect {
                    startLoad(if (_uiState.value.load is DashboardLoad.Loading) LoadMode.Loud else LoadMode.Quiet)
                }
            }
            // D-05: a Back to Home posted before this ViewModel existed is applied now, so the first load is the fresh
            // one and no second pair runs.
            launchRequest.dashboard.consume()
            startLoad(LoadMode.Loud)
            // A launch posted while this ViewModel lives (the Dashboard is still on screen under the flow) reloads
            // quietly, once.
            viewModelScope.launch {
                launchRequest.dashboard.pending.filterNotNull().collect {
                    launchRequest.dashboard.consume()?.let { _ ->
                        startLoad(if (_uiState.value.load is DashboardLoad.Loaded) LoadMode.Quiet else LoadMode.Loud)
                    }
                }
            }
        }

        fun retry() = startLoad(LoadMode.Loud)

        fun refresh() = startLoad(LoadMode.Pull)

        /**
         * The tab is shown again. The screen calls this from `LifecycleEventEffect(ON_RESUME)` and
         * `LaunchedEffect(Unit)`. A call while a current load runs does nothing, so first composition never
         * double-loads; a Loaded screen reloads quietly.
         */
        fun onScreenShown() {
            if (loadJob?.isActive == true && loadIsCurrent) return
            startLoad(if (_uiState.value.load is DashboardLoad.Loaded) LoadMode.Quiet else LoadMode.Loud)
        }

        /**
         * The screen was hidden or disposed (`ON_PAUSE`, disposal; Nav3 may not deliver ON_RESUME to a covered entry).
         * A load started before this may predate a change made while hidden, so the next [onScreenShown] restarts it.
         */
        fun onScreenHidden() {
            loadIsCurrent = false
        }

        fun consumeRefreshFailure() {
            _uiState.update { it.copy(refreshFailed = false) }
        }

        /**
         * Starts the call for [item] with the client as the peer (D-14, Join site 2). The server has the final say on
         * `POST /join`, so the window is not re-checked here (Phase 9 D-02).
         */
        fun join(item: ExpertBookingItem) {
            callStarter.startCall(
                CallStartRequest(
                    bookingId = item.bookingId,
                    peerId = item.client.id,
                    peerName = item.client.name,
                    peerAvatarUrl = item.client.avatarUrl,
                    topic = item.topic,
                    scheduledStart = item.startTime,
                    scheduledEnd = item.endTime,
                    meetingType = MeetingType.fromWire(item.session.meetingType),
                ),
            )
        }

        /**
         * D-12: "View Calendar" and the reschedule banner land on the Bookings tab's Upcoming list. The Bookings
         * ViewModel consumes the hand-off; the screen then asks the navigator to select that tab.
         */
        fun openBookingsUpcoming() {
            launchRequest.bookings.post(BookingsLaunch(BookingsTab.Upcoming))
        }

        private fun startLoad(mode: LoadMode) {
            loadJob?.cancel()
            loadIsCurrent = true
            _uiState.update {
                when (mode) {
                    LoadMode.Loud -> it.copy(load = DashboardLoad.Loading)
                    LoadMode.Pull -> it.copy(isRefreshing = true)
                    LoadMode.Quiet -> it
                }
            }
            loadJob =
                viewModelScope.launch {
                    try {
                        val (upcoming, pending) = fetch()
                        _uiState.update { applyLoaded(it, upcoming, pending).copy(isRefreshing = false) }
                    } catch (e: ApiException) {
                        _uiState.update {
                            when (mode) {
                                LoadMode.Loud ->
                                    it.copy(load = DashboardLoad.Failed(e.toSectionMessage()), isRefreshing = false)
                                LoadMode.Pull -> it.copy(refreshFailed = true, isRefreshing = false)
                                LoadMode.Quiet -> it.copy(isRefreshing = false)
                            }
                        }
                    }
                }
        }

        private suspend fun fetch(): Pair<ExpertBookingsPayload, List<BookingRequestItem>> =
            coroutineScope {
                val upcoming = async { bookings.list(TYPE_UPCOMING) }
                val pending = async { requests.list() }
                upcoming.await() to pending.await()
            }

        private fun applyLoaded(
            state: ExpertDashboardUiState,
            upcoming: ExpertBookingsPayload,
            pending: List<BookingRequestItem>,
        ): ExpertDashboardUiState =
            state.copy(
                load = DashboardLoad.Loaded,
                firstName =
                    dashboardFirstName(
                        userManager.authUser.value?.firstName,
                        userManager.profile.value?.let { it.personalDetails.firstName },
                    ),
                pendingCount = pending.size,
                rescheduleCount = upcoming.bookings.clientRescheduleCount(),
                todaysSessions = upcoming.bookings.expertTodaysSessions(clock, ZoneId.systemDefault()),
            )
    }
