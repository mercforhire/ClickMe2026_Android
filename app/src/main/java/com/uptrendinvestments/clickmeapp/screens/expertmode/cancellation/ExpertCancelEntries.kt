package com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.BottomBar
import com.uptrendinvestments.clickmeapp.navigation.Navigator
import com.uptrendinvestments.clickmeapp.screens.expertmode.showExpertBookings

/**
 * Registers the expert Cancel Booking route in the expert shell, with the bottom bar hidden.
 *
 * D-17: a finished cancel calls [showExpertBookings], which resets both stacks and selects Bookings. The ViewModel has
 * already posted the Past-tab hand-off to `ExpertLaunchRequest.bookings`, so the screen only resets navigation.
 */
fun EntryProviderScope<NavKey>.expertCancelEntries(navigator: Navigator) {
    entry<ExpertCancelKey>(metadata = BottomBar.hidden()) { key ->
        ExpertCancellationScreen(
            viewModel =
                hiltViewModel<ExpertCancellationViewModel, ExpertCancellationViewModel.Factory>(
                    creationCallback = { factory -> factory.create(key) },
                ),
            onBack = navigator::goBack,
            onCancelled = navigator::showExpertBookings,
        )
    }
}
