package com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.managers.CallStarter
import com.uptrendinvestments.clickmeapp.managers.JoinClock
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestItem
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingsPayload
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.realtime.RealtimeConnection
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsLaunch
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsTab
import com.uptrendinvestments.clickmeapp.screens.expertmode.ExpertLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests.BookingRequestsRepository
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository.Companion.TYPE_UPCOMING
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/**
 * Expert Dashboard workload state machine (EXP-05). Port of the booking and request half of iOS
 * `ExpertDashboardViewModel`, with the pending count taken from the list size (RESEARCH C2: the list endpoint has no
 * pagination block, so the iOS total is always the item count).
 *
 * The load fetches upcoming bookings (limit 50) and the pending requests in parallel. Both are hard: a failure of
 * either gives [DashboardLoad.Failed] (D-13).
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
        private val joinClock: JoinClock,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ExpertDashboardUiState())
        val uiState: StateFlow<ExpertDashboardUiState> = _uiState.asStateFlow()

        init {
            startLoad()
        }

        fun retry() {
            _uiState.update { it.copy(load = DashboardLoad.Loading) }
            startLoad()
        }

        /**
         * D-12: "View Calendar" and the reschedule banner land on the Bookings tab's Upcoming list. The Bookings
         * ViewModel consumes the hand-off; the screen then asks the navigator to select that tab.
         */
        fun openBookingsUpcoming() {
            launchRequest.bookings.post(BookingsLaunch(BookingsTab.Upcoming))
        }

        private fun startLoad() {
            viewModelScope.launch {
                try {
                    val (upcoming, pending) = fetch()
                    _uiState.update { applyLoaded(it, upcoming, pending) }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(load = DashboardLoad.Failed(e.toSectionMessage())) }
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
            )
    }
