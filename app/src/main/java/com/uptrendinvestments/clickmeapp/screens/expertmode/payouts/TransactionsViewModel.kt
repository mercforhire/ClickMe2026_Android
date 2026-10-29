package com.uptrendinvestments.clickmeapp.screens.expertmode.payouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.model.responses.EarningsTransactionItem
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

/**
 * State holder for the Activity list (PAYOUT-03). Port of iOS `TransactionsViewModel`. Loads page 1 on creation.
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

        init {
            loadFirstPage()
        }

        /** Reloads page 1 after a failed first load. */
        fun retry() {
            _uiState.update { it.copy(load = TransactionsLoad.Loading) }
            loadFirstPage()
        }

        private fun loadFirstPage() {
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    try {
                        val rows = financial.transactions(page = 1, limit = TRANSACTIONS_PAGE_SIZE).transactions
                        val load = if (rows.isEmpty()) TransactionsLoad.Empty else TransactionsLoad.Loaded
                        _uiState.update { it.copy(load = load, rows = rows) }
                    } catch (e: ApiException) {
                        _uiState.update { it.copy(load = TransactionsLoad.Failed(e.toSectionMessage())) }
                    }
                }
        }
    }
