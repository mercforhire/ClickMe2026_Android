package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** `GET /experts/{id}/reviews` payload. */
@JsonClass(generateAdapter = true)
data class ExpertReviewsPayload(
    val summary: ReviewSummaryDto = ReviewSummaryDto(),
    val reviews: List<ReviewItemDto> = emptyList(),
    val pagination: PaginationMeta? = null,
)

/** [overallRating] is null while the expert has no reviews. */
@JsonClass(generateAdapter = true)
data class ReviewSummaryDto(
    @Json(name = "expert_id") val expertId: String? = null,
    @Json(name = "overall_rating") val overallRating: Double? = null,
    @Json(name = "total_reviews") val totalReviews: Int = 0,
)

@JsonClass(generateAdapter = true)
data class ReviewItemDto(
    val id: String,
    val reviewer: ReviewerDto = ReviewerDto(),
    val rating: Int,
    val comment: String? = null,
)

@JsonClass(generateAdapter = true)
data class ReviewerDto(
    val name: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
)
