package com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation

import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingOrigin
import kotlinx.serialization.Serializable

/**
 * Nav3 route to the expert Cancel Booking screen, a full-screen push from the expert Booking Summary. The [origin]
 * only keeps content keys unique across the Dashboard and Bookings stacks (see [ExpertBookingOrigin]); the screen
 * loads the booking by [bookingId].
 */
@Serializable
data class ExpertCancelKey(
    val bookingId: String,
    val origin: ExpertBookingOrigin,
) : NavKey
