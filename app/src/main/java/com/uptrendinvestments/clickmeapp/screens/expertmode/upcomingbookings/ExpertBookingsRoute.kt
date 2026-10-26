package com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings

import androidx.compose.runtime.Composable
import com.uptrendinvestments.clickmeapp.navigation.Navigator
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.AvailabilityKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.AvailabilityOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingSummaryKey

/**
 * The expert Bookings tab root route. Cards open the expert Booking Summary with origin Bookings, so the entry lives on
 * the Bookings stack (D-15); the empty Upcoming state opens Availability with origin Bookings.
 */
@Composable
fun ExpertBookingsRoute(navigator: Navigator) {
    ExpertBookingsScreen(
        onOpenBooking = { navigator.navigate(ExpertBookingSummaryKey(it, ExpertBookingOrigin.Bookings)) },
        onUpdateAvailability = { navigator.navigate(AvailabilityKey(AvailabilityOrigin.Bookings)) },
    )
}
