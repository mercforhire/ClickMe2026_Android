package com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.constants.Countries
import com.uptrendinvestments.clickmeapp.constants.Country
import com.uptrendinvestments.clickmeapp.di.ApplicationScope
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.model.responses.LanguageItem
import com.uptrendinvestments.clickmeapp.model.responses.UserProfileData
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.login.toDialogError
import com.uptrendinvestments.clickmeapp.screens.signup.basicinfo.LanguageCatalog
import com.uptrendinvestments.clickmeapp.screens.signup.basicinfo.LanguageSheetState
import com.uptrendinvestments.clickmeapp.util.clampLength
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

/**
 * [snapshot] is null until the profile has been read (the screen shows a spinner, or a retry state when [loadFailed]).
 * [dialog] is the save-failure message, [fieldErrors] the "Please enter your first/last name." messages under the name
 * fields.
 */
data class ClientProfileSettingsUiState(
    val snapshot: ProfileSnapshot? = null,
    val loadFailed: Boolean = false,
    val email: String = "",
    val status: AutoSaveStatus = AutoSaveStatus.Idle,
    val fieldErrors: Map<ProfileTextFieldId, FieldError> = emptyMap(),
    val dialog: FieldError? = null,
    val professionalSheetOpen: Boolean = false,
    val avatarUrl: String? = null,
    val avatar: AvatarUiState = AvatarUiState(),
    val languageSheet: LanguageSheetState? = null,
    val countrySheetOpen: Boolean = false,
    val countryQuery: String = "",
) {
    /** The static country list narrowed by [countryQuery], ignoring case. */
    val visibleCountries: List<Country>
        get() = Countries.all.filter { it.name.contains(countryQuery.trim(), ignoreCase = true) }
}

private val EmptySnapshot =
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
    )

private const val KEY_FIRST_NAME = "client_settings_first_name"
private const val KEY_LAST_NAME = "client_settings_last_name"
private const val KEY_BIO = "client_settings_bio"
private const val KEY_JOB_TITLE = "client_settings_job_title"
private const val KEY_COMPANY = "client_settings_company"
private const val KEY_CITY = "client_settings_city"
private const val KEY_STATE_PROVINCE = "client_settings_state_province"
private const val KEY_COUNTRY = "client_settings_country"
private const val KEY_LANGUAGE_IDS = "client_settings_language_ids"
private const val KEY_LANGUAGE_LABELS = "client_settings_language_labels"
private const val KEY_TOUCHED = "client_settings_touched"

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
 * State holder for client Profile Settings: iOS-style auto-save (no Save button, no discard dialog). Every change is
 * handed to an [AutoSaver] that PATCHes the changed groups 1.2 s after the last edit. Leaving the screen flushes a
 * pending edit on the application scope so it is neither lost nor cancelled with this ViewModel.
 *
 * The screen renders from the cached profile, so there is no loading state in the normal case; if the cache is empty
 * the profile is refreshed once.
 *
 * Process death (CONTEXT D-12): every edit is mirrored to [SavedStateHandle] as plain Strings and two parallel
 * `ArrayList<String>` (language ids, with "" for a custom language, and labels), and a touched flag is set. Only the
 * fields the user edited are written, so [seed] lays those over the server values and the restored edit wins over a
 * late fetch (RESEARCH Pitfall 7). A restored value that differs from the server goes through [AutoSaver.onEdited]
 * like a keystroke, so it is saved after the normal debounce. No DTO or secret is stored (Pitfall 6; the profile
 * form has no secret).
 */
@HiltViewModel
class ClientProfileSettingsViewModel
    @Inject
    constructor(
        private val repository: ProfileSettingsRepository,
        private val userManager: UserManager,
        private val avatarController: ProfileAvatarController,
        @ApplicationScope appScope: CoroutineScope,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ClientProfileSettingsUiState())
        val uiState: StateFlow<ClientProfileSettingsUiState> = _uiState.asStateFlow()

        private val saver =
            AutoSaver(
                scope = viewModelScope,
                flushScope = appScope,
                baseline = EmptySnapshot,
                persist = { old, new -> persist(old, new) },
                onFailure = ::onSaveFailed,
                sendsNothing = { old, new -> buildProfilePatch(old, new) == null },
            )

        private var seeded = false

        /** The names the server holds, trimmed: an emptied name is held (never sent) while the server has one. */
        @Volatile
        private var serverFirstName = ""

        @Volatile
        private var serverLastName = ""

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
                userManager.profile.collect { profile ->
                    if (profile != null) {
                        _uiState.update { it.copy(avatarUrl = profile.personalDetails.avatarUrl) }
                        // Also covers a profile that arrives after a failed first load (a later refresh elsewhere).
                        seed(profile)
                    }
                }
            }
            val cached = userManager.profile.value
            if (cached != null) {
                seed(cached)
            } else {
                loadProfile()
            }
        }

        private var loadJob: Job? = null

        /** Refreshes the profile once; a failure with nothing cached shows the retry state instead of a spinner. */
        private fun loadProfile() {
            _uiState.update { it.copy(loadFailed = false) }
            loadJob =
                viewModelScope.launch {
                    try {
                        userManager.refreshProfile()
                    } catch (_: ApiException) {
                        // Handled below: there is nothing to edit without a profile.
                    }
                    val profile = userManager.profile.value
                    if (profile != null) seed(profile) else _uiState.update { it.copy(loadFailed = true) }
                }
        }

        /** The retry state's action: runs the first-load refresh again. */
        fun retryLoad() {
            if (seeded || loadJob?.isActive == true) return
            loadProfile()
        }

        private fun seed(profile: UserProfileData) {
            if (seeded) return
            seeded = true
            // A legacy alpha-2 (or unrecognizable) stored country is seeded as alpha-3 (or blank), so the baseline and
            // every later location PATCH use the form the server requires.
            val server =
                ProfileSnapshot.from(profile).let {
                    it.copy(countryCode = Countries.normalize(it.countryCode).orEmpty())
                }
            // Restored edits win over the fetched profile; they differ from the baseline, so the saver sends them.
            val snapshot = restoredOver(server)
            serverFirstName = server.firstName.trim()
            serverLastName = server.lastName.trim()
            saver.rebase(server)
            _uiState.update {
                it.copy(
                    snapshot = snapshot,
                    loadFailed = false,
                    email = profile.personalDetails.email.orEmpty(),
                    fieldErrors = snapshot.requiredNameErrors(serverFirstName, serverLastName),
                )
            }
            if (snapshot != server) saver.onEdited(snapshot)
        }

        /** [server] with every field the user edited before recreation put back (values cut to their maximum). */
        private fun restoredOver(server: ProfileSnapshot): ProfileSnapshot {
            if (savedStateHandle.get<Boolean>(KEY_TOUCHED) != true) return server
            var result = server
            for (field in ProfileTextFieldId.entries) {
                val saved = savedStateHandle.get<String>(field.savedStateKey()) ?: continue
                result = result.with(field, saved.clampLength(field.maxLength))
            }
            savedStateHandle.get<String>(KEY_COUNTRY)?.let {
                result = result.copy(countryCode = Countries.normalize(it).orEmpty())
            }
            val ids = savedStateHandle.get<ArrayList<String>>(KEY_LANGUAGE_IDS)
            val labels = savedStateHandle.get<ArrayList<String>>(KEY_LANGUAGE_LABELS)
            if (ids != null && labels != null) {
                result =
                    result.copy(
                        languages =
                            ids.mapIndexed { i, id ->
                                ProfileLanguage(id.ifBlank { null }, labels.getOrNull(i).orEmpty())
                            },
                    )
            }
            return result
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
            val next = current.with(field, clamped)
            mirrorField(field, clamped)
            val required = next.requiredNameErrors(serverFirstName, serverLastName).filterKeys { it == field }
            _uiState.update { it.copy(snapshot = next, fieldErrors = it.fieldErrors - field + required) }
            saver.onEdited(next)
        }

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
            val next = current.copy(countryCode = Countries.normalize(code).orEmpty())
            savedStateHandle[KEY_COUNTRY] = next.countryCode
            savedStateHandle[KEY_TOUCHED] = true
            _uiState.update { it.copy(snapshot = next, countrySheetOpen = false, countryQuery = "") }
            saver.onEdited(next)
        }

        fun dismissCountry() = _uiState.update { it.copy(countrySheetOpen = false, countryQuery = "") }

        // ---- spoken languages ----

        /** Opens the language sheet with the catalogue entries the user has as the working selection. */
        fun openLanguages() {
            val snapshot = _uiState.value.snapshot ?: return
            val selection =
                snapshot.languages.mapNotNull { language ->
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
            val customs = current.languages.filter { it.id.isNullOrBlank() }
            val next = current.copy(languages = sheet.selection.map { ProfileLanguage(it.id, it.label) } + customs)
            savedStateHandle[KEY_LANGUAGE_IDS] = ArrayList(next.languages.map { it.id.orEmpty() })
            savedStateHandle[KEY_LANGUAGE_LABELS] = ArrayList(next.languages.map { it.label })
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
                        setCatalog(LanguageCatalog.Loaded(repository.languages()))
                    } catch (_: ApiException) {
                        setCatalog(LanguageCatalog.Failed)
                    }
                }
        }

        private fun setCatalog(value: LanguageCatalog) {
            catalog = value
            _uiState.update { state -> state.copy(languageSheet = state.languageSheet?.copy(catalog = value)) }
        }

        fun openProfessionalSheet() = _uiState.update { it.copy(professionalSheetOpen = true) }

        /** "Update details": applies the sheet's working copy, closes the sheet and schedules the save. */
        fun applyProfessional(
            jobTitle: String,
            company: String,
        ) {
            val current = _uiState.value.snapshot ?: return
            val next =
                current.copy(
                    jobTitle = jobTitle.clampLength(ProfileTextFieldId.JobTitle.maxLength),
                    company = company.clampLength(ProfileTextFieldId.Company.maxLength),
                )
            mirrorField(ProfileTextFieldId.JobTitle, next.jobTitle)
            mirrorField(ProfileTextFieldId.Company, next.company)
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
            old: ProfileSnapshot,
            new: ProfileSnapshot,
        ) {
            val patch = buildProfilePatch(old, new) ?: return
            repository.updateProfile(patch)
            patch.basic?.firstName?.let { serverFirstName = it }
            patch.basic?.lastName?.let { serverLastName = it }
            try {
                userManager.refreshProfile()
            } catch (_: ApiException) {
                // The save succeeded; the hub card catches up on the next profile refresh.
            }
        }

        /**
         * A `422 VALIDATION_ERROR` opens the dialog with the field names to check ([ProfileSaveErrorMapper]); it never
         * shows the server's validator text. Any other failure keeps the server's own message or the connection text.
         */
        private fun onSaveFailed(error: ApiException) {
            _uiState.update { it.copy(dialog = error.toProfileSaveDialogError()) }
        }
    }

internal const val HTTP_UNPROCESSABLE = 422

internal fun ProfileSnapshot.with(
    field: ProfileTextFieldId,
    value: String,
): ProfileSnapshot =
    when (field) {
        ProfileTextFieldId.FirstName -> copy(firstName = value)
        ProfileTextFieldId.LastName -> copy(lastName = value)
        ProfileTextFieldId.Bio -> copy(bio = value)
        ProfileTextFieldId.JobTitle -> copy(jobTitle = value)
        ProfileTextFieldId.Company -> copy(company = value)
        ProfileTextFieldId.City -> copy(city = value)
        ProfileTextFieldId.StateProvince -> copy(stateProvince = value)
    }

/**
 * The save-failure message: the server's own text when it sent one, otherwise "Check your connection and try again."
 * Null for ACCOUNT_SUSPENDED, whose dialog is global.
 */
internal fun ApiException.toSaveDialogError(): FieldError? {
    val base = toDialogError() ?: return null
    return if (this is ApiException.Http && userMessage == null) FieldError.Res(R.string.login_error_network) else base
}
