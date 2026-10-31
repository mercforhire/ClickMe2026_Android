package com.uptrendinvestments.clickmeapp.screens.expertmode.reschedule

import com.uptrendinvestments.clickmeapp.model.requests.ExpertRescheduleProposalRequest
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.ProposeOutcome
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RescheduleProposing
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.toProposedStartWire
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/**
 * Expert propose: one PATCH, never a card. Mirrors `ExpertRescheduleResponder`; locked by
 * RESCHEDULE-PAYMENT-CONVERSION.md sections 4 and 6 (experts are never gated on a card). Its only dependency is the
 * expert bookings repository and [ExpertRescheduleProposalRequest] cannot carry a card id, so even a server answer of
 * PAYMENT_METHOD_REQUIRED is just [ProposeOutcome.Failed]. The note is never logged.
 */
class ExpertRescheduleProposer
    @Inject
    constructor(
        private val bookings: ExpertBookingsRepository,
    ) : RescheduleProposing {
        override suspend fun propose(
            bookingId: String,
            proposedStart: Instant,
            zone: ZoneId,
            note: String?,
        ): ProposeOutcome =
            try {
                bookings.reschedule(
                    bookingId,
                    ExpertRescheduleProposalRequest(proposedStart.toProposedStartWire(), zone.id, note),
                )
                ProposeOutcome.Sent
            } catch (e: ApiException) {
                ProposeOutcome.Failed(e)
            }
    }
