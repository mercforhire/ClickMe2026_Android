package com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount

import androidx.lifecycle.ViewModel
import com.uptrendinvestments.clickmeapp.managers.CallGuard
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** The two forms of the flow. */
enum class DeleteStep { Warning, Reason }

/** Longest comment the form accepts (UI-SPEC A5: the backend sets no maximum). */
const val FEEDBACK_MAX = 1000

/** The comments counter appears from this length. */
const val FEEDBACK_COUNTER_FROM = 800

data class DeleteAccountUiState(
    val step: DeleteStep = DeleteStep.Warning,
    val reason: DeleteAccountReason? = null,
    val feedback: String = "",
    val password: String = "",
    val passwordError: FieldError? = null,
    val isDeleting: Boolean = false,
    val dialog: FieldError? = null,
) {
    val canConfirm: Boolean get() = false
}

@HiltViewModel
class DeleteAccountViewModel
    @Inject
    constructor(
        private val repository: DeleteAccountRepository,
        private val callGuard: CallGuard,
        private val deletionState: AccountDeletionState,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DeleteAccountUiState())
        val uiState: StateFlow<DeleteAccountUiState> = _uiState.asStateFlow()

        fun continueFromWarning() = Unit

        fun backToWarning() = Unit

        fun selectReason(reason: DeleteAccountReason) = Unit

        fun onFeedbackChange(value: String) = Unit

        fun onPasswordChange(value: String) = Unit

        fun confirmDeletion() = Unit

        fun dismissDialog() = Unit
    }
