package com.uptrendinvestments.clickmeapp.screens.clientmode.writereview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ReviewLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ReviewsRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ReviewsRepository.Companion.RATING_MAX
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ReviewsRepository.Companion.RATING_MIN
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
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

        /** Loads the review context again after a failure. */
        fun retry() {
            load()
        }

        private fun load() {
            _uiState.update { it.copy(load = WriteReviewLoad.Loading) }
            viewModelScope.launch {
                val context =
                    try {
                        repository.context(key.bookingId)
                    } catch (e: ApiException) {
                        _uiState.update { it.copy(load = WriteReviewLoad.Failed(e.toSectionMessage())) }
                        return@launch
                    }
                val readOnly = context.hasSubmitted
                val rating =
                    if (readOnly) {
                        context.prefill?.rating?.coerceIn(RATING_MIN, RATING_MAX) ?: DEFAULT_RATING
                    } else {
                        savedStateHandle.get<Int>(KEY_RATING)?.takeIf { it in RATING_MIN..RATING_MAX }
                            ?: DEFAULT_RATING
                    }
                val comment =
                    if (readOnly) {
                        context.prefill?.comment.orEmpty()
                    } else {
                        savedStateHandle.get<String>(KEY_COMMENT).orEmpty().take(ReviewsRepository.COMMENT_MAX)
                    }
                _uiState.update {
                    it.copy(
                        load = WriteReviewLoad.Ready,
                        readOnly = readOnly,
                        rating = rating,
                        comment = comment,
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

        /** Selects [stars] (1..5). Ignored until the form is ready, once it is read-only and while a submit runs. */
        fun onRatingSelected(stars: Int) {
            if (!isEditable() || stars !in RATING_MIN..RATING_MAX) return
            savedStateHandle[KEY_RATING] = stars
            _uiState.update { it.copy(rating = stars) }
        }

        /** Sets the comment, truncated to the backend limit. Same guards as [onRatingSelected]. */
        fun onCommentChanged(text: String) {
            if (!isEditable()) return
            val capped = text.take(ReviewsRepository.COMMENT_MAX)
            savedStateHandle[KEY_COMMENT] = capped
            _uiState.update { it.copy(comment = capped) }
        }

        /**
         * Posts the review. Nothing else in this class calls the repository's `submit`, and only an editable, idle
         * form reaches it (T-13-23).
         */
        fun submit() {
            if (!isEditable()) return
            val state = _uiState.value
            _uiState.update { it.copy(submitting = true, submitError = null) }
            viewModelScope.launch {
                try {
                    repository.submit(key.bookingId, state.rating, state.comment)
                } catch (e: ApiException) {
                    // The form stays intact and editable; only the dialog message changes.
                    _uiState.update { it.copy(submitting = false, submitError = e.toSubmitError()) }
                    return@launch
                }
                launchRequest.onSubmitted(key.bookingId, key.opener)
                _uiState.update { it.copy(submitting = false, submitted = true) }
            }
        }

        /** Closes the submit error dialog. */
        fun dismissSubmitError() {
            _uiState.update { it.copy(submitError = null) }
        }

        /**
         * The server message, else the connection line for a network failure, else the generic line. Null for
         * ACCOUNT_SUSPENDED because the global suspended dialog owns it (Phase 2 D-08).
         */
        private fun ApiException.toSubmitError(): FieldError? =
            when (this) {
                is ApiException.Http ->
                    when {
                        code == ErrorCode.AccountSuspended -> null
                        else -> userMessage?.let { FieldError.Text(it) } ?: genericError
                    }
                is ApiException.Network -> FieldError.Res(R.string.login_error_network)
                is ApiException.Decoding -> genericError
            }

        private fun isEditable(): Boolean {
            val state = _uiState.value
            return state.load is WriteReviewLoad.Ready && !state.readOnly && !state.submitting
        }

        /** The screen has popped for [WriteReviewUiState.submitted]. */
        fun onSubmittedHandled() {
            _uiState.update { it.copy(submitted = false) }
        }

        private companion object {
            val genericError = FieldError.Res(R.string.write_review_error_generic)
            const val KEY_RATING = "rating"
            const val KEY_COMMENT = "comment"
        }
    }
