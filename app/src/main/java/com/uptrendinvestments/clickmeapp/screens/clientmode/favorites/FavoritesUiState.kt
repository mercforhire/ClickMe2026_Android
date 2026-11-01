package com.uptrendinvestments.clickmeapp.screens.clientmode.favorites

import com.uptrendinvestments.clickmeapp.model.responses.FavoriteExpertItem
import com.uptrendinvestments.clickmeapp.screens.login.FieldError

/**
 * One Saved Experts row. A null [name] renders the existing "Expert" fallback (`explore_expert_name_fallback`); the
 * endpoint carries no bio, so none is held.
 */
data class FavoriteExpertUi(
    val expertId: String,
    val name: String?,
    val headline: String,
    val tags: List<String>,
    val avatarUrl: String?,
)

/** The list's load state. An empty [Loaded] list is the empty state (zero-one-many, UI-SPEC E3). */
sealed interface FavoritesLoad {
    data object Loading : FavoritesLoad

    data class Loaded(
        val experts: List<FavoriteExpertUi>,
    ) : FavoritesLoad

    data class Failed(
        val message: FieldError,
    ) : FavoritesLoad
}

/**
 * Saved Experts screen state. [isRefreshing] is the pull indicator, [refreshFailed] a one-shot snackbar flag for a
 * failed pull, and [removeFailed] a one-shot snackbar message for a failed removal (rows already rolled back).
 */
data class FavoritesUiState(
    val load: FavoritesLoad = FavoritesLoad.Loading,
    val isRefreshing: Boolean = false,
    val refreshFailed: Boolean = false,
    val removeFailed: FieldError? = null,
)

/**
 * Ports iOS `FavoritesViewModel.map`: a blank name stays null for the UI's "Expert" fallback, a missing headline
 * becomes "", and blank tags are dropped. The endpoint has no bio, so none is carried.
 */
fun FavoriteExpertItem.toUi(): FavoriteExpertUi =
    FavoriteExpertUi(
        expertId = expertId,
        name = fullName?.takeIf { it.isNotBlank() },
        headline = headline.orEmpty(),
        tags = expertiseTags.orEmpty().filter { it.isNotBlank() },
        avatarUrl = avatarUrl,
    )
