package com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings

import com.uptrendinvestments.clickmeapp.model.requests.AcceptRescheduleRequest
import com.uptrendinvestments.clickmeapp.model.requests.CancelBookingRequest
import com.uptrendinvestments.clickmeapp.model.requests.DeclineRescheduleRequest
import com.uptrendinvestments.clickmeapp.model.requests.ExpertRescheduleProposalRequest
import com.uptrendinvestments.clickmeapp.model.responses.CancelBookingData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingDetail
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingsPayload
import com.uptrendinvestments.clickmeapp.model.responses.RescheduleResponseData
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The signed-in expert's own bookings (`/expert/bookings`). Reads: [list] and [detail] (which loads
 * `.../details`, never the bare booking id). Writes: [cancel], [acceptReschedule], [declineReschedule] and [reschedule]. Mirrors
 * iOS `Expert Mode/Upcoming Bookings`. Every call throws [com.uptrendinvestments.clickmeapp.network.ApiException]
 * only.
 *
 * Stateless by design: nothing is cached, because nothing clears singletons on logout. The server scopes every call
 * to the caller. Cancel and decline reasons are user-written and never logged.
 */
@Singleton
class ExpertBookingsRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
    ) {
        /** One page of the [type] feed (`upcoming` or `past`); the default page size covers a whole bucket (D-15). */
        suspend fun list(
            type: String,
            page: Int = PAGE,
            limit: Int = PAGE_SIZE,
        ): ExpertBookingsPayload = apiCall { api.getExpertBookings(type, page, limit) }.data

        suspend fun detail(bookingId: String): ExpertBookingDetail =
            apiCall { api.getExpertBookingDetails(bookingId) }.data

        /** [reason] is "label: comment", built and capped at 500 by the caller. */
        suspend fun cancel(
            bookingId: String,
            reason: String,
        ): CancelBookingData = apiCall { api.cancelExpertBooking(bookingId, CancelBookingRequest(reason)) }.data

        /** Always sends `{}`: the expert never supplies a card, and the strict schema rejects a missing body. */
        suspend fun acceptReschedule(bookingId: String): RescheduleResponseData =
            apiCall { api.acceptExpertReschedule(bookingId, AcceptRescheduleRequest()) }.data

        /** A null [reason] sends `{}`; otherwise `{"reason":"..."}` (at most 500 characters, capped by the caller). */
        suspend fun declineReschedule(
            bookingId: String,
            reason: String?,
        ): RescheduleResponseData =
            apiCall { api.declineExpertReschedule(bookingId, DeclineRescheduleRequest(reason)) }.data

        /** Proposes a new time (Phase 16.2). The body has no card property; the note is never logged. */
        suspend fun reschedule(
            bookingId: String,
            body: ExpertRescheduleProposalRequest,
        ): RescheduleResponseData = apiCall { api.rescheduleExpertBooking(bookingId, body) }.data

        companion object {
            const val TYPE_UPCOMING = "upcoming"
            const val TYPE_PAST = "past"
            const val PAGE = 1
            const val PAGE_SIZE = 50
        }
    }
