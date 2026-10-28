package com.uptrendinvestments.clickmeapp.screens.clientmode.writereview

import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ReviewsRepository
import com.uptrendinvestments.clickmeapp.screens.login.FieldError

/** A new review starts at five stars (iOS `selectedStars = 5`, UI-SPEC A5). */
const val DEFAULT_RATING = 5

/** The comment length from which the character counter is shown. */
const val COUNTER_FROM = 800

/** Who is reviewed and for which session. [expertName] and [expertAvatarUrl] start as the navigation seed. */
data class ReviewHeader(
    val expertName: String?,
    val expertAvatarUrl: String?,
    val sessionTopic: String?,
)

/** Where the review context load stands. */
sealed interface WriteReviewLoad {
    data object Loading : WriteReviewLoad

    data class Failed(
        val message: FieldError,
    ) : WriteReviewLoad

    data object Ready : WriteReviewLoad
}

/**
 * State of Write Review. [readOnly] is true when the booking already has a review (D-19: no edit, no resubmit).
 * [submitted] is a one-shot flag the screen consumes to pop.
 */
data class WriteReviewUiState(
    val load: WriteReviewLoad = WriteReviewLoad.Loading,
    val header: ReviewHeader = ReviewHeader(null, null, null),
    val rating: Int = DEFAULT_RATING,
    val comment: String = "",
    val readOnly: Boolean = false,
    val submitting: Boolean = false,
    val submitError: FieldError? = null,
    val submitted: Boolean = false,
) {
    val showCounter: Boolean get() = comment.length >= COUNTER_FROM
    val atCap: Boolean get() = comment.length >= ReviewsRepository.COMMENT_MAX

    /** Never prints the comment text (T-13-24), only its length. */
    override fun toString(): String =
        "WriteReviewUiState(load=$load, rating=$rating, commentLength=${comment.length}, readOnly=$readOnly, " +
            "submitting=$submitting, submitError=${submitError != null}, submitted=$submitted)"
}
