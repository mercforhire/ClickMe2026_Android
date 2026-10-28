package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** The person being reviewed, as `GET /bookings/{id}/reviews/context` returns it (extra `roles` is ignored). */
@JsonClass(generateAdapter = true)
data class ReviewTargetDto(
    val id: String? = null,
    val name: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
)

/**
 * The caller's earlier review, present only when one exists (extra `tags` is ignored). [comment] is user text, so
 * [toString] prints the rating only.
 */
@JsonClass(generateAdapter = true)
data class ReviewPrefillDto(
    val rating: Int,
    val comment: String? = null,
) {
    override fun toString(): String = "ReviewPrefillDto(rating=$rating)"
}

/** `data` of `GET /bookings/{id}/reviews/context` (`reviews.controller.js`). */
@JsonClass(generateAdapter = true)
data class ReviewContextData(
    @Json(name = "has_submitted") val hasSubmitted: Boolean = false,
    @Json(name = "review_target") val reviewTarget: ReviewTargetDto? = null,
    @Json(name = "session_topic") val sessionTopic: String? = null,
    val prefill: ReviewPrefillDto? = null,
)

/** `data` of `POST /bookings/{id}/reviews` (201). The server `message` is ignored. */
@JsonClass(generateAdapter = true)
data class SubmitReviewData(
    @Json(name = "review_id") val reviewId: String,
    val status: String? = null,
)
