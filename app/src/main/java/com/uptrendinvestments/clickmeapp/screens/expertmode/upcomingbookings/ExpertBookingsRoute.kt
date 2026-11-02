package com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings

import androidx.compose.runtime.Composable
import com.uptrendinvestments.clickmeapp.navigation.Navigator
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RescheduleOpener
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.AvailabilityKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.AvailabilityOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingSummaryKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.reschedule.ExpertRescheduleKey

/**
 * The expert Bookings tab root route. Cards open the expert Booking Summary with origin Bookings, so the entry lives on
 * the Bookings stack (D-15); the empty Upcoming state opens Availability with origin Bookings. Reschedule on a card
 * the server allows opens the expert picker with origin Bookings and opener ExpertBookings (Phase 16.2), never the
 * summary; a send pops back here. A note control on a card opens the expert private note editor with origin Bookings
 * (Phase 16.4), never the summary; returning here reloads quietly, so a saved note's preview shows without new code.
 */
@Composable
fun ExpertBookingsRoute(navigator: Navigator) {
    ExpertBookingsScreen(
        onOpenBooking = { navigator.navigate(ExpertBookingSummaryKey(it, ExpertBookingOrigin.Bookings)) },
        onUpdateAvailability = { navigator.navigate(AvailabilityKey(AvailabilityOrigin.Bookings)) },
        onReschedule = {
            navigator.navigate(ExpertRescheduleKey(it, ExpertBookingOrigin.Bookings, RescheduleOpener.ExpertBookings))
        },
        onPrivateNote = { navigator.navigate(it) },
    )
}
