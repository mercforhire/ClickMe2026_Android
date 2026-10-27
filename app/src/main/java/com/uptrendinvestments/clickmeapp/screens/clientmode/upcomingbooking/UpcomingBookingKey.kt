package com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking

import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.StackOrigin
import kotlinx.serialization.Serializable

/**
 * Nav3 route to Upcoming Booking Details (BOOK-06). Reached from a My Bookings Upcoming card and from Explore's
 * Today's Sessions card. It carries only the booking id: the screen always loads its own data, so it works the same
 * from either entry point (RESEARCH Pitfall 12). The id is not a secret. [origin] is [StackOrigin.Chats] when a
 * chat thread pushed it and only keeps the key distinct across stacks.
 */
@Serializable
data class UpcomingBookingKey(
    val bookingId: String,
    val origin: StackOrigin = StackOrigin.Default,
) : NavKey
