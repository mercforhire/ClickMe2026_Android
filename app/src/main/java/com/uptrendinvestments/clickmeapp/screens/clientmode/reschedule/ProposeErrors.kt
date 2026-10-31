package com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.serverTextOrUnknown
import com.uptrendinvestments.clickmeapp.screens.login.FieldError

private const val HTTP_NOT_FOUND = 404

/**
 * What the reschedule picker does with a failed propose call.
 *
 * Propose-specific copy per D-13 and UI-SPEC's error table. The accept-side mappers in
 * `mybookings/RescheduleErrors.kt` and `bookingsummary/ExpertRescheduleErrors.kt` are deliberately not reused: their
 * copy is accept-worded and they reload instead of clearing the slot.
 */
sealed interface ProposeFailure {
    /** Nothing to show here: the global suspended-account dialog owns ACCOUNT_SUSPENDED (Phase 2 D-08). */
    data object Silent : ProposeFailure

    /**
     * Show [message] in the "Couldn't reschedule booking" dialog.
     *
     * @property clearSlot after OK the selected slot is cleared; the date and message are kept (UI-SPEC "After OK").
     * @property reloadSlots after OK the slots for the date are reloaded (the expert's time changed under the user).
     * @property leave after OK the picker is popped so the origin screen refreshes (booking gone or not movable).
     */
    data class Show(
        val message: FieldError,
        val clearSlot: Boolean = false,
        val reloadSlots: Boolean = false,
        val leave: Boolean = false,
    ) : ProposeFailure
}

/**
 * Client propose error mapping (D-13, RS-C7). Branches on [ErrorCode] and the HTTP status only, never on message text;
 * server text is only ever wrapped in [FieldError.Text], which is rendered as plain text (T-16.2-13).
 *
 * RESCHEDULE_OUTSIDE_AUTH_WINDOW is terminal for this move, not a field error, and shows the fixed iOS copy whatever
 * the server says. RESCHEDULE_TOO_FAR_AHEAD is the D-01 server backstop (time can pass between picking a slot and
 * submitting); with VALIDATION_ERROR it shows the server text and clears the slot.
 */
fun ApiException.toClientProposeFailure(): ProposeFailure =
    when (this) {
        is ApiException.Http ->
            when (code) {
                ErrorCode.AccountSuspended -> ProposeFailure.Silent
                ErrorCode.RescheduleOutsideAuthWindow ->
                    ProposeFailure.Show(FieldError.Res(R.string.reschedule_start_error_outside_window_client))
                ErrorCode.PaymentMethodRequired ->
                    ProposeFailure.Show(FieldError.Res(R.string.reschedule_card_unusable_message))
                else -> commonProposeFailure(R.string.reschedule_error_expert_unavailable)
            }
        is ApiException.Network -> ProposeFailure.Show(FieldError.Res(R.string.login_error_network))
        is ApiException.Decoding -> ProposeFailure.Show(FieldError.Res(R.string.login_error_unknown))
    }

/**
 * Expert propose error mapping. Same table as the client except the fixed copy for RESCHEDULE_OUTSIDE_AUTH_WINDOW and
 * the slot-taken message, and PAYMENT_METHOD_REQUIRED shows the server text (experts are never gated on a card).
 */
fun ApiException.toExpertProposeFailure(): ProposeFailure =
    when (this) {
        is ApiException.Http ->
            when (code) {
                ErrorCode.AccountSuspended -> ProposeFailure.Silent
                ErrorCode.RescheduleOutsideAuthWindow ->
                    ProposeFailure.Show(FieldError.Res(R.string.reschedule_start_error_outside_window_expert))
                else -> commonProposeFailure(R.string.expert_reschedule_error_slot_unavailable)
            }
        is ApiException.Network -> ProposeFailure.Show(FieldError.Res(R.string.login_error_network))
        is ApiException.Decoding -> ProposeFailure.Show(FieldError.Res(R.string.login_error_unknown))
    }

/** The rows both roles share; [slotTakenMessage] is the role's copy for EXPERT_UNAVAILABLE and SLOT_UNAVAILABLE. */
private fun ApiException.Http.commonProposeFailure(slotTakenMessage: Int): ProposeFailure =
    when (code) {
        ErrorCode.RescheduleTooFarAhead ->
            ProposeFailure.Show(
                userMessage?.let { FieldError.Text(it) }
                    ?: FieldError.Res(R.string.reschedule_start_error_too_far_ahead),
                clearSlot = true,
            )
        ErrorCode.ValidationError ->
            ProposeFailure.Show(
                userMessage?.let { FieldError.Text(it) }
                    ?: FieldError.Res(R.string.reschedule_start_error_time_passed),
                clearSlot = true,
            )
        ErrorCode.ExpertUnavailable, ErrorCode.SlotUnavailable ->
            ProposeFailure.Show(FieldError.Res(slotTakenMessage), clearSlot = true, reloadSlots = true)
        ErrorCode.InvalidStatus ->
            ProposeFailure.Show(FieldError.Res(R.string.reschedule_start_error_invalid_status), leave = true)
        ErrorCode.NotFound -> notFound()
        else -> if (status == HTTP_NOT_FOUND) notFound() else ProposeFailure.Show(serverTextOrUnknown())
    }

private fun notFound() = ProposeFailure.Show(FieldError.Res(R.string.reschedule_error_not_found), leave = true)
