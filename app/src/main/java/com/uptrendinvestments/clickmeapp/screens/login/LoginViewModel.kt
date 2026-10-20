package com.uptrendinvestments.clickmeapp.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

/** The one inline error line of the temporary login screen. */
sealed interface LoginError {
    /** The backend's own message (wrong credentials, field error). Shown verbatim. */
    data class Server(
        val message: String,
    ) : LoginError

    data object MissingFields : LoginError

    data object Network : LoginError

    data object Unknown : LoginError
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val error: LoginError? = null,
    val isSubmitting: Boolean = false,
)

/**
 * Temporary login (D-07). Phase 3 (AUTH-01) adds iOS email validation, forgot-password and the signup entry.
 * Routing after login is [UserManager.resolvePostAuthDestination], the iOS `LoginView` routing.
 */
@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val userManager: UserManager,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(LoginUiState())
        val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

        private val _destination = MutableStateFlow<LaunchDestination?>(null)
        val destination: StateFlow<LaunchDestination?> = _destination.asStateFlow()

        fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }

        fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }

        fun onDestinationConsumed() {
            _destination.value = null
        }

        fun submit() {
            val current = _uiState.value
            if (current.isSubmitting) return
            // iOS trims the email: a trailing space otherwise trips the backend's strict validator (422).
            val email = current.email.trim()
            if (email.isEmpty() || current.password.isEmpty()) {
                _uiState.update { it.copy(error = LoginError.MissingFields) }
                return
            }
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            viewModelScope.launch {
                try {
                    val user = userManager.login(email, current.password)
                    _destination.value = userManager.resolvePostAuthDestination(user.userRoles)
                } catch (e: ApiException) {
                    _uiState.update { it.copy(error = map(e)) }
                } finally {
                    _uiState.update { it.copy(isSubmitting = false) }
                }
            }
        }

        /** Null means "show nothing": a suspension is handled by the global dialog (D-08). */
        private fun map(e: ApiException): LoginError? =
            when (e) {
                is ApiException.Http -> {
                    val message = e.userMessage
                    when {
                        e.code == ErrorCode.AccountSuspended -> null
                        message != null -> LoginError.Server(message)
                        else -> LoginError.Unknown
                    }
                }
                is ApiException.Network -> LoginError.Network
                is ApiException.Decoding -> LoginError.Unknown
            }
    }
