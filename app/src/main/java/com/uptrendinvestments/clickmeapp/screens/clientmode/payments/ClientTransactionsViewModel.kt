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

        /**
         * Serves first load, retry and pull. A 500 here is a real failure: the end-of-list 500 rule is by requested
         * page, not by position in the list.
         */
        private fun loadFirstPage(pull: Boolean) {
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
    }
