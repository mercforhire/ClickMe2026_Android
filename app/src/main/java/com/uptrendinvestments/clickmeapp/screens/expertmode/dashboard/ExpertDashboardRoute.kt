package com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard

import androidx.compose.runtime.Composable
import com.uptrendinvestments.clickmeapp.navigation.Navigator
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingSummaryKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.ExpertBookingsRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests.IncomingRequestsKey

/**
 * The Dashboard tab root route (EXP-05): renders the Hilt-backed [ExpertDashboardScreen] and turns its callbacks into
 * navigation. The signature is the one `HomeExpertScreen` already calls, so that file does not change.
 *
 * The Bookings tab is selected with `navigate(ExpertBookingsRoot)`, which switches tabs and leaves the Dashboard
 * stack as it is (UI-SPEC tab switches); the screen has already posted the Upcoming hand-off.
 */
@Composable
fun ExpertDashboardRoute(
    navigator: Navigator,
    avatarUrl: String?,
    onOpenModeSwitch: () -> Unit,
) {
    ExpertDashboardScreen(
        avatarUrl = avatarUrl,
        onOpenModeSwitch = onOpenModeSwitch,
        onOpenRequests = { navigator.navigate(IncomingRequestsKey) },
        onOpenBookingsUpcoming = { navigator.navigate(ExpertBookingsRoot) },
        onOpenBooking = { navigator.navigate(ExpertBookingSummaryKey(it, ExpertBookingOrigin.Dashboard)) },
    )
}
