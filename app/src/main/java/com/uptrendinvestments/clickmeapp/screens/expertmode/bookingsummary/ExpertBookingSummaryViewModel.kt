package com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.managers.CallGuard
import com.uptrendinvestments.clickmeapp.managers.CallStartRequest
import com.uptrendinvestments.clickmeapp.managers.CallStarter
import com.uptrendinvestments.clickmeapp.managers.JoinClock
import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingDetail
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.realtime.RealtimeConnection
import com.uptrendinvestments.clickmeapp.realtime.RealtimeEvent
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.util.Locale

/**
 * Expert Booking Summary state machine (EXP-08 / EXP-09). Port of iOS `ExpertBookingSummaryViewModel`, loading from
 * `GET expert/bookings/{id}/details` (never a bare-id GET, RESEARCH C1).
 *
 * The route carries only the booking id, so the screen loads its own booking whichever list opened it. Loads are
 * latest-wins. Quiet reloads never show Loading and a failed one keeps the details already on screen. They are
 * triggered by [refreshQuietly] (screen shown again, for example back from the Cancel screen) and by a realtime
 * `booking.update` for this booking id only (D-13). The loaded [ExpertBookingDetail] is kept in memory
 * ([loadedDetail]) for the reschedule card (11-13).
 *
 * Join (D-14): [join] starts the call through the one [CallStarter] with the client as the peer. Cancel is guarded
 * while this booking's call is live ([attemptCancel], D-17).
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
        private var loadIsCurrent = false

        init {
            startLoad(quiet = false)
            // Only this booking's updates reload; startLoad cancels the previous load, so a burst of events ends with
            // one latest-wins result.
            viewModelScope.launch {
                realtime.events
                    .filterIsInstance<RealtimeEvent.BookingUpdate>()
                    .filter { it.bookingId == bookingId }
                    .collect { startLoad(quiet = _uiState.value.load is ExpertSummaryLoad.Loaded) }
            }
        }

        fun retry() {
            startLoad(quiet = false)
        }

        /**
         * The screen is shown again (`ON_RESUME`, first composition). A call while a current load runs does nothing, so
         * first composition never double-loads; a Loaded screen reloads quietly.
         */
        fun refreshQuietly() {
            if (loadJob?.isActive == true && loadIsCurrent) return
            startLoad(quiet = _uiState.value.load is ExpertSummaryLoad.Loaded)
        }

        /**
         * The screen was hidden or disposed. A load started before this may predate a change made while hidden, so the
         * next [refreshQuietly] restarts it.
         */
        fun onScreenHidden() {
            loadIsCurrent = false
        }

        /** Starts a quiet reload and waits for it. The reschedule controller (11-13) reloads through this. */
        suspend fun reloadQuietly() {
            startLoad(quiet = true).join()
        }

        /**
         * Starts the call with the client as the peer. Does nothing before the booking has loaded. The join window only
         * enables the button; the server has the final say on `/join`, so this does not re-check it (Phase 9 D-02).
         */
        fun join() {
            val details = (_uiState.value.load as? ExpertSummaryLoad.Loaded)?.details ?: return
            callStarter.startCall(
                CallStartRequest(
                    bookingId = details.bookingId,
                    peerId = details.clientId,
                    peerName = details.clientName,
                    peerAvatarUrl = details.clientAvatarUrl,
                    topic = details.topic,
                    scheduledStart = details.start,
                    scheduledEnd = details.end,
                    meetingType = MeetingType.fromWire(details.meetingType),
                ),
            )
        }

        /** True when Cancel may open; false (with the guard dialog queued) while this booking's call is live (D-17). */
        fun attemptCancel(): Boolean = callGuard.attemptModifyBooking(bookingId, RestrictedAction.CancelBooking)

        private fun startLoad(quiet: Boolean): Job {
            loadJob?.cancel()
            loadIsCurrent = true
            if (!quiet) _uiState.value = ExpertBookingSummaryUiState(ExpertSummaryLoad.Loading)
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
                        if (!quiet) {
                            _uiState.value = ExpertBookingSummaryUiState(ExpertSummaryLoad.Failed(e.toSectionMessage()))
                        }
                    }
                }
            loadJob = job
            return job
        }
    }
