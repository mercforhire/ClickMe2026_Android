package com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleFailure
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.serverTextOrUnknown
import com.uptrendinvestments.clickmeapp.screens.login.FieldError

private const val HTTP_FORBIDDEN = 403
private const val HTTP_NOT_FOUND = 404

/**
 * Error mapping for the expert's accept and decline of a client's reschedule (EXP-09, RESEARCH C9). Do not reuse the
 * client mapper `toRescheduleFailure`: it is worded for the client and has no `SLOT_UNAVAILABLE` branch, which is the
 * expert's main failure (the database exclusion constraint on the 30-minute session buffer).
 *
 * Branches on [ErrorCode] and then the HTTP status; the message text is never parsed. Server text is only wrapped in
 * [FieldError.Text], which renders as plain text. Every code that means "the booking changed under you" reloads.
 */
fun ApiException.toExpertRescheduleFailure(): RescheduleFailure =
    when (this) {
        is ApiException.Http ->
            when (code) {
                ErrorCode.AccountSuspended -> RescheduleFailure.Silent
                ErrorCode.SlotUnavailable ->
                    RescheduleFailure.Show(
                        FieldError.Res(R.string.expert_reschedule_error_slot_unavailable),
                        reload = true,
                    )
                ErrorCode.Forbidden -> forbidden()
                ErrorCode.InvalidStatus ->
                    RescheduleFailure.Show(FieldError.Res(R.string.reschedule_error_not_pending), reload = true)
                ErrorCode.ValidationError,
                ErrorCode.ClientUnavailable,
                ErrorCode.RescheduleOutsideAuthWindow,
                -> RescheduleFailure.Show(serverTextOrUnknown(), reload = true)
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
