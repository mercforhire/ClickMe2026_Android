package com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard

import androidx.compose.runtime.Composable
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.navigation.Navigator
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.AvailabilityKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.AvailabilityOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingSummaryKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.ExpertBookingsRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.TopicsRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests.IncomingRequestsKey
import com.uptrendinvestments.clickmeapp.screens.notifications.notificationsKeyFor

/**
 * The Dashboard tab root route (EXP-05): renders the Hilt-backed [ExpertDashboardScreen] and turns its callbacks into
 * navigation. The signature is the one `HomeExpertScreen` already calls, so that file does not change.
 *
 * The Bookings and Topics tabs are selected with `navigate(ExpertBookingsRoot)` and `navigate(TopicsRoot)`, which switch
 * tabs and leave the Dashboard stack as it is (UI-SPEC tab switches); the screen has already posted the Upcoming
 * hand-off. Availability opens as a screen on the Dashboard stack, keyed by its Dashboard origin (D-11).
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
        onOpenAvailability = { navigator.navigate(AvailabilityKey(AvailabilityOrigin.Dashboard)) },
        onOpenTopics = { navigator.navigate(TopicsRoot) },
        onOpenNotifications = { navigator.navigate(notificationsKeyFor(AppMode.Expert)) },
    )
}
