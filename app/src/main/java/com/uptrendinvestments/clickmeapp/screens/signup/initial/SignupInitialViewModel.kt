package com.uptrendinvestments.clickmeapp.screens.signup.initial

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.PasswordStrength
import com.uptrendinvestments.clickmeapp.managers.LaunchDestination
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.login.AuthValidators
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.login.fieldMessage
import com.uptrendinvestments.clickmeapp.screens.login.toDialogError
import com.uptrendinvestments.clickmeapp.screens.signup.SignupSetupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The signup Initial fields a focus request can target. */
enum class SignupField { Email, Password }

/**
 * Signup Initial form state. [emailError] and [passwordError] are inline messages; [dialogError] opens the shared
 * error dialog. [didAttemptSubmit] switches on live validation (D-02, iOS `didAttemptSubmit`).
 */
data class SignupInitialUiState(
    val email: String = "",
    val password: String = "",
    val emailError: FieldError? = null,
    val passwordError: FieldError? = null,
    val dialogError: FieldError? = null,
    val isSubmitting: Boolean = false,
    val didAttemptSubmit: Boolean = false,
) {
    val strength: PasswordStrength get() = PasswordStrength.evaluate(password)
}

/**
 * Signup Initial step (SIGN-01), ported from iOS `SignupInitialViewModel`: email and password only (D-01).
 * No validation until the first submit, then live. On success the setup draft is seeded and [destination]
 * becomes [LaunchDestination.SignupSetup], which makes AppViewModel replace the auth stack with the hub.
 * Only the email is mirrored to [SavedStateHandle]; the password stays in memory (T-04-26).
 */
@HiltViewModel
class SignupInitialViewModel
    @Inject
    constructor(
        private val userManager: UserManager,
        private val repository: SignupSetupRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(SignupInitialUiState(email = savedStateHandle.get<String>(EMAIL_KEY).orEmpty()))
        val uiState: StateFlow<SignupInitialUiState> = _uiState.asStateFlow()

        private val _destination = MutableStateFlow<LaunchDestination?>(null)
        val destination: StateFlow<LaunchDestination?> = _destination.asStateFlow()

        private val _focusRequest = MutableStateFlow<SignupField?>(null)
        val focusRequest: StateFlow<SignupField?> = _focusRequest.asStateFlow()

        fun onEmailChange(value: String) {
            savedStateHandle[EMAIL_KEY] = value
            _uiState.update { state ->
                state.copy(
                    email = value,
                    emailError = if (state.didAttemptSubmit) AuthValidators.signupEmail(value) else null,
                )
            }
        }

        fun onPasswordChange(value: String) {
            _uiState.update { state ->
                state.copy(
                    password = value,
                    passwordError = if (state.didAttemptSubmit) AuthValidators.signupPassword(value) else null,
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
            val emailError = AuthValidators.signupEmail(current.email)
            val passwordError = AuthValidators.signupPassword(current.password)
            if (emailError != null || passwordError != null) {
                _uiState.update {
                    it.copy(didAttemptSubmit = true, emailError = emailError, passwordError = passwordError)
                }
                _focusRequest.value = if (emailError != null) SignupField.Email else SignupField.Password
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
                    val email = current.email.trim()
                    val data = userManager.signup(email, current.password)
                    repository.seedFromSignup(email, data.emailVerified)
                    _destination.value = LaunchDestination.SignupSetup
                } catch (e: ApiException) {
                    handle(e)
                } finally {
                    _uiState.update { it.copy(isSubmitting = false) }
                }
            }
        }

        /** D-02: 409 inline under the email, 422 per field, everything else in the dialog. */
        private fun handle(e: ApiException) {
            if (e is ApiException.Http) {
                when {
                    // The global suspended-account dialog owns this (Phase 2 D-08).
                    e.code == ErrorCode.AccountSuspended -> return
                    e.status == 409 || e.code == ErrorCode.EmailTaken -> {
                        val error =
                            e.serverMessage?.let { FieldError.Text(it) }
                                ?: FieldError.Res(R.string.signup_initial_error_email_taken)
                        _uiState.update { it.copy(emailError = error) }
                        return
                    }
                    e.status == 422 -> {
                        val email = e.fieldMessage("email")?.let { FieldError.Text(it) }
                        val password = e.fieldMessage("password")?.let { FieldError.Text(it) }
                        if (email != null || password != null) {
                            _uiState.update {
                                it.copy(
                                    emailError = email ?: it.emailError,
                                    passwordError = password ?: it.passwordError,
                                )
                            }
                            return
                        }
                    }
                }
            }
            _uiState.update { it.copy(dialogError = e.toDialogError()) }
        }

        private companion object {
            const val EMAIL_KEY = "signup_email"
        }
    }
