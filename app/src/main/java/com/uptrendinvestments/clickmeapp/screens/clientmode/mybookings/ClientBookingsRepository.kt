package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings

import com.uptrendinvestments.clickmeapp.model.requests.AcceptRescheduleRequest
import com.uptrendinvestments.clickmeapp.model.requests.CancelBookingRequest
import com.uptrendinvestments.clickmeapp.model.requests.DeclineRescheduleRequest
import com.uptrendinvestments.clickmeapp.model.requests.RescheduleProposalRequest
import com.uptrendinvestments.clickmeapp.model.responses.CancelBookingData
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingDetail
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingsPayload
import com.uptrendinvestments.clickmeapp.model.responses.RescheduleResponseData
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The signed-in client's own bookings. Reads: [list] (`GET /client/bookings`, Explore Today's Sessions and the My
 * Bookings tabs) and [detail] (`GET /client/bookings/{id}`, Request Sent, Upcoming Booking Details, Cancellation
 * and Booking Summary). Writes: [cancel], [acceptReschedule], [declineReschedule] and [reschedule].
 *
 * The server scopes every call to the caller (404 otherwise, 403 for a proposal the expert did not make). Reason
 * text and `pm_` ids are passed through unchanged and never logged.
 */
@Singleton
class ClientBookingsRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
    ) {
        suspend fun list(
            type: String,
            page: Int = 1,
            limit: Int = 20,
        ): ClientBookingsPayload = apiCall { api.getClientBookings(type, page, limit) }.data

        suspend fun detail(bookingId: String): ClientBookingDetail =
            apiCall { api.getClientBookingDetail(bookingId) }.data

        /** [reason] is "label: comment", built and capped at 500 by the caller (D-05). */
        suspend fun cancel(
            bookingId: String,
            reason: String,
        ): CancelBookingData = apiCall { api.cancelClientBooking(bookingId, CancelBookingRequest(reason)) }.data

        /** A null [paymentMethodId] sends `{}`; otherwise the saved `pm_...` id (D-02). */
        suspend fun acceptReschedule(
            bookingId: String,
            paymentMethodId: String? = null,
        ): RescheduleResponseData =
            apiCall { api.acceptClientReschedule(bookingId, AcceptRescheduleRequest(paymentMethodId)) }.data

        /**
         * Proposes a new time (Phase 16.2). The body is built by the proposer; the card id and the note are passed
         * through and never logged.
         */
        suspend fun reschedule(
            bookingId: String,
            body: RescheduleProposalRequest,
        ): RescheduleResponseData = apiCall { api.rescheduleClientBooking(bookingId, body) }.data

        /** [reason] is trimmed and capped at 500 by the caller (D-04); null sends `{}`. */
        suspend fun declineReschedule(
            bookingId: String,
            reason: String?,
        ): RescheduleResponseData =
            apiCall { api.declineClientReschedule(bookingId, DeclineRescheduleRequest(reason)) }.data

        companion object {
            const val TYPE_UPCOMING = "upcoming"
            const val TYPE_PAST = "past"
        }
    }
