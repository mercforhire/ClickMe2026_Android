package com.uptrendinvestments.clickmeapp.screens.expertmode.settings

import androidx.lifecycle.ViewModel
import com.uptrendinvestments.clickmeapp.components.web.isAllowedMeetingUrl
import com.uptrendinvestments.clickmeapp.di.ApplicationScope
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.AutoSaveStatus
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.AvatarUiState
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.ProfileAvatarController
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.ProfileSettingsRepository
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.ProfileSnapshot
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.ProfileTextFieldId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** Everything the auto-saver compares: the shared profile fields plus the (valid) meeting link. */
data class ExpertSettingsSnapshot(
    val profile: ProfileSnapshot,
    val meetingUrl: String,
)

/** Where the initial `GET expert/profile` stands. */
sealed interface ExpertSettingsLoad {
    data object Loading : ExpertSettingsLoad

    data object Ready : ExpertSettingsLoad

    data class Failed(
        val message: FieldError,
    ) : ExpertSettingsLoad
}

data class ExpertProfileSettingsUiState(
    val load: ExpertSettingsLoad = ExpertSettingsLoad.Loading,
    val snapshot: ExpertSettingsSnapshot? = null,
    /** What the link field shows: the typed text, which may be invalid (and then is never saved). */
    val meetingUrlText: String = "",
    val meetingUrlError: FieldError? = null,
    val email: String = "",
    val status: AutoSaveStatus = AutoSaveStatus.Idle,
    val fieldErrors: Map<ProfileTextFieldId, FieldError> = emptyMap(),
    val dialog: FieldError? = null,
    val avatarUrl: String? = null,
    val avatar: AvatarUiState = AvatarUiState(),
)

@HiltViewModel
class ExpertProfileSettingsViewModel
    @Inject
    constructor(
        private val repository: ExpertProfileSettingsRepository,
        private val profileRepository: ProfileSettingsRepository,
        private val avatar: ProfileAvatarController,
        private val userManager: UserManager,
        @ApplicationScope private val appScope: CoroutineScope,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ExpertProfileSettingsUiState())
        val uiState: StateFlow<ExpertProfileSettingsUiState> = _uiState.asStateFlow()

        fun onFieldChange(
            field: ProfileTextFieldId,
            value: String,
        ) = Unit

        fun onMeetingUrlChange(value: String) {
            isAllowedMeetingUrl(value)
        }

        fun retryLoad() = Unit

        fun retrySave() = Unit

        fun dismissDialog() = Unit

        fun flushPendingSave() = Unit
    }
