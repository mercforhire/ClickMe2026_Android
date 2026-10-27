package com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Which expert tab stack opened a booking screen (Dashboard, Bookings, or Chats from a chat thread); it only decides which stack owns the entry. Mirrors the
 * `AvailabilityOrigin` trick: `NavigationState.toDecoratedEntries` flattens every stack in use, so two stacks holding
 * an equal key would produce duplicate content keys.
 */
@Serializable
enum class ExpertBookingOrigin { Dashboard, Bookings, Chats }

/**
 * Nav3 route to the expert Booking Summary. The [origin] only distinguishes the tab stack (Dashboard Expert Tools or
 * the Bookings tab); the screen itself ignores it and loads the booking by [bookingId].
 */
@Serializable
data class ExpertBookingSummaryKey(
    val bookingId: String,
    val origin: ExpertBookingOrigin,
) : NavKey
