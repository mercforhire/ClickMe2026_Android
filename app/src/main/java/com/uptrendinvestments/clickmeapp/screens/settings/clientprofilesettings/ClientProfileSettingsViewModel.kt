package com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.di.ApplicationScope
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.model.responses.LanguageItem
import com.uptrendinvestments.clickmeapp.model.responses.UserProfileData
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.login.toDialogError
import com.uptrendinvestments.clickmeapp.screens.signup.basicinfo.LanguageCatalog
import com.uptrendinvestments.clickmeapp.screens.signup.basicinfo.LanguageSheetState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
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
 * [snapshot] is null until the profile has been read (the screen shows a spinner). [dialog] is the save-failure
 * message, [fieldErrors] the per-field messages from a 422.
 */
data class ClientProfileSettingsUiState(
    val snapshot: ProfileSnapshot? = null,
    val email: String = "",
    val status: AutoSaveStatus = AutoSaveStatus.Idle,
    val fieldErrors: Map<ProfileTextFieldId, FieldError> = emptyMap(),
    val dialog: FieldError? = null,
    val professionalSheetOpen: Boolean = false,
    val avatarUrl: String? = null,
    val avatar: AvatarUiState = AvatarUiState(),
    val languageSheet: LanguageSheetState? = null,
)

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

/**
 * State holder for client Profile Settings: iOS-style auto-save (no Save button, no discard dialog). Every change is
 * handed to an [AutoSaver] that PATCHes the changed groups 1.2 s after the last edit. Leaving the screen flushes a
 * pending edit on the application scope so it is neither lost nor cancelled with this ViewModel.
 *
 * The screen renders from the cached profile, so there is no loading state in the normal case; if the cache is empty
 * the profile is refreshed once.
 */
@HiltViewModel
class ClientProfileSettingsViewModel
    @Inject
    constructor(
        private val repository: ProfileSettingsRepository,
        private val userManager: UserManager,
        private val avatarController: ProfileAvatarController,
        @ApplicationScope appScope: CoroutineScope,
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
            )

        private var seeded = false

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
                    }
                }
            }
            val cached = userManager.profile.value
            if (cached != null) {
                seed(cached)
            } else {
                viewModelScope.launch {
                    try {
                        userManager.refreshProfile()
                    } catch (_: ApiException) {
                        // The screen keeps its spinner; there is nothing to edit without a profile.
                    }
                    seed(userManager.profile.filterNotNull().first())
                }
            }
        }

        private fun seed(profile: UserProfileData) {
            if (seeded) return
            seeded = true
            val snapshot = ProfileSnapshot.from(profile)
            saver.rebase(snapshot)
            _uiState.update { it.copy(snapshot = snapshot, email = profile.personalDetails.email.orEmpty()) }
        }

        /** Applies [value] (cut to the field's maximum) and schedules the save. */
        fun onFieldChange(
            field: ProfileTextFieldId,
            value: String,
        ) {
            val current = _uiState.value.snapshot ?: return
            val next = current.with(field, value.take(field.maxLength))
            _uiState.update { it.copy(snapshot = next, fieldErrors = it.fieldErrors - field) }
            saver.onEdited(next)
        }

        /** The system photo picker returned [uri]; the controller imports it and asks for the crop screen. */
        fun onPhotoPicked(uri: Uri) = avatarController.onPicked(uri, viewModelScope)

        /** The screen pushed the crop route. */
        fun avatarCropOpened() = avatarController.onCropOpened()

        fun dismissAvatarError() = avatarController.dismissError()

        /** Called on every resume: uploads a confirmed crop (or cleans up after a cancelled one). */
        fun onScreenShown() = avatarController.onReturnedFromCrop()

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
                    jobTitle = jobTitle.take(ProfileTextFieldId.JobTitle.maxLength),
                    company = company.take(ProfileTextFieldId.Company.maxLength),
                )
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
            try {
                userManager.refreshProfile()
            } catch (_: ApiException) {
                // The save succeeded; the hub card catches up on the next profile refresh.
            }
        }

        /**
         * A 422 that names one of this screen's fields shows its message under that field; anything else (or a 422
         * for a field this screen does not have) opens the dialog.
         */
        private fun onSaveFailed(error: ApiException) {
            val perField = (error as? ApiException.Http)?.takeIf { it.status == HTTP_UNPROCESSABLE }?.fieldMessages()
            if (!perField.isNullOrEmpty()) {
                _uiState.update { it.copy(fieldErrors = it.fieldErrors + perField) }
            } else {
                _uiState.update { it.copy(dialog = error.toSaveDialogError()) }
            }
        }
    }

private const val HTTP_UNPROCESSABLE = 422

/** Server field name (`basic.first_name` or bare `first_name`) to the field it belongs to. */
private fun fieldForServerName(name: String): ProfileTextFieldId? =
    when (name.substringAfterLast('.')) {
        "first_name" -> ProfileTextFieldId.FirstName
        "last_name" -> ProfileTextFieldId.LastName
        "bio" -> ProfileTextFieldId.Bio
        "job_title" -> ProfileTextFieldId.JobTitle
        "company" -> ProfileTextFieldId.Company
        "city" -> ProfileTextFieldId.City
        "state_province" -> ProfileTextFieldId.StateProvince
        else -> null
    }

private fun ApiException.Http.fieldMessages(): Map<ProfileTextFieldId, FieldError> =
    fieldErrors
        .mapNotNull { dto -> fieldForServerName(dto.field)?.let { it to FieldError.Text(dto.message) } }
        .toMap()

private fun ProfileSnapshot.with(
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
