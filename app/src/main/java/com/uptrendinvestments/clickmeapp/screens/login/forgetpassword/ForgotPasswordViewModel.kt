package com.uptrendinvestments.clickmeapp.screens.login.forgetpassword

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.model.requests.ForgotPasswordRequest
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import com.uptrendinvestments.clickmeapp.screens.login.AuthValidators
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.login.fieldMessage
import com.uptrendinvestments.clickmeapp.screens.login.toDialogError
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: FieldError? = null,
    val dialogError: FieldError? = null,
    val saveState: SaveState = SaveState.Idle,
    val didAttemptSend: Boolean = false,
)

private const val KEY_EMAIL = "forgot_email"

/**
 * Port of iOS `LoginForgetPassViewModel` (D-08). Requests the password-reset code; the screen then opens Set New
 * Password with the trimmed email. There is no resend and no cooldown (D-09). The endpoint is public, so the
 * ViewModel talks to [ClickMeApi] directly. The email arrives through [start], never through shared state.
 */
@HiltViewModel
class ForgotPasswordViewModel
    @Inject
    constructor(
        private val api: ClickMeApi,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ForgotPasswordUiState())
        val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

        private val _navigateToReset = MutableStateFlow<String?>(null)
        val navigateToReset: StateFlow<String?> = _navigateToReset.asStateFlow()

        private val _focusEmail = MutableStateFlow(false)
        val focusEmail: StateFlow<Boolean> = _focusEmail.asStateFlow()

        private var started = false

        /** Applies the Login email once per instance; a value restored from [SavedStateHandle] wins. */
        fun start(initialEmail: String) {
            if (started) return
            started = true
            val email = savedStateHandle.get<String>(KEY_EMAIL) ?: initialEmail
            _uiState.update { it.copy(email = email) }
        }

        fun onEmailChange(value: String) {
            savedStateHandle[KEY_EMAIL] = value
            _uiState.update {
                it.copy(
                    email = value,
                    emailError = if (it.didAttemptSend) AuthValidators.forgotEmail(value) else null,
                )
            }
        }

        fun send() {
            val current = _uiState.value
            if (current.saveState != SaveState.Idle) return
            val identity = current.email.trim()
            val validation = AuthValidators.forgotEmail(identity)
            if (validation != null) {
                _uiState.update { it.copy(didAttemptSend = true, emailError = validation) }
                _focusEmail.value = true
                return
            }
            _uiState.update {
                it.copy(didAttemptSend = true, emailError = null, dialogError = null, saveState = SaveState.Saving)
            }
            viewModelScope.launch {
                try {
                    apiCall { api.forgotPassword(ForgotPasswordRequest(identity = identity)) }
                    _uiState.update { it.copy(saveState = SaveState.Saved) }
                    delay(Motion.SAVED_CONFIRMATION_HOLD_MS.toLong())
                    _navigateToReset.value = identity
                } catch (e: ApiException) {
                    val inline = (e as? ApiException.Http)?.fieldMessage("identity", "email")
                    _uiState.update {
                        it.copy(
                            saveState = SaveState.Idle,
                            emailError = inline?.let { message -> FieldError.Text(message) } ?: it.emailError,
                            dialogError = e.toDialogError(),
                        )
                    }
                }
            }
        }

        fun dismissDialog() = _uiState.update { it.copy(dialogError = null) }

        /** Back from Set New Password must show an active button again, not "Code Sent!". */
        fun onNavigated() {
            _navigateToReset.value = null
            _uiState.update { it.copy(saveState = SaveState.Idle) }
        }

        fun onFocusConsumed() {
            _focusEmail.value = false
        }
    }
