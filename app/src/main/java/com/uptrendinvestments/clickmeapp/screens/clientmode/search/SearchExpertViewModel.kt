package com.uptrendinvestments.clickmeapp.screens.clientmode.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.CategoryStore
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Search tab state machine. Port of iOS `SearchExpertViewModel`.
 *
 * Query, chip, sort and results live only in memory: there is no saved-state handle, so the state lasts as long as
 * the Search tab's Nav3 entry and comes back fresh after process death (D-02). This is a deliberate exception to the
 * Phase 15 POL-02 restore rule: a typed query is user input and is never persisted.
 *
 * One [searchJob] is the only writer of `results`, and every trigger cancels it first, so the newest request always
 * wins (D-10). The chip vocabulary loads in its own [categoriesJob] and never blocks or cancels a search (D-11).
 */
@HiltViewModel
class SearchExpertViewModel
    @Inject
    constructor(
        private val repository: SearchRepository,
        private val categoryStore: CategoryStore,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SearchExpertUiState())
        val uiState: StateFlow<SearchExpertUiState> = _uiState.asStateFlow()

        private var searchJob: Job? = null
        private var categoriesJob: Job? = null

        private enum class SearchMode { Fresh, Refresh }

        init {
            // iOS `load()`: the chips are fire-and-forget, the browse set is searched with the empty query.
            loadCategories()
            runSearch(SearchMode.Fresh)
        }

        /**
         * Fills the chip vocabulary from the cached category store. Like iOS, a failure is swallowed so that only the
         * All chip remains (D-11). It never touches `results`.
         */
        private fun loadCategories() {
            categoriesJob?.cancel()
            categoriesJob =
                viewModelScope.launch {
                    val chips =
                        try {
                            categoryStore.fetch().map { SearchCategoryChipUi(slug = it.id, name = it.name) }
                        } catch (_: ApiException) {
                            emptyList()
                        }
                    _uiState.update { it.copy(categories = chips) }
                }
        }

        /** iOS `performSearch`: every new search shows the skeleton first (D-07) and reads the request at send time. */
        private fun runSearch(mode: SearchMode) {
            searchJob?.cancel()
            searchJob =
                viewModelScope.launch {
                    if (mode == SearchMode.Fresh) _uiState.update { it.copy(results = SearchResults.Loading) }
                    val request = _uiState.value
                    try {
                        val rows =
                            repository.search(
                                q = request.query.trim(),
                                categorySlug = request.selectedCategorySlug.takeIf { it != ALL_CATEGORIES_SLUG },
                                sort = request.sort.apiValue,
                            )
                        val loaded = SearchResults.Loaded(rows.map { it.toSearchExpertUi() })
                        _uiState.update { it.copy(results = loaded) }
                    } catch (e: ApiException) {
                        _uiState.update { it.copy(results = SearchResults.Failed(e.toSearchMessage())) }
                    }
                }
        }
    }

/** iOS `message(for:)`: the server's message when there is one, else the generic connection copy (D-08). */
private fun ApiException.toSearchMessage(): FieldError =
    (this as? ApiException.Http)?.userMessage?.let { FieldError.Text(it) } ?: FieldError.Res(R.string.error_state_body)
