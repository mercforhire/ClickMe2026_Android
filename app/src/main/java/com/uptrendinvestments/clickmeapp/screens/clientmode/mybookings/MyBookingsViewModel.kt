package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.managers.CallStartRequest
import com.uptrendinvestments.clickmeapp.managers.CallStarter
import com.uptrendinvestments.clickmeapp.managers.JoinClock
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingItem
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.realtime.RealtimeConnection
import com.uptrendinvestments.clickmeapp.realtime.RealtimeEvent
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.ClientBookingsRepository.Companion.TYPE_PAST
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.ClientBookingsRepository.Companion.TYPE_UPCOMING
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ReviewLaunchRequest
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
import javax.inject.Inject

/**
 * My Bookings state machine (BOOK-05). Port of iOS `MyBookingsViewModel`.
 *
 * Every load fetches both buckets (`upcoming` and `past`, page 1, limit 20, D-09) and derives both tabs from the
 * pair with one combined load state, because the rejoin rule moves rows from the server's Past bucket into
 * Upcoming (RESEARCH Finding 1). iOS calls the two buckets sequentially only for Swift Sendable reasons; here they
 * run in parallel. Page 1 only is a known limit, logged in STATE by plan 08-15.
 *
 * Three refresh triggers (D-10): pull-to-refresh ([refresh], flags a failure for the snackbar); a quiet reload when
 * the screen or the Bookings tab is shown again ([onScreenShown]); and a realtime reload on any `booking.update`.
 * Neither reload shows Loading and a failed one keeps the previous lists (D-11). They differ on failure only
 * (Phase 15 D-05): the tab-return reload stays silent, while the pull and the realtime reload flag the failure so the
 * refresh-failed snackbar offers Retry.
 *
 * D-07 hand-off: the cancel flow posts a [BookingsLaunch] to [BookingsLaunchRequest]. It is consumed in `init` when
 * posted before this ViewModel exists, and on every later emission otherwise; either way it selects its tab, carries
 * the notice for the snackbar, forces a fresh reload and is applied exactly once.
 *
 * D-19 review hand-off: a review submitted from a Past card posts to the `bookingsReviewed` slot of
 * [ReviewLaunchRequest]. It is consumed like the D-07 hand-off, starts a quiet reload and raises
 * `reviewThanks`. Only that slot is read: a review from Booking Summary belongs to the Booking Summary screen.
 * [reviewedBookingIds] lets the list hide the Past pill for ids reviewed this session.
 *
 * Switching sub-tabs never fetches, since both buckets are always loaded together.
 *
 * Join Call (CALL-01, D-01): [join] starts the call through the one [CallStarter] and [joinNow] is the shared 30 s
 * clock that enables each card's button within the join window. iOS wires an empty callback on this card; Android
 * goes beyond it.
 */
@HiltViewModel
class MyBookingsViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val repository: ClientBookingsRepository,
        private val realtime: RealtimeConnection,
        private val clock: Clock,
        private val launchRequest: BookingsLaunchRequest,
        private val reviewLaunch: ReviewLaunchRequest,
        private val callStarter: CallStarter,
        joinClock: JoinClock,
    ) : ViewModel() {
        private enum class LoadMode {
            /** Shows the spinner and a failure message. */
            Loud,

            /** Silent: keeps the previous lists and swallows a failure. */
            Quiet,

            /** Pull-to-refresh: shows the refresh indicator and flags a failure. */
            Pull,

            /**
             * Refetch started by a realtime event (D-05): behaves like [Quiet] (no skeleton, no indicator, lists
             * kept) but flags a failure so the refresh-failed snackbar offers Retry.
             */
            Realtime,
        }

        private val _uiState =
            MutableStateFlow(
                MyBookingsUiState(
                    selectedTab =
                        BookingsTab.entries.firstOrNull { it.name == savedStateHandle.get<String>(KEY_SELECTED_TAB) }
                            ?: BookingsTab.Upcoming,
                ),
            )
        val uiState: StateFlow<MyBookingsUiState> = _uiState.asStateFlow()

        /** Shared 30 s wall clock that turns a visible Join button on without leaving the screen (D-03). */
        val joinNow: StateFlow<Instant> = joinClock.now

        /** Ids reviewed this session, from either opener; the Past pill is hidden for them (UI-SPEC Open Item 2). */
        val reviewedBookingIds: StateFlow<Set<String>> = reviewLaunch.reviewedBookingIds

        private var loadJob: Job? = null
        private var loadIsCurrent = false

        init {
            // D-10 trigger 3: any booking.update refetches. The payload is never read (T-08-25); startLoad cancels
            // the in-flight load, so a burst of events ends with one latest-wins result (T-08-24).
            viewModelScope.launch {
                realtime.events.filterIsInstance<RealtimeEvent.BookingUpdate>().collect {
                    startLoad(
                        if (_uiState.value.load is MyBookingsLoad.Loading) LoadMode.Loud else LoadMode.Realtime,
                    )
                }
            }
            // D-07: a launch posted before this ViewModel existed is applied now, before the first load, so the
            // first load is the fresh one and no second pair runs.
            launchRequest.consume()?.let { applyLaunch(it, reload = false) }
            startLoad(LoadMode.Loud)
            // A launch posted while this ViewModel lives (the Bookings stack is still on screen under the cancel
            // flow) selects its tab and forces a fresh reload.
            viewModelScope.launch {
                launchRequest.pending.filterNotNull().collect {
                    launchRequest.consume()?.let { launch -> applyLaunch(launch, reload = true) }
                }
            }
            // D-19: same shape for a review submitted from a Past card. Posted before this ViewModel existed it is
            // applied before the first load (which is already fresh); otherwise it forces a quiet reload.
            if (reviewLaunch.bookingsReviewed.consume() != null) {
                _uiState.update { it.copy(reviewThanks = true) }
            }
            viewModelScope.launch {
                reviewLaunch.bookingsReviewed.pending.filterNotNull().collect {
                    if (reviewLaunch.bookingsReviewed.consume() != null) {
                        _uiState.update { it.copy(reviewThanks = true) }
                        startLoad(if (_uiState.value.load is MyBookingsLoad.Loaded) LoadMode.Quiet else LoadMode.Loud)
                    }
                }
            }
        }

        /** Starts the call for [item]. The server has the final say on `/join`, so the window is not re-checked (D-02). */
        fun join(item: ClientBookingItem) {
            callStarter.startCall(
                CallStartRequest(
                    bookingId = item.bookingId,
                    peerId = item.expert.id,
                    peerName = item.expert.fullName,
                    peerAvatarUrl = item.expert.avatarUrl,
                    topic = item.topic,
                    scheduledStart = item.startTime,
                    scheduledEnd = item.endTime,
                    meetingType = MeetingType.fromWire(item.meetingType),
                ),
            )
        }

        fun retry() = startLoad(LoadMode.Loud)

        fun refresh() = startLoad(LoadMode.Pull)

        /**
         * D-10 trigger 2: the screen, or the Bookings tab, is shown again. The screen calls this from
         * `LifecycleEventEffect(ON_RESUME)` and `LaunchedEffect(Unit)`. A call while a current load runs does
         * nothing, so first composition never double-loads (RESEARCH Finding 6); a Loaded screen reloads quietly.
         */
        fun onScreenShown() {
            if (loadJob?.isActive == true && loadIsCurrent) return
            startLoad(if (_uiState.value.load is MyBookingsLoad.Loaded) LoadMode.Quiet else LoadMode.Loud)
        }

        /**
         * The screen was hidden or disposed (`ON_PAUSE`, `DisposableEffect` disposal; Nav3 may not deliver ON_RESUME
         * to a covered entry, Phase 7 A10). A load started before this may predate a change made while hidden, so
         * the next [onScreenShown] restarts it (MakeABooking WR-06).
         */
        fun onScreenHidden() {
            loadIsCurrent = false
        }

        fun consumeNotice() {
            _uiState.update { it.copy(notice = null) }
        }

        fun consumeReviewThanks() {
            _uiState.update { it.copy(reviewThanks = false) }
        }

        private fun applyLaunch(
            launch: BookingsLaunch,
            reload: Boolean,
        ) {
            savedStateHandle[KEY_SELECTED_TAB] = launch.tab.name
            _uiState.update { it.copy(selectedTab = launch.tab, notice = launch.notice) }
            if (reload) {
                startLoad(if (_uiState.value.load is MyBookingsLoad.Loaded) LoadMode.Quiet else LoadMode.Loud)
            }
        }

        /** Selects [tab]; no fetch, both buckets are already loaded. Survives rotation and process death. */
        fun selectTab(tab: BookingsTab) {
            savedStateHandle[KEY_SELECTED_TAB] = tab.name
            _uiState.update { it.copy(selectedTab = tab) }
        }

        fun consumeRefreshFailure() {
            _uiState.update { it.copy(refreshFailed = false) }
        }

        private fun startLoad(mode: LoadMode) {
            loadJob?.cancel()
            loadIsCurrent = true
            _uiState.update {
                when (mode) {
                    LoadMode.Loud -> it.copy(load = MyBookingsLoad.Loading)
                    LoadMode.Pull -> it.copy(isRefreshing = true)
                    LoadMode.Quiet, LoadMode.Realtime -> it
                }
            }
            loadJob =
                viewModelScope.launch {
                    try {
                        val lists = loadAll()
                        _uiState.update {
                            it.copy(lists = lists, load = MyBookingsLoad.Loaded, isRefreshing = false)
                        }
                    } catch (e: ApiException) {
                        _uiState.update {
                            when (mode) {
                                LoadMode.Loud ->
                                    it.copy(load = MyBookingsLoad.Failed(e.toSectionMessage()), isRefreshing = false)
                                LoadMode.Pull, LoadMode.Realtime -> it.copy(refreshFailed = true, isRefreshing = false)
                                LoadMode.Quiet -> it.copy(isRefreshing = false)
                            }
                        }
                    }
                }
        }

        private suspend fun loadAll(): BookingLists =
            coroutineScope {
                val upcoming = async { repository.list(TYPE_UPCOMING, PAGE, PAGE_SIZE) }
                val past = async { repository.list(TYPE_PAST, PAGE, PAGE_SIZE) }
                buildBookingLists(upcoming.await().bookings, past.await().bookings, clock.instant())
            }

        companion object {
            const val KEY_SELECTED_TAB = "my_bookings_selected_tab"
            const val PAGE = 1
            const val PAGE_SIZE = 20
        }
    }
