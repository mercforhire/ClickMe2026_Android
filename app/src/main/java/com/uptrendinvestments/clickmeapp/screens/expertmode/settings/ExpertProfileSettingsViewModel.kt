package com.uptrendinvestments.clickmeapp.screens.expertmode.settings

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.web.isAllowedMeetingUrl
import com.uptrendinvestments.clickmeapp.constants.Countries
import com.uptrendinvestments.clickmeapp.constants.Country
import com.uptrendinvestments.clickmeapp.di.ApplicationScope
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.model.responses.ExpertProfileData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagItem
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
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.requiredNameErrors
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.toProfileSaveDialogError
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.toSaveDialogError
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.with
import com.uptrendinvestments.clickmeapp.screens.signup.basicinfo.LanguageCatalog
import com.uptrendinvestments.clickmeapp.screens.signup.basicinfo.LanguageSheetState
import com.uptrendinvestments.clickmeapp.util.clampLength
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
    /** The profile's expertise tag labels, shown as chips; the Edit Expertise sheet saves them itself. */
    val expertiseTags: List<String> = emptyList(),
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

private const val KEY_FIRST_NAME = "expert_settings_first_name"
private const val KEY_LAST_NAME = "expert_settings_last_name"
private const val KEY_BIO = "expert_settings_bio"
private const val KEY_JOB_TITLE = "expert_settings_job_title"
private const val KEY_COMPANY = "expert_settings_company"
private const val KEY_CITY = "expert_settings_city"
private const val KEY_STATE_PROVINCE = "expert_settings_state_province"
private const val KEY_COUNTRY = "expert_settings_country"
private const val KEY_MEETING_URL = "expert_settings_meeting_url"
private const val KEY_LANGUAGE_IDS = "expert_settings_language_ids"
private const val KEY_LANGUAGE_LABELS = "expert_settings_language_labels"
private const val KEY_TOUCHED = "expert_settings_touched"

private fun ProfileTextFieldId.savedStateKey(): String =
    when (this) {
        ProfileTextFieldId.FirstName -> KEY_FIRST_NAME
        ProfileTextFieldId.LastName -> KEY_LAST_NAME
        ProfileTextFieldId.Bio -> KEY_BIO
        ProfileTextFieldId.JobTitle -> KEY_JOB_TITLE
        ProfileTextFieldId.Company -> KEY_COMPANY
        ProfileTextFieldId.City -> KEY_CITY
        ProfileTextFieldId.StateProvince -> KEY_STATE_PROVINCE
    }

/**
 * State holder for Expert Settings (iOS `ExpertProfileSettingsViewModel`). It loads `GET expert/profile`, then
 * auto-saves like the client screen: the shared fields go through `PATCH user/profile` and the Skype / Zoom link through
 * `PATCH expert/profile`, both from one [AutoSaver]. A non-blank link is sent only when it passes
 * [isAllowedMeetingUrl], so a typo cannot break the link booked clients join through. A blank field over a saved link
 * is a deliberate clear and sends `""`; an expert with no saved link never sends the key (iOS CR-01).
 *
 * Process death (CONTEXT D-12): edited fields, the country, the languages (two parallel `ArrayList<String>`: ids with
 * "" for a custom language, and labels) and the typed meeting link are mirrored to [SavedStateHandle] with a touched
 * flag. [seed] lays them over the fetched profile, so a restored edit wins over the late `GET expert/profile` (RESEARCH
 * Pitfall 7), and a restored value that differs from the server is handed to the auto-saver like a keystroke. The link
 * is restored as typed text but re-validated: a restored blank or invalid link never reaches the saver snapshot.
 * Expertise tags come from the server and are not mirrored. Only Strings and string lists are stored (Pitfall 6).
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
        private val savedStateHandle: SavedStateHandle,
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
                sendsNothing = ::sendsNothing,
            )

        /** The link the server has; an invalid or blank typed value falls back to it so it never triggers a save. */
        @Volatile
        private var savedMeetingUrl = ""

        /** The names the server holds, trimmed: an emptied name is held (never sent) while the server has one. */
        @Volatile
        private var serverFirstName = ""

        @Volatile
        private var serverLastName = ""

        /** A link the server answered 422 for; it is not sent again until the user changes the link text. */
        @Volatile
        private var rejectedMeetingUrl: String? = null
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
            serverFirstName = snapshot.profile.firstName.trim()
            serverLastName = snapshot.profile.lastName.trim()
            saver.rebase(snapshot)
            val restored = restoredOver(snapshot)
            val restoredText = restoredMeetingUrlText() ?: meetingUrl
            _uiState.update {
                it.copy(
                    load = ExpertSettingsLoad.Ready,
                    snapshot = restored,
                    fieldErrors = restored.profile.requiredNameErrors(serverFirstName, serverLastName),
                    meetingUrlText = restoredText,
                    meetingUrlError = meetingUrlErrorFor(restoredText),
                    email = personal?.email.orEmpty(),
                    avatarUrl = profile.avatarUrl,
                    expertiseTags = profile.expertiseTags.orEmpty().map { tag -> tag.label },
                )
            }
            if (restored != snapshot) saver.onEdited(restored)
        }

        private fun touched() = savedStateHandle.get<Boolean>(KEY_TOUCHED) == true

        /** The meeting link text the user typed before recreation, or null when there is none to restore. */
        private fun restoredMeetingUrlText(): String? =
            if (touched()) savedStateHandle.get<String>(KEY_MEETING_URL) else null

        /** [server] with every field the user edited before recreation put back (values cut to their maximum). */
        private fun restoredOver(server: ExpertSettingsSnapshot): ExpertSettingsSnapshot {
            if (!touched()) return server
            var profile = server.profile
            for (field in ProfileTextFieldId.entries) {
                val saved = savedStateHandle.get<String>(field.savedStateKey()) ?: continue
                profile = profile.with(field, saved.clampLength(field.maxLength))
            }
            savedStateHandle.get<String>(KEY_COUNTRY)?.let {
                profile = profile.copy(countryCode = Countries.normalize(it).orEmpty())
            }
            val ids = savedStateHandle.get<ArrayList<String>>(KEY_LANGUAGE_IDS)
            val labels = savedStateHandle.get<ArrayList<String>>(KEY_LANGUAGE_LABELS)
            if (ids != null && labels != null) {
                profile =
                    profile.copy(
                        languages =
                            ids.mapIndexed { i, id ->
                                ProfileLanguage(id.ifBlank { null }, labels.getOrNull(i).orEmpty())
                            },
                    )
            }
            // Blank text restored over a saved link is a clear (iOS CR-01); an invalid link keeps the server's, so a
            // bad URL is never stored. With no restored text there is nothing to put back.
            val link = restoredMeetingUrlText()?.trim()
            val meetingUrl =
                when {
                    link == null -> server.meetingUrl
                    link.isEmpty() -> ""
                    isAllowedMeetingUrl(link) -> link
                    else -> server.meetingUrl
                }
            return ExpertSettingsSnapshot(profile = profile, meetingUrl = meetingUrl)
        }

        private fun meetingUrlErrorFor(text: String): FieldError? =
            if (text.isNotBlank() && !isAllowedMeetingUrl(text.trim())) {
                FieldError.Res(R.string.expert_settings_meeting_invalid)
            } else {
                null
            }

        private fun mirrorField(
            field: ProfileTextFieldId,
            value: String,
        ) {
            savedStateHandle[field.savedStateKey()] = value
            savedStateHandle[KEY_TOUCHED] = true
        }

        /** Applies [value] (cut to the field's maximum) and schedules the save. */
        fun onFieldChange(
            field: ProfileTextFieldId,
            value: String,
        ) {
            val current = _uiState.value.snapshot ?: return
            val clamped = value.clampLength(field.maxLength)
            val next = current.copy(profile = current.profile.with(field, clamped))
            mirrorField(field, clamped)
            val required = next.profile.requiredNameErrors(serverFirstName, serverLastName).filterKeys { it == field }
            _uiState.update { it.copy(snapshot = next, fieldErrors = it.fieldErrors - field + required) }
            saver.onEdited(next)
        }

        /**
         * Stores the typed [value]. Blank text enters the saver snapshot as "" (clearing a saved link, like iOS); a
         * non-blank value enters it only when it passes [isAllowedMeetingUrl]. Any other value keeps the last saved
         * link there (and shows the inline error), so a `javascript:` URL that clients would open is never stored.
         */
        fun onMeetingUrlChange(value: String) {
            val current = _uiState.value.snapshot ?: return
            val trimmed = value.trim()
            if (trimmed != rejectedMeetingUrl) rejectedMeetingUrl = null
            val next =
                current.copy(
                    meetingUrl =
                        when {
                            trimmed.isEmpty() -> ""
                            isAllowedMeetingUrl(trimmed) -> trimmed
                            else -> savedMeetingUrl
                        },
                )
            val error = meetingUrlErrorFor(value)
            savedStateHandle[KEY_MEETING_URL] = value
            savedStateHandle[KEY_TOUCHED] = true
            _uiState.update { it.copy(snapshot = next, meetingUrlText = value, meetingUrlError = error) }
            saver.onEdited(next)
        }

        /** The Edit Expertise sheet saved [tags] on its own; only the chips change, nothing is auto-saved. */
        fun onExpertiseSaved(tags: List<ExpertiseTagItem>) =
            _uiState.update { it.copy(expertiseTags = tags.map { tag -> tag.label }) }

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
            savedStateHandle[KEY_COUNTRY] = next.profile.countryCode
            savedStateHandle[KEY_TOUCHED] = true
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
            savedStateHandle[KEY_LANGUAGE_IDS] = ArrayList(languages.map { it.id.orEmpty() })
            savedStateHandle[KEY_LANGUAGE_LABELS] = ArrayList(languages.map { it.label })
            savedStateHandle[KEY_TOUCHED] = true
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
                    jobTitle = jobTitle.clampLength(ProfileTextFieldId.JobTitle.maxLength),
                    company = company.clampLength(ProfileTextFieldId.Company.maxLength),
                )
            val next = current.copy(profile = profile)
            mirrorField(ProfileTextFieldId.JobTitle, profile.jobTitle)
            mirrorField(ProfileTextFieldId.Company, profile.company)
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
                patch.basic?.firstName?.let { serverFirstName = it }
                patch.basic?.lastName?.let { serverLastName = it }
                try {
                    userManager.refreshProfile()
                } catch (_: ApiException) {
                    // The save succeeded; the hub card catches up on the next profile refresh.
                }
            }
            val link = new.meetingUrl
            // Compared with what the server has, not with the baseline: a rejected link stays in the snapshot but is
            // never re-sent (it would fail every later auto-save), and a link that matches the server needs no call.
            // A blank link differing from the server's means the expert cleared it, so "" is sent (the backend stores
            // NULL, BACK-04); an expert who never had a link never sends the key. Non-blank links keep the guard.
            if (linkWillSend(new)) {
                try {
                    repository.updateMeetingUrl(link)
                    savedMeetingUrl = link
                } catch (e: ApiException.Http) {
                    val rejection = e.takeIf { it.status == HTTP_UNPROCESSABLE }?.meetingUrlRejection() ?: throw e
                    // The other fields did save, so this is not a failed save: the message sits under the link field.
                    rejectedMeetingUrl = link
                    _uiState.update { it.copy(meetingUrlError = FieldError.Text(rejection)) }
                }
            }
        }

        /**
         * Whether saving [snapshot] sends the meeting link: it differs from what the server has and was not rejected, and
         * is either blank (a clear) or a valid link.
         */
        private fun linkWillSend(snapshot: ExpertSettingsSnapshot): Boolean {
            val link = snapshot.meetingUrl
            if (link == savedMeetingUrl || link == rejectedMeetingUrl) return false
            return link.isEmpty() || isAllowedMeetingUrl(link)
        }

        /** True when saving [new] over [old] would make no request, so the saver adopts it without a Saving flash. */
        private fun sendsNothing(
            old: ExpertSettingsSnapshot,
            new: ExpertSettingsSnapshot,
        ): Boolean = buildProfilePatch(old.profile, new.profile) == null && !linkWillSend(new)

        /**
         * A 422 that names `meeting_url` shows its message under the link field. Any other `422 VALIDATION_ERROR` opens
         * the dialog with the field names to check ([ProfileSaveErrorMapper]), never the server's validator text. Other
         * failures keep the server's own message or the connection text.
         */
        private fun onSaveFailed(error: ApiException) {
            val http = (error as? ApiException.Http)?.takeIf { it.status == HTTP_UNPROCESSABLE }
            val link = http?.fieldErrors?.firstOrNull { it.field.substringAfterLast('.') == MEETING_URL_FIELD }
            if (link != null) {
                _uiState.update { it.copy(meetingUrlError = FieldError.Text(link.message)) }
                return
            }
            _uiState.update { it.copy(dialog = error.toProfileSaveDialogError()) }
        }
    }

private const val MEETING_URL_FIELD = "meeting_url"

/** The server's message for the `meeting_url` field of a 422, or null when the response names no such field. */
private fun ApiException.Http.meetingUrlRejection(): String? =
    fieldErrors.lastOrNull { it.field.substringAfterLast('.') == MEETING_URL_FIELD }?.message

/** The load error body: the server's own text, else "Check your connection and try again." */
private fun ApiException.toLoadMessage(): FieldError =
    toSaveDialogError() ?: FieldError.Res(R.string.login_error_network)
