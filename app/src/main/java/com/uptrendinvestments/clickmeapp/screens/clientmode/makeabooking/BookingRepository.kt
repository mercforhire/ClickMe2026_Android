package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import com.uptrendinvestments.clickmeapp.model.requests.BookingRequestBody
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertAvailabilityData
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
 * shows the expert's base slots and the server re-checks the duration when the booking is confirmed.
 */
@Singleton
class BookingRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
    ) {
        /** One month of the expert's availability grid (`yyyy-MM`), without a topic filter. */
        suspend fun availability(
            expertId: String,
            month: YearMonth,
        ): ExpertAvailabilityData = apiCall { api.getExpertAvailability(expertId, month.toString(), null) }.data

        /** `POST /bookings/confirm`, free path. */
        suspend fun requestBooking(body: BookingRequestBody): BookingRequestData =
            apiCall { api.requestBooking(body) }.data
    }
