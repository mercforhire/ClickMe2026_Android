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
 * (D-06, D-07): the list shows what the server returned, reloaded after every successful change. Only when that
 * reload fails is the confirmed change applied locally (WR-07).
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

        /** Bumped by every list fetch; only the newest fetch may apply its answer (WR-07). */
        private var listGeneration = 0

        /** A SetupIntent minted but not yet used (sheet cancelled), with its monotonic mint time (research 4). */
        private var reusableSetup: Pair<AddCardRequest, Long>? = null

        init {
            load(showLoading = true)
        }

        fun retry() = load(showLoading = true)

        /**
         * [showLoading] is the first load and Retry: the list shows the spinner, a failure the error state. Without
         * it this is the reload after a card was added: the current list stays, and a failure says the card was
         * saved but the list could not be refreshed, instead of silently leaving the new card out (WR-07).
         */
        private fun load(showLoading: Boolean) {
            loadJob?.cancel()
            val generation = ++listGeneration
            if (showLoading) _uiState.update { it.copy(list = PaymentMethodsListState.Loading) }
            loadJob =
                viewModelScope.launch {
                    try {
                        val cards = repository.list()
                        if (generation == listGeneration) {
                            _uiState.update { it.copy(list = PaymentMethodsListState.Loaded(cards)) }
                        }
                    } catch (e: ApiException) {
                        if (generation != listGeneration) return@launch
                        if (showLoading) {
                            _uiState.update { it.copy(list = PaymentMethodsListState.Failed(e.toSectionMessage())) }
                        } else if (e.toDialogError() != null) {
                            _uiState.update {
                                it.copy(
                                    dialog =
                                        PaymentMethodsDialog(
                                            R.string.payment_methods_error_load_heading,
                                            FieldError.Res(R.string.payment_methods_error_refresh_after_add),
                                        ),
                                )
                            }
                        }
                    }
                }
        }

        /**
         * Reloads the list after a successful set-default or remove (D-07: the server's answer, including its
         * default). WR-07: it supersedes any older load, and a load started after it supersedes it, so an older
         * answer can never put a removed card back. If the reload fails, the change the server already confirmed is
         * shown anyway (as iOS does locally): a removed card disappears, a new default gets the badge. Another card
         * is never promoted to default by the app (D-07).
         */
        private suspend fun reloadAfter(operation: CardOperation) {
            loadJob?.cancel()
            val generation = ++listGeneration
            try {
                val cards = repository.list()
                if (generation == listGeneration) {
                    _uiState.update { it.copy(list = PaymentMethodsListState.Loaded(cards)) }
                }
            } catch (e: ApiException) {
                if (generation == listGeneration) applyConfirmedChange(operation)
            }
        }

        private fun applyConfirmedChange(operation: CardOperation) {
            _uiState.update { state ->
                val list = state.list as? PaymentMethodsListState.Loaded ?: return@update state
                val id = operation.cardId
                val cards =
                    when (operation.kind) {
                        CardOperationKind.Remove -> list.cards.filterNot { it.id == id }
                        CardOperationKind.SetDefault -> list.cards.map { it.copy(isDefault = it.id == id) }
                    }
                state.copy(list = PaymentMethodsListState.Loaded(cards))
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
                    reloadAfter(operation)
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
