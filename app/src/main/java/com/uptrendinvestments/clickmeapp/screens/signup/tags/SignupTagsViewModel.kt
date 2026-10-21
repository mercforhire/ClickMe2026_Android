package com.uptrendinvestments.clickmeapp.screens.signup.tags

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagItem
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.login.toDialogError
import com.uptrendinvestments.clickmeapp.screens.signup.SignupSetupRepository
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Load state of the expertise tag catalog (`GET /meta/expertise-tags`). */
sealed interface TagCatalog {
    data object Loading : TagCatalog

    data class Loaded(
        val items: List<ExpertiseTagItem>,
    ) : TagCatalog

    data object Failed : TagCatalog
}

data class SignupTagsUiState(
    val query: String = "",
    /** Selected tags in tap order; the first one is the primary tag (D-14). */
    val selected: List<ExpertiseTagItem> = emptyList(),
    val catalog: TagCatalog = TagCatalog.Loading,
    val dialogError: FieldError? = null,
    val saveState: SaveState = SaveState.Idle,
) {
    /** Catalog tags that are not selected and match the search (case-insensitive substring). */
    val suggested: List<ExpertiseTagItem>
        get() {
            val loaded = catalog as? TagCatalog.Loaded ?: return emptyList()
            val selectedIds = selected.mapTo(HashSet()) { it.id }
            val needle = query.trim()
            return loaded.items.filter { tag ->
                tag.id !in selectedIds && (needle.isEmpty() || tag.label.contains(needle, ignoreCase = true))
            }
        }

    /** A search is active and nothing unselected matches it. */
    val noMatch: Boolean
        get() = catalog is TagCatalog.Loaded && query.isNotBlank() && suggested.isEmpty()
}

private const val KEY_SELECTED_IDS = "tags_selected_ids"
private const val KEY_SELECTED_LABELS = "tags_selected_labels"
private const val KEY_QUERY = "tags_query"
private const val KEY_TOUCHED = "tags_touched"

/**
 * Expertise Tags step (SIGN-02, D-14, D-15, D-25). Port of iOS `SignupTagsViewModel` (catalog tags only). The
 * catalog is loaded once and filtered client-side. Selection keeps tap order (the first tag is primary on the
 * server) and is seeded from the draft until the first edit. The repository applies the D-25 empty-list rule.
 */
@HiltViewModel
class SignupTagsViewModel
    @Inject
    constructor(
        private val repository: SignupSetupRepository,
        private val api: ClickMeApi,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(restoredState())
        val uiState: StateFlow<SignupTagsUiState> = _uiState.asStateFlow()

        private val _saved = MutableStateFlow(false)
        val saved: StateFlow<Boolean> = _saved.asStateFlow()

        private var touched: Boolean = savedStateHandle.get<Boolean>(KEY_TOUCHED) ?: false

        init {
            viewModelScope.launch {
                repository.draft.collect { draft ->
                    // A late hydrate re-seeds a form the user has not edited yet.
                    if (!touched) _uiState.update { it.copy(selected = draft.expertiseTags) }
                }
            }
            loadCatalog()
        }

        private fun restoredState(): SignupTagsUiState {
            val ids = savedStateHandle.get<ArrayList<String>>(KEY_SELECTED_IDS)
            val labels = savedStateHandle.get<ArrayList<String>>(KEY_SELECTED_LABELS)
            val restoredSelection =
                if (ids != null && labels != null && ids.size == labels.size) {
                    ids.indices.map { ExpertiseTagItem(ids[it], labels[it]) }
                } else {
                    null
                }
            return SignupTagsUiState(
                query = savedStateHandle.get<String>(KEY_QUERY).orEmpty(),
                selected = restoredSelection ?: repository.draft.value.expertiseTags,
            )
        }

        private fun loadCatalog() {
            _uiState.update { it.copy(catalog = TagCatalog.Loading) }
            viewModelScope.launch {
                val catalog =
                    try {
                        TagCatalog.Loaded(apiCall { api.getExpertiseTags() }.data.tags)
                    } catch (e: ApiException) {
                        TagCatalog.Failed
                    }
                _uiState.update { it.copy(catalog = catalog) }
            }
        }

        fun retry() {
            if (_uiState.value.catalog is TagCatalog.Failed) loadCatalog()
        }

        fun onQueryChange(value: String) {
            savedStateHandle[KEY_QUERY] = value
            _uiState.update { it.copy(query = value) }
        }

        /** Appends [tag] at the end (D-14). Catalog tags only: there is no free-text entry (T-04-36). */
        fun select(tag: ExpertiseTagItem) {
            if (_uiState.value.selected.any { it.id == tag.id }) return
            updateSelection { it + tag }
        }

        fun remove(id: String) {
            updateSelection { list -> list.filterNot { it.id == id } }
        }

        private fun updateSelection(transform: (List<ExpertiseTagItem>) -> List<ExpertiseTagItem>) {
            touched = true
            val next = transform(_uiState.value.selected)
            savedStateHandle[KEY_TOUCHED] = true
            savedStateHandle[KEY_SELECTED_IDS] = ArrayList(next.map { it.id })
            savedStateHandle[KEY_SELECTED_LABELS] = ArrayList(next.map { it.label })
            _uiState.update { it.copy(selected = next) }
        }

        /** Save is always enabled; zero tags is allowed and the repository decides whether to send `[]` (D-04). */
        fun save() {
            val current = _uiState.value
            if (current.saveState != SaveState.Idle) return
            _uiState.update { it.copy(saveState = SaveState.Saving, dialogError = null) }
            viewModelScope.launch {
                try {
                    repository.saveTags(current.selected)
                    _uiState.update { it.copy(saveState = SaveState.Saved) }
                    delay(Motion.SAVED_CONFIRMATION_HOLD_MS.toLong())
                    _saved.value = true
                } catch (e: ApiException) {
                    _uiState.update { it.copy(saveState = SaveState.Idle, dialogError = e.toDialogError()) }
                }
            }
        }

        fun dismissDialog() {
            _uiState.update { it.copy(dialogError = null) }
        }

        /** Resets the Saved state after the screen navigated away, so a returning screen can save again. */
        fun onSavedConsumed() {
            _saved.value = false
            _uiState.update { it.copy(saveState = SaveState.Idle) }
        }
    }
