package com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingDetail
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsLaunch
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsNotice
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsTab
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.CancelFailure
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.ClientBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.toCancelFailure
import com.uptrendinvestments.clickmeapp.util.Money
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock

/**
 * Cancel Booking state machine (BOOK-07). Port of iOS `ClientCancellationViewModel` with the Android fixes: the
 * comment is sent (D-05), the preview follows the 24 hour rule (D-06) and the server result is shown (D-07).
 *
 * The screen loads the booking itself by id and keeps the detail in memory only. Step, reason and comment are
 * mirrored to [SavedStateHandle] (D-08) so they survive rotation and process death; unknown saved values fall back to
 * the defaults. The comment is never logged.
 *
 * D-07: the server result is authoritative. The idempotent "already cancelled" 200 is a success. `refund_amount` is in
 * major units, and a real paid cancel returns 0 because the hold is voided (RESEARCH Finding 2), so the notice says
 * "Booking cancelled" unless the response reports a refund and a currency. A 404 or 409 INVALID_STATUS returns to
 * My Bookings (Upcoming) once the dialog is dismissed; any other error keeps the confirm step for a retry.
 *
 * D-05: [confirm] is single flight. The in-flight call completes in the ViewModel scope (the screen blocks Back while
 * [ClientCancellationUiState.isSubmitting], Pitfall 7), so the result hand-off cannot be lost.
 */
@HiltViewModel(assistedFactory = ClientCancellationViewModel.Factory::class)
class ClientCancellationViewModel
    @AssistedInject
    constructor(
        @Assisted key: CancelBookingKey,
        private val savedStateHandle: SavedStateHandle,
        private val repository: ClientBookingsRepository,
        private val clock: Clock,
        private val launchRequest: BookingsLaunchRequest,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(key: CancelBookingKey): ClientCancellationViewModel
        }

        private val bookingId = key.bookingId

        private val _uiState = MutableStateFlow(restoredState())
        val uiState: StateFlow<ClientCancellationUiState> = _uiState.asStateFlow()

        /** The loaded booking, memory only. */
        private var detail: ClientBookingDetail? = null
        private var loadJob: Job? = null

        init {
            load()
        }

        fun retry() = load()

        private fun restoredState(): ClientCancellationUiState {
            val savedStep = savedStateHandle.get<String>(KEY_STEP)
            val savedReason = savedStateHandle.get<String>(KEY_REASON)
            val savedComment = savedStateHandle.get<String>(KEY_COMMENT)
            return ClientCancellationUiState(
                step = CancelStep.entries.firstOrNull { it.name == savedStep } ?: CancelStep.Reason,
                reason =
                    CancellationReason.entries.firstOrNull { it.name == savedReason }
                        ?: CancellationReason.ChangeOfPlans,
                comment = savedComment?.let(::capCancelComment).orEmpty(),
            )
        }

        private fun load() {
            loadJob?.cancel()
            _uiState.update { it.copy(load = CancellationLoad.Loading) }
            loadJob =
                viewModelScope.launch {
                    try {
                        val loaded = repository.detail(bookingId)
                        detail = loaded
                        _uiState.update {
                            it.copy(
                                load = CancellationLoad.Loaded(loaded.toCancellationSummary()),
                                refund =
                                    if (it.step == CancelStep.Confirm) {
                                        refundPreview(loaded, clock.instant())
                                    } else {
                                        it.refund
                                    },
                            )
                        }
                    } catch (e: ApiException) {
                        _uiState.update { it.copy(load = CancellationLoad.Failed(e.toSectionMessage())) }
                    }
                }
        }

        fun selectReason(reason: CancellationReason) {
            if (_uiState.value.isSubmitting) return
            savedStateHandle[KEY_REASON] = reason.name
            _uiState.update { it.copy(reason = reason) }
        }

        fun updateComment(text: String) {
            if (_uiState.value.isSubmitting) return
            val capped = capCancelComment(text)
            savedStateHandle[KEY_COMMENT] = capped
            _uiState.update { it.copy(comment = capped) }
        }

        /** Moves to the confirm step and recomputes the preview now, so the 24 hour boundary uses this moment. */
        fun continueToConfirm() {
            if (_uiState.value.isSubmitting) return
            savedStateHandle[KEY_STEP] = CancelStep.Confirm.name
            _uiState.update {
                it.copy(
                    step = CancelStep.Confirm,
                    refund = detail?.let { d -> refundPreview(d, clock.instant()) } ?: it.refund,
                )
            }
        }

        fun backToReason() {
            if (_uiState.value.isSubmitting) return
            savedStateHandle[KEY_STEP] = CancelStep.Reason.name
            _uiState.update { it.copy(step = CancelStep.Reason) }
        }

        /** Sends the cancel once (D-05) and hands the server's result to My Bookings (D-07). */
        fun confirm() {
            val current = _uiState.value
            if (current.isSubmitting || current.load !is CancellationLoad.Loaded) return
            _uiState.update { it.copy(isSubmitting = true) }
            viewModelScope.launch {
                try {
                    val data = repository.cancel(bookingId, cancelReason(current.reason, current.comment))
                    val currency = data.currency
                    val notice =
                        if (data.refundAmount > 0.0 && currency != null) {
                            BookingsNotice.CancelledWithRefund(Money.formatMajor(data.refundAmount, currency))
                        } else {
                            BookingsNotice.Cancelled
                        }
                    launchRequest.post(BookingsLaunch(BookingsTab.Past, notice))
                    _uiState.update { it.copy(isFinished = true) }
                } catch (e: ApiException) {
                    when (val failure = e.toCancelFailure()) {
                        CancelFailure.Silent -> _uiState.update { it.copy(isSubmitting = false) }
                        is CancelFailure.Show ->
                            _uiState.update {
                                it.copy(
                                    isSubmitting = false,
                                    error = CancelErrorDialog(failure.message, failure.returnToBookings),
                                )
                            }
                    }
                }
            }
        }

        /** Closes the error dialog; when the booking is gone or not cancellable, leaves for My Bookings (Upcoming). */
        fun dismissError() {
            val error = _uiState.value.error ?: return
            if (error.returnToBookings) {
                launchRequest.post(BookingsLaunch(BookingsTab.Upcoming))
                _uiState.update { it.copy(error = null, isFinished = true) }
            } else {
                _uiState.update { it.copy(error = null) }
            }
        }

        fun consumeFinished() {
            _uiState.update { it.copy(isFinished = false) }
        }

        companion object {
            const val KEY_STEP = "cancel_step"
            const val KEY_REASON = "cancel_reason"
            const val KEY_COMMENT = "cancel_comment"
        }
    }
