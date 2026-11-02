package com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary

import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingDetail
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.DEFAULT_DURATION_MINS
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingStatusDisplay
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleProposalUi
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.STATUS_COMPLETED
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.summaryStatusDisplay
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.toRescheduleProposalUi
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.util.PriceDisplay
import java.time.Duration
import java.time.Instant

/** Label above the price on the summary card (D-13). */
enum class SummaryPriceLabel { PricePaid, SessionFee, Refunded }

/**
 * Display seed for "Book Again" (iOS `BookAgainSeed`). Display only: Make a Booking re-fetches the expert and the
 * server re-checks bookability (T-08-40).
 */
data class BookAgainSeed(
    val expertId: String,
    val expertName: String?,
    val expertTitle: String?,
    val expertImageUrl: String?,
) {
    fun toBookingKey(nameFallback: String): BookingKey =
        BookingKey(
            expertId = expertId,
            expertName = expertName ?: nameFallback,
            expertTitle = expertTitle.orEmpty(),
            expertImageUrl = expertImageUrl,
        )
}

/**
 * What Booking Summary shows for one booking (D-13). The review the user left (Feedback block) and the Leave a
 * review control follow [SummaryReview] and the booking status (D-18).
 *
 * @property expertId the expert's user id, the peer for Message Expert (D-14); null disables the control.
 * @property privateNote the client's own full note, or null when there is none or it is blank (Phase 16.4, D-08).
 *   User-written: shown on the My Note card, never logged, so [toString] leaves it out.
 */
data class BookingSummaryDetails(
    val bookingId: String,
    val status: BookingStatusDisplay,
    val expertName: String?,
    val expertTitle: String?,
    val expertAvatarUrl: String?,
    val expertId: String? = null,
    val topicTitle: String?,
    val start: Instant,
    val end: Instant,
    val price: PriceDisplay,
    val priceLabel: SummaryPriceLabel,
    val bookAgain: BookAgainSeed?,
    val reschedule: RescheduleProposalUi?,
    val privateNote: String? = null,
) {
    override fun toString(): String = "BookingSummaryDetails(bookingId=$bookingId)"
}

/**
 * What the review-context call said about this booking (D-18). [Unknown] covers "not loaded yet", "not a completed
 * booking" and "the call failed": all show Leave a review and no Feedback block, and Write Review resolves the truth.
 */
sealed interface SummaryReview {
    data object Unknown : SummaryReview

    data object NotSubmitted : SummaryReview

    /** A submitted review. [toString] prints the rating only, so a comment never reaches logs. */
    data class Submitted(
        val rating: Int,
        val comment: String?,
    ) : SummaryReview {
        override fun toString(): String = "Submitted(rating=$rating)"
    }
}

/** Booking Summary load state: fetch by booking id, retry on failure. */
sealed interface BookingSummaryUiState {
    data object Loading : BookingSummaryUiState

    data class Failed(
        val message: FieldError,
    ) : BookingSummaryUiState

    data class Loaded(
        val details: BookingSummaryDetails,
        val review: SummaryReview = SummaryReview.Unknown,
    ) : BookingSummaryUiState
}

/** The Feedback block shows only when the booking has a submitted review (D-18). */
val BookingSummaryUiState.Loaded.showFeedback: Boolean get() = review is SummaryReview.Submitted

/**
 * Leave a review shows for a completed booking with no known submitted review, including while the context call is
 * loading or has failed. [reviewedIds] are the bookings reviewed this session, which hide it before the reload lands.
 */
fun BookingSummaryUiState.Loaded.showLeaveReview(reviewedIds: Set<String>): Boolean =
    details.status.raw == STATUS_COMPLETED && !showFeedback && details.bookingId !in reviewedIds

/**
 * Maps the booking detail to the display model. The price label follows the payment status: `captured` is "Price
 * paid", `refunded` is "Refunded", anything else (held, none, unknown) and every free topic is "Session fee".
 * Blank strings become null (a whitespace note written from iOS counts as no note); a missing end time falls back to
 * start plus the topic duration.
 */
fun ClientBookingDetail.toSummaryDetails(now: Instant): BookingSummaryDetails =
    BookingSummaryDetails(
        bookingId = bookingId,
        status = summaryStatusDisplay(status),
        expertName = expert.fullName?.takeIf(String::isNotBlank),
        expertTitle = expert.title?.takeIf(String::isNotBlank),
        expertAvatarUrl = expert.avatarUrl?.takeIf(String::isNotBlank),
        expertId = expert.id?.takeIf(String::isNotBlank),
        topicTitle = topic.title.takeIf(String::isNotBlank),
        start = startTime,
        end = endTime ?: startTime.plus(Duration.ofMinutes((topic.durationMins ?: DEFAULT_DURATION_MINS).toLong())),
        price = PriceDisplay.of(topic.isFree, topic.price?.amount, topic.price?.currency),
        priceLabel =
            when {
                topic.isFree -> SummaryPriceLabel.SessionFee
                paymentStatus == "captured" -> SummaryPriceLabel.PricePaid
                paymentStatus == "refunded" -> SummaryPriceLabel.Refunded
                else -> SummaryPriceLabel.SessionFee
            },
        bookAgain =
            expert.id?.let {
                BookAgainSeed(
                    expertId = it,
                    expertName = expert.fullName?.takeIf(String::isNotBlank),
                    expertTitle = expert.title?.takeIf(String::isNotBlank),
                    expertImageUrl = expert.avatarUrl,
                )
            },
        reschedule = toRescheduleProposalUi(now),
        privateNote = privateNote?.takeIf(String::isNotBlank),
    )
