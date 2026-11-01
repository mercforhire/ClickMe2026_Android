package com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.model.responses.PublicProfileDetailsData
import com.uptrendinvestments.clickmeapp.model.responses.TopicOfDiscussionDto
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.favorites.FavoritesRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.favorites.toFavoriteFailure
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingKey
import com.uptrendinvestments.clickmeapp.util.PriceDisplay
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val HTTP_NOT_FOUND = 404

/**
 * Expert Profile state machine (DISC-03). Port of iOS `ExpertProfileViewModel`.
 *
 * - D-15: the state starts as a preview built from the route [key]; `details` and `reviews` load in two
 *   independent jobs so one failure never cancels the other.
 * - D-13: a details 404 (suspended, deleted or blocked expert) is [DetailsState.Unavailable]; any other
 *   failure is [DetailsState.Error]. [retry] re-sends both requests.
 * - D-12 / D-15: the reviews summary replaces the hero rating once it arrives; zero reviews is
 *   [HeroRating.NoReviews]; a reviews failure keeps the seed rating, or hides the line when there is none.
 * - D-04: a null `is_bookable` counts as bookable (iOS parity).
 * - Favorite heart (16.3 D-01..D-04): it starts from `is_favorite` on the details response, a tap flips it at once
 *   and calls PUT or DELETE, the server's answer then wins, a failure rolls it back with a one-shot message, and a
 *   tap while a toggle is in flight is dropped. Two deliberate iOS deviations: the start state comes from the
 *   details flag (iOS scans the favorites list), and a failure is a snackbar (iOS shows an alert).
 */
@HiltViewModel(assistedFactory = ExpertProfileViewModel.Factory::class)
class ExpertProfileViewModel
    @AssistedInject
    constructor(
        @Assisted key: ExpertProfileKey,
        private val repository: ExpertProfileRepository,
        private val favorites: FavoritesRepository,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(key: ExpertProfileKey): ExpertProfileViewModel
        }

        private val seedRating: HeroRating = seedRatingOf(key)

        private val _uiState =
            MutableStateFlow(
                ExpertProfileUiState(
                    expertId = key.expertId,
                    name = key.name,
                    title = key.title,
                    imageUrl = key.imageUrl,
                    tags = key.tags,
                    rating = seedRating,
                ),
            )
        val uiState: StateFlow<ExpertProfileUiState> = _uiState.asStateFlow()

        private var detailsJob: Job? = null
        private var reviewsJob: Job? = null
        private var favoriteInFlight = false

        init {
            load()
        }

        /** Re-sends both requests (Retry on the error state). */
        fun retry() = load()

        /**
         * Heart tap (D-03, D-04): flips at once, then PUT when it was not favorited and DELETE when it was, and
         * takes the server's `is_favorite`. Any [ApiException] rolls back and sets [ExpertProfileUiState.favoriteError].
         * Ignored while a toggle is in flight or before the details have loaded; taps are never queued.
         */
        fun onToggleFavorite() {
            val current = _uiState.value
            if (favoriteInFlight || current.details !is DetailsState.Loaded) return
            val previous = current.isFavorite
            favoriteInFlight = true
            _uiState.update { it.copy(isFavorite = !previous) }
            viewModelScope.launch {
                try {
                    val result =
                        if (previous) favorites.remove(current.expertId) else favorites.add(current.expertId)
                    _uiState.update { it.copy(isFavorite = result.isFavorite) }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isFavorite = previous, favoriteError = e.toFavoriteFailure()) }
                } finally {
                    favoriteInFlight = false
                }
            }
        }

        /** The screen has shown the favorite failure message. */
        fun consumeFavoriteError() {
            _uiState.update { it.copy(favoriteError = null) }
        }

        /** The Book Session destination, built from what the profile currently shows. */
        fun bookingKey(): BookingKey = _uiState.value.let { BookingKey(it.expertId, it.name, it.title, it.imageUrl) }

        private fun load() {
            detailsJob?.cancel()
            reviewsJob?.cancel()
            _uiState.update {
                it.copy(details = DetailsState.Loading, reviews = ReviewsState.Loading, rating = seedRating)
            }
            val expertId = _uiState.value.expertId
            detailsJob = viewModelScope.launch { loadDetails(expertId) }
            reviewsJob = viewModelScope.launch { loadReviews(expertId) }
        }

        private suspend fun loadDetails(expertId: String) {
            try {
                val data = repository.details(expertId)
                _uiState.update { it.mergeDetails(data) }
            } catch (e: ApiException) {
                val failed =
                    if ((e as? ApiException.Http)?.status == HTTP_NOT_FOUND) {
                        DetailsState.Unavailable
                    } else {
                        DetailsState.Error
                    }
                _uiState.update { it.copy(details = failed) }
            }
        }

        private suspend fun loadReviews(expertId: String) {
            try {
                val payload = repository.reviewsPreview(expertId)
                val total = payload.summary.totalReviews
                val rating =
                    if (total == 0) {
                        HeroRating.NoReviews
                    } else {
                        HeroRating.Shown(payload.summary.overallRating ?: seedRatingValue(), total)
                    }
                val rows =
                    payload.reviews.take(REVIEWS_PREVIEW_LIMIT).map {
                        ReviewRowUi(it.id, it.reviewer.name, it.rating, it.comment)
                    }
                _uiState.update { it.copy(rating = rating, reviews = ReviewsState.Loaded(rows)) }
            } catch (e: ApiException) {
                val rating = if (seedRating is HeroRating.Pending) HeroRating.Hidden else seedRating
                _uiState.update { it.copy(rating = rating, reviews = ReviewsState.Failed) }
            }
        }

        private fun seedRatingValue(): Double = (seedRating as? HeroRating.Shown)?.rating ?: 0.0

        private fun ExpertProfileUiState.mergeDetails(data: PublicProfileDetailsData): ExpertProfileUiState {
            val profile = data.profile
            return copy(
                name = profile.name.ifBlank { name },
                title = profile.title?.takeIf { it.isNotBlank() } ?: title,
                // A reload that lands mid-toggle must not overwrite the optimistic heart (D-04).
                isFavorite = if (favoriteInFlight) isFavorite else data.isFavorite,
                details =
                    DetailsState.Loaded(
                        ProfileDetailsUi(
                            experienceYears = profile.stats.experienceYears,
                            totalBookings = profile.stats.totalBookings,
                            bio = profile.bio,
                            topics = data.topicsOfDiscussion.map { it.toRow() },
                            isBookable = data.isBookable ?: true,
                        ),
                    ),
            )
        }

        private fun TopicOfDiscussionDto.toRow() =
            TopicRowUi(
                id = id,
                title = title,
                durationMins = durationMins,
                description = description,
                price = PriceDisplay.of(price.isFree, price.amount, price.currency),
            )

        private companion object {
            fun seedRatingOf(key: ExpertProfileKey): HeroRating {
                val count = key.reviewCount
                val rating = key.rating
                return when {
                    count == 0 -> HeroRating.NoReviews
                    rating != null && count != null && count > 0 -> HeroRating.Shown(rating, count)
                    else -> HeroRating.Pending
                }
            }
        }
    }
