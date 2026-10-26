package com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests

import com.uptrendinvestments.clickmeapp.model.requests.AcceptBookingRequestBody
import com.uptrendinvestments.clickmeapp.model.requests.DeclineBookingRequestBody
import com.uptrendinvestments.clickmeapp.model.responses.AcceptBookingRequestData
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestDetail
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestItem
import com.uptrendinvestments.clickmeapp.model.responses.DeclineBookingRequestData
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The signed-in expert's pending booking requests (`/expert/booking-requests`). Mirrors iOS
 * `Expert Mode/Incoming Requests`. Every call throws [com.uptrendinvestments.clickmeapp.network.ApiException] only.
 *
 * Stateless by design: nothing is cached, because nothing clears singletons on logout and a cache would show the
 * previous expert's requests to the next one. The client message and the decline reason text are user-written, so
 * they are never logged.
 */
@Singleton
class BookingRequestsRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
    ) {
        /** `GET /expert/booking-requests`: every pending request in server order (expiration first). */
        suspend fun list(): List<BookingRequestItem> = apiCall { api.getBookingRequests() }.data.requests

        /** `GET /expert/booking-requests/{id}`. */
        suspend fun detail(requestId: String): BookingRequestDetail = apiCall { api.getBookingRequest(requestId) }.data

        /** `POST /expert/booking-requests/{id}/accept`. A null [message] is omitted, so the body is `{}`. */
        suspend fun accept(
            requestId: String,
            message: String?,
        ): AcceptBookingRequestData =
            apiCall { api.acceptBookingRequest(requestId, AcceptBookingRequestBody(message)) }.data

        /** `POST /expert/booking-requests/{id}/decline`. Both [reasonCode] and [reasonText] are required. */
        suspend fun decline(
            requestId: String,
            reasonCode: String,
            reasonText: String,
        ): DeclineBookingRequestData =
            apiCall { api.declineBookingRequest(requestId, DeclineBookingRequestBody(reasonCode, reasonText)) }.data
    }
