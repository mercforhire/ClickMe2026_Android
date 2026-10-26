package com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.managers.CallStartRequest
import com.uptrendinvestments.clickmeapp.managers.CallStarter
import com.uptrendinvestments.clickmeapp.managers.JoinClock
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingItem
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.realtime.RealtimeConnection
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsTab
import com.uptrendinvestments.clickmeapp.screens.expertmode.ExpertLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository.Companion.PAGE
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository.Companion.PAGE_SIZE
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository.Companion.TYPE_PAST
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository.Companion.TYPE_UPCOMING
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Expert Bookings tab state machine (EXP-08). Port of iOS `UpcomingBookingsViewModel`, built on the Phase 8
 * `MyBookingsViewModel` state machine (RESEARCH Pattern 1, Pitfall 7).
 *
 * Every load fetches both buckets (`upcoming` and `past`, page 1, limit 50, D-15) and derives both tabs from the pair
 * with one combined load state, because the rejoin rule moves rows from the server's Past bucket into Upcoming
 * (RESEARCH C12). iOS calls the two buckets sequentially only for Swift Sendable reasons; here they run in parallel.
 *
 * Load modes: Loud shows the skeleton and a failure message, Pull is pull-to-refresh and flags a failure, Quiet keeps
 * the previous lists and swallows a failure (D-13). A newer load cancels the one in flight, so the latest wins.
 *
 * Switching sub-tabs never fetches, since both buckets are always loaded together.
 *
 * Join (D-14): [join] starts the call through the one [CallStarter] with the client as the peer, and [joinNow] is the
 * shared 30 s clock that enables each card's button within the join window. The window is not re-checked in [join]:
 * the server has the final say on `POST /join` (Phase 9 D-02).
 */
@HiltViewModel
class ExpertBookingsViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val repository: ExpertBookingsRepository,
        private val realtime: RealtimeConnection,
        private val clock: Clock,
        private val launchRequest: ExpertLaunchRequest,
        private val callStarter: CallStarter,
        joinClock: JoinClock,
    ) : ViewModel() {
        private enum class LoadMode {
            /** Shows the skeleton and a failure message. */
            Loud,

            /** Silent: keeps the previous lists and swallows a failure. */
            Quiet,

            /** Pull-to-refresh: shows the refresh indicator and flags a failure. */
            Pull,
        }

        private val _uiState =
            MutableStateFlow(
                ExpertBookingsUiState(
                    selectedTab =
                        BookingsTab.entries.firstOrNull { it.name == savedStateHandle.get<String>(KEY_SELECTED_TAB) }
                            ?: BookingsTab.Upcoming,
                ),
            )
        val uiState: StateFlow<ExpertBookingsUiState> = _uiState.asStateFlow()

        /** Shared 30 s wall clock that turns a visible Join button on without leaving the screen. */
        val joinNow: StateFlow<Instant> = joinClock.now

        private var loadJob: Job? = null
        private var loadIsCurrent = false

        init {
            startLoad(LoadMode.Loud)
        }

        /**
         * Starts the call for [item] with the client as the peer. The server has the final say on `/join`, so the
         * window is not re-checked here.
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

        fun retry() = startLoad(LoadMode.Loud)

        /** Pull-to-refresh: keeps the lists visible, shows the indicator and flags a failure (D-13). */
        fun refresh() = startLoad(LoadMode.Pull)

        /** Selects [tab]; no fetch, both buckets are already loaded. Survives rotation and process death. */
        fun selectTab(tab: BookingsTab) {
            savedStateHandle[KEY_SELECTED_TAB] = tab.name
            _uiState.update { it.copy(selectedTab = tab) }
        }

        private fun startLoad(mode: LoadMode) {
            loadJob?.cancel()
            loadIsCurrent = true
            _uiState.update {
                when (mode) {
                    LoadMode.Loud -> it.copy(load = ExpertBookingsLoad.Loading)
                    LoadMode.Pull -> it.copy(isRefreshing = true)
                    LoadMode.Quiet -> it
                }
            }
            loadJob =
                viewModelScope.launch {
                    try {
                        val lists = loadAll()
                        _uiState.update {
                            it.copy(lists = lists, load = ExpertBookingsLoad.Loaded, isRefreshing = false)
                        }
                    } catch (e: ApiException) {
                        val failed = ExpertBookingsLoad.Failed(e.toSectionMessage())
                        _uiState.update {
                            when (mode) {
                                LoadMode.Loud -> it.copy(load = failed, isRefreshing = false)
                                LoadMode.Pull -> it.copy(refreshFailed = true, isRefreshing = false)
                                LoadMode.Quiet -> it.copy(isRefreshing = false)
                            }
                        }
                    }
                }
        }

        private suspend fun loadAll(): ExpertBookingLists =
            coroutineScope {
                val upcoming = async { repository.list(TYPE_UPCOMING, PAGE, PAGE_SIZE) }
                val past = async { repository.list(TYPE_PAST, PAGE, PAGE_SIZE) }
                buildExpertBookingLists(upcoming.await().bookings, past.await().bookings, clock.instant())
            }

        companion object {
            const val KEY_SELECTED_TAB = "expert_bookings_selected_tab"
        }
    }
