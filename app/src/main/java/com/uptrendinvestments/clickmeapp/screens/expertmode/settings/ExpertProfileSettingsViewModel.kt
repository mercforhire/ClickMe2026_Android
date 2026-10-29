package com.uptrendinvestments.clickmeapp.screens.expertmode.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.web.isAllowedMeetingUrl
import com.uptrendinvestments.clickmeapp.constants.Countries
import com.uptrendinvestments.clickmeapp.di.ApplicationScope
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.model.responses.ExpertProfileData
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.AutoSaveStatus
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.AutoSaver
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.AvatarUiState
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.HTTP_UNPROCESSABLE
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.ProfileAvatarController
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.ProfileLanguage
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.ProfileSettingsRepository
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.ProfileSnapshot
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.ProfileTextFieldId
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.buildProfilePatch
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.fieldMessages
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.toSaveDialogError
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.with
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

/**
 * [snapshot] is null until the profile has loaded. [meetingUrlText] is what the link field shows (possibly invalid,
 * and then never saved); [meetingUrlError] is the inline message under it.
 */
data class ExpertProfileSettingsUiState(
    val load: ExpertSettingsLoad = ExpertSettingsLoad.Loading,
    val snapshot: ExpertSettingsSnapshot? = null,
    val meetingUrlText: String = "",
    val meetingUrlError: FieldError? = null,
    val email: String = "",
    val status: AutoSaveStatus = AutoSaveStatus.Idle,
    val fieldErrors: Map<ProfileTextFieldId, FieldError> = emptyMap(),
    val dialog: FieldError? = null,
    val avatarUrl: String? = null,
    val avatar: AvatarUiState = AvatarUiState(),
)

private val EmptySnapshot =
    ExpertSettingsSnapshot(
        profile =
            ProfileSnapshot(
                firstName = "",
                lastName = "",
                bio = "",
                jobTitle = "",
                company = "",
                city = "",
                stateProvince = "",
                countryCode = "",
                languages = emptyList(),
            ),
        meetingUrl = "",
    )

/**
 * State holder for Expert Settings (iOS `ExpertProfileSettingsViewModel`). It loads `GET expert/profile`, then
 * auto-saves like the client screen: the shared fields go through `PATCH user/profile` and the Skype / Zoom link through
 * `PATCH expert/profile`, both from one [AutoSaver]. A blank or non-http(s) link is never sent, so a typo cannot wipe or
 * break the link booked clients join through.
 */
@HiltViewModel
class ExpertProfileSettingsViewModel
    @Inject
    constructor(
        private val repository: ExpertProfileSettingsRepository,
        private val profileRepository: ProfileSettingsRepository,
        private val avatarController: ProfileAvatarController,
        private val userManager: UserManager,
        @ApplicationScope appScope: CoroutineScope,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ExpertProfileSettingsUiState())
        val uiState: StateFlow<ExpertProfileSettingsUiState> = _uiState.asStateFlow()

        private val saver =
            AutoSaver(
                scope = viewModelScope,
                flushScope = appScope,
                baseline = EmptySnapshot,
                persist = { old, new -> persist(old, new) },
                onFailure = ::onSaveFailed,
            )

        /** The link the server has; an invalid or blank typed value falls back to it so it never triggers a save. */
        private var savedMeetingUrl = ""
        private var loadJob: Job? = null

        init {
            viewModelScope.launch {
                saver.status.collect { status -> _uiState.update { it.copy(status = status) } }
            }
            load()
        }

        private fun load() {
            _uiState.update { it.copy(load = ExpertSettingsLoad.Loading) }
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    try {
                        seed(repository.load())
                    } catch (e: ApiException) {
                        _uiState.update { it.copy(load = ExpertSettingsLoad.Failed(e.toLoadMessage())) }
                    }
                }
        }

        /** "Retry" on the load error. */
        fun retryLoad() = load()

        private fun seed(profile: ExpertProfileData) {
            val personal = profile.personalInfo
            val professional = profile.professionalDetails
            val location = profile.locationDetails
            val meetingUrl = professional?.meetingUrl.orEmpty().trim()
            val snapshot =
                ExpertSettingsSnapshot(
                    profile =
                        ProfileSnapshot(
                            firstName = personal?.firstName.orEmpty(),
                            lastName = personal?.lastName.orEmpty(),
                            bio = personal?.bio.orEmpty(),
                            jobTitle = professional?.jobTitle.orEmpty(),
                            company = professional?.company.orEmpty(),
                            city = location?.city.orEmpty(),
                            stateProvince = location?.stateProvince.orEmpty(),
                            countryCode = Countries.normalize(location?.countryCode.orEmpty()).orEmpty(),
                            languages =
                                profile.languages
                                    .orEmpty()
                                    .map { ProfileLanguage(it.id, it.label.orEmpty()) }
                                    .filter { it.label.isNotBlank() },
                        ),
                    meetingUrl = meetingUrl,
                )
            savedMeetingUrl = meetingUrl
            saver.rebase(snapshot)
            _uiState.update {
                it.copy(
                    load = ExpertSettingsLoad.Ready,
                    snapshot = snapshot,
                    meetingUrlText = meetingUrl,
                    meetingUrlError = null,
                    email = personal?.email.orEmpty(),
                    avatarUrl = profile.avatarUrl,
                )
            }
        }

        /** Applies [value] (cut to the field's maximum) and schedules the save. */
        fun onFieldChange(
            field: ProfileTextFieldId,
            value: String,
        ) {
            val current = _uiState.value.snapshot ?: return
            val next = current.copy(profile = current.profile.with(field, value.take(field.maxLength)))
            _uiState.update { it.copy(snapshot = next, fieldErrors = it.fieldErrors - field) }
            saver.onEdited(next)
        }

        /**
         * Stores the typed [value]. Only a non-blank value that passes [isAllowedMeetingUrl] enters the saver snapshot;
         * anything else keeps the last saved link there (and shows the inline error when non-blank), so it can neither
         * wipe the link nor store a `javascript:` URL that clients would open.
         */
        fun onMeetingUrlChange(value: String) {
            val current = _uiState.value.snapshot ?: return
            val trimmed = value.trim()
            val valid = trimmed.isNotEmpty() && isAllowedMeetingUrl(trimmed)
            val next = current.copy(meetingUrl = if (valid) trimmed else savedMeetingUrl)
            val error =
                if (trimmed.isNotEmpty() && !valid) FieldError.Res(R.string.expert_settings_meeting_invalid) else null
            _uiState.update { it.copy(snapshot = next, meetingUrlText = value, meetingUrlError = error) }
            saver.onEdited(next)
        }

        /** The status icon's tap: re-sends the newest snapshot. */
        fun retrySave() = saver.retry()

        fun dismissDialog() = _uiState.update { it.copy(dialog = null) }

        /** Saves a pending edit now, outside this ViewModel's scope. Called before the screen pops. */
        fun flushPendingSave() = saver.flush()

        override fun onCleared() {
            // Backstop for a gesture back that bypassed the screen's own flush.
            saver.flush()
            super.onCleared()
        }

        private suspend fun persist(
            old: ExpertSettingsSnapshot,
            new: ExpertSettingsSnapshot,
        ) {
            val patch = buildProfilePatch(old.profile, new.profile)
            if (patch != null) {
                profileRepository.updateProfile(patch)
                try {
                    userManager.refreshProfile()
                } catch (_: ApiException) {
                    // The save succeeded; the hub card catches up on the next profile refresh.
                }
            }
            val link = new.meetingUrl
            if (link != old.meetingUrl && link.isNotBlank() && isAllowedMeetingUrl(link)) {
                repository.updateMeetingUrl(link)
            }
            savedMeetingUrl = link
        }

        /** A 422 that names a field shows its message under that field; anything else opens the dialog. */
        private fun onSaveFailed(error: ApiException) {
            val http = (error as? ApiException.Http)?.takeIf { it.status == HTTP_UNPROCESSABLE }
            val perField = http?.fieldMessages().orEmpty()
            val link = http?.fieldErrors?.firstOrNull { it.field.substringAfterLast('.') == MEETING_URL_FIELD }
            if (perField.isNotEmpty() || link != null) {
                _uiState.update {
                    it.copy(
                        fieldErrors = it.fieldErrors + perField,
                        meetingUrlError = link?.let { dto -> FieldError.Text(dto.message) } ?: it.meetingUrlError,
                    )
                }
            } else {
                _uiState.update { it.copy(dialog = error.toSaveDialogError()) }
            }
        }
    }

private const val MEETING_URL_FIELD = "meeting_url"

/** The load error body: the server's own text, else "Check your connection and try again." */
private fun ApiException.toLoadMessage(): FieldError =
    toSaveDialogError() ?: FieldError.Res(R.string.login_error_network)
