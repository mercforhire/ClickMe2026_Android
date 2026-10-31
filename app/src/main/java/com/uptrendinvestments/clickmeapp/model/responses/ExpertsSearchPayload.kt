package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * `GET /experts/search` payload. Ports iOS `ExpertsSearchPayload`. The response's paging block is deliberately not
 * declared because Search loads page 1 only (D-04), so a malformed paging block can never fail a search.
 */
@JsonClass(generateAdapter = true)
data class ExpertsSearchPayload(
    val experts: List<ExpertSearchResultItem> = emptyList(),
)

/**
 * One search row. Ports iOS `ExpertSearchResultItem.swift`. Every field except [expertId] is nullable with a default,
 * so a row with a missing or null display field (name, headline, avatar, rating, tags) still decodes and the UI applies
 * its fallback. [expertId] is required: the RPC returns it as the non-null primary-key `uuid`, and a row without it
 * cannot be opened, so a row with a missing or null id fails the whole decode (the screen shows its inline error).
 * [totalReviews] is parsed for iOS parity but the Search card does not show it. The remaining RPC columns (experience
 * years, relevance score, the favorite flag owned by Phase 16.3, matched topic ids, total count) are intentionally
 * undeclared and Moshi ignores them.
 */
@JsonClass(generateAdapter = true)
data class ExpertSearchResultItem(
    @Json(name = "expert_id") val expertId: String,
    @Json(name = "full_name") val fullName: String? = null,
    @Json(name = "headline") val headline: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "avg_rating") val avgRating: Double? = null,
    @Json(name = "total_reviews") val totalReviews: Int? = null,
    @Json(name = "expertise_tags") val expertiseTags: List<String>? = null,
)
