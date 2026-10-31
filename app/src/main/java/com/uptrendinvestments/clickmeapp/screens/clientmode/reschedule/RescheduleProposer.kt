package com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule

import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.model.requests.RescheduleProposalRequest
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.ClientBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.PaymentMethodsRepository
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/** Result of proposing a new time for a booking. */
sealed interface ProposeOutcome {
    data object Sent : ProposeOutcome

    /** PAYMENT_METHOD_REQUIRED and the client has no saved card: offer "Add a payment method". */
    data object NeedsPaymentMethod : ProposeOutcome

    /** The retry with a saved card was rejected with PAYMENT_METHOD_REQUIRED again. */
    data object CardUnusable : ProposeOutcome

    /** PAYMENT_METHOD_REQUIRED, then loading the saved cards failed. No retry was made. */
    data object CardsUnavailable : ProposeOutcome

    /** Any other failure; the caller maps it to copy so the proposer holds none. */
    data class Failed(
        val error: ApiException,
    ) : ProposeOutcome
}

/**
 * The propose call the reschedule picker drives. Same split as `RescheduleResponding` (Phase 8 D-18): the client
 * implementation ([ClientRescheduleProposer]) retries once with a saved card, the expert implementation never touches
 * cards. [note] arrives already normalized (`normalizeRescheduleNote`, D-15).
 */
interface RescheduleProposing {
    suspend fun propose(
        bookingId: String,
        proposedStart: Instant,
        zone: ZoneId,
        note: String?,
    ): ProposeOutcome
}

/**
 * Client propose with the single saved-card recovery (D-06, D-07, RESCHEDULE-PAYMENT-CONVERSION.md section 3.1).
 *
 * The first PATCH carries no card. PAYMENT_METHOD_REQUIRED does not mean "no card" (a one-off PaymentSheet card is
 * single-use), so only on that code are the saved cards listed and the PATCH retried exactly once with the default
 * card, else the first card. That is at most two PATCH calls and one list call; a second PAYMENT_METHOD_REQUIRED ends
 * in [ProposeOutcome.CardUnusable], never a loop and never a SetupIntent.
 *
 * The card id comes only from the caller's own `GET /me/payment-methods`, lives in a local for one request, and is
 * never logged or stored. Nor is the note. This class never retries after it returns; the caller only submits on a tap
 * (D-08).
 */
class ClientRescheduleProposer
    @Inject
    constructor(
        private val bookings: ClientBookingsRepository,
        private val paymentMethods: PaymentMethodsRepository,
    ) : RescheduleProposing {
        override suspend fun propose(
            bookingId: String,
            proposedStart: Instant,
            zone: ZoneId,
            note: String?,
        ): ProposeOutcome {
            val body =
                RescheduleProposalRequest(
                    proposedStart = proposedStart.toProposedStartWire(),
                    timezone = zone.id,
                    note = note,
                )
            try {
                bookings.reschedule(bookingId, body)
                return ProposeOutcome.Sent
            } catch (e: ApiException) {
                if (!e.isPaymentMethodRequired()) return ProposeOutcome.Failed(e)
            }
            val cards =
                try {
                    paymentMethods.list()
                } catch (_: ApiException) {
                    return ProposeOutcome.CardsUnavailable
                }
            val cardId =
                (cards.firstOrNull { it.isDefault } ?: cards.firstOrNull())?.id
                    ?: return ProposeOutcome.NeedsPaymentMethod
            return try {
                bookings.reschedule(bookingId, body.copy(stripePaymentMethodId = cardId))
                ProposeOutcome.Sent
            } catch (e: ApiException) {
                if (e.isPaymentMethodRequired()) ProposeOutcome.CardUnusable else ProposeOutcome.Failed(e)
            }
        }

        private fun ApiException.isPaymentMethodRequired(): Boolean =
            this is ApiException.Http && code == ErrorCode.PaymentMethodRequired
    }
