package com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings

import androidx.lifecycle.ViewModel
import com.uptrendinvestments.clickmeapp.di.ApplicationScope
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** The editable text fields, each with the server maximum it is clamped to. */
enum class ProfileTextFieldId(
    val maxLength: Int,
) {
    FirstName(NAME_MAX),
    LastName(NAME_MAX),
    Bio(BIO_MAX),
    JobTitle(PROFESSIONAL_MAX),
    Company(PROFESSIONAL_MAX),
    City(LOCATION_MAX),
    StateProvince(LOCATION_MAX),
}

data class ClientProfileSettingsUiState(
    val snapshot: ProfileSnapshot? = null,
    val email: String = "",
    val status: AutoSaveStatus = AutoSaveStatus.Idle,
    val fieldErrors: Map<ProfileTextFieldId, FieldError> = emptyMap(),
    val dialog: FieldError? = null,
    val professionalSheetOpen: Boolean = false,
)

/** State holder for client Profile Settings (stub, implemented in the GREEN step). */
@HiltViewModel
class ClientProfileSettingsViewModel
    @Inject
    constructor(
        private val repository: ProfileSettingsRepository,
        private val userManager: UserManager,
        @ApplicationScope private val appScope: CoroutineScope,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ClientProfileSettingsUiState())
        val uiState: StateFlow<ClientProfileSettingsUiState> = _uiState.asStateFlow()

        fun onFieldChange(
            field: ProfileTextFieldId,
            value: String,
        ) {
            TODO("not implemented")
        }
    }
