package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.capNotes
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.PaymentMethodsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Result of accepting a reschedule proposal. */
sealed interface AcceptOutcome {
    data object Accepted : AcceptOutcome

    /** PAYMENT_METHOD_REQUIRED and the client has no saved card: offer "Add a payment method". */
    data object NeedsPaymentMethod : AcceptOutcome

    /** The retry with a saved card was rejected with PAYMENT_METHOD_REQUIRED again. */
    data object CardUnusable : AcceptOutcome

    /** PAYMENT_METHOD_REQUIRED, then loading the saved cards failed. No retry was made. */
    data object CardsUnavailable : AcceptOutcome

    data class Failed(
        val failure: RescheduleFailure,
    ) : AcceptOutcome
}

/** Result of declining a reschedule proposal. */
sealed interface DeclineOutcome {
    data object Declined : DeclineOutcome

    data class Failed(
        val failure: RescheduleFailure,
    ) : DeclineOutcome
}

/** The decline reason as sent to the backend: trimmed, capped at 500, and null when blank (D-04). */
fun normalizeDeclineReason(raw: String?): String? =
    raw?.trim()?.takeIf { it.isNotEmpty() }?.let { capNotes(it, DECLINE_REASON_MAX_CHARS) }

/**
 * The accept and decline calls [RescheduleResponseController] drives. The client implementation is
 * [RescheduleResponder]; the expert implementation (Phase 11, 11-13) has no saved-card retry. Keeping the controller
 * typed to this interface means the in-flight, dialog and notice logic exists once (D-18).
 */
interface RescheduleResponding {
    suspend fun accept(bookingId: String): AcceptOutcome

    suspend fun decline(
        bookingId: String,
        reason: String?,
    ): DeclineOutcome
}

/**
 * Stateless accept and decline of a reschedule proposal, shared by Upcoming Booking Details and Booking Summary
 * (D-01).
 *
 * Accept (D-02, iOS parity with `BookingSummaryViewModel.acceptReschedule`): send `{}` first. The backend check is
 * DB-only (RESCHEDULE-PAYMENT-CONVERSION.md section 3.1), so PAYMENT_METHOD_REQUIRED does not mean the client has no
 * saved cards. On that code the saved cards are loaded and the accept is retried exactly once with the default card,
 * else the first card. A second PAYMENT_METHOD_REQUIRED ends in [AcceptOutcome.CardUnusable]; there is never a loop.
 *
 * Decline sends the trimmed reason (D-04). Repeating either call is safe: the server answers the repeat with an
 * idempotent 200. Neither the `pm_` id nor the reason is ever logged.
 */
class RescheduleResponder
    @Inject
    constructor(
        private val bookings: ClientBookingsRepository,
        private val paymentMethods: PaymentMethodsRepository,
    ) : RescheduleResponding {
        override suspend fun accept(bookingId: String): AcceptOutcome {
            try {
                bookings.acceptReschedule(bookingId)
                return AcceptOutcome.Accepted
            } catch (e: ApiException) {
                if (!e.isPaymentMethodRequired()) return AcceptOutcome.Failed(e.toRescheduleFailure())
            }
            val cards =
                try {
                    paymentMethods.list()
                } catch (_: ApiException) {
                    return AcceptOutcome.CardsUnavailable
                }
            val cardId =
                (cards.firstOrNull { it.isDefault } ?: cards.firstOrNull())?.id
                    ?: return AcceptOutcome.NeedsPaymentMethod
            return try {
                bookings.acceptReschedule(bookingId, cardId)
                AcceptOutcome.Accepted
            } catch (e: ApiException) {
                if (e.isPaymentMethodRequired()) {
                    AcceptOutcome.CardUnusable
                } else {
                    AcceptOutcome.Failed(e.toRescheduleFailure())
                }
            }
        }

        override suspend fun decline(
            bookingId: String,
            reason: String?,
        ): DeclineOutcome =
            try {
                bookings.declineReschedule(bookingId, normalizeDeclineReason(reason))
                DeclineOutcome.Declined
            } catch (e: ApiException) {
                DeclineOutcome.Failed(e.toRescheduleFailure())
            }

        private fun ApiException.isPaymentMethodRequired(): Boolean =
            this is ApiException.Http && code == ErrorCode.PaymentMethodRequired
    }

/**
 * In-flight, dialog and notice state for responding to a reschedule proposal. Both detail ViewModels create one in
 * `init` with `viewModelScope` and their own quiet [reload], so the logic exists once (D-01). A result that arrives
 * after the user left is dropped with `viewModelScope` (UI-SPEC navigation table).
 *
 * Not optimistic: no field of the booking is touched here. After any response the booking is reloaded first and only
 * then is the dialog or notice set. `submitting` stays set until the reload finished, so the card and Cancel stay
 * disabled, and a second accept or decline while one is in flight does nothing.
 */
class RescheduleResponseController(
    private val scope: CoroutineScope,
    private val responder: RescheduleResponding,
    private val bookingId: String,
    private val reload: suspend () -> Unit,
) {
    private val _state = MutableStateFlow(RescheduleUiState())
    val state: StateFlow<RescheduleUiState> = _state.asStateFlow()

    fun accept() {
        if (_state.value.isSubmitting) return
        _state.update { it.copy(submitting = RescheduleSubmitting.Accepting) }
        scope.launch {
            try {
                when (val outcome = responder.accept(bookingId)) {
                    AcceptOutcome.Accepted -> succeed(RescheduleNotice.Accepted)
                    AcceptOutcome.NeedsPaymentMethod -> showDialog(RescheduleDialog.PaymentMethodNeeded)
                    AcceptOutcome.CardUnusable -> showDialog(RescheduleDialog.CardUnusable)
                    AcceptOutcome.CardsUnavailable ->
                        showDialog(
                            RescheduleDialog.Error(FieldError.Res(R.string.reschedule_error_cards_unavailable)),
                        )
                    is AcceptOutcome.Failed -> fail(outcome.failure)
                }
            } finally {
                _state.update { it.copy(submitting = RescheduleSubmitting.None) }
            }
        }
    }

    fun decline(reason: String) {
        if (_state.value.isSubmitting) return
        _state.update { it.copy(submitting = RescheduleSubmitting.Declining) }
        scope.launch {
            try {
                when (val outcome = responder.decline(bookingId, reason)) {
                    DeclineOutcome.Declined -> succeed(RescheduleNotice.Declined)
                    is DeclineOutcome.Failed -> fail(outcome.failure)
                }
            } finally {
                _state.update { it.copy(submitting = RescheduleSubmitting.None) }
            }
        }
    }

    fun dismissDialog() {
        _state.update { it.copy(dialog = null) }
    }

    fun consumeNotice() {
        _state.update { it.copy(notice = null) }
    }

    private suspend fun succeed(notice: RescheduleNotice) {
        reload()
        _state.update { it.copy(dialog = null, notice = notice) }
    }

    private fun showDialog(dialog: RescheduleDialog) {
        _state.update { it.copy(dialog = dialog) }
    }

    private suspend fun fail(failure: RescheduleFailure) {
        when (failure) {
            RescheduleFailure.Silent -> Unit
            is RescheduleFailure.Show -> {
                if (failure.reload) reload()
                showDialog(RescheduleDialog.Error(failure.message, popOnDismiss = failure.pop))
            }
        }
    }
}
