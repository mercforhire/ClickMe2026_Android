package com.uptrendinvestments.clickmeapp.screens.clientmode.explore

import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.util.PriceDisplay

/** Load state of one Explore section; each home section fails and retries on its own (D-16). */
sealed interface SectionState<out T> {
    data object Loading : SectionState<Nothing>

    data class Loaded<T>(
        val data: T,
    ) : SectionState<T>

    data class Error(
        val message: FieldError,
    ) : SectionState<Nothing>
}

/** A trending category chip. [name] stays nullable; the UI applies the "Category" fallback. */
data class CategoryChipUi(
    val slug: String,
    val name: String?,
)

data class FeaturedTopicUi(
    val topicId: String,
    val title: String,
    val price: PriceDisplay,
    val expertId: String,
    val expertName: String?,
    val expertImageUrl: String?,
)

data class ExpertCardUi(
    val expertId: String,
    val name: String?,
    val title: String?,
    val imageUrl: String?,
    val rating: Double,
    val reviewCount: Int,
    val tags: List<String>,
)

data class TodaysSessionUi(
    val bookingId: String,
    val expertName: String?,
    val expertAvatarUrl: String?,
    val topic: String?,
    val timeRange: String,
)

data class ExploreUiState(
    val todaysSessions: List<TodaysSessionUi> = emptyList(),
    val categories: SectionState<List<CategoryChipUi>> = SectionState.Loading,
    val featuredTopics: SectionState<List<FeaturedTopicUi>> = SectionState.Loading,
    val experts: SectionState<List<ExpertCardUi>> = SectionState.Loading,
    val selection: Set<String> = emptySet(),
    val isRefreshing: Boolean = false,
    /** One-shot: set when a pull-to-refresh failed while data was on screen; the screen consumes it. */
    val refreshFailed: Boolean = false,
) {
    val hasFilter: Boolean get() = selection.isNotEmpty()
}
