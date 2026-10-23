package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.login.FieldError

private const val HTTP_FORBIDDEN = 403
private const val HTTP_NOT_FOUND = 404

/** What the booking detail screens do with a failed accept or decline of a reschedule. */
sealed interface RescheduleFailure {
    /** Nothing to show here: the global suspended-account dialog owns ACCOUNT_SUSPENDED (Phase 2 D-08). */
    data object Silent : RescheduleFailure

    /**
     * Show [message] in the "Couldn't update booking" dialog.
     *
     * @property reload the booking changed under the user, so reload the detail.
     * @property pop the booking is gone, so leave the screen once the dialog is dismissed.
     */
    data class Show(
        val message: FieldError,
        val reload: Boolean = false,
        val pop: Boolean = false,
    ) : RescheduleFailure
}

/** What the cancel flow does with a failed `POST /bookings/:id/cancel`. */
sealed interface CancelFailure {
    /** Nothing to show here: the global suspended-account dialog owns ACCOUNT_SUSPENDED (Phase 2 D-08). */
    data object Silent : CancelFailure

    /**
     * Show [message] in the "Couldn't cancel booking" dialog.
     *
     * @property returnToBookings the booking is gone or no longer cancellable, so go back to My Bookings
     *   instead of staying on the confirm step.
     */
    data class Show(
        val message: FieldError,
        val returnToBookings: Boolean = false,
    ) : CancelFailure
}

private fun ApiException.Http.serverTextOrUnknown(): FieldError =
    userMessage?.let { FieldError.Text(it) } ?: FieldError.Res(R.string.login_error_unknown)

/**
 * Error mapping for the accept and decline reschedule calls, per backend RESCHEDULE-PAYMENT-CONVERSION.md section 3
 * and the Phase 8 RESEARCH error taxonomy (which overrides the UI-SPEC table). Branches on [ErrorCode] and the
 * HTTP status only; the message text is never parsed for logic. Server text is only ever wrapped in
 * [FieldError.Text], which is rendered as plain text (T-06-13).
 */
fun ApiException.toRescheduleFailure(): RescheduleFailure =
    when (this) {
        is ApiException.Http ->
            when (code) {
                ErrorCode.AccountSuspended -> RescheduleFailure.Silent
                ErrorCode.Forbidden -> forbidden()
                ErrorCode.InvalidStatus ->
                    RescheduleFailure.Show(FieldError.Res(R.string.reschedule_error_not_pending), reload = true)
                ErrorCode.ExpertUnavailable ->
                    RescheduleFailure.Show(FieldError.Res(R.string.reschedule_error_expert_unavailable), reload = true)
                ErrorCode.ValidationError ->
                    RescheduleFailure.Show(FieldError.Res(R.string.reschedule_error_time_passed), reload = true)
                ErrorCode.RescheduleOutsideAuthWindow ->
                    RescheduleFailure.Show(
                        userMessage?.let { FieldError.Text(it) }
                            ?: FieldError.Res(R.string.reschedule_error_outside_window),
                        reload = true,
                    )
                ErrorCode.InvalidPaymentMethod -> RescheduleFailure.Show(serverTextOrUnknown())
                // Defensive: the D-02 recovery normally handles PAYMENT_METHOD_REQUIRED before this mapper.
                ErrorCode.PaymentMethodRequired ->
                    RescheduleFailure.Show(FieldError.Res(R.string.reschedule_card_unusable_message))
                ErrorCode.NotFound -> notFound()
                else ->
                    when (status) {
                        HTTP_FORBIDDEN -> forbidden()
                        HTTP_NOT_FOUND -> notFound()
                        else -> RescheduleFailure.Show(serverTextOrUnknown())
                    }
            }
        is ApiException.Network -> RescheduleFailure.Show(FieldError.Res(R.string.login_error_network))
        is ApiException.Decoding -> RescheduleFailure.Show(FieldError.Res(R.string.login_error_unknown))
    }

private fun forbidden() = RescheduleFailure.Show(FieldError.Res(R.string.reschedule_error_forbidden), reload = true)

private fun notFound() = RescheduleFailure.Show(FieldError.Res(R.string.reschedule_error_not_found), pop = true)

/**
 * Error mapping for the cancel call (D-07): a missing or no-longer-cancellable booking returns to My Bookings,
 * everything else stays on the confirm step. Branches on [ErrorCode] and the HTTP status only.
 */
fun ApiException.toCancelFailure(): CancelFailure =
    when (this) {
        is ApiException.Http ->
            when {
                code == ErrorCode.AccountSuspended -> CancelFailure.Silent
                code == ErrorCode.NotFound || status == HTTP_NOT_FOUND ->
                    CancelFailure.Show(FieldError.Res(R.string.cancel_booking_error_not_found), returnToBookings = true)
                code == ErrorCode.InvalidStatus ->
                    CancelFailure.Show(
                        FieldError.Res(R.string.cancel_booking_error_not_cancellable),
                        returnToBookings = true,
                    )
                else -> CancelFailure.Show(serverTextOrUnknown())
            }
        is ApiException.Network -> CancelFailure.Show(FieldError.Res(R.string.login_error_network))
        is ApiException.Decoding -> CancelFailure.Show(FieldError.Res(R.string.login_error_unknown))
    }
