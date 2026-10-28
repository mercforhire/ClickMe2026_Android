package com.uptrendinvestments.clickmeapp.screens.clientmode.reviews

import com.uptrendinvestments.clickmeapp.model.requests.SubmitReviewRequest
import com.uptrendinvestments.clickmeapp.model.responses.ReviewContextData
import com.uptrendinvestments.clickmeapp.model.responses.SubmitReviewData
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The post-session review (`/bookings/{id}/reviews`). Ports iOS `ClickMeAPI+Reviews`, with a request body that
 * follows the backend `submitReviewSchema`.
 *
 * Stateless: nothing here is cleared on logout, so it caches nothing; throws ApiException only (a 404 or 422
 * reaches the caller with the server message); review comment text is never logged.
 */
@Singleton
class ReviewsRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
    ) {
        /** `GET /bookings/{id}/reviews/context`: who is reviewed, whether a review exists, and its prefill. */
        suspend fun context(bookingId: String): ReviewContextData = apiCall { api.getReviewContext(bookingId) }.data

        /**
         * `POST /bookings/{id}/reviews`. [comment] is trimmed and a blank one is omitted (the server rejects an
         * empty string). [rating] must be [RATING_MIN] to [RATING_MAX].
         */
        suspend fun submit(
            bookingId: String,
            rating: Int,
            comment: String?,
        ): SubmitReviewData {
            require(rating in RATING_MIN..RATING_MAX) { "rating must be $RATING_MIN to $RATING_MAX" }
            val body = SubmitReviewRequest(rating, comment?.trim()?.takeIf { it.isNotEmpty() })
            return apiCall { api.submitReview(bookingId, body) }.data
        }

        companion object {
            const val RATING_MIN = 1
            const val RATING_MAX = 5

            /** The backend `comment` maximum length. */
            const val COMMENT_MAX = 1000
        }
    }
