package com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.serverTextOrUnknown
import com.uptrendinvestments.clickmeapp.screens.login.FieldError

/** What Request Decision does with a failed accept or decline. */
sealed interface RequestDecisionFailure {
    /** Nothing to show here: the global suspended-account dialog owns ACCOUNT_SUSPENDED (Phase 2 D-08). */
    data object Silent : RequestDecisionFailure

    /**
     * 409 ALREADY_PROCESSED: the request was handled, expired or cancelled by the client in the meantime. Show
     * [message], then leave for a refreshed Incoming Requests list. Never a success (T-11-37).
     */
    data class AlreadyProcessed(
        val message: FieldError,
    ) : RequestDecisionFailure

    /** Any other failure: show [message] and stay, keeping the screen and the sheet inputs (D-07). */
    data class Show(
        val message: FieldError,
    ) : RequestDecisionFailure
}

/**
 * Error mapping for `POST expert/booking-requests/{id}/accept` and `/decline`. Branches on [ErrorCode] only; the
 * server text is only ever wrapped in [FieldError.Text], which renders as plain text. A server message that is
 * missing falls back to the generic copy, a lost connection to the network copy.
 */
fun ApiException.toRequestDecisionFailure(): RequestDecisionFailure =
    when (this) {
        is ApiException.Http ->
            when (code) {
                ErrorCode.AccountSuspended -> RequestDecisionFailure.Silent
                ErrorCode.AlreadyProcessed -> RequestDecisionFailure.AlreadyProcessed(serverTextOrUnknown())
                else -> RequestDecisionFailure.Show(serverTextOrUnknown())
            }
        is ApiException.Network -> RequestDecisionFailure.Show(FieldError.Res(R.string.login_error_network))
        is ApiException.Decoding -> RequestDecisionFailure.Show(FieldError.Res(R.string.login_error_unknown))
    }
