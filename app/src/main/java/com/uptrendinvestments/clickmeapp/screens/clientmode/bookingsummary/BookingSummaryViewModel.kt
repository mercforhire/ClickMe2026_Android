package com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.network.ApiException
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
import kotlinx.coroutines.launch
import java.time.Clock

/**
 * Booking Summary state machine (BOOK-08, D-13). Port of iOS `BookingSummaryViewModel` minus Message Expert, Leave a
 * Review and Feedback, which belong to Phases 12-13.
 *
 * The route carries only the booking id, so the booking is fetched on creation. Loads are latest-wins: a new load
 * cancels the previous one. A quiet load never shows Loading and keeps the previous summary when it fails, so
 * returning from Payment Methods refreshes silently (D-02).
 *
 * This is the second home of the shared reschedule card (D-01): accept and decline run through the same
 * [RescheduleResponseController] as Upcoming Booking Details, reloading the booking before any dialog or notice
 * is set. There is no realtime collection here; only Upcoming reloads on `booking.update`.
 */
@HiltViewModel(assistedFactory = BookingSummaryViewModel.Factory::class)
class BookingSummaryViewModel
    @AssistedInject
    constructor(
        @Assisted key: BookingSummaryKey,
        private val repository: ClientBookingsRepository,
        responder: RescheduleResponder,
        private val clock: Clock,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(key: BookingSummaryKey): BookingSummaryViewModel
        }

        private val bookingId = key.bookingId

        private val _uiState = MutableStateFlow<BookingSummaryUiState>(BookingSummaryUiState.Loading)
        val uiState: StateFlow<BookingSummaryUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null
        private var loadIsCurrent = false

        private val rescheduleController =
            RescheduleResponseController(
                scope = viewModelScope,
                responder = responder,
                bookingId = bookingId,
                reload = { startLoad(quiet = true).join() },
            )

        /** Reschedule response state (in-flight, outcome dialog, success notice). */
        val rescheduleState: StateFlow<RescheduleUiState> = rescheduleController.state

        init {
            startLoad(quiet = false)
        }

        fun retry() {
            startLoad(quiet = false)
        }

        /**
         * The screen is shown again (`ON_RESUME`, first composition). A call while a current load runs does nothing,
         * so first composition never double-loads; otherwise the summary reloads quietly, or loudly if it has not
         * loaded yet.
         */
        fun refreshQuietly() {
            if (loadJob?.isActive == true && loadIsCurrent) return
            startLoad(quiet = _uiState.value is BookingSummaryUiState.Loaded)
        }

        /**
         * The screen was hidden or disposed. A load started before this may predate a change made while hidden (a
         * payment method added), so the next [refreshQuietly] restarts it.
         */
        fun onScreenHidden() {
            loadIsCurrent = false
        }

        fun accept() = rescheduleController.accept()

        fun decline(reason: String) = rescheduleController.decline(reason)

        fun dismissRescheduleDialog() = rescheduleController.dismissDialog()

        fun consumeRescheduleNotice() = rescheduleController.consumeNotice()

        private fun startLoad(quiet: Boolean): Job {
            loadJob?.cancel()
            loadIsCurrent = true
            if (!quiet) _uiState.value = BookingSummaryUiState.Loading
            val job =
                viewModelScope.launch {
                    try {
                        val detail = repository.detail(bookingId)
                        _uiState.value = BookingSummaryUiState.Loaded(detail.toSummaryDetails(clock.instant()))
                    } catch (e: ApiException) {
                        if (!quiet) _uiState.value = BookingSummaryUiState.Failed(e.toSectionMessage())
                    }
                }
            loadJob = job
            return job
        }
    }
