package com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings

import androidx.compose.runtime.Composable
import com.uptrendinvestments.clickmeapp.navigation.Navigator
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.ExpertTabRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.ExpertBookingsRoot

/**
 * The expert Bookings tab root route.
 *
 * Plan 11-04 replaces the body with the expert bookings screen and keeps this signature. Until then it renders the
 * tab placeholder.
 */
@Composable
fun ExpertBookingsRoute(navigator: Navigator) {
    ExpertTabRoot(ExpertBookingsRoot)
}
