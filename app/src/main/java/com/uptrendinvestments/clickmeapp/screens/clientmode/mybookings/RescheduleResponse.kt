package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings

import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.capNotes
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.PaymentMethodsRepository
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
    ) {
        suspend fun accept(bookingId: String): AcceptOutcome {
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

        suspend fun decline(
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
