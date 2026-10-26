package com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.managers.CallGuard
import com.uptrendinvestments.clickmeapp.managers.CallStarter
import com.uptrendinvestments.clickmeapp.managers.JoinClock
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingDetail
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.realtime.RealtimeConnection
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.util.Locale

/**
 * Expert Booking Summary state machine (EXP-08 / EXP-09). Port of iOS `ExpertBookingSummaryViewModel`, loading from
 * `GET expert/bookings/{id}/details` (never a bare-id GET, RESEARCH C1).
 *
 * The route carries only the booking id, so the screen loads its own booking whichever list opened it. Loads are
 * latest-wins. The loaded [ExpertBookingDetail] is kept in memory ([loadedDetail]) for the reschedule card (11-13).
 */
@HiltViewModel(assistedFactory = ExpertBookingSummaryViewModel.Factory::class)
class ExpertBookingSummaryViewModel
    @AssistedInject
    constructor(
        @Assisted key: ExpertBookingSummaryKey,
        private val repository: ExpertBookingsRepository,
        private val realtime: RealtimeConnection,
        private val clock: Clock,
        private val callStarter: CallStarter,
        val callGuard: CallGuard,
        joinClock: JoinClock,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(key: ExpertBookingSummaryKey): ExpertBookingSummaryViewModel
        }

        private val bookingId = key.bookingId

        private val _uiState = MutableStateFlow(ExpertBookingSummaryUiState())
        val uiState: StateFlow<ExpertBookingSummaryUiState> = _uiState.asStateFlow()

        /** Shared 30 s wall clock that turns the Join button on without leaving the screen. */
        val joinNow: StateFlow<Instant> = joinClock.now

        /** The last successfully loaded raw detail, or null before the first load completes. */
        var loadedDetail: ExpertBookingDetail? = null
            private set

        private var loadJob: Job? = null

        init {
            startLoad()
        }

        fun retry() {
            startLoad()
        }

        private fun startLoad(): Job {
            loadJob?.cancel()
            _uiState.value = ExpertBookingSummaryUiState(ExpertSummaryLoad.Loading)
            val job =
                viewModelScope.launch {
                    try {
                        val detail = repository.detail(bookingId)
                        loadedDetail = detail
                        _uiState.value =
                            ExpertBookingSummaryUiState(
                                ExpertSummaryLoad.Loaded(detail.toSummaryDetails(clock.instant(), Locale.getDefault())),
                            )
                    } catch (e: ApiException) {
                        _uiState.value = ExpertBookingSummaryUiState(ExpertSummaryLoad.Failed(e.toSectionMessage()))
                    }
                }
            loadJob = job
            return job
        }
    }
