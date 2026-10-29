package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * `GET /help/faqs` data, ported from iOS `FaqCategory` and `FaqArticlePreview`. The list is lenient: every list
 * defaults to empty, so a body without `trending_articles` or `featured_articles` still decodes.
 */
@JsonClass(generateAdapter = true)
data class FaqsPayload(
    val categories: List<FaqCategory> = emptyList(),
    @Json(name = "trending_articles") val trendingArticles: List<FaqArticlePreview> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class FaqCategory(
    val id: String,
    val title: String,
    @Json(name = "icon_url") val iconUrl: String? = null,
    @Json(name = "display_order") val displayOrder: Int = 0,
    @Json(name = "featured_articles") val featuredArticles: List<FaqArticlePreview> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class FaqArticlePreview(
    val id: String,
    @Json(name = "category_id") val categoryId: String? = null,
    val question: String,
    val snippet: String? = null,
    @Json(name = "view_count") val viewCount: Int = 0,
)
