package com.uptrendinvestments.clickmeapp.screens.settings.accountsecurity

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.PasswordStrength
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.managers.CallGuard
import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
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
 * Local validation in the iOS order (`AccountSecurityViewModel.validate`). Returns the first failing field and its
 * message resource, or null when the form may be sent.
 */
fun validateChangePassword(
    current: String,
    new: String,
    confirm: String,
): Pair<PasswordField, Int>? =
    when {
        current.isEmpty() -> PasswordField.Current to R.string.account_security_error_current_empty
        new.isEmpty() -> PasswordField.New to R.string.account_security_error_new_empty
        new.length < MIN_PASSWORD_LENGTH -> PasswordField.New to R.string.account_security_error_new_short
        !DIGIT.containsMatchIn(new) -> PasswordField.New to R.string.account_security_error_new_no_number
        new == current -> PasswordField.New to R.string.account_security_error_new_same
        confirm.isEmpty() -> PasswordField.Confirm to R.string.account_security_error_confirm_empty
        confirm != new -> PasswordField.Confirm to R.string.account_security_error_confirm_mismatch
        else -> null
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

            val failure = validateChangePassword(form.current, form.new, form.confirm)
            if (failure != null) {
                _uiState.update { it.copy(fieldErrors = mapOf(failure.first to FieldError.Res(failure.second))) }
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

        /** Any failure shows the generic failure dialog; server field mapping follows in the next step. */
        private fun handle(e: ApiException) {
            _uiState.update {
                it.copy(
                    dialog =
                        AccountSecurityDialog.Error(
                            R.string.account_security_failed_title,
                            FieldError.Res(R.string.account_security_failed_body),
                        ),
                )
            }
        }
    }
