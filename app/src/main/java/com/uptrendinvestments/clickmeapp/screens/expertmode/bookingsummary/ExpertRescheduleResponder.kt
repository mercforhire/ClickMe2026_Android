package com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary

import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.AcceptOutcome
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.DeclineOutcome
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleResponding
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.normalizeDeclineReason
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository
import javax.inject.Inject

/**
 * The expert's accept and decline of a client's reschedule request (EXP-09, D-18), behind the shared
 * `RescheduleResponseController`.
 *
 * Unlike the client responder there is no payment step and so no saved-card retry: accept sends `{}` once and any
 * failure is mapped by [toExpertRescheduleFailure]. Repeating either call is safe because the server answers a
 * repeat with an idempotent 200, which is success here. The decline reason is trimmed and capped by
 * [normalizeDeclineReason], and is never logged.
 */
class ExpertRescheduleResponder
    @Inject
    constructor(
        private val bookings: ExpertBookingsRepository,
    ) : RescheduleResponding {
        override suspend fun accept(bookingId: String): AcceptOutcome =
            try {
                bookings.acceptReschedule(bookingId)
                AcceptOutcome.Accepted
            } catch (e: ApiException) {
                AcceptOutcome.Failed(e.toExpertRescheduleFailure())
            }

        override suspend fun decline(
            bookingId: String,
            reason: String?,
        ): DeclineOutcome =
            try {
                bookings.declineReschedule(bookingId, normalizeDeclineReason(reason))
                DeclineOutcome.Declined
            } catch (e: ApiException) {
                DeclineOutcome.Failed(e.toExpertRescheduleFailure())
            }
    }
