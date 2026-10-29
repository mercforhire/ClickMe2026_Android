package com.uptrendinvestments.clickmeapp.screens.settings.accountsecurity

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.PasswordStrength
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.managers.CallGuard
import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The three password inputs of the change-password form. */
enum class PasswordField { Current, New, Confirm }

/** What the Account Security dialog shows: the success confirmation or an error with its own title. */
sealed interface AccountSecurityDialog {
    data object Success : AccountSecurityDialog

    data class Error(
        @StringRes val title: Int,
        val message: FieldError,
    ) : AccountSecurityDialog
}

/**
 * Account Security form state. The three passwords live here, in memory only: no saved state, never printed
 * ([toString] redacts them), wiped after a successful change (T-14-10).
 */
data class AccountSecurityUiState(
    val current: String = "",
    val new: String = "",
    val confirm: String = "",
    val fieldErrors: Map<PasswordField, FieldError> = emptyMap(),
    val saveState: SaveState = SaveState.Idle,
    val dialog: AccountSecurityDialog? = null,
) {
    val strength: PasswordStrength get() = PasswordStrength.evaluate(new)

    override fun toString(): String =
        "AccountSecurityUiState(passwords=<redacted>, fieldErrors=${fieldErrors.keys}, " +
            "saveState=$saveState, dialog=$dialog)"
}

private const val MIN_PASSWORD_LENGTH = 8
private val DIGIT = Regex("[0-9]")

/**
 * Local validation like iOS `AccountSecurityViewModel.validate`: three independent checks, so every failing field is
 * reported in one pass (current empty; new empty, short, no number or same as current; confirm empty or different).
 * Returns the message resource per failing field, empty when the form may be sent.
 */
fun validateChangePassword(
    current: String,
    new: String,
    confirm: String,
): Map<PasswordField, Int> {
    val errors = LinkedHashMap<PasswordField, Int>()
    if (current.isEmpty()) errors[PasswordField.Current] = R.string.account_security_error_current_empty
    val newError =
        when {
            new.isEmpty() -> R.string.account_security_error_new_empty
            new.length < MIN_PASSWORD_LENGTH -> R.string.account_security_error_new_short
            !DIGIT.containsMatchIn(new) -> R.string.account_security_error_new_no_number
            new == current -> R.string.account_security_error_new_same
            else -> null
        }
    if (newError != null) errors[PasswordField.New] = newError
    val confirmError =
        when {
            confirm.isEmpty() -> R.string.account_security_error_confirm_empty
            confirm != new -> R.string.account_security_error_confirm_mismatch
            else -> null
        }
    if (confirmError != null) errors[PasswordField.Confirm] = confirmError
    return errors
}

/**
 * Port of iOS `AccountSecurityViewModel`: validate on tap, send `PATCH user/password`, wipe the form on success.
 * Both password actions go through the call guard (Phase 9 D-13). Takes no saved-state handle on purpose.
 */
@HiltViewModel
class AccountSecurityViewModel
    @Inject
    constructor(
        private val repository: AccountSecurityRepository,
        private val callGuard: CallGuard,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AccountSecurityUiState())
        val uiState: StateFlow<AccountSecurityUiState> = _uiState.asStateFlow()

        fun onCurrentChange(value: String) = edit(PasswordField.Current) { it.copy(current = value) }

        fun onNewChange(value: String) = edit(PasswordField.New) { it.copy(new = value) }

        fun onConfirmChange(value: String) = edit(PasswordField.Confirm) { it.copy(confirm = value) }

        private fun edit(
            field: PasswordField,
            change: (AccountSecurityUiState) -> AccountSecurityUiState,
        ) {
            _uiState.update { change(it).let { next -> next.copy(fieldErrors = next.fieldErrors - field) } }
        }

        fun dismissDialog() {
            _uiState.update { it.copy(dialog = null) }
        }

        /** True when the deletion flow may open; false during a live call (the guard queues its dialog). */
        fun requestDeleteAccount(): Boolean = callGuard.attempt(RestrictedAction.DeleteAccount)

        fun submit() {
            val form = _uiState.value
            if (form.saveState != SaveState.Idle) return

            val failures = validateChangePassword(form.current, form.new, form.confirm)
            if (failures.isNotEmpty()) {
                _uiState.update { state -> state.copy(fieldErrors = failures.mapValues { FieldError.Res(it.value) }) }
                return
            }
            if (!callGuard.attempt(RestrictedAction.ChangePassword)) return

            _uiState.update { it.copy(saveState = SaveState.Saving, fieldErrors = emptyMap(), dialog = null) }
            viewModelScope.launch {
                try {
                    repository.updatePassword(form.current, form.new)
                    _uiState.update {
                        it.copy(
                            current = "",
                            new = "",
                            confirm = "",
                            fieldErrors = emptyMap(),
                            saveState = SaveState.Idle,
                            dialog = AccountSecurityDialog.Success,
                        )
                    }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(saveState = SaveState.Idle) }
                    handle(e)
                }
            }
        }

        /**
         * Maps by HTTP status like iOS `handle(submitError:)`: 403 is a wrong current password, 422 names fields,
         * 429 is the rate limit; everything else is the generic failure dialog. A suspended account is owned by the
         * global suspended-account dialog, so nothing is shown here (Phase 2 D-08).
         */
        private fun handle(e: ApiException) {
            if (e is ApiException.Http) {
                if (e.code == ErrorCode.AccountSuspended) return
                when (e.status) {
                    HTTP_FORBIDDEN -> {
                        val message =
                            e.userMessage?.let { FieldError.Text(it) }
                                ?: FieldError.Res(R.string.account_security_wrong_current)
                        setFieldError(PasswordField.Current, message)
                        return
                    }
                    HTTP_UNPROCESSABLE -> if (mapFieldErrors(e)) return
                    HTTP_TOO_MANY_REQUESTS -> {
                        showFailure(FieldError.Res(R.string.account_security_too_many))
                        return
                    }
                }
            }
            // The server's own text when it sent one (iOS `decodeStandardMessage`), otherwise the generic body.
            val serverMessage = (e as? ApiException.Http)?.userMessage
            showFailure(
                serverMessage?.let { FieldError.Text(it) } ?: FieldError.Res(R.string.account_security_failed_body),
            )
        }

        private fun setFieldError(
            field: PasswordField,
            error: FieldError,
        ) {
            _uiState.update { it.copy(fieldErrors = it.fieldErrors + (field to error)) }
        }

        private fun showFailure(message: FieldError) {
            _uiState.update {
                it.copy(dialog = AccountSecurityDialog.Error(R.string.account_security_failed_title, message))
            }
        }

        /**
         * Places `current_password` and `new_password` errors on their fields and opens the dialog with the last
         * message for any other field, like iOS (`apiError`). False when the envelope held no entries.
         */
        private fun mapFieldErrors(e: ApiException.Http): Boolean {
            if (e.fieldErrors.isEmpty()) return false
            var errors = emptyMap<PasswordField, FieldError>()
            var unmatched: String? = null
            // The last message for a field wins, like iOS.
            for (fieldError in e.fieldErrors) {
                when (fieldError.field) {
                    "current_password" ->
                        errors =
                            errors + (PasswordField.Current to FieldError.Text(fieldError.message))
                    "new_password" -> errors = errors + (PasswordField.New to FieldError.Text(fieldError.message))
                    else -> unmatched = fieldError.message
                }
            }
            _uiState.update { it.copy(fieldErrors = errors) }
            unmatched?.let { showFailure(FieldError.Text(it)) }
            return true
        }

        private companion object {
            const val HTTP_FORBIDDEN = 403
            const val HTTP_UNPROCESSABLE = 422
            const val HTTP_TOO_MANY_REQUESTS = 429
        }
    }
