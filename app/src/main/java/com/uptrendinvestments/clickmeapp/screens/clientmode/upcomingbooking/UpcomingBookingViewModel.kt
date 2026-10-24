package com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.realtime.RealtimeConnection
import com.uptrendinvestments.clickmeapp.realtime.RealtimeEvent
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.ClientBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleResponder
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleResponseController
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleUiState
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

/**
 * Upcoming Booking Details state machine (BOOK-06). Port of iOS `UpcomingBookingViewModel` with the D-14 fixes
 * (real status, Money price, payment pill).
 *
 * The route carries only the booking id, so the screen loads its own booking whichever list opened it (RESEARCH
 * Pitfall 12). Loads are latest-wins. Quiet reloads never show Loading and a failed one keeps the details already
 * on screen. They are triggered by [refreshQuietly] (screen shown again, for example back from Payment Methods,
 * D-02) and by a realtime `booking.update` for this booking id only.
 *
 * Reschedule response (BOOK-09, D-01): accept and decline are delegated to the shared [RescheduleResponseController],
 * which reloads the booking before it shows the outcome. Nothing is optimistic. A response that completes after
 * the user left is dropped with `viewModelScope`. The iOS follow-up for the missing response card is logged in
 * plan 08-15.
 */
@HiltViewModel(assistedFactory = UpcomingBookingViewModel.Factory::class)
class UpcomingBookingViewModel
    @AssistedInject
    constructor(
        @Assisted key: UpcomingBookingKey,
        private val repository: ClientBookingsRepository,
        responder: RescheduleResponder,
        private val realtime: RealtimeConnection,
        private val clock: Clock,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(key: UpcomingBookingKey): UpcomingBookingViewModel
        }

        private val bookingId = key.bookingId

        private val _uiState = MutableStateFlow<UpcomingBookingUiState>(UpcomingBookingUiState.Loading)
        val uiState: StateFlow<UpcomingBookingUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null
        private var loadIsCurrent = false

        private val controller =
            RescheduleResponseController(
                scope = viewModelScope,
                responder = responder,
                bookingId = bookingId,
                reload = { startLoad(quiet = true).join() },
            )

        val rescheduleState: StateFlow<RescheduleUiState> = controller.state

        init {
            startLoad(quiet = false)
            // Only this booking's updates reload (T-08-35); startLoad cancels the previous load, so a burst of
            // events ends with one latest-wins result.
            viewModelScope.launch {
                realtime.events
                    .filterIsInstance<RealtimeEvent.BookingUpdate>()
                    .filter { it.bookingId == bookingId }
                    .collect { startLoad(quiet = _uiState.value is UpcomingBookingUiState.Loaded) }
            }
        }

        fun retry() {
            startLoad(quiet = false)
        }

        /**
         * The screen is shown again (`ON_RESUME`, first composition). A call while a current load runs does nothing,
         * so first composition never double-loads (RESEARCH Findings 5-6); a Loaded screen reloads quietly.
         */
        fun refreshQuietly() {
            if (loadJob?.isActive == true && loadIsCurrent) return
            startLoad(quiet = _uiState.value is UpcomingBookingUiState.Loaded)
        }

        /**
         * The screen was hidden or disposed. A load started before this may predate a change made while hidden (for
         * example a card added in Payment Methods), so the next [refreshQuietly] restarts it.
         */
        fun onScreenHidden() {
            loadIsCurrent = false
        }

        fun accept() = controller.accept()

        fun decline(reason: String) = controller.decline(reason)

        fun dismissRescheduleDialog() = controller.dismissDialog()

        fun consumeRescheduleNotice() = controller.consumeNotice()

        private fun startLoad(quiet: Boolean): Job {
            loadJob?.cancel()
            loadIsCurrent = true
            if (!quiet) _uiState.value = UpcomingBookingUiState.Loading
            val job =
                viewModelScope.launch {
                    try {
                        val details = repository.detail(bookingId).toUpcomingDetails(clock.instant())
                        _uiState.value = UpcomingBookingUiState.Loaded(details)
                    } catch (e: ApiException) {
                        if (!quiet) _uiState.value = UpcomingBookingUiState.Failed(e.toSectionMessage())
                    }
                }
            loadJob = job
            return job
        }
    }
