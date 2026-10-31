package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import com.uptrendinvestments.clickmeapp.model.requests.BookingRequestBody
import com.uptrendinvestments.clickmeapp.model.requests.ConfirmBookingRequest
import com.uptrendinvestments.clickmeapp.model.requests.PaymentIntentRequest
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestData
import com.uptrendinvestments.clickmeapp.model.responses.ConfirmBookingData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertAvailabilityData
import com.uptrendinvestments.clickmeapp.model.responses.PaymentIntentResponse
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Booking reads and writes. Every call throws [com.uptrendinvestments.clickmeapp.network.ApiException] only.
 *
 * `topic_id` is never sent on the availability call: the backend would narrow the grid to the topic duration,
 * but iOS never sends it (D-07 iOS parity, user decision on research Open Question 2), so the grid always
 * shows the expert's base slots and the server re-checks the duration when the booking is requested.
 * Phase 16.2: the reschedule pickers pass `excludeBookingId`.
 *
 * Three booking writes: the free request ([requestBooking]), the payment-intent call for short-lead paid
 * bookings ([paymentIntent], PAY-03; a 422 LONG_LEAD_REQUIRES_SAVED_CARD is now shown as an error, quick
 * 261028-to1) and the server-side confirm ([confirm], Path A).
 */
@Singleton
class BookingRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
    ) {
        /**
         * One month of the expert's availability grid (`yyyy-MM`), without a topic filter. [excludeBookingId] is
         * null for Make a Booking (request unchanged); the reschedule pickers pass the booking being moved.
         */
        suspend fun availability(
            expertId: String,
            month: YearMonth,
            excludeBookingId: String? = null,
        ): ExpertAvailabilityData =
            apiCall { api.getExpertAvailability(expertId, month.toString(), null, excludeBookingId) }.data

        /** `POST /bookings/request`, free path. Paid bookings go through [paymentIntent] and [confirm]. */
        suspend fun requestBooking(body: BookingRequestBody): BookingRequestData =
            apiCall { api.requestBooking(body) }.data

        /** `POST /bookings/payment-intent`. The response is a bare body (no envelope, no amounts). */
        suspend fun paymentIntent(request: PaymentIntentRequest): PaymentIntentResponse =
            apiCall { api.createPaymentIntent(request) }

        /**
         * `POST /bookings/confirm`, 201 on success. A 402 PAYMENT_REQUIRES_ACTION surfaces as
         * `ApiException.Http` with `detail["client_secret"]` and `detail["stripe_account"]` for 3DS (PAY-05).
         */
        suspend fun confirm(request: ConfirmBookingRequest): ConfirmBookingData =
            apiCall { api.confirmBooking(request) }.data
    }
