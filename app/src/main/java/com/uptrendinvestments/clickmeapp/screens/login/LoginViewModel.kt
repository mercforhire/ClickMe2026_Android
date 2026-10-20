package com.uptrendinvestments.clickmeapp.screens.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.LaunchDestination
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.network.ApiException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The login fields a focus request can target. */
enum class LoginField { Email, Password }

/**
 * Login form state. [emailError] and [passwordError] are inline messages; [dialogError] opens the shared
 * error dialog (D-04). [didAttemptSubmit] switches on live validation (D-02, iOS `didAttemptLogin`).
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: FieldError? = null,
    val passwordError: FieldError? = null,
    val dialogError: FieldError? = null,
    val isSubmitting: Boolean = false,
    val didAttemptSubmit: Boolean = false,
)

/**
 * Login (AUTH-01), ported from iOS `LoginViewModel`: no validation until the first submit, then live
 * re-validation on every edit. Routing after login is [UserManager.resolvePostAuthDestination].
 * Only the email is mirrored to [SavedStateHandle]; the password stays in memory (T-03-13).
 */
@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val userManager: UserManager,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(LoginUiState(email = savedStateHandle.get<String>(EMAIL_KEY).orEmpty()))
        val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

        private val _destination = MutableStateFlow<LaunchDestination?>(null)
        val destination: StateFlow<LaunchDestination?> = _destination.asStateFlow()

        private val _focusRequest = MutableStateFlow<LoginField?>(null)
        val focusRequest: StateFlow<LoginField?> = _focusRequest.asStateFlow()

        fun onEmailChange(value: String) {
            savedStateHandle[EMAIL_KEY] = value
            _uiState.update { state ->
                state.copy(
                    email = value,
                    emailError = if (state.didAttemptSubmit) AuthValidators.loginEmail(value) else null,
                    // A server password error (the 401) goes stale as soon as the credentials change.
                    passwordError = state.passwordError.takeUnless { it is FieldError.Text },
                )
            }
        }

        fun onPasswordChange(value: String) {
            _uiState.update { state ->
                state.copy(
                    password = value,
                    passwordError = if (state.didAttemptSubmit) AuthValidators.loginPassword(value) else null,
                )
            }
        }

        /** D-10 hook: the Password Changed return pre-fills the email and starts from a clean form. */
        fun prefill(email: String) {
            savedStateHandle[EMAIL_KEY] = email
            _uiState.update {
                it.copy(
                    email = email,
                    password = "",
                    emailError = null,
                    passwordError = null,
                    dialogError = null,
                    didAttemptSubmit = false,
                )
            }
        }

        fun dismissDialog() = _uiState.update { it.copy(dialogError = null) }

        fun onDestinationConsumed() {
            _destination.value = null
        }

        fun onFocusRequestConsumed() {
            _focusRequest.value = null
        }

        fun submit() {
            val current = _uiState.value
            if (current.isSubmitting) return
            val emailError = AuthValidators.loginEmail(current.email)
            val passwordError = AuthValidators.loginPassword(current.password)
            if (emailError != null || passwordError != null) {
                _uiState.update {
                    it.copy(didAttemptSubmit = true, emailError = emailError, passwordError = passwordError)
                }
                _focusRequest.value = if (emailError != null) LoginField.Email else LoginField.Password
                return
            }
            _uiState.update {
                it.copy(
                    didAttemptSubmit = true,
                    emailError = null,
                    passwordError = null,
                    dialogError = null,
                    isSubmitting = true,
                )
            }
            viewModelScope.launch {
                try {
                    // iOS trims the email: a trailing space otherwise trips the backend's strict validator (422).
                    val user = userManager.login(current.email.trim(), current.password)
                    _destination.value = userManager.resolvePostAuthDestination(user.userRoles)
                } catch (e: ApiException) {
                    handle(e)
                } finally {
                    _uiState.update { it.copy(isSubmitting = false) }
                }
            }
        }

        /** D-04: 401 inline under the password, 422 email inline, everything else in the dialog. */
        private fun handle(e: ApiException) {
            if (e is ApiException.Http) {
                when {
                    // The global suspended-account dialog owns this (Phase 2 D-08).
                    e.code == ErrorCode.AccountSuspended -> return
                    e.status == 401 -> {
                        val error =
                            e.userMessage?.let { FieldError.Text(it) } ?: FieldError.Res(R.string.login_error_unknown)
                        _uiState.update { it.copy(passwordError = error) }
                        return
                    }
                    e.status == 422 -> {
                        val message = e.fieldMessage("email")
                        if (message != null) {
                            _uiState.update { it.copy(emailError = FieldError.Text(message)) }
                            return
                        }
                    }
                }
            }
            _uiState.update { it.copy(dialogError = e.toDialogError()) }
        }

        private companion object {
            const val EMAIL_KEY = "login_email"
        }
    }
