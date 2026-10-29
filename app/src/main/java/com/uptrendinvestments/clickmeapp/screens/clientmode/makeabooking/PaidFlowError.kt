package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.login.FieldError

/**
 * iOS `PaidPhase`. [Confirm] starts once a payment method has been chosen.
 */
enum class PaidPhase { CreateIntent, Confirm }

/** What the booking screen does with a failed paid-flow call. Every failure also resets the paid flow. */
sealed interface PaidFailure {
    /** Nothing to show here: the global suspended-account dialog owns ACCOUNT_SUSPENDED (Phase 2 D-08). */
    data object Silent : PaidFailure

    /**
     * Run the 3DS challenge for [clientSecret] on the expert's connected account [stripeAccount] (PAY-05).
     * `toString()` prints the type name only so the secret cannot reach a log.
     */
    data class StartChallenge(
        val clientSecret: String,
        val stripeAccount: String,
    ) : PaidFailure {
        override fun toString(): String = "StartChallenge"
    }

    /**
     * Show [message] in the error dialog.
     *
     * @property paymentReference a `pi_...` id the dialog appends as "Payment reference: ..." (D-13, D-16).
     * @property newAttemptId the booking attempt id is spent: mint a new one (409 conflict, PI codes, slot taken).
     * @property clearSlotAndReload D-14: drop the selected slot and reload availability.
     * @property resetMeetingType D-14: switch the meeting type back to In-app Voice.
     * @property outcomeUnknown D-16: the confirm call may have charged; keep the attempt id and remember it.
     */
    data class Show(
        val message: FieldError,
        val paymentReference: String? = null,
        val newAttemptId: Boolean = false,
        val clearSlotAndReload: Boolean = false,
        val resetMeetingType: Boolean = false,
        val outcomeUnknown: Boolean = false,
    ) : PaidFailure
}

/**
 * Port of iOS `handlePaidFlowError` (D-13) as a pure function. Server text is only ever wrapped in
 * [FieldError.Text], which is rendered as plain text (T-07-10). The slot and meeting rows deliberately use the
 * free-path copy (D-14).
 *
 * [afterUnknownOutcome] is true when the previous confirm for this same attempt id ended in an unknown outcome:
 * the server then sees the user's own pending booking as the conflict, so a slot-taken answer points to My
 * Bookings (D-19, RESEARCH Pitfall 11a). A failure with [PaidFailure.Show.outcomeUnknown] tells the ViewModel to
 * keep the attempt id and remember the unknown outcome (D-16).
 *
 * A 402 only starts a challenge when `detail` has a client secret and a non-blank `stripe_account`; a missing
 * account is a hard failure and never falls back to the platform account (D-15, T-07-11).
 */
fun ApiException.toPaidFailure(
    phase: PaidPhase,
    paymentIntentId: String?,
    afterUnknownOutcome: Boolean = false,
): PaidFailure {
    val confirming = phase == PaidPhase.Confirm
    val reference = if (confirming) paymentIntentId else null
    return when (this) {
        is ApiException.Http -> toHttpPaidFailure(confirming, reference, afterUnknownOutcome)
        is ApiException.Network ->
            PaidFailure.Show(
                FieldError.Res(R.string.login_error_network),
                paymentReference = reference,
                outcomeUnknown = confirming,
            )
        is ApiException.Decoding ->
            PaidFailure.Show(
                FieldError.Res(R.string.login_error_unknown),
                paymentReference = reference,
                outcomeUnknown = confirming,
            )
    }
}

/**
 * WR-02: an unmapped 5xx on `/bookings/confirm` (a 500 raised after Stripe already authorized, or a proxy 502/504)
 * is an unknown outcome like a timeout (D-16): the booking or the hold may exist, so the attempt id is remembered
 * and a later slot-taken answer points to My Bookings (D-19). Mapped codes keep their own rows.
 */
private fun ApiException.Http.toHttpPaidFailure(
    confirming: Boolean,
    reference: String?,
    afterUnknownOutcome: Boolean,
): PaidFailure {
    val serverText = userMessage?.let { FieldError.Text(it) } ?: FieldError.Res(R.string.login_error_unknown)

    fun res(id: Int) = PaidFailure.Show(FieldError.Res(id))
    return when (code) {
        ErrorCode.AccountSuspended -> PaidFailure.Silent
        ErrorCode.PaymentRequiresAction -> challengeOrFailure(serverText)
        ErrorCode.BookingAttemptConflict ->
            PaidFailure.Show(FieldError.Res(R.string.make_a_booking_error_try_again), newAttemptId = true)
        ErrorCode.IntentExpired, ErrorCode.PiContextMismatch, ErrorCode.PiAmountMismatch ->
            PaidFailure.Show(serverText, newAttemptId = true)
        ErrorCode.SlotUnavailable ->
            PaidFailure.Show(
                FieldError.Res(
                    if (afterUnknownOutcome) {
                        R.string.make_a_booking_error_slot_taken_check_bookings
                    } else {
                        R.string.make_a_booking_error_slot_taken
                    },
                ),
                newAttemptId = true,
                clearSlotAndReload = true,
            )
        ErrorCode.MeetingLinkUnavailable ->
            PaidFailure.Show(FieldError.Res(R.string.make_a_booking_error_no_link), resetMeetingType = true)
        ErrorCode.LongLeadRequiresSavedCard -> res(R.string.make_a_booking_error_paid_window)
        ErrorCode.ExpertNotBookable -> res(R.string.make_a_booking_error_not_bookable)
        ErrorCode.InvalidPaymentMethod -> res(R.string.make_a_booking_error_invalid_payment_method)
        ErrorCode.DirectChargeDisabled -> res(R.string.make_a_booking_error_payments_unavailable)
        ErrorCode.PaymentFailed -> res(R.string.make_a_booking_error_card_declined)
        ErrorCode.DuplicateBooking -> res(R.string.make_a_booking_error_duplicate)
        ErrorCode.AmountBelowMinimum -> res(R.string.make_a_booking_error_amount_below_minimum)
        ErrorCode.InvalidPromo -> res(R.string.make_a_booking_error_invalid_promo)
        ErrorCode.CannotBookSelf -> res(R.string.make_a_booking_error_self)
        else ->
            PaidFailure.Show(
                serverText,
                paymentReference = reference,
                outcomeUnknown = confirming && status >= HTTP_SERVER_ERROR,
            )
    }
}

private const val HTTP_SERVER_ERROR = 500

/** PAY-05 / D-15: a usable 402 needs both the client secret and the connected account. */
private fun ApiException.Http.challengeOrFailure(serverText: FieldError): PaidFailure {
    val clientSecret = (detail?.get("client_secret") as? String)?.takeIf { it.isNotBlank() }
    val stripeAccount = (detail?.get("stripe_account") as? String)?.takeIf { it.isNotBlank() }
    return when {
        clientSecret == null -> PaidFailure.Show(serverText)
        stripeAccount == null -> PaidFailure.Show(FieldError.Res(R.string.make_a_booking_error_sca_unavailable))
        else -> PaidFailure.StartChallenge(clientSecret, stripeAccount)
    }
}
