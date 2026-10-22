package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings

import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingDetail
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingsPayload
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the signed-in client's own bookings (`GET /client/bookings`). Explore uses the upcoming list for Today's
 * Sessions; Phase 8 (My Bookings) reuses this repository for the Upcoming and Past tabs.
 *
 * [detail] reads one booking (`GET /client/bookings/{id}`) for Request Sent; Phase 8 Upcoming Booking Details
 * reuses it. The server scopes the read to the caller and returns 404 for any other booking.
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

        companion object {
            const val TYPE_UPCOMING = "upcoming"
            const val TYPE_PAST = "past"
        }
    }
