package com.uptrendinvestments.clickmeapp.screens.expertmode.settings.subviews

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.model.responses.ExpertTag
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagItem
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.expertmode.settings.ExpertiseTagsRepository
import com.uptrendinvestments.clickmeapp.screens.expertmode.toExpertDialogError
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/** Load state of the tag catalog shown in the Edit Expertise sheet. */
sealed interface ExpertiseCatalogState {
    data object Loading : ExpertiseCatalogState

    data class Failed(
        val error: FieldError,
    ) : ExpertiseCatalogState

    data class Loaded(
        val items: List<ExpertiseTagItem>,
    ) : ExpertiseCatalogState
}

data class ExpertiseEditorUiState(
    val query: String = "",
    /** Selected tags in order; the first one is the primary tag. */
    val selected: List<ExpertiseTagItem> = emptyList(),
    val catalog: ExpertiseCatalogState = ExpertiseCatalogState.Loading,
    val isSaving: Boolean = false,
    val dialog: FieldError? = null,
    /** Set once after a successful save; the sheet hands it to its caller and calls `onSavedConsumed`. */
    val saved: List<ExpertiseTagItem>? = null,
) {
    /** Catalog tags matching the trimmed query (case-insensitive substring); selected tags stay in the list. */
    val rows: List<ExpertiseTagItem>
        get() {
            val loaded = catalog as? ExpertiseCatalogState.Loaded ?: return emptyList()
            val needle = query.trim()
            return if (needle.isEmpty()) {
                loaded.items
            } else {
                loaded.items.filter { it.label.contains(needle, ignoreCase = true) }
            }
        }

    /** Ids (lower-cased, as the server compares them) of the selected tags. */
    val selectedIds: Set<String>
        get() = selected.mapTo(HashSet()) { it.id.lowercase(Locale.ROOT) }

    val canSave: Boolean
        get() = selected.isNotEmpty() && !isSaving
}

private const val KEY_SELECTED_IDS = "expertise_selected_ids"
private const val KEY_SELECTED_LABELS = "expertise_selected_labels"
private const val KEY_QUERY = "expertise_query"

/**
 * Backs the Edit Expertise sheet (iOS `ExpertiseEditorViewModel`, D-08). Loads the full tag catalog and the
 * expert's current profile tags in parallel, keeps the selection in order (first = primary) and saves it with
 * `PATCH /expert/profile`. An empty selection can never be saved, so the profile's tags cannot be wiped by accident
 * (T-10-34). The caller scopes a fresh instance per opening, so every open re-seeds from the server.
 */
@HiltViewModel
class ExpertiseEditorViewModel
    @Inject
    constructor(
        private val repository: ExpertiseTagsRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val restoredSelection: List<ExpertiseTagItem>? = restoreSelection()

        private val _uiState =
            MutableStateFlow(
                ExpertiseEditorUiState(
                    query = savedStateHandle.get<String>(KEY_QUERY).orEmpty(),
                    selected = restoredSelection.orEmpty(),
                ),
            )
        val uiState: StateFlow<ExpertiseEditorUiState> = _uiState.asStateFlow()

        /** True once the user (or a restored state) owns the selection, so a late profile load must not overwrite it. */
        private var touched: Boolean = restoredSelection != null

        init {
            loadCatalog()
            if (restoredSelection == null) loadProfileTags()
        }

        private fun restoreSelection(): List<ExpertiseTagItem>? {
            val ids = savedStateHandle.get<ArrayList<String>>(KEY_SELECTED_IDS)
            val labels = savedStateHandle.get<ArrayList<String>>(KEY_SELECTED_LABELS)
            return if (ids != null && labels != null && ids.size == labels.size) {
                ids.indices.map { ExpertiseTagItem(ids[it], labels[it]) }
            } else {
                null
            }
        }

        private fun loadCatalog() {
            _uiState.update { it.copy(catalog = ExpertiseCatalogState.Loading) }
            viewModelScope.launch {
                val catalog =
                    try {
                        ExpertiseCatalogState.Loaded(repository.catalog())
                    } catch (e: ApiException) {
                        ExpertiseCatalogState.Failed(e.toSectionMessage())
                    }
                _uiState.update { it.copy(catalog = catalog) }
            }
        }

        /** The profile tags seed the selection, primary first (iOS primaryId rule). A failure leaves it empty. */
        private fun loadProfileTags() {
            viewModelScope.launch {
                val tags =
                    try {
                        repository.profileTags()
                    } catch (e: ApiException) {
                        return@launch
                    }
                if (touched) return@launch
                val ordered = primaryFirst(tags)
                _uiState.update { it.copy(selected = ordered) }
            }
        }

        private fun primaryFirst(tags: List<ExpertTag>): List<ExpertiseTagItem> {
            val primary = tags.firstOrNull { it.isPrimary }
            val rest = if (primary == null) tags else tags.filter { it !== primary }
            return listOfNotNull(primary).plus(rest).map { ExpertiseTagItem(it.id, it.label) }
        }

        fun retry() {
            if (_uiState.value.catalog is ExpertiseCatalogState.Failed) loadCatalog()
        }

        fun onQueryChange(value: String) {
            savedStateHandle[KEY_QUERY] = value
            _uiState.update { it.copy(query = value) }
        }

        /** Adds [tag] at the end, or removes it when already selected. The first selected tag is primary. */
        fun toggle(tag: ExpertiseTagItem) {
            val id = tag.id.lowercase(Locale.ROOT)
            if (id in _uiState.value.selectedIds) {
                remove(tag.id)
            } else {
                updateSelection { it + tag }
            }
        }

        fun remove(id: String) {
            updateSelection { list -> list.filterNot { it.id.equals(id, ignoreCase = true) } }
        }

        private fun updateSelection(transform: (List<ExpertiseTagItem>) -> List<ExpertiseTagItem>) {
            touched = true
            val next = transform(_uiState.value.selected)
            savedStateHandle[KEY_SELECTED_IDS] = ArrayList(next.map { it.id })
            savedStateHandle[KEY_SELECTED_LABELS] = ArrayList(next.map { it.label })
            _uiState.update { it.copy(selected = next) }
        }

        /** Saves the selection. Ignored while saving and for an empty selection (the sheet explains why). */
        fun save() {
            val current = _uiState.value
            if (current.isSaving || current.selected.isEmpty()) return
            _uiState.update { it.copy(isSaving = true, dialog = null) }
            viewModelScope.launch {
                try {
                    repository.saveProfileTags(current.selected)
                    _uiState.update { it.copy(isSaving = false, saved = current.selected) }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(isSaving = false, dialog = e.toExpertDialogError()) }
                }
            }
        }

        fun dismissDialog() {
            _uiState.update { it.copy(dialog = null) }
        }

        fun onSavedConsumed() {
            _uiState.update { it.copy(saved = null) }
        }
    }
