package com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.CancelFailure
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.serverTextOrUnknown
import com.uptrendinvestments.clickmeapp.screens.login.FieldError

private const val HTTP_NOT_FOUND = 404

/**
 * The lost race: another cancel won while this one was in flight (409 ALREADY_CANCELLED, RESEARCH C8). The booking is
 * cancelled, which is what the expert wanted, so the flow treats it as a success.
 */
fun ApiException.isAlreadyCancelled(): Boolean = this is ApiException.Http && code == ErrorCode.AlreadyCancelled

/**
 * Error mapping for the expert cancel call (D-17, RESEARCH C8). A missing booking (404) or one that can no longer be
 * cancelled (409 INVALID_STATUS, which is also what a second cancel returns) goes back to the refreshed bookings
 * list; everything else stays on the confirm step for a retry. Branches on [ErrorCode] and the HTTP status only;
 * server text is only ever wrapped in [FieldError.Text].
 */
fun ApiException.toExpertCancelFailure(): CancelFailure =
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
