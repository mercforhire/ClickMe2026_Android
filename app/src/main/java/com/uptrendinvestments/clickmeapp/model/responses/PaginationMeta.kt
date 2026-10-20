package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Pagination block of list responses, mirroring iOS `PaginationMeta`. */
@JsonClass(generateAdapter = true)
data class PaginationMeta(
    @Json(name = "current_page") val currentPage: Int,
    @Json(name = "total_pages") val totalPages: Int,
    @Json(name = "total_count") val totalCount: Int? = null,
    @Json(name = "has_more") val hasMore: Boolean? = null,
) {
    /** Prefers the server flag; otherwise derives it from the page counts. */
    val hasMoreOrDerived: Boolean
        get() = hasMore ?: (currentPage < totalPages)
}
