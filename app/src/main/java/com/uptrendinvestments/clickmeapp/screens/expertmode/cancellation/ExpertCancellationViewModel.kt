package com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.managers.CallGuard
import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingDetail
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.CancelErrorDialog
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.CancelStep
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.capCancelComment
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsLaunch
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsNotice
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsTab
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.CancelFailure
import com.uptrendinvestments.clickmeapp.screens.expertmode.ExpertLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository
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

/**
 * Expert Cancel Booking state machine (EXP-09, D-17). Port of iOS `ExpertCancellationViewModel` into the Phase 8
 * two-step pattern: step 1 picks a reason and an optional comment, step 2 confirms.
 *
 * The screen loads the booking itself by id and keeps the detail in memory only. Step, reason and comment are
 * mirrored to [SavedStateHandle] so they survive rotation and process death; unknown saved values fall back to the
 * defaults. The comment is never logged.
 *
 * [confirm] is single flight and guarded by `CallGuard.attemptModifyBooking`. The server result is authoritative: the
 * notice comes from the response's `refund_amount` (major units), which is 0 for a voided hold, so it normally reads
 * "Booking cancelled".
 */
@HiltViewModel(assistedFactory = ExpertCancellationViewModel.Factory::class)
class ExpertCancellationViewModel
    @AssistedInject
    constructor(
        @Assisted key: ExpertCancelKey,
        private val savedStateHandle: SavedStateHandle,
        private val repository: ExpertBookingsRepository,
        private val launchRequest: ExpertLaunchRequest,
        private val callGuard: CallGuard,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(key: ExpertCancelKey): ExpertCancellationViewModel
        }

        private val bookingId = key.bookingId

        private val _uiState = MutableStateFlow(restoredState())
        val uiState: StateFlow<ExpertCancellationUiState> = _uiState.asStateFlow()

        /** The loaded booking, memory only. */
        private var detail: ExpertBookingDetail? = null
        private var loadJob: Job? = null

        init {
            load()
        }

        fun retry() = load()

        private fun restoredState(): ExpertCancellationUiState {
            val savedStep = savedStateHandle.get<String>(KEY_STEP)
            val savedReason = savedStateHandle.get<String>(KEY_REASON)
            val savedComment = savedStateHandle.get<String>(KEY_COMMENT)
            val reason = ExpertCancelReason.entries.firstOrNull { it.name == savedReason }
            val step = CancelStep.entries.firstOrNull { it.name == savedStep } ?: CancelStep.Reason
            return ExpertCancellationUiState(
                // The confirm step needs a reason, so a lost reason returns to step 1.
                step = if (reason == null) CancelStep.Reason else step,
                reason = reason,
                comment = savedComment?.let(::capCancelComment).orEmpty(),
            )
        }

        /** The client-money line follows the detail payload only (iOS D-14/D-16); it does not depend on the time. */
        private fun previewFor(detail: ExpertBookingDetail): ExpertRefundPreview {
            val fee = detail.session.consultationFee
            return expertRefundPreview(
                amountMajor = fee?.amount,
                currency = fee?.currency,
                authorizationExpiresAt = detail.authorizationExpiresAt,
                paymentActionRequired = detail.paymentActionRequired,
                paymentNotCharged = detail.paymentNotCharged,
            )
        }

        private fun load() {
            loadJob?.cancel()
            _uiState.update { it.copy(load = ExpertCancelLoad.Loading) }
            loadJob =
                viewModelScope.launch {
                    try {
                        val loaded = repository.detail(bookingId)
                        detail = loaded
                        _uiState.update {
                            it.copy(
                                load = ExpertCancelLoad.Loaded(loaded.toExpertCancelSummary()),
                                refund = if (it.step == CancelStep.Confirm) previewFor(loaded) else it.refund,
                            )
                        }
                    } catch (e: ApiException) {
                        _uiState.update { it.copy(load = ExpertCancelLoad.Failed(e.toSectionMessage())) }
                    }
                }
        }

        fun selectReason(reason: ExpertCancelReason) {
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

        /** Moves to the confirm step. Needs a reason, so the disabled Continue button cannot be bypassed. */
        fun continueToConfirm() {
            val current = _uiState.value
            if (current.isSubmitting || current.reason == null) return
            savedStateHandle[KEY_STEP] = CancelStep.Confirm.name
            _uiState.update { it.copy(step = CancelStep.Confirm, refund = detail?.let(::previewFor) ?: it.refund) }
        }

        fun backToReason() {
            if (_uiState.value.isSubmitting) return
            savedStateHandle[KEY_STEP] = CancelStep.Reason.name
            _uiState.update { it.copy(step = CancelStep.Reason) }
        }

        /**
         * Sends the cancel once and hands the server's result to the Bookings tab. Blocked for the booking that is in
         * a call; CallCenter shows the guard dialog.
         */
        fun confirm() {
            val current = _uiState.value
            val reason = current.reason ?: return
            if (current.isSubmitting || current.load !is ExpertCancelLoad.Loaded) return
            if (!callGuard.attemptModifyBooking(bookingId, RestrictedAction.CancelBooking)) return
            _uiState.update { it.copy(isSubmitting = true) }
            viewModelScope.launch {
                try {
                    val data = repository.cancel(bookingId, expertCancelReason(reason, current.comment))
                    finishCancelled(noticeFor(data.refundAmount, data.currency))
                } catch (e: ApiException) {
                    if (e.isAlreadyCancelled()) {
                        finishCancelled(BookingsNotice.Cancelled)
                    } else {
                        showFailure(e.toExpertCancelFailure())
                    }
                }
            }
        }

        private fun showFailure(failure: CancelFailure) {
            when (failure) {
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

        /** Closes the error dialog; when the booking is gone or not cancellable, leaves for the refreshed bookings. */
        fun dismissError() {
            val error = _uiState.value.error ?: return
            if (error.returnToBookings) {
                launchRequest.bookings.post(BookingsLaunch(BookingsTab.Upcoming))
                _uiState.update { it.copy(error = null, isFinished = true) }
            } else {
                _uiState.update { it.copy(error = null) }
            }
        }

        private fun noticeFor(
            refundAmount: Double,
            currency: String?,
        ): BookingsNotice =
            if (refundAmount > 0.0 && currency != null) {
                BookingsNotice.CancelledWithRefund(Money.formatMajor(refundAmount, currency))
            } else {
                BookingsNotice.Cancelled
            }

        private fun finishCancelled(notice: BookingsNotice) {
            launchRequest.bookings.post(BookingsLaunch(BookingsTab.Past, notice))
            _uiState.update { it.copy(isFinished = true) }
        }

        fun consumeFinished() {
            _uiState.update { it.copy(isFinished = false) }
        }

        companion object {
            const val KEY_STEP = "expert_cancel_step"
            const val KEY_REASON = "expert_cancel_reason"
            const val KEY_COMMENT = "expert_cancel_comment"
        }
    }
