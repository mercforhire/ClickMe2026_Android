package com.uptrendinvestments.clickmeapp.screens.clientmode.reviews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.model.responses.ExpertReviewsPayload
import com.uptrendinvestments.clickmeapp.model.responses.ReviewItemDto
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
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

/**
 * Full Reviews list state machine: a port of iOS `ExpertReviewsViewModel` (page 1 only, Phase 16.3 D-16) with the
 * Phase 15 states. Pull-to-refresh and the refresh-failed snackbar are Android additions (D-16). Loads on creation;
 * a first-load failure is [ExpertReviewsLoad.Failed] and [retry] reloads; a failed pull over loaded content keeps the
 * content and raises [ExpertReviewsUiState.refreshFailed] once; a pull on a failed load behaves as a first load.
 * Review text is never logged.
 */
@HiltViewModel(assistedFactory = ExpertReviewsViewModel.Factory::class)
class ExpertReviewsViewModel
    @AssistedInject
    constructor(
        @Assisted key: ExpertReviewsKey,
        private val repository: ReviewsRepository,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(key: ExpertReviewsKey): ExpertReviewsViewModel
        }

        private enum class LoadMode { Loud, Pull }

        private val _uiState =
            MutableStateFlow(ExpertReviewsUiState(key.expertId, reviewsTitleFirstName(key.expertName)))
        val uiState: StateFlow<ExpertReviewsUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null

        init {
            startLoad(LoadMode.Loud)
        }

        /** Reloads from the failed state (Retry). */
        fun retry() = startLoad(LoadMode.Loud)

        /** Pull-to-refresh: keeps loaded content on screen; from the failed state it is a first load. */
        fun refresh() = startLoad(if (_uiState.value.load is ExpertReviewsLoad.Loaded) LoadMode.Pull else LoadMode.Loud)

        /** Clears the one-shot refresh-failed flag after the snackbar was shown. */
        fun consumeRefreshFailure() {
            _uiState.update { it.copy(refreshFailed = false) }
        }

        private fun startLoad(mode: LoadMode) {
            loadJob?.cancel()
            _uiState.update {
                it.copy(
                    refreshFailed = false,
                    isRefreshing = mode == LoadMode.Pull,
                    load = if (mode == LoadMode.Loud) ExpertReviewsLoad.Loading else it.load,
                )
            }
            val expertId = _uiState.value.expertId
            loadJob =
                viewModelScope.launch {
                    try {
                        val payload = repository.expertReviews(expertId)
                        _uiState.update { it.copy(load = payload.toLoaded(), isRefreshing = false) }
                    } catch (e: ApiException) {
                        _uiState.update {
                            if (mode == LoadMode.Loud) {
                                it.copy(load = ExpertReviewsLoad.Failed(e.toSectionMessage()), isRefreshing = false)
                            } else {
                                it.copy(refreshFailed = true, isRefreshing = false)
                            }
                        }
                    }
                }
        }

        private fun ExpertReviewsPayload.toLoaded() =
            ExpertReviewsLoad.Loaded(
                overallRating = summary.overallRating ?: 0.0,
                totalReviews = summary.totalReviews,
                reviews = reviews.map { it.toUi() },
            )

        private fun ReviewItemDto.toUi() =
            ExpertReviewUi(
                id = id,
                reviewerName = reviewer.name.nonBlank(),
                avatarUrl = reviewer.avatarUrl,
                rating = rating,
                topic = sessionContext?.topic.nonBlank(),
                dateLabel = sessionContext?.dateLabel.nonBlank(),
                comment = comment.nonBlank(),
            )

        private fun String?.nonBlank(): String? = this?.takeIf { it.isNotBlank() }
    }
