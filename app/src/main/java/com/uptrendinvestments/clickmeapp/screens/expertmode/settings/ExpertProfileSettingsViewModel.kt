package com.uptrendinvestments.clickmeapp.screens.expertmode.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.web.isAllowedMeetingUrl
import com.uptrendinvestments.clickmeapp.constants.Countries
import com.uptrendinvestments.clickmeapp.constants.Country
import com.uptrendinvestments.clickmeapp.di.ApplicationScope
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.model.responses.ExpertProfileData
import com.uptrendinvestments.clickmeapp.model.responses.LanguageItem
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
import com.uptrendinvestments.clickmeapp.screens.signup.basicinfo.LanguageCatalog
import com.uptrendinvestments.clickmeapp.screens.signup.basicinfo.LanguageSheetState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
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
    val professionalSheetOpen: Boolean = false,
    val languageSheet: LanguageSheetState? = null,
    val countrySheetOpen: Boolean = false,
    val countryQuery: String = "",
) {
    /** The alpha-3 code of the chosen country, blank when none. */
    val selectedCountryCode: String
        get() = snapshot?.profile?.countryCode.orEmpty()

    /** The static country list narrowed by [countryQuery], ignoring case. */
    val visibleCountries: List<Country>
        get() = Countries.all.filter { it.name.contains(countryQuery.trim(), ignoreCase = true) }
}

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

        /** The language catalogue, fetched once per ViewModel. */
        private var catalog: LanguageCatalog = LanguageCatalog.Loading
        private var catalogJob: Job? = null

        init {
            viewModelScope.launch {
                saver.status.collect { status -> _uiState.update { it.copy(status = status) } }
            }
            viewModelScope.launch {
                avatarController.state.collect { avatar -> _uiState.update { it.copy(avatar = avatar) } }
            }
            viewModelScope.launch {
                // After an upload the controller refreshes the cached profile; the first value is the one already
                // cached, which the expert profile load supersedes.
                userManager.profile.drop(1).collect { profile ->
                    profile?.personalDetails?.avatarUrl?.let { url -> _uiState.update { it.copy(avatarUrl = url) } }
                }
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

        // ---- avatar ----

        /** The system photo picker returned [uri]; the controller imports it and asks for the crop screen. */
        fun onPhotoPicked(uri: Uri) = avatarController.onPicked(uri, viewModelScope)

        /** The screen pushed the crop route. */
        fun avatarCropOpened() = avatarController.onCropOpened()

        fun dismissAvatarError() = avatarController.dismissError()

        /** Called on every resume: uploads a confirmed crop (or cleans up after a cancelled one). */
        fun onScreenShown() = avatarController.onReturnedFromCrop()

        // ---- country ----

        fun openCountry() = _uiState.update { it.copy(countrySheetOpen = true, countryQuery = "") }

        fun onCountryQuery(value: String) = _uiState.update { it.copy(countryQuery = value) }

        /** Applies [code] (normalized to alpha-3), closes the sheet and schedules the save. */
        fun selectCountry(code: String) {
            val current = _uiState.value.snapshot ?: return
            val next = current.copy(profile = current.profile.copy(countryCode = Countries.normalize(code).orEmpty()))
            _uiState.update { it.copy(snapshot = next, countrySheetOpen = false, countryQuery = "") }
            saver.onEdited(next)
        }

        fun dismissCountry() = _uiState.update { it.copy(countrySheetOpen = false, countryQuery = "") }

        // ---- spoken languages ----

        /** Opens the language sheet with the catalogue entries the expert has as the working selection. */
        fun openLanguages() {
            val profile = _uiState.value.snapshot?.profile ?: return
            val selection =
                profile.languages.mapNotNull { language ->
                    language.id?.takeIf { it.isNotBlank() }?.let { LanguageItem(it, language.label) }
                }
            _uiState.update { it.copy(languageSheet = LanguageSheetState(selection = selection, catalog = catalog)) }
            // Reopening after a failure retries; a loaded catalogue is never fetched again.
            if (catalog !is LanguageCatalog.Loaded && catalogJob?.isActive != true) loadCatalog()
        }

        fun onLanguageQuery(value: String) =
            _uiState.update { state -> state.copy(languageSheet = state.languageSheet?.copy(query = value)) }

        /** Adds [item] to the working selection, or removes it when already selected (tap order is kept). */
        fun toggleLanguage(item: LanguageItem) =
            _uiState.update { state ->
                val sheet = state.languageSheet ?: return@update state
                val selection =
                    if (sheet.selection.any { it.id == item.id }) {
                        sheet.selection.filterNot { it.id == item.id }
                    } else {
                        sheet.selection + item
                    }
                state.copy(languageSheet = sheet.copy(selection = selection))
            }

        /**
         * "Done": the selection (in tap order) followed by the custom languages the catalogue cannot express, which
         * are kept as they are. Closes the sheet and schedules the save.
         */
        fun applyLanguages() {
            val sheet = _uiState.value.languageSheet ?: return
            val current = _uiState.value.snapshot ?: return
            val customs = current.profile.languages.filter { it.id.isNullOrBlank() }
            val languages = sheet.selection.map { ProfileLanguage(it.id, it.label) } + customs
            val next = current.copy(profile = current.profile.copy(languages = languages))
            _uiState.update { it.copy(snapshot = next, languageSheet = null) }
            saver.onEdited(next)
        }

        /** Cancel (or dismissing the sheet): nothing is applied or saved. */
        fun dismissLanguages() = _uiState.update { it.copy(languageSheet = null) }

        fun retryLanguages() {
            if (catalogJob?.isActive == true) return
            loadCatalog()
        }

        private fun loadCatalog() {
            setCatalog(LanguageCatalog.Loading)
            catalogJob =
                viewModelScope.launch {
                    try {
                        setCatalog(LanguageCatalog.Loaded(profileRepository.languages()))
                    } catch (_: ApiException) {
                        setCatalog(LanguageCatalog.Failed)
                    }
                }
        }

        private fun setCatalog(value: LanguageCatalog) {
            catalog = value
            _uiState.update { state -> state.copy(languageSheet = state.languageSheet?.copy(catalog = value)) }
        }

        // ---- professional details ----

        fun openProfessionalSheet() = _uiState.update { it.copy(professionalSheetOpen = true) }

        /** "Update details": applies the sheet's working copy, closes the sheet and schedules the save. */
        fun applyProfessional(
            jobTitle: String,
            company: String,
        ) {
            val current = _uiState.value.snapshot ?: return
            val profile =
                current.profile.copy(
                    jobTitle = jobTitle.take(ProfileTextFieldId.JobTitle.maxLength),
                    company = company.take(ProfileTextFieldId.Company.maxLength),
                )
            val next = current.copy(profile = profile)
            _uiState.update {
                it.copy(
                    snapshot = next,
                    professionalSheetOpen = false,
                    fieldErrors = it.fieldErrors - ProfileTextFieldId.JobTitle - ProfileTextFieldId.Company,
                )
            }
            saver.onEdited(next)
        }

        /** "Discard changes" (or dismissing the sheet): closes it; nothing is applied or saved. */
        fun discardProfessional() = _uiState.update { it.copy(professionalSheetOpen = false) }

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
