package com.uptrendinvestments.clickmeapp.screens.clientmode.explore.categoriesmodal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.managers.CategoryStore
import com.uptrendinvestments.clickmeapp.model.responses.CategoryItem
import com.uptrendinvestments.clickmeapp.network.ApiException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Load state of the category catalog behind the All Categories sheet (`CategoryStore`, D-06). */
sealed interface CategoryCatalog {
    data object Loading : CategoryCatalog

    data class Loaded(
        val items: List<CategoryItem>,
    ) : CategoryCatalog

    data object Failed : CategoryCatalog
}

data class CategoriesSheetUiState(
    val catalog: CategoryCatalog = CategoryCatalog.Loading,
    val query: String = "",
    /** Working multi-select of category slugs; handed back only by Apply (D-10). */
    val draft: Set<String> = emptySet(),
) {
    /** Categories matching the search (case-insensitive substring on the name), in server order. */
    val visible: List<CategoryItem>
        get() {
            val loaded = catalog as? CategoryCatalog.Loaded ?: return emptyList()
            val needle = query.trim()
            return if (needle.isEmpty()) {
                loaded.items
            } else {
                loaded.items.filter { it.name.contains(needle, ignoreCase = true) }
            }
        }

    /** A search is active and nothing matches it. */
    val noMatch: Boolean
        get() = catalog is CategoryCatalog.Loaded && query.isNotBlank() && visible.isEmpty()
}

internal const val KEY_DRAFT = "categories_sheet_draft"
internal const val KEY_QUERY = "categories_sheet_query"

/**
 * All Categories sheet state (DISC-02, D-06, D-10). Port of iOS `CategoriesModalViewModel` with an explicit Apply: the
 * draft starts as the shared Explore selection ([open]), Clear empties only the draft, and the caller applies the draft
 * on Apply or discards it on any other dismissal. The catalog comes from [CategoryStore] (cached for 7 days, D-07);
 * Retry forces [CategoryStore.refresh]. Search is local only and never sent to the server. Draft and query persist in
 * [SavedStateHandle].
 */
@HiltViewModel
class CategoriesSheetViewModel
    @Inject
    constructor(
        private val categoryStore: CategoryStore,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(
                CategoriesSheetUiState(
                    query = savedStateHandle.get<String>(KEY_QUERY).orEmpty(),
                    draft = savedStateHandle.get<ArrayList<String>>(KEY_DRAFT).orEmpty().toSet(),
                ),
            )
        val uiState: StateFlow<CategoriesSheetUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null

        /** Starts a sheet session: the draft is the current Explore selection and the search is cleared (D-10). */
        fun open(selection: Set<String>) {
            persistDraft(selection)
            savedStateHandle[KEY_QUERY] = ""
            _uiState.update { it.copy(draft = selection, query = "") }
        }

        /** Loads the catalog through `CategoryStore.fetch()` (memory or disk within 7 days, otherwise network). */
        fun ensureLoaded() {
            if (_uiState.value.catalog is CategoryCatalog.Loaded) return
            if (loadJob?.isActive == true) return
            load { categoryStore.fetch() }
        }

        /** Retry after a failed load: always goes to the network (D-06). */
        fun retry() {
            load { categoryStore.refresh() }
        }

        private fun load(source: suspend () -> List<CategoryItem>) {
            loadJob?.cancel()
            _uiState.update { it.copy(catalog = CategoryCatalog.Loading) }
            loadJob =
                viewModelScope.launch {
                    val catalog =
                        try {
                            CategoryCatalog.Loaded(source())
                        } catch (e: ApiException) {
                            CategoryCatalog.Failed
                        }
                    _uiState.update { it.copy(catalog = catalog) }
                }
        }

        fun onQueryChange(query: String) {
            savedStateHandle[KEY_QUERY] = query
            _uiState.update { it.copy(query = query) }
        }

        /** Adds or removes [slug] from the draft. Slugs only come from server data (T-05-26). */
        fun toggle(slug: String) {
            val next = _uiState.value.draft.let { if (slug in it) it - slug else it + slug }
            persistDraft(next)
            _uiState.update { it.copy(draft = next) }
        }

        /** Empties the draft only; the sheet stays open. */
        fun clear() {
            persistDraft(emptySet())
            _uiState.update { it.copy(draft = emptySet()) }
        }

        private fun persistDraft(draft: Set<String>) {
            savedStateHandle[KEY_DRAFT] = ArrayList(draft.sorted())
        }
    }
