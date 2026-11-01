package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * `GET /client/favorites` payload. Ports iOS `FavoritesPayload`. The response's paging block is deliberately not
 * declared because Saved Experts loads one page only (D-12), so a malformed paging block can never fail the list.
 */
@JsonClass(generateAdapter = true)
data class FavoritesPayload(
    val experts: List<FavoriteExpertItem> = emptyList(),
)

/**
 * One saved expert. Ports iOS `FavoriteExpertItem.swift`. Every field except [expertId] is nullable with a default,
 * so a row with a missing or null display field still decodes and the UI applies its fallback. [expertId] is
 * required: a row without it cannot be opened. The RPC's extra columns (rating, review count, favorited_at,
 * total_count) are intentionally undeclared and Moshi ignores them; the iOS hourly-rate fields no longer exist
 * server-side.
 */
@JsonClass(generateAdapter = true)
data class FavoriteExpertItem(
    @Json(name = "expert_id") val expertId: String,
    @Json(name = "full_name") val fullName: String? = null,
    @Json(name = "headline") val headline: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "expertise_tags") val expertiseTags: List<String>? = null,
)

/**
 * `PUT` / `DELETE /client/favorites/{expertId}` result. Ports iOS `ToggleFavoriteData`: the add answers
 * `is_favorite: true`, the remove `false`. Both are idempotent server-side.
 */
@JsonClass(generateAdapter = true)
data class ToggleFavoriteData(
    @Json(name = "expert_id") val expertId: String,
    @Json(name = "is_favorite") val isFavorite: Boolean,
)
