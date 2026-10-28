package com.uptrendinvestments.clickmeapp.screens.clientmode.writereview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ReviewLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ReviewsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Write Review (client only, D-19). Port of iOS `WriteReviewViewModel`, with the D-19 deviation that an already
 * submitted review is read-only (iOS allows an edit).
 *
 * A successful submit marks the booking reviewed and posts the hand-off to the screen in [WriteReviewKey.opener].
 */
@HiltViewModel(assistedFactory = WriteReviewViewModel.Factory::class)
class WriteReviewViewModel
    @AssistedInject
    constructor(
        @Assisted private val key: WriteReviewKey,
        private val savedStateHandle: SavedStateHandle,
        private val repository: ReviewsRepository,
        private val launchRequest: ReviewLaunchRequest,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(key: WriteReviewKey): WriteReviewViewModel
        }

        private val _uiState =
            MutableStateFlow(
                WriteReviewUiState(header = ReviewHeader(key.expertName, key.expertAvatarUrl, null)),
            )
        val uiState: StateFlow<WriteReviewUiState> = _uiState.asStateFlow()

        init {
            load()
        }

        private fun load() {
            viewModelScope.launch {
                val context = repository.context(key.bookingId)
                _uiState.update {
                    it.copy(
                        load = WriteReviewLoad.Ready,
                        header =
                            ReviewHeader(
                                expertName = context.reviewTarget?.name?.takeIf(String::isNotBlank) ?: key.expertName,
                                expertAvatarUrl =
                                    context.reviewTarget?.avatarUrl?.takeIf(String::isNotBlank)
                                        ?: key.expertAvatarUrl,
                                sessionTopic = context.sessionTopic,
                            ),
                    )
                }
            }
        }

        /** Posts the review. Nothing else in this class calls the repository's `submit`. */
        fun submit() {
            val state = _uiState.value
            viewModelScope.launch {
                repository.submit(key.bookingId, state.rating, state.comment)
                launchRequest.onSubmitted(key.bookingId, key.opener)
                _uiState.update { it.copy(submitting = false, submitted = true) }
            }
        }

        /** The screen has popped for [WriteReviewUiState.submitted]. */
        fun onSubmittedHandled() {
            _uiState.update { it.copy(submitted = false) }
        }
    }
