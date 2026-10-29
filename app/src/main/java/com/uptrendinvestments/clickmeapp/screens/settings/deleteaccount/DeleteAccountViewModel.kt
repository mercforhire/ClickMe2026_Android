package com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount

import androidx.lifecycle.ViewModel
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.di.ApplicationScope
import com.uptrendinvestments.clickmeapp.managers.CallGuard
import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.util.clampLength
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The two forms of the flow (UI-SPEC Screen 4). The Farewell is not a step: it is hosted at the app root. */
enum class DeleteStep { Warning, Reason }

/** Longest comment the form accepts (UI-SPEC A5: the backend sets no maximum). */
const val FEEDBACK_MAX = 1000

/** The comments counter appears from this length. */
const val FEEDBACK_COUNTER_FROM = 800

private const val HTTP_BAD_REQUEST = 400
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val HTTP_CONFLICT = 409
private const val HTTP_TOO_MANY_REQUESTS = 429
private val PASSWORD_INCORRECT = FieldError.Res(R.string.delete_account_password_incorrect)
private val GENERIC = FieldError.Res(R.string.delete_account_generic)

/**
 * Everything the Delete Account screen shows. [password] and the deletion token are secrets: this state is never put
 * in a saved-state handle, and [toString] leaves out the password and the comments.
 */
data class DeleteAccountUiState(
    val step: DeleteStep = DeleteStep.Warning,
    val reason: DeleteAccountReason? = null,
    val feedback: String = "",
    val password: String = "",
    val passwordError: FieldError? = null,
    val isDeleting: Boolean = false,
    val dialog: FieldError? = null,
) {
    /** The final "Delete Account" is available once a reason and a password are given and nothing is in flight. */
    val canConfirm: Boolean get() = reason != null && password.isNotBlank() && !isDeleting

    override fun toString(): String =
        "DeleteAccountUiState(step=$step, reason=$reason, isDeleting=$isDeleting, password=<redacted>)"
}

/**
 * State holder for Delete Account. Ports iOS `DeleteAccountViewModel` (PROF-04, CONTEXT D-05): Warning, then Reason
 * plus password; one confirm runs `POST user/account/delete-request` and then `DELETE user/account`.
 *
 * The DELETE is irreversible, so a single [DeleteAccountUiState.isDeleting] flag covers both calls and every entry
 * point returns early while it is set (T-14-13). On success it stays set: the root Farewell covers the screen
 * ([AccountDeletionState]) and nothing else runs. The password and the token live in memory or in a local variable
 * only (T-14-14). The request pair runs on the application scope so that leaving the screen cannot cancel it.
 */
@HiltViewModel
class DeleteAccountViewModel
    @Inject
    constructor(
        private val repository: DeleteAccountRepository,
        private val callGuard: CallGuard,
        private val deletionState: AccountDeletionState,
        @ApplicationScope private val appScope: CoroutineScope,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DeleteAccountUiState())
        val uiState: StateFlow<DeleteAccountUiState> = _uiState.asStateFlow()

        /** Warning's "Delete Account": moves to the Reason form unless a live call blocks the action. */
        fun continueFromWarning() {
            if (_uiState.value.isDeleting) return
            if (!callGuard.attempt(RestrictedAction.DeleteAccount)) return
            _uiState.update { it.copy(step = DeleteStep.Reason) }
        }

        fun backToWarning() {
            if (_uiState.value.isDeleting) return
            _uiState.update { it.copy(step = DeleteStep.Warning) }
        }

        fun selectReason(reason: DeleteAccountReason) {
            if (_uiState.value.isDeleting) return
            _uiState.update { it.copy(reason = reason) }
        }

        fun onFeedbackChange(value: String) {
            if (_uiState.value.isDeleting) return
            _uiState.update { it.copy(feedback = value.clampLength(FEEDBACK_MAX)) }
        }

        fun onPasswordChange(value: String) {
            if (_uiState.value.isDeleting) return
            _uiState.update { it.copy(password = value, passwordError = null) }
        }

        /**
         * The user's final tap. Returns early when a request pair is in flight, the form is incomplete or a live call
         * blocks the action, so the irreversible DELETE can never be sent twice for one confirmation.
         */
        fun confirmDeletion() {
            val state = _uiState.value
            if (state.isDeleting || !state.canConfirm) return
            val reason = state.reason ?: return
            if (!callGuard.attempt(RestrictedAction.DeleteAccount)) return
            val password = state.password
            val feedback = state.feedback.trim().ifEmpty { null }
            _uiState.update { it.copy(isDeleting = true, passwordError = null, dialog = null) }
            // The application scope on purpose: once the server revokes the sessions, a 401 elsewhere can log the user
            // out and dispose this screen. That must not cancel the request pair or skip markDeleted() (the Farewell).
            // onFailure only touches _uiState, which is safe on a cleared ViewModel.
            appScope.launch {
                var failedCall = FailedCall.RequestDeletion
                try {
                    // A local on purpose: the token is never stored, saved or logged.
                    val token = repository.requestDeletion(password)
                    failedCall = FailedCall.Delete
                    repository.deleteAccount(token, reason, feedback)
                    deletionState.markDeleted()
                } catch (e: ApiException) {
                    onFailure(e, failedCall)
                }
            }
        }

        private enum class FailedCall { RequestDeletion, Delete }

        /**
         * Keeps the user on the form with the right message and re-enables it. Never retries by itself: a 409 spends
         * the token, so a retry runs both calls again and mints a new one (RESEARCH Pitfall 5).
         */
        private fun onFailure(
            e: ApiException,
            failedCall: FailedCall,
        ) {
            val failure: Failure =
                when {
                    e is ApiException.Http && e.code == ErrorCode.AccountSuspended -> Failure.Silent
                    e is ApiException.Network -> Failure.Dialog(FieldError.Res(R.string.delete_account_network))
                    e !is ApiException.Http -> Failure.Dialog(FieldError.Res(R.string.delete_account_generic))
                    e.status == HTTP_TOO_MANY_REQUESTS ->
                        Failure.Dialog(FieldError.Res(R.string.delete_account_too_many))
                    // A wrong password is a code-less 401 (RESEARCH Pitfall 4); 403 is treated the same.
                    failedCall == FailedCall.RequestDeletion &&
                        (e.status == HTTP_UNAUTHORIZED || e.status == HTTP_FORBIDDEN) ->
                        Failure.Password(e.userMessage?.let { FieldError.Text(it) } ?: PASSWORD_INCORRECT)
                    failedCall == FailedCall.Delete &&
                        e.status == HTTP_BAD_REQUEST &&
                        e.code == ErrorCode.InvalidToken ->
                        Failure.Password(FieldError.Res(R.string.delete_account_expired))
                    // The server message is user-ready (upcoming bookings, session in progress).
                    e.status == HTTP_CONFLICT ->
                        Failure.Dialog(e.userMessage?.let { FieldError.Text(it) } ?: GENERIC)
                    else -> Failure.Dialog(GENERIC)
                }
            _uiState.update {
                when (failure) {
                    is Failure.Password -> it.copy(isDeleting = false, passwordError = failure.error)
                    is Failure.Dialog -> it.copy(isDeleting = false, dialog = failure.error)
                    Failure.Silent -> it.copy(isDeleting = false)
                }
            }
        }

        private sealed interface Failure {
            data class Password(
                val error: FieldError,
            ) : Failure

            data class Dialog(
                val error: FieldError,
            ) : Failure

            /** ACCOUNT_SUSPENDED is owned by the global suspended-account dialog (Phase 2 D-08). */
            data object Silent : Failure
        }

        fun dismissDialog() {
            _uiState.update { it.copy(dialog = null) }
        }
    }
