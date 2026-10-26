package com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.BottomBar
import com.uptrendinvestments.clickmeapp.navigation.Navigator
import com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation.ExpertCancelKey

/**
 * Registers the expert Booking Summary route in the expert shell with the bottom bar hidden. Cancel Booking pushes
 * [ExpertCancelKey] carrying the same origin, so the Cancel route lands on the stack of the tab that opened the
 * summary (D-17). The screen only calls `onCancel` after the call guard allowed it.
 */
fun EntryProviderScope<NavKey>.expertBookingSummaryEntries(navigator: Navigator) {
    entry<ExpertBookingSummaryKey>(metadata = BottomBar.hidden()) { key ->
        ExpertBookingSummaryScreen(
            viewModel =
                hiltViewModel<ExpertBookingSummaryViewModel, ExpertBookingSummaryViewModel.Factory>(
                    creationCallback = { factory -> factory.create(key) },
                ),
            onBack = navigator::goBack,
            onCancel = { navigator.navigate(ExpertCancelKey(key.bookingId, key.origin)) },
        )
    }
}
