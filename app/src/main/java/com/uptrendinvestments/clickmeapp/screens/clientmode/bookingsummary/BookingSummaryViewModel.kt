package com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.navigation.StackOrigin
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.ClientBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleResponder
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleResponseController
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleUiState
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.STATUS_COMPLETED
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ReviewLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ReviewsRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.ReviewOpener
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.WriteReviewKey
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock

/**
 * Booking Summary state machine (BOOK-08, D-13). Port of iOS `BookingSummaryViewModel`.
 *
 * The route carries only the booking id, so the booking is fetched on creation. Loads are latest-wins: a new load
 * cancels the previous one. A quiet load never shows Loading and keeps the previous summary when it fails, so
 * returning from Payment Methods refreshes silently (D-02).
 *
 * This is the second home of the shared reschedule card (D-01): accept and decline run through the same
 * [RescheduleResponseController] as Upcoming Booking Details, reloading the booking before any dialog or notice
 * is set. There is no realtime collection here; only Upcoming reloads on `booking.update`.
 *
 * Leave a review (D-18, D-19): [leaveReviewKey] builds the Write Review route from the loaded booking. A
 * `ReviewSubmitted` for this booking in [ReviewLaunchRequest.summaryReviewed] is consumed in `init` and on every
 * later emission; it triggers a quiet reload and sets [reviewThanks] so the screen shows "Thanks for your review".
 */
@HiltViewModel(assistedFactory = BookingSummaryViewModel.Factory::class)
class BookingSummaryViewModel
    @AssistedInject
    constructor(
        @Assisted key: BookingSummaryKey,
        private val repository: ClientBookingsRepository,
        responder: RescheduleResponder,
        private val clock: Clock,
        private val reviews: ReviewsRepository,
        private val reviewLaunch: ReviewLaunchRequest,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(key: BookingSummaryKey): BookingSummaryViewModel
        }

        private val bookingId = key.bookingId
        private val origin: StackOrigin = key.origin

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

        /** Booking ids reviewed this session, which hide Leave a review before the reload lands (D-18). */
        val reviewedBookingIds: StateFlow<Set<String>> = reviewLaunch.reviewedBookingIds

        private val _reviewThanks = MutableStateFlow(false)

        /** True once after a review for this booking was submitted; the screen shows the snackbar (D-19). */
        val reviewThanks: StateFlow<Boolean> = _reviewThanks.asStateFlow()

        init {
            startLoad(quiet = false)
            viewModelScope.launch {
                reviewLaunch.summaryReviewed.pending.collect { pending ->
                    if (pending?.bookingId == bookingId && reviewLaunch.summaryReviewed.consume() != null) {
                        // A loud load in flight is already fresh; only a shown summary needs the quiet reload.
                        if (_uiState.value is BookingSummaryUiState.Loaded) startLoad(quiet = true)
                        _reviewThanks.value = true
                    }
                }
            }
        }

        fun consumeReviewThanks() {
            _reviewThanks.value = false
        }

        /** The Write Review route for the shown booking, or null before it has loaded (D-18). */
        fun leaveReviewKey(): WriteReviewKey? {
            val details = (_uiState.value as? BookingSummaryUiState.Loaded)?.details ?: return null
            return WriteReviewKey(
                bookingId = details.bookingId,
                expertName = details.expertName,
                expertAvatarUrl = details.expertAvatarUrl,
                opener = ReviewOpener.BookingSummary,
                origin = origin,
            )
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

        /**
         * Reads the review context of a completed booking (D-18). A failure is silent: the previous value stays
         * (Unknown on a first load), so Leave a review is shown and Write Review resolves the true state.
         */
        private suspend fun loadReviewContext(details: BookingSummaryDetails) {
            if (details.status.raw != STATUS_COMPLETED) {
                _uiState.update { (it as? BookingSummaryUiState.Loaded)?.copy(review = SummaryReview.Unknown) ?: it }
                return
            }
            val review =
                try {
                    val context = reviews.context(bookingId)
                    when {
                        !context.hasSubmitted -> SummaryReview.NotSubmitted
                        else ->
                            SummaryReview.Submitted(
                                rating = context.prefill?.rating ?: DEFAULT_RATING,
                                comment = context.prefill?.comment?.takeIf(String::isNotBlank),
                            )
                    }
                } catch (_: ApiException) {
                    return
                }
            _uiState.update { (it as? BookingSummaryUiState.Loaded)?.copy(review = review) ?: it }
        }

        private fun startLoad(quiet: Boolean): Job {
            loadJob?.cancel()
            loadIsCurrent = true
            if (!quiet) _uiState.value = BookingSummaryUiState.Loading
            val job =
                viewModelScope.launch {
                    try {
                        val detail = repository.detail(bookingId)
                        val details = detail.toSummaryDetails(clock.instant())
                        val previous = (_uiState.value as? BookingSummaryUiState.Loaded)?.review
                        _uiState.value = BookingSummaryUiState.Loaded(details, previous ?: SummaryReview.Unknown)
                        loadReviewContext(details)
                    } catch (e: ApiException) {
                        if (!quiet) _uiState.value = BookingSummaryUiState.Failed(e.toSectionMessage())
                    }
                }
            loadJob = job
            return job
        }

        private companion object {
            /** A submitted review whose prefill is missing is shown as five stars, defensively (D-18). */
            const val DEFAULT_RATING = 5
        }
    }
