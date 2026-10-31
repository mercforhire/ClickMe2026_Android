package com.uptrendinvestments.clickmeapp.screens.clientmode.search

import com.uptrendinvestments.clickmeapp.model.responses.ExpertSearchResultItem
import com.uptrendinvestments.clickmeapp.screens.login.FieldError

/** The All chip. iOS `selectedCategorySlug == ""` means no category filter (D-11). */
const val ALL_CATEGORIES_SLUG = ""

/**
 * One result row. Ports iOS `ExpertSearchResult`. [name] stays nullable so the UI applies the shared Expert
 * fallback (`R.string.explore_expert_name_fallback`), like `ExpertCardUi`. [bio] is always empty because search rows
 * carry none (D-05).
 */
data class SearchExpertUi(
    val expertId: String,
    val name: String?,
    val headline: String,
    val avatarUrl: String?,
    val rating: Double,
    val tags: List<String>,
    val bio: String = "",
)

/** A category filter chip. [slug] is the `category` query value; the All chip uses [ALL_CATEGORIES_SLUG]. */
data class SearchCategoryChipUi(
    val slug: String,
    val name: String,
)

/**
 * The sort options in the iOS `sortOptions` order. [apiValue] is the backend `sort` value (`sortValue(for:)`).
 * Labels are resolved in the UI, so this file has no string resources.
 */
enum class SearchSort(
    val apiValue: String,
) {
    MostPopular("relevance"),
    HighestRated("rating"),
}

/** Load state of the result list. [Failed] is shown inline with a retry (D-08). */
sealed interface SearchResults {
    data object Loading : SearchResults

    data class Loaded(
        val experts: List<SearchExpertUi>,
    ) : SearchResults

    data class Failed(
        val message: FieldError,
    ) : SearchResults
}

/**
 * Search tab state. Ports the published properties of iOS `SearchExpertViewModel`. Lives only in the ViewModel and is
 * never written to saved state (D-02). [query] is the raw field text; the request trims it (D-10). [refreshFailed] is
 * one-shot and consumed by the screen (D-09).
 */
data class SearchExpertUiState(
    val query: String = "",
    val selectedCategorySlug: String = ALL_CATEGORIES_SLUG,
    val sort: SearchSort = SearchSort.MostPopular,
    val categories: List<SearchCategoryChipUi> = emptyList(),
    val results: SearchResults = SearchResults.Loading,
    val isRefreshing: Boolean = false,
    val refreshFailed: Boolean = false,
)

/** Ports iOS `SearchExpertViewModel.map(_:)`: headline "", rating 0, tags []; the name stays null (D-05). */
fun ExpertSearchResultItem.toSearchExpertUi(): SearchExpertUi =
    SearchExpertUi(
        expertId = expertId,
        name = fullName,
        headline = headline.orEmpty(),
        avatarUrl = avatarUrl,
        rating = avgRating ?: 0.0,
        tags = expertiseTags.orEmpty(),
    )
