package com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.MonotonicClock
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.login.toDialogError
import com.uptrendinvestments.clickmeapp.stripe.SETUP_INTENT_REUSE_WINDOW_MS
import com.uptrendinvestments.clickmeapp.stripe.SheetOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State holder for Payment Methods. Port of iOS `PaymentMethodsViewModel` without its optimistic updates
 * (D-06, D-07): the list always shows what the server returned, reloaded after every successful change.
 *
 * One busy operation (set-default or remove) at a time (D-06). The ViewModel is Stripe-SDK free: the screen's
 * Stripe host consumes [PaymentMethodsUiState.addCardRequest] and reports a [SheetOutcome] back.
 */
@HiltViewModel
class PaymentMethodsViewModel
    @Inject
    constructor(
        private val repository: PaymentMethodsRepository,
        private val monotonicClock: MonotonicClock,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(PaymentMethodsUiState())
        val uiState: StateFlow<PaymentMethodsUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null

        /** A SetupIntent minted but not yet used (sheet cancelled), with its monotonic mint time (research 4). */
        private var reusableSetup: Pair<AddCardRequest, Long>? = null

        init {
            load(showLoading = true)
        }

        fun retry() = load(showLoading = true)

        private fun load(showLoading: Boolean) {
            loadJob?.cancel()
            if (showLoading) _uiState.update { it.copy(list = PaymentMethodsListState.Loading) }
            loadJob =
                viewModelScope.launch {
                    try {
                        val cards = repository.list()
                        _uiState.update { it.copy(list = PaymentMethodsListState.Loaded(cards)) }
                    } catch (e: ApiException) {
                        if (showLoading) {
                            _uiState.update { it.copy(list = PaymentMethodsListState.Failed(e.toSectionMessage())) }
                        }
                    }
                }
        }

        /** Reloads the list after a mutation; a failure keeps the current list and shows nothing. */
        private suspend fun reloadSilently() {
            try {
                val cards = repository.list()
                _uiState.update { it.copy(list = PaymentMethodsListState.Loaded(cards)) }
            } catch (e: ApiException) {
                // Keep the previous list; the next open or retry reloads it.
            }
        }

        private fun isKnownCard(cardId: String): Boolean {
            val list = _uiState.value.list as? PaymentMethodsListState.Loaded ?: return false
            return list.cards.any { it.id == cardId }
        }

        fun setDefault(cardId: String) {
            val state = _uiState.value
            val list = state.list as? PaymentMethodsListState.Loaded
            val card = list?.cards?.firstOrNull { it.id == cardId }
            if (state.busy != null || card == null || card.isDefault) return
            runCardOperation(
                CardOperation(cardId, CardOperationKind.SetDefault),
                R.string.payment_methods_error_update_title,
            ) { repository.setDefault(cardId) }
        }

        /** Called after the user confirms the removal in the screen's dialog (D-06). */
        fun remove(cardId: String) {
            if (_uiState.value.busy != null || !isKnownCard(cardId)) return
            runCardOperation(
                CardOperation(cardId, CardOperationKind.Remove),
                R.string.payment_methods_error_remove_title,
            ) { repository.delete(cardId) }
        }

        private fun runCardOperation(
            operation: CardOperation,
            errorTitle: Int,
            call: suspend () -> Unit,
        ) {
            _uiState.update { it.copy(busy = operation) }
            viewModelScope.launch {
                try {
                    call()
                    reloadSilently()
                    _uiState.update { it.copy(busy = null) }
                } catch (e: ApiException) {
                    _uiState.update { state ->
                        state.copy(
                            busy = null,
                            dialog = e.toDialogError()?.let { PaymentMethodsDialog(errorTitle, it) } ?: state.dialog,
                        )
                    }
                }
            }
        }

        fun addCard() {
            val state = _uiState.value
            if (state.isAddingCard || state.addCardRequest != null) return
            val cached = reusableSetup
            if (cached != null && monotonicClock.nowMillis() - cached.second < SETUP_INTENT_REUSE_WINDOW_MS) {
                _uiState.update { it.copy(isAddingCard = true, addCardRequest = cached.first) }
                return
            }
            reusableSetup = null
            _uiState.update { it.copy(isAddingCard = true) }
            viewModelScope.launch {
                try {
                    val data = repository.createSetupIntent()
                    val request = AddCardRequest(data.clientSecret, data.customerId, data.ephemeralKey)
                    reusableSetup = request to monotonicClock.nowMillis()
                    _uiState.update { it.copy(addCardRequest = request) }
                } catch (e: ApiException) {
                    // No auto-retry: setup-intent is limited to 10 calls per 15 minutes (research correction 4).
                    _uiState.update { state ->
                        state.copy(
                            isAddingCard = false,
                            dialog =
                                e.toDialogError()?.let {
                                    PaymentMethodsDialog(R.string.payment_methods_error_add_title, it)
                                } ?: state.dialog,
                        )
                    }
                }
            }
        }

        /** The screen has taken the request and is presenting the sheet. */
        fun onAddCardLaunched() {
            _uiState.update { it.copy(addCardRequest = null) }
        }

        fun onAddCardResult(outcome: SheetOutcome) {
            when (outcome) {
                SheetOutcome.Completed -> {
                    reusableSetup = null
                    _uiState.update { it.copy(isAddingCard = false) }
                    load(showLoading = false)
                }
                SheetOutcome.Canceled -> _uiState.update { it.copy(isAddingCard = false) }
                is SheetOutcome.Failed -> {
                    reusableSetup = null
                    val message = outcome.message?.let(FieldError::Text) ?: FieldError.Res(R.string.login_error_unknown)
                    _uiState.update {
                        it.copy(
                            isAddingCard = false,
                            dialog = PaymentMethodsDialog(R.string.payment_methods_error_add_title, message),
                        )
                    }
                }
            }
        }

        fun dismissDialog() {
            _uiState.update { it.copy(dialog = null) }
        }
    }
