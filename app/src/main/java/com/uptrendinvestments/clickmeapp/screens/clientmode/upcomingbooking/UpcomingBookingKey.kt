package com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Nav3 route to Upcoming Booking Details (BOOK-06). Reached from a My Bookings Upcoming card and from Explore's
 * Today's Sessions card. It carries only the booking id: the screen always loads its own data, so it works the same
 * from either entry point (RESEARCH Pitfall 12). The id is not a secret.
 */
@Serializable
data class UpcomingBookingKey(
    val bookingId: String,
) : NavKey
