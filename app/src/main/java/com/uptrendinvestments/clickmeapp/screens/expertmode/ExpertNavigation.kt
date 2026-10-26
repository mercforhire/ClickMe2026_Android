package com.uptrendinvestments.clickmeapp.screens.expertmode

import com.uptrendinvestments.clickmeapp.navigation.Navigator
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.DashboardRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.ExpertBookingsRoot

/**
 * "Back to Home" (D-05): resets the Dashboard stack to its root, then selects the Dashboard, whichever tab the
 * flow ended on.
 */
fun Navigator.showDashboard() = resetAndSelect(DashboardRoot, setOf(DashboardRoot))

/**
 * After a finished cancel (D-17): resets the Dashboard and Bookings stacks, then selects Bookings. Both stacks are
 * cleared so no Summary or Cancel screen of a cancelled booking stays reachable by Back, whichever tab the flow
 * started from (the expert counterpart of the client `showMyBookings`).
 */
fun Navigator.showExpertBookings() = resetAndSelect(ExpertBookingsRoot, setOf(DashboardRoot, ExpertBookingsRoot))
