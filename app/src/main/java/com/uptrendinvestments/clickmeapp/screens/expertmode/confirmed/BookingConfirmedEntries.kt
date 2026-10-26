package com.uptrendinvestments.clickmeapp.screens.expertmode.confirmed

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.BottomBar
import com.uptrendinvestments.clickmeapp.navigation.Navigator
import com.uptrendinvestments.clickmeapp.screens.expertmode.showDashboard

/**
 * Registers the Booking Confirmed route in the expert shell with the bottom bar hidden.
 *
 * Confirmed replaces Request Decision after an accept (D-04, the replace itself is done by the decision screen), so
 * system Back pops to Incoming Requests. "Back to Home" resets the Dashboard stack and selects the Dashboard tab
 * ([showDashboard], D-05).
 */
fun EntryProviderScope<NavKey>.bookingConfirmedEntries(navigator: Navigator) {
    entry<BookingConfirmedKey>(metadata = BottomBar.hidden()) { key ->
        BookingConfirmedScreen(
            key = key,
            onBack = navigator::goBack,
            onBackToHome = navigator::showDashboard,
        )
    }
}
