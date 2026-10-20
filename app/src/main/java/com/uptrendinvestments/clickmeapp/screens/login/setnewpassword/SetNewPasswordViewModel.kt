package com.uptrendinvestments.clickmeapp.screens.login.setnewpassword

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.PasswordStrength
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.model.requests.ResetPasswordRequest
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import com.uptrendinvestments.clickmeapp.screens.login.AuthValidators
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
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

/** The inputs of the Set New Password form, used to drive focus after a failed submit. */
enum class ResetField { Code, NewPassword, Confirm }

data class SetNewPasswordUiState(
    val email: String = "",
    val code: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val codeError: FieldError? = null,
    val newPasswordError: FieldError? = null,
    val confirmError: FieldError? = null,
    val dialogError: FieldError? = null,
    val saveState: SaveState = SaveState.Idle,
    val didAttemptSubmit: Boolean = false,
) {
    val strength: PasswordStrength get() = PasswordStrength.evaluate(newPassword)
    val passwordsMatch: Boolean get() = confirmPassword.isNotEmpty() && confirmPassword == newPassword
}

/**
 * Port of iOS `LoginResetPasswordViewModel`: verify the 6-digit code and set the new password.
 * Only the code is mirrored to saved state; both passwords live in memory only (T-03-20).
 * There is no resend: an expired code means going Back and requesting a new one (D-09).
 */
@HiltViewModel
class SetNewPasswordViewModel
    @Inject
    constructor(
        private val api: ClickMeApi,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(SetNewPasswordUiState(code = savedStateHandle.get<String>(KEY_CODE).orEmpty()))
        val uiState: StateFlow<SetNewPasswordUiState> = _uiState.asStateFlow()

        private val _passwordChanged = MutableStateFlow<String?>(null)
        val passwordChanged: StateFlow<String?> = _passwordChanged.asStateFlow()

        private val _focusRequest = MutableStateFlow<ResetField?>(null)
        val focusRequest: StateFlow<ResetField?> = _focusRequest.asStateFlow()

        /** The email comes from the Forgot Password step and is not editable here. */
        fun start(email: String) {
            _uiState.update { it.copy(email = email) }
        }

        fun onCodeChange(raw: String) {
            val code = AuthValidators.sanitizeCode(raw)
            savedStateHandle[KEY_CODE] = code
            _uiState.update {
                it.copy(code = code, codeError = if (it.didAttemptSubmit) AuthValidators.resetCode(code) else null)
            }
        }

        fun onNewPasswordChange(value: String) {
            _uiState.update {
                it.copy(
                    newPassword = value,
                    newPasswordError = if (it.didAttemptSubmit) AuthValidators.newPassword(value) else null,
                )
            }
        }

        fun onConfirmChange(value: String) {
            _uiState.update {
                it.copy(
                    confirmPassword = value,
                    confirmError =
                        if (it.didAttemptSubmit) AuthValidators.confirmPassword(it.newPassword, value) else null,
                )
            }
        }

        fun dismissDialog() {
            _uiState.update { it.copy(dialogError = null) }
        }

        fun onNavigated() {
            _passwordChanged.value = null
        }

        fun onFocusRequestConsumed() {
            _focusRequest.value = null
        }

        fun submit() {
            val current = _uiState.value
            if (current.saveState != SaveState.Idle) return

            val codeError = AuthValidators.resetCode(current.code)
            val newPasswordError = AuthValidators.newPassword(current.newPassword)
            val confirmError = AuthValidators.confirmPassword(current.newPassword, current.confirmPassword)
            _uiState.update {
                it.copy(
                    didAttemptSubmit = true,
                    codeError = codeError,
                    newPasswordError = newPasswordError,
                    confirmError = confirmError,
                    dialogError = null,
                )
            }
            firstFailing(codeError, newPasswordError, confirmError)?.let {
                _focusRequest.value = it
                return
            }
            if (current.email.isBlank()) {
                _uiState.update { it.copy(dialogError = FieldError.Res(R.string.error_reset_missing_email)) }
                return
            }

            _uiState.update { it.copy(saveState = SaveState.Saving) }
            viewModelScope.launch {
                try {
                    apiCall {
                        api.resetPassword(
                            ResetPasswordRequest(
                                current.email,
                                current.code.trim(),
                                current.newPassword,
                                current.confirmPassword,
                            ),
                        )
                    }
                    _uiState.update { it.copy(saveState = SaveState.Saved) }
                    delay(Motion.SAVED_CONFIRMATION_HOLD_MS.toLong())
                    _passwordChanged.value = current.email
                } catch (e: ApiException) {
                    _uiState.update { it.copy(saveState = SaveState.Idle) }
                    handle(e)
                }
            }
        }

        private fun firstFailing(
            code: FieldError?,
            newPassword: FieldError?,
            confirm: FieldError?,
        ): ResetField? =
            when {
                code != null -> ResetField.Code
                newPassword != null -> ResetField.NewPassword
                confirm != null -> ResetField.Confirm
                else -> null
            }

        /** 400 and 422 are mapped by HTTP status like iOS, not by error code (RESEARCH Pitfall 10). */
        private fun handle(e: ApiException) {
            if (e is ApiException.Http && e.status == 400) {
                _uiState.update { it.copy(codeError = FieldError.Res(R.string.error_code_expired)) }
                _focusRequest.value = ResetField.Code
                return
            }
            if (e is ApiException.Http && e.status == 422 && mapFieldErrors(e)) return
            _uiState.update { it.copy(dialogError = e.toDialogError()) }
        }

        /** Returns true when at least one server field error was placed on a field or the dialog. */
        private fun mapFieldErrors(e: ApiException.Http): Boolean {
            var state = _uiState.value
            var handled = false
            // The last message for a field wins, like iOS.
            for (fieldError in e.fieldErrors) {
                val message = FieldError.Text(fieldError.message)
                when (fieldError.field) {
                    "password" -> state = state.copy(newPasswordError = message)
                    "password_confirmation" -> state = state.copy(confirmError = message)
                    "code" -> state = state.copy(codeError = message)
                    "email" -> state = state.copy(dialogError = message)
                    else -> continue
                }
                handled = true
            }
            if (!handled) return false
            _uiState.value = state
            firstFailing(state.codeError, state.newPasswordError, state.confirmError)?.let {
                _focusRequest.value = it
            }
            return true
        }

        private companion object {
            const val KEY_CODE = "reset_code"
        }
    }
