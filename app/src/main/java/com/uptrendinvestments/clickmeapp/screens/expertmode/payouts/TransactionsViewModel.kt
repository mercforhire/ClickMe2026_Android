package com.uptrendinvestments.clickmeapp.screens.expertmode.payouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.model.responses.EarningsTransactionItem
import com.uptrendinvestments.clickmeapp.model.responses.EarningsTransactionsData
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.ExpertFinancialRepository
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Rows per page of the Activity list (iOS `pageSize`). The server clamps the limit to 1..100. */
const val TRANSACTIONS_PAGE_SIZE = 20

/** The first page of the earnings ledger. */
sealed interface TransactionsLoad {
    data object Loading : TransactionsLoad

    data object Loaded : TransactionsLoad

    /** The first page came back with no rows. */
    data object Empty : TransactionsLoad

    data class Failed(
        val message: FieldError,
    ) : TransactionsLoad
}

/**
 * Activity list state. [rows] keep the server order (newest first). [isPaging] and [pageError] describe the trailing
 * next-page row; [isRefreshing] and [refreshFailed] describe pull-to-refresh.
 */
data class TransactionsUiState(
    val load: TransactionsLoad = TransactionsLoad.Loading,
    val rows: List<EarningsTransactionItem> = emptyList(),
    val isPaging: Boolean = false,
    val pageError: FieldError? = null,
    val isRefreshing: Boolean = false,
    val refreshFailed: Boolean = false,
)

/** A page is requested when one of the last this-many rows becomes visible (iOS: the last three rows). */
internal const val PREFETCH_DISTANCE = 3

/**
 * State holder for the Activity list (PAYOUT-03). Port of iOS `TransactionsViewModel`. Loads page 1 on creation and
 * the next page when one of the last [PREFETCH_DISTANCE] rows becomes visible.
 *
 * Rows keep the server order (newest first) and are never re-sorted; a row id already shown is not added again, so a
 * ledger that shifts between page fetches cannot repeat a row. A failed later page keeps the loaded rows and sets
 * [TransactionsUiState.pageError]; while it shows no further page is requested until [retryPage]. Every first-page
 * load cancels the page in flight, so a refresh or retry always ends with one coherent list.
 *
 * Nothing is logged: the rows hold client names.
 */
@HiltViewModel
class TransactionsViewModel
    @Inject
    constructor(
        private val financial: ExpertFinancialRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(TransactionsUiState())
        val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null
        private var pageJob: Job? = null
        private var nextPage = 2
        private var hasMore = false

        init {
            loadFirstPage(pull = false)
        }

        /** Reloads page 1 after a failed first load. */
        fun retry() {
            _uiState.update { it.copy(load = TransactionsLoad.Loading, isRefreshing = false) }
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
            val idle = state.load == TransactionsLoad.Loaded && !state.isPaging && !state.isRefreshing
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

        private fun loadFirstPage(pull: Boolean) {
            pageJob?.cancel()
            loadJob?.cancel()
            _uiState.update { it.copy(isPaging = false) }
            loadJob =
                viewModelScope.launch {
                    try {
                        val data = financial.transactions(page = 1, limit = TRANSACTIONS_PAGE_SIZE)
                        val rows = data.transactions.distinctBy { it.id }
                        hasMore = data.hasMorePages()
                        nextPage = 2
                        val load = if (rows.isEmpty()) TransactionsLoad.Empty else TransactionsLoad.Loaded
                        _uiState.update {
                            it.copy(load = load, rows = rows, pageError = null, isRefreshing = false)
                        }
                    } catch (e: ApiException) {
                        _uiState.update {
                            if (pull) {
                                it.copy(isRefreshing = false, refreshFailed = true)
                            } else {
                                it.copy(load = TransactionsLoad.Failed(e.toSectionMessage()), isRefreshing = false)
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
                        val data = financial.transactions(page = page, limit = TRANSACTIONS_PAGE_SIZE)
                        hasMore = data.hasMorePages()
                        nextPage = page + 1
                        _uiState.update { it.copy(rows = it.rows.appendNew(data.transactions), isPaging = false) }
                    } catch (e: ApiException) {
                        _uiState.update { it.copy(isPaging = false, pageError = e.toSectionMessage()) }
                    }
                }
        }

        /** The server flag, else the page counts; without a pagination block a full page means there may be more. */
        private fun EarningsTransactionsData.hasMorePages(): Boolean =
            pagination?.hasMoreOrDerived ?: (transactions.size == TRANSACTIONS_PAGE_SIZE)

        /** Appends the rows of [next] whose id is not shown yet, after the existing rows and in server order. */
        private fun List<EarningsTransactionItem>.appendNew(
            next: List<EarningsTransactionItem>,
        ): List<EarningsTransactionItem> {
            val known = mapTo(HashSet()) { it.id }
            return this + next.filter { known.add(it.id) }
        }
    }
