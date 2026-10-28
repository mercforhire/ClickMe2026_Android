package com.uptrendinvestments.clickmeapp.testing

import com.uptrendinvestments.clickmeapp.model.responses.ReviewContextData
import com.uptrendinvestments.clickmeapp.model.responses.ReviewPrefillDto
import com.uptrendinvestments.clickmeapp.model.responses.ReviewTargetDto
import com.uptrendinvestments.clickmeapp.model.responses.SubmitReviewData

/** Review DTO builders shared by every Phase 13 test. The prefill is built only when [prefillRating] is set. */
fun reviewContext(
    hasSubmitted: Boolean = false,
    name: String? = "Ada Lovelace",
    avatarUrl: String? = null,
    topic: String? = "Algorithms",
    prefillRating: Int? = null,
    prefillComment: String? = null,
) = ReviewContextData(
    hasSubmitted = hasSubmitted,
    reviewTarget = ReviewTargetDto(id = "e1", name = name, avatarUrl = avatarUrl),
    sessionTopic = topic,
    prefill = prefillRating?.let { ReviewPrefillDto(rating = it, comment = prefillComment) },
)

fun submitReviewData(reviewId: String = "r1") = SubmitReviewData(reviewId = reviewId, status = "published")
