package com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Nav3 route to the Cancel Booking screen (BOOK-07), a full-screen push (D-08). It carries only the booking id: the
 * screen loads the booking itself (`GET /client/bookings/:id`) and computes the refund preview from that detail, so
 * it never depends on the previous screen's data. The id is not a secret.
 */
@Serializable
data class CancelBookingKey(
    val bookingId: String,
) : NavKey
