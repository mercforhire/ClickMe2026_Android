package com.uptrendinvestments.clickmeapp.screens.clientmode.reviews

import com.uptrendinvestments.clickmeapp.managers.SessionDataCleaner
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.ReviewOpener
import com.uptrendinvestments.clickmeapp.screens.expertmode.OneShotSlot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** "The review for [bookingId] was submitted": the opener reloads and says thanks (D-19). */
data class ReviewSubmitted(
    val bookingId: String,
)

/**
 * Hand-off from a submitted review to the screen that opened Write Review (D-19).
 *
 * A singleton and not a navigation result because the opener may be on another tab stack and its ViewModel may be
 * covered by Write Review when the review completes (Phase 8 launch-argument pattern, UI-SPEC Open Item 3).
 * There is one slot per consumer ([summaryReviewed] for Booking Summary, [bookingsReviewed] for My Bookings), so
 * one consumer never swallows the other's message.
 *
 * [reviewedBookingIds] hides "Leave a review" for the rest of the session, because the bookings list carries no
 * review flag (UI-SPEC Open Item 2). Memory only, cleared with the session.
 */
@Singleton
class ReviewLaunchRequest
    @Inject
    constructor() : SessionDataCleaner {
        val summaryReviewed = OneShotSlot<ReviewSubmitted>()
        val bookingsReviewed = OneShotSlot<ReviewSubmitted>()

        private val _reviewedBookingIds = MutableStateFlow<Set<String>>(emptySet())
        val reviewedBookingIds: StateFlow<Set<String>> = _reviewedBookingIds.asStateFlow()

        /** Marks [bookingId] reviewed and posts the hand-off to the slot of the [opener] screen. */
        fun onSubmitted(
            bookingId: String,
            opener: ReviewOpener,
        ) {
            _reviewedBookingIds.update { it + bookingId }
            val message = ReviewSubmitted(bookingId)
            when (opener) {
                ReviewOpener.BookingSummary -> summaryReviewed.post(message)
                ReviewOpener.MyBookings -> bookingsReviewed.post(message)
            }
        }

        override suspend fun clearSessionData() {
            summaryReviewed.consume()
            bookingsReviewed.consume()
            _reviewedBookingIds.value = emptySet()
        }
    }
