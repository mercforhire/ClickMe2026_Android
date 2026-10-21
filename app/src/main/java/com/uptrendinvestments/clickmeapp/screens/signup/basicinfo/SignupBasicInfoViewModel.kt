package com.uptrendinvestments.clickmeapp.screens.signup.basicinfo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.constants.Countries
import com.uptrendinvestments.clickmeapp.constants.Country
import com.uptrendinvestments.clickmeapp.model.responses.LanguageItem
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.login.toDialogError
import com.uptrendinvestments.clickmeapp.screens.signup.SignupDraft
import com.uptrendinvestments.clickmeapp.screens.signup.SignupSetupRepository
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** Where the language catalog (`GET /meta/languages`) stands; loaded once per ViewModel. */
sealed interface LanguageCatalog {
    data object Loading : LanguageCatalog

    data class Loaded(
        val items: List<LanguageItem>,
    ) : LanguageCatalog

    data object Failed : LanguageCatalog
}

/** The Language Selection sheet: a working copy of the selection (in tap order), the search text and the catalog. */
data class LanguageSheetState(
    val query: String = "",
    val selection: List<LanguageItem> = emptyList(),
    val catalog: LanguageCatalog = LanguageCatalog.Loading,
) {
    /** Catalog entries whose label contains [query], ignoring case. Empty until the catalog is loaded. */
    val visibleItems: List<LanguageItem>
        get() =
            when (catalog) {
                is LanguageCatalog.Loaded ->
                    catalog.items.filter { it.label.contains(query.trim(), ignoreCase = true) }
                else -> emptyList()
            }
}

data class SignupBasicInfoUiState(
    val firstName: String = "",
    val city: String = "",
    val provinceState: String = "",
    val countryCode: String = "",
    val languages: List<LanguageItem> = emptyList(),
    val firstNameError: FieldError? = null,
    val cityError: FieldError? = null,
    val provinceError: FieldError? = null,
    val countryError: FieldError? = null,
    val languagesError: FieldError? = null,
    val dialogError: FieldError? = null,
    val saveState: SaveState = SaveState.Idle,
    val showCountrySheet: Boolean = false,
    val countryQuery: String = "",
    val languageSheet: LanguageSheetState? = null,
) {
    /** Display name of the chosen country, or null when none is chosen. */
    val countryName: String? get() = countryCode.takeIf { it.isNotBlank() }?.let(Countries::displayName)

    /** The static country list narrowed by [countryQuery], ignoring case. */
    val visibleCountries: List<Country>
        get() = Countries.all.filter { it.name.contains(countryQuery.trim(), ignoreCase = true) }
}

private const val KEY_FIRST_NAME = "basic_first_name"
private const val KEY_CITY = "basic_city"
private const val KEY_PROVINCE = "basic_province"
private const val KEY_COUNTRY = "basic_country"
private const val KEY_LANGUAGE_IDS = "basic_language_ids"
private const val KEY_LANGUAGE_LABELS = "basic_language_labels"
private const val KEY_TOUCHED = "basic_touched"
private const val UNPROCESSABLE = 422

/**
 * Port of iOS `SignupBasicInfoViewModel` and the Language Selection sheet (D-16). The form is seeded from the shared
 * draft (English when the draft has no languages, Pitfall 6) until the first edit, and is mirrored to
 * [SavedStateHandle] so typed values survive rotation and process death. Save is always enabled and sends only the
 * filled fields (D-04); languages go out in tap order (D-14) with no primary-language helper (D-24).
 */
@HiltViewModel
class SignupBasicInfoViewModel
    @Inject
    constructor(
        private val repository: SignupSetupRepository,
        private val api: ClickMeApi,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SignupBasicInfoUiState())
        val uiState: StateFlow<SignupBasicInfoUiState> = _uiState.asStateFlow()

        private val _saved = MutableStateFlow(false)
        val saved: StateFlow<Boolean> = _saved.asStateFlow()

        private var touched = savedStateHandle.get<Boolean>(KEY_TOUCHED) == true
        private var catalog: LanguageCatalog = LanguageCatalog.Loading
        private var catalogJob: Job? = null

        init {
            if (touched) {
                restoreFromSavedState()
            } else {
                seedFrom(repository.draft.value)
                // A late hydrate (for example after process death) re-seeds until the user edits something.
                viewModelScope.launch {
                    repository.draft.collect { draft -> if (!touched) seedFrom(draft) }
                }
            }
        }

        private fun seedFrom(draft: SignupDraft) {
            _uiState.update {
                it.copy(
                    firstName = draft.firstName,
                    city = draft.city,
                    provinceState = draft.provinceState,
                    countryCode = draft.countryCode,
                    languages = draft.languages.ifEmpty { listOf(ENGLISH) },
                )
            }
        }

        private fun restoreFromSavedState() {
            val ids = savedStateHandle.get<ArrayList<String>>(KEY_LANGUAGE_IDS).orEmpty()
            val labels = savedStateHandle.get<ArrayList<String>>(KEY_LANGUAGE_LABELS).orEmpty()
            _uiState.update {
                it.copy(
                    firstName = savedStateHandle.get<String>(KEY_FIRST_NAME).orEmpty(),
                    city = savedStateHandle.get<String>(KEY_CITY).orEmpty(),
                    provinceState = savedStateHandle.get<String>(KEY_PROVINCE).orEmpty(),
                    countryCode = savedStateHandle.get<String>(KEY_COUNTRY).orEmpty(),
                    languages = ids.mapIndexed { index, id -> LanguageItem(id, labels.getOrNull(index) ?: id) },
                )
            }
        }

        /** Marks the form edited (so the draft no longer re-seeds it) and mirrors it to saved state. */
        private fun markEdited() {
            touched = true
            val state = _uiState.value
            savedStateHandle[KEY_FIRST_NAME] = state.firstName
            savedStateHandle[KEY_CITY] = state.city
            savedStateHandle[KEY_PROVINCE] = state.provinceState
            savedStateHandle[KEY_COUNTRY] = state.countryCode
            savedStateHandle[KEY_LANGUAGE_IDS] = ArrayList(state.languages.map { it.id })
            savedStateHandle[KEY_LANGUAGE_LABELS] = ArrayList(state.languages.map { it.label })
            savedStateHandle[KEY_TOUCHED] = true
        }

        private fun edit(transform: (SignupBasicInfoUiState) -> SignupBasicInfoUiState) {
            _uiState.update(transform)
            markEdited()
        }

        // ---- text fields ----

        fun onFirstNameChange(value: String) = edit { it.copy(firstName = value, firstNameError = null) }

        fun onCityChange(value: String) = edit { it.copy(city = value, cityError = null) }

        fun onProvinceChange(value: String) = edit { it.copy(provinceState = value, provinceError = null) }

        // ---- country sheet ----

        fun openCountrySheet() = _uiState.update { it.copy(showCountrySheet = true, countryQuery = "") }

        fun onCountryQueryChange(value: String) = _uiState.update { it.copy(countryQuery = value) }

        fun selectCountry(code: String) =
            edit { it.copy(countryCode = code, countryError = null, showCountrySheet = false, countryQuery = "") }

        fun dismissCountrySheet() = _uiState.update { it.copy(showCountrySheet = false, countryQuery = "") }

        // ---- languages ----

        fun removeLanguage(id: String) =
            edit { state -> state.copy(languages = state.languages.filterNot { it.id == id }, languagesError = null) }

        fun openLanguageSheet() {
            _uiState.update {
                it.copy(languageSheet = LanguageSheetState(selection = it.languages, catalog = catalog))
            }
            // Reopening after a failure retries; a loaded catalog is never fetched again.
            if (catalog !is LanguageCatalog.Loaded && catalogJob?.isActive != true) loadCatalog()
        }

        fun onLanguageQueryChange(value: String) =
            _uiState.update { state -> state.copy(languageSheet = state.languageSheet?.copy(query = value)) }

        /** Adds [item] to the working selection, or removes it when already selected (D-14 tap order). */
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

        fun confirmLanguageSheet() {
            val selection = _uiState.value.languageSheet?.selection ?: return
            edit { it.copy(languages = selection, languagesError = null, languageSheet = null) }
        }

        fun dismissLanguageSheet() = _uiState.update { it.copy(languageSheet = null) }

        fun retryLanguages() {
            if (catalogJob?.isActive == true) return
            loadCatalog()
        }

        private fun loadCatalog() {
            setCatalog(LanguageCatalog.Loading)
            catalogJob =
                viewModelScope.launch {
                    try {
                        val items = apiCall { api.getLanguages() }.data.languages
                        setCatalog(LanguageCatalog.Loaded(items))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: ApiException) {
                        setCatalog(LanguageCatalog.Failed)
                    }
                }
        }

        private fun setCatalog(value: LanguageCatalog) {
            catalog = value
            _uiState.update { state -> state.copy(languageSheet = state.languageSheet?.copy(catalog = value)) }
        }

        // ---- save ----

        fun save() {
            val current = _uiState.value
            if (current.saveState != SaveState.Idle) return
            _uiState.update {
                it.copy(
                    firstNameError = null,
                    cityError = null,
                    provinceError = null,
                    countryError = null,
                    languagesError = null,
                    dialogError = null,
                    saveState = SaveState.Saving,
                )
            }
            viewModelScope.launch {
                try {
                    repository.saveBasicInfo(
                        firstName = current.firstName,
                        city = current.city,
                        provinceState = current.provinceState,
                        countryCode = current.countryCode,
                        languages = current.languages,
                    )
                    _uiState.update { it.copy(saveState = SaveState.Saved) }
                    delay(Motion.SAVED_CONFIRMATION_HOLD_MS.toLong())
                    _saved.value = true
                } catch (e: ApiException) {
                    _uiState.update { state -> mapFailure(state, e).copy(saveState = SaveState.Idle) }
                }
            }
        }

        /** A 422 lands on the matching field; anything else (or an unknown 422 field) becomes the dialog. */
        private fun mapFailure(
            state: SignupBasicInfoUiState,
            e: ApiException,
        ): SignupBasicInfoUiState {
            if (e !is ApiException.Http || e.status != UNPROCESSABLE) return state.copy(dialogError = e.toDialogError())
            var next = state
            var handled = false
            for (fieldError in e.fieldErrors) {
                val message = FieldError.Text(fieldError.message)
                val field = fieldError.field
                next =
                    when {
                        field == "first_name" -> next.copy(firstNameError = message)
                        field == "location.city" || field == "city" -> next.copy(cityError = message)
                        field == "location.province_state" -> next.copy(provinceError = message)
                        field == "location.country_code" -> next.copy(countryError = message)
                        field.startsWith("languages") -> next.copy(languagesError = message)
                        else -> continue
                    }
                handled = true
            }
            return if (handled) next else state.copy(dialogError = e.toDialogError())
        }

        fun dismissDialog() = _uiState.update { it.copy(dialogError = null) }

        /** Called after the screen has navigated away so a returning screen shows an active Save button. */
        fun onSavedConsumed() {
            _saved.value = false
            _uiState.update { it.copy(saveState = SaveState.Idle) }
        }

        companion object {
            /** Pre-selected on a fresh form (D-16). Kept out of the draft so an untouched list is never sent. */
            val ENGLISH = LanguageItem("en", "English")
        }
    }
