package com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.model.responses.PublicProfileDetailsData
import com.uptrendinvestments.clickmeapp.model.responses.TopicOfDiscussionDto
import com.uptrendinvestments.clickmeapp.network.ApiException
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
 */
@HiltViewModel(assistedFactory = ExpertProfileViewModel.Factory::class)
class ExpertProfileViewModel
    @AssistedInject
    constructor(
        @Assisted key: ExpertProfileKey,
        private val repository: ExpertProfileRepository,
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

        init {
            load()
        }

        /** Re-sends both requests (Retry on the error state). */
        fun retry() = load()

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
