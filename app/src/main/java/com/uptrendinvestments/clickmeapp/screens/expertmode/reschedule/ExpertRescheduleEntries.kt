package com.uptrendinvestments.clickmeapp.screens.expertmode.reschedule

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.BottomBar
import com.uptrendinvestments.clickmeapp.navigation.Navigator

/**
 * Registers the expert Reschedule picker route in the expert shell, with the bottom bar hidden.
 *
 * D-10: the picker is pushed on the stack that opened it (Expert Booking Summary or the Bookings list), so going back
 * or finishing a send pops to that opener, whose one-shot slot the ViewModel has already filled.
 */
fun EntryProviderScope<NavKey>.expertRescheduleEntries(navigator: Navigator) {
    entry<ExpertRescheduleKey>(metadata = BottomBar.hidden()) { key ->
        ExpertRescheduleScreen(
            viewModel =
                hiltViewModel<ExpertRescheduleViewModel, ExpertRescheduleViewModel.Factory>(
                    creationCallback = { factory -> factory.create(key) },
                ),
            onBack = navigator::goBack,
        )
    }
}
