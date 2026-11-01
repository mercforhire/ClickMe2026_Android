package com.uptrendinvestments.clickmeapp.screens.clientmode.reviews

import com.uptrendinvestments.clickmeapp.screens.login.FieldError

/**
 * One row of the full Reviews list. A dedicated model: the profile preview's `ReviewRowUi` carries no topic or date.
 * A null name, topic, date label or comment is shown with the UI fallback (or the line is omitted).
 */
data class ExpertReviewUi(
    val id: String,
    val reviewerName: String?,
    val avatarUrl: String?,
    val rating: Int,
    val topic: String?,
    val dateLabel: String?,
    val comment: String?,
)

sealed interface ExpertReviewsLoad {
    data object Loading : ExpertReviewsLoad

    data class Loaded(
        val overallRating: Double,
        val totalReviews: Int,
        val reviews: List<ExpertReviewUi>,
    ) : ExpertReviewsLoad

    data class Failed(
        val message: FieldError,
    ) : ExpertReviewsLoad
}

/**
 * [titleFirstName] is null when no usable name is known; the UI then titles the screen "Reviews".
 * [isRefreshing] is a pull over loaded content; [refreshFailed] is raised once when that pull fails.
 */
data class ExpertReviewsUiState(
    val expertId: String,
    val titleFirstName: String?,
    val load: ExpertReviewsLoad = ExpertReviewsLoad.Loading,
    val isRefreshing: Boolean = false,
    val refreshFailed: Boolean = false,
)

/**
 * The first space-separated word of the trimmed [expertName], or null when there is none. Port of iOS
 * `ExpertReviewsViewModel.navigationTitle`; the UI formats "{first}'s Reviews" or falls back to "Reviews".
 */
fun reviewsTitleFirstName(expertName: String): String? = expertName.trim().split(' ').firstOrNull { it.isNotEmpty() }
