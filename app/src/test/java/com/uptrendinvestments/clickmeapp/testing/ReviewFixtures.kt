package com.uptrendinvestments.clickmeapp.testing

import com.uptrendinvestments.clickmeapp.model.responses.ExpertReviewsPayload
import com.uptrendinvestments.clickmeapp.model.responses.ReviewContextData
import com.uptrendinvestments.clickmeapp.model.responses.ReviewItemDto
import com.uptrendinvestments.clickmeapp.model.responses.ReviewPrefillDto
import com.uptrendinvestments.clickmeapp.model.responses.ReviewSessionContextDto
import com.uptrendinvestments.clickmeapp.model.responses.ReviewSummaryDto
import com.uptrendinvestments.clickmeapp.model.responses.ReviewTargetDto
import com.uptrendinvestments.clickmeapp.model.responses.ReviewerDto
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

/** One full-list review row (Phase 16.3); the session context is built from [topic] and [dateLabel]. */
fun reviewItem(
    id: String = "r1",
    name: String? = "Sam Lee",
    avatarUrl: String? = null,
    rating: Int = 5,
    comment: String? = "Great session",
    topic: String? = "Career advice",
    dateLabel: String? = "Oct 12, 2026",
) = ReviewItemDto(
    id = id,
    reviewer = ReviewerDto(name = name, avatarUrl = avatarUrl),
    rating = rating,
    comment = comment,
    sessionContext = ReviewSessionContextDto(topic, null, dateLabel),
)

fun reviewsPayload(
    overall: Double?,
    total: Int,
    items: List<ReviewItemDto> = emptyList(),
) = ExpertReviewsPayload(
    summary = ReviewSummaryDto(overallRating = overall, totalReviews = total),
    reviews = items,
)

fun submitReviewData(reviewId: String = "r1") = SubmitReviewData(reviewId = reviewId, status = "published")
