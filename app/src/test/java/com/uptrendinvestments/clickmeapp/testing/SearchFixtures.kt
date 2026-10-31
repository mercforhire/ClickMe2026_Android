package com.uptrendinvestments.clickmeapp.testing

import com.uptrendinvestments.clickmeapp.model.responses.CategoryItem
import com.uptrendinvestments.clickmeapp.model.responses.ExpertSearchResultItem
import com.uptrendinvestments.clickmeapp.model.responses.ExpertsSearchPayload
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.SearchExpertUi

/** One `/experts/search` row as the server returns it. */
fun searchResultItem(
    id: String = "e1",
    fullName: String? = "Ada Lovelace",
    headline: String? = "Engineer",
    avatarUrl: String? = null,
    avgRating: Double? = 4.5,
    totalReviews: Int? = 12,
    tags: List<String>? = listOf("AI"),
): ExpertSearchResultItem =
    ExpertSearchResultItem(
        expertId = id,
        fullName = fullName,
        headline = headline,
        avatarUrl = avatarUrl,
        avgRating = avgRating,
        totalReviews = totalReviews,
        expertiseTags = tags,
    )

fun searchPayload(vararg rows: ExpertSearchResultItem): ExpertsSearchPayload = ExpertsSearchPayload(rows.toList())

/** One mapped result row, as the ViewModel exposes it. */
fun searchExpertUi(
    id: String = "e1",
    name: String? = "Ada Lovelace",
    headline: String = "Engineer",
    avatarUrl: String? = null,
    rating: Double = 4.5,
    tags: List<String> = listOf("AI"),
    bio: String = "",
): SearchExpertUi = SearchExpertUi(id, name, headline, avatarUrl, rating, tags, bio)

/** The category list the Search chips are built from. */
fun searchCategoryItems(): List<CategoryItem> =
    listOf(
        CategoryItem(id = "business", name = "Business"),
        CategoryItem(id = "design", name = "Design"),
    )
