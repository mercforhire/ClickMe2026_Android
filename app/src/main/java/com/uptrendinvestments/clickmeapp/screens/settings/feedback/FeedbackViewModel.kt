package com.uptrendinvestments.clickmeapp.screens.settings.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.model.requests.SubmitFeedbackRequest
import com.uptrendinvestments.clickmeapp.model.responses.FeedbackTypeItem
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The feedback types load: [Loading] on entry and Retry; [Failed] (also an empty list) is the error state. */
sealed interface FeedbackLoad {
    data object Loading : FeedbackLoad

    data object Ready : FeedbackLoad

    data class Failed(
        val message: FieldError,
    ) : FeedbackLoad
}

/**
 * Send Feedback state. [details] and [email] are user-written, so [toString] prints [load] and [saveState] only and
 * the state is never logged or put in saved state (RESEARCH V8).
 */
data class FeedbackUiState(
    val load: FeedbackLoad = FeedbackLoad.Loading,
    val types: List<FeedbackTypeItem> = emptyList(),
    val selectedTypeId: String? = null,
    val details: String = "",
    val email: String = "",
    val showDetailsError: Boolean = false,
    val emailError: String? = null,
    val saveState: SaveState = SaveState.Idle,
    val dialog: FieldError? = null,
    val showThanks: Boolean = false,
) {
    override fun toString(): String = "FeedbackUiState(load=$load, saveState=$saveState)"
}

/**
 * State holder for Send Feedback (PARITY-AUDIT #6, D-05, D-06). Types come from the server; details are gated at
 * [FEEDBACK_DETAILS_MIN] trimmed characters before any request; the email is prefilled from the signed-in user.
 */
@HiltViewModel
class FeedbackViewModel
    @Inject
    constructor(
        private val repository: FeedbackRepository,
        private val userManager: UserManager,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(FeedbackUiState())
        val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null

        init {
            retry()
        }

        /** First load and Retry: a single latest-wins job. An empty list is as unusable as a failure. */
        fun retry() {
            loadJob?.cancel()
            _uiState.update { it.copy(load = FeedbackLoad.Loading) }
            loadJob =
                viewModelScope.launch {
                    try {
                        val types = repository.types()
                        if (types.isEmpty()) {
                            _uiState.update {
                                it.copy(load = FeedbackLoad.Failed(FieldError.Res(R.string.login_error_unknown)))
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    load = FeedbackLoad.Ready,
                                    types = types,
                                    selectedTypeId = it.selectedTypeId ?: types.first().id,
                                )
                            }
                        }
                    } catch (e: ApiException) {
                        _uiState.update { it.copy(load = FeedbackLoad.Failed(e.toSectionMessage())) }
                    }
                }
        }

        fun onSelectType(id: String) {
            _uiState.update { it.copy(selectedTypeId = id) }
        }

        fun onDetailsChange(text: String) {
            _uiState.update { it.copy(details = text) }
        }

        fun onEmailChange(text: String) {
            _uiState.update { it.copy(email = text) }
        }

        /** Sends the trimmed details and email. Ignored while sending and after success; short details never leave. */
        fun submit() {
            val state = _uiState.value
            if (state.saveState != SaveState.Idle) return
            if (feedbackDetailsTooShort(state.details)) return
            val typeId = state.selectedTypeId ?: return
            _uiState.update { it.copy(saveState = SaveState.Saving) }
            viewModelScope.launch {
                try {
                    repository.submit(
                        SubmitFeedbackRequest(
                            typeId = typeId,
                            details = state.details.trim(),
                            email = state.email.trim().ifEmpty { null },
                        ),
                    )
                    _uiState.update { it.copy(saveState = SaveState.Saved, showThanks = true) }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(saveState = SaveState.Idle) }
                }
            }
        }
    }
