package com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile

import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.util.PriceDisplay

/** What the hero rating line shows (D-12, D-15). */
sealed interface HeroRating {
    /** Rating unknown and still loading: show a placeholder. */
    data object Pending : HeroRating

    /** Zero reviews: "No reviews yet". */
    data object NoReviews : HeroRating

    data class Shown(
        val rating: Double,
        val count: Int,
    ) : HeroRating

    /** Reviews failed and there was no seed rating: hide the line. */
    data object Hidden : HeroRating
}

/** Load state of `GET /experts/{id}/details` (D-13). */
sealed interface DetailsState {
    data object Loading : DetailsState

    data class Loaded(
        val details: ProfileDetailsUi,
    ) : DetailsState

    /** 404: suspended, deleted or blocked expert. */
    data object Unavailable : DetailsState

    /** Network failure or a non-404 error. */
    data object Error : DetailsState
}

data class ProfileDetailsUi(
    val experienceYears: Int?,
    val totalBookings: Int,
    val bio: String?,
    val topics: List<TopicRowUi>,
    /** False only when the server says so; null counts as bookable (D-04). */
    val isBookable: Boolean,
)

data class TopicRowUi(
    val id: String,
    val title: String,
    val durationMins: Int?,
    val description: String?,
    val price: PriceDisplay,
)

/** Load state of the reviews preview (`GET /experts/{id}/reviews`, page 1). */
sealed interface ReviewsState {
    data object Loading : ReviewsState

    data class Loaded(
        val items: List<ReviewRowUi>,
    ) : ReviewsState

    data object Failed : ReviewsState
}

data class ReviewRowUi(
    val id: String,
    /** Null when the reviewer has no name; the screen falls back to "Anonymous". */
    val author: String?,
    val rating: Int,
    val comment: String?,
)

data class ExpertProfileUiState(
    val expertId: String,
    val name: String,
    val title: String,
    val imageUrl: String?,
    val tags: List<String>,
    val rating: HeroRating,
    val details: DetailsState = DetailsState.Loading,
    val reviews: ReviewsState = ReviewsState.Loading,
    /** The heart: seeded from `is_favorite` on the details response (D-01), flipped at once by a tap (D-03). */
    val isFavorite: Boolean = false,
    /** One-shot message for a failed favorite toggle; the screen shows it as a snackbar and consumes it (D-03). */
    val favoriteError: FieldError? = null,
) {
    /** The title is unknown (opened from a Featured Topic) and details have not arrived yet. */
    val isTitlePending: Boolean get() = title.isBlank() && details is DetailsState.Loading
}
