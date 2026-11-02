package com.uptrendinvestments.clickmeapp.screens.clientmode.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.model.responses.ClientTransactionItem
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Rows per page of Payment History (iOS `pageSize`). The server clamps the limit to 1..100. */
const val CLIENT_TRANSACTIONS_PAGE_SIZE = 20

/** A page is requested when one of the last this-many rows becomes visible (iOS: the last three rows). */
private const val PREFETCH_DISTANCE = 3

/** The status the server answers with for a page past the end (`CLIENT-TRANSACTIONS.md` section 7). */
private const val HTTP_SERVER_ERROR = 500

/** The first page of the payment history. */
sealed interface ClientTransactionsLoad {
    data object Loading : ClientTransactionsLoad

    data object Loaded : ClientTransactionsLoad

    /** The first page came back with no rows. */
    data object Empty : ClientTransactionsLoad

    data class Failed(
        val message: FieldError,
    ) : ClientTransactionsLoad
}

/**
 * Payment History state. [rows] keep the server order (newest first). [isPaging] and [pageError] describe the trailing
 * next-page row; [isRefreshing] and [refreshFailed] describe pull-to-refresh.
 */
data class ClientTransactionsUiState(
    val load: ClientTransactionsLoad = ClientTransactionsLoad.Loading,
    val rows: List<ClientTransactionItem> = emptyList(),
    val isPaging: Boolean = false,
    val pageError: FieldError? = null,
    val isRefreshing: Boolean = false,
    val refreshFailed: Boolean = false,
)

/**
 * State holder for Payment History. Port of iOS `ClientTransactionsViewModel` with the Phase 15 states (D-04): rows
 * stay visible during a pull, a failed pull keeps them and flags a snackbar, and every first-page load cancels the
 * page in flight so a refresh or retry always ends with one coherent list.
 *
 * Rows keep the server order and are never re-sorted; a booking id already shown is not added again, so a ledger that
 * shifts between page fetches cannot hand `LazyColumn` a duplicate key.
 *
 * Nothing is logged: the rows hold expert names.
 */
@HiltViewModel
class ClientTransactionsViewModel
    @Inject
    constructor(
        private val repository: ClientTransactionsRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ClientTransactionsUiState())
        val uiState: StateFlow<ClientTransactionsUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null
        private var pageJob: Job? = null
        private var nextPage = 2
        private var hasMore = false

        init {
            loadFirstPage(pull = false)
        }

        /** Reloads page 1 after a failed first load. */
        fun retry() {
            _uiState.update { it.copy(load = ClientTransactionsLoad.Loading, isRefreshing = false) }
            loadFirstPage(pull = false)
        }

        /** Pull-to-refresh: reloads page 1 and replaces the list. A failure keeps the rows and flags it. */
        fun refresh() {
            _uiState.update { it.copy(isRefreshing = true) }
            loadFirstPage(pull = true)
        }

        /** The Snackbar for a failed refresh was shown. */
        fun consumeRefreshFailure() {
            _uiState.update { it.copy(refreshFailed = false) }
        }

        /** The list reports the index of its last visible item; near the end this requests the next page. */
        fun onRowVisible(index: Int) {
            val state = _uiState.value
            val idle = state.load == ClientTransactionsLoad.Loaded && !state.isPaging && !state.isRefreshing
            if (!idle || state.pageError != null || !hasMore) return
            if (index < state.rows.size - PREFETCH_DISTANCE) return
            loadNextPage()
        }

        /**
         * Requests the page that failed again. Ignored while a refresh runs: the refresh restarts the list at page 2,
         * and a stale page request landing after it would skip a page.
         */
        fun retryPage() {
            val state = _uiState.value
            if (state.isPaging || state.isRefreshing || state.pageError == null) return
            loadNextPage()
        }

        /**
         * Serves first load, retry and pull. A 500 here is a real failure: the end-of-list 500 rule is by requested
         * page, not by position in the list, so it lives only in [loadNextPage].
         */
        private fun loadFirstPage(pull: Boolean) {
            pageJob?.cancel()
            _uiState.update { it.copy(isPaging = false) }
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    try {
                        val data = repository.page(page = 1, limit = CLIENT_TRANSACTIONS_PAGE_SIZE)
                        val rows = data.transactions.distinctBy { it.bookingId }
                        hasMore = data.hasMore
                        nextPage = 2
                        val load = if (rows.isEmpty()) ClientTransactionsLoad.Empty else ClientTransactionsLoad.Loaded
                        _uiState.update {
                            it.copy(load = load, rows = rows, pageError = null, isRefreshing = false)
                        }
                    } catch (e: ApiException) {
                        _uiState.update {
                            if (pull) {
                                it.copy(isRefreshing = false, refreshFailed = true)
                            } else {
                                it.copy(
                                    load = ClientTransactionsLoad.Failed(e.toSectionMessage()),
                                    isRefreshing = false,
                                )
                            }
                        }
                    }
                }
        }

        private fun loadNextPage() {
            val page = nextPage
            _uiState.update { it.copy(isPaging = true, pageError = null) }
            pageJob =
                viewModelScope.launch {
                    try {
                        val data = repository.page(page = page, limit = CLIENT_TRANSACTIONS_PAGE_SIZE)
                        hasMore = data.hasMore
                        nextPage = page + 1
                        _uiState.update { it.copy(rows = it.rows.appendNew(data.transactions), isPaging = false) }
                    } catch (e: ApiException) {
                        if (e is ApiException.Http && e.status == HTTP_SERVER_ERROR) {
                            // The server answers a page past the end with a 500 (`CLIENT-TRANSACTIONS.md` section 7;
                            // iOS `loadNextPageIfNeeded`): the list simply ends, with no footer and no further request.
                            hasMore = false
                            _uiState.update { it.copy(isPaging = false, pageError = null) }
                        } else {
                            _uiState.update { it.copy(isPaging = false, pageError = e.toSectionMessage()) }
                        }
                    }
                }
        }

        /** Appends the rows of [next] whose booking id is not shown yet, after the existing rows and in server order. */
        private fun List<ClientTransactionItem>.appendNew(
            next: List<ClientTransactionItem>,
        ): List<ClientTransactionItem> {
            val known = mapTo(HashSet()) { it.bookingId }
            return this + next.filter { known.add(it.bookingId) }
        }
    }
