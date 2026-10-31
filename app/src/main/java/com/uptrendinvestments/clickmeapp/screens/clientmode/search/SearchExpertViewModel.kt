package com.uptrendinvestments.clickmeapp.screens.clientmode.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.CategoryStore
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** iOS `searchDebounce`: how long typing must pause before the text search is sent (D-10). */
internal const val SEARCH_DEBOUNCE_MS = 300L

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
         * Text change from the field (iOS `onSearchTextChanged`). The field keeps the raw [text]; only the request
         * trims it. The search waits [SEARCH_DEBOUNCE_MS] and every change restarts the wait. There is no check
         * against the previous query, so a trailing-space edit re-searches as on iOS (D-10).
         */
        fun onQueryChange(text: String) {
            _uiState.update { it.copy(query = text) }
            runSearch(SearchMode.Fresh, debounce = true)
        }

        /**
         * Chip tap (iOS `onCategoryChanged`): single select, searches at once and cancels a pending debounce.
         * Re-selecting the selected chip re-runs and keeps it selected, with no toggle (D-11).
         */
        fun onCategorySelected(slug: String) {
            _uiState.update { it.copy(selectedCategorySlug = slug) }
            runSearch(SearchMode.Fresh, debounce = false)
        }

        /** Sort pick (iOS `onSortChanged`): searches at once, including when the current sort is picked again (D-10). */
        fun onSortSelected(sort: SearchSort) {
            _uiState.update { it.copy(sort = sort) }
            runSearch(SearchMode.Fresh, debounce = false)
        }

        /**
         * Pull-to-refresh. With rows on screen it keeps them and only raises `isRefreshing`, and a failure raises the
         * one-shot `refreshFailed` instead of replacing them. With no rows (empty, failed or loading) it behaves as
         * a new search (D-09). A deliberate departure from iOS, whose `reload()` wipes to the error view. Missing
         * chips are fetched again, which repairs a failed first fetch (iOS `reload() -> load()`).
         */
        fun onRefresh() {
            val results = _uiState.value.results
            val hasRows = results is SearchResults.Loaded && results.experts.isNotEmpty()
            runSearch(if (hasRows) SearchMode.Refresh else SearchMode.Fresh)
            repairMissingChips()
        }

        /** Retry from the inline error: a new search with the current query and filters, plus the chip repair. */
        fun onRetry() {
            runSearch(SearchMode.Fresh)
            repairMissingChips()
        }

        /** The screen calls this right after showing the refresh-failed snackbar, so the flag fires once. */
        fun onRefreshFailureConsumed() {
            _uiState.update { it.copy(refreshFailed = false) }
        }

        private fun repairMissingChips() {
            if (_uiState.value.categories.isEmpty()) loadCategories()
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

        /**
         * iOS `performSearch`. A Fresh search shows the skeleton first (D-07) and a failure replaces the rows (D-08).
         * A Refresh keeps the rows on screen. Both clear the refresh flags before launching, so a new search that
         * cancels an in-flight refresh never leaves the spinner on or a stale snackbar request behind. The screen
         * dismisses a snackbar that is already visible when the query, filters or rows change.
         */
        private fun runSearch(
            mode: SearchMode,
            debounce: Boolean = false,
        ) {
            searchJob?.cancel()
            _uiState.update { it.copy(isRefreshing = mode == SearchMode.Refresh, refreshFailed = false) }
            searchJob =
                viewModelScope.launch {
                    // The request values are read after the wait, so the latest trimmed text always wins.
                    if (debounce) delay(SEARCH_DEBOUNCE_MS)
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
                        // A Fresh search is a new result set, so the screen starts it at the top. A Refresh keeps the
                        // same rows and so keeps the scroll position.
                        val revisionStep = if (mode == SearchMode.Fresh) 1 else 0
                        _uiState.update {
                            it.copy(
                                results = loaded,
                                isRefreshing = false,
                                searchRevision = it.searchRevision + revisionStep,
                            )
                        }
                    } catch (e: ApiException) {
                        if (mode == SearchMode.Refresh) {
                            _uiState.update { it.copy(isRefreshing = false, refreshFailed = true) }
                        } else {
                            _uiState.update { it.copy(results = SearchResults.Failed(e.toSearchMessage())) }
                        }
                    }
                }
        }
    }

/** iOS `message(for:)`: the server's message when there is one, else the generic connection copy (D-08). */
private fun ApiException.toSearchMessage(): FieldError =
    userMessage?.let(FieldError::Text) ?: FieldError.Res(R.string.error_state_body)
