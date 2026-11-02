package com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.BottomBar
import com.uptrendinvestments.clickmeapp.navigation.Navigator
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RescheduleOpener
import com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation.ExpertCancelKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.reschedule.ExpertRescheduleKey

/**
 * Registers the expert Booking Summary route in the expert shell with the bottom bar hidden. Cancel Booking pushes
 * [ExpertCancelKey] carrying the same origin, so the Cancel route lands on the stack of the tab that opened the
 * summary (D-17). The screen only calls `onCancel` after the call guard allowed it. Reschedule pushes
 * [ExpertRescheduleKey] with the same origin and the Expert Summary opener (Phase 16.2), so the
 * picker's pop-back lands here and the send notice reaches this screen's slot.
 *
 * My Note pushes the [com.uptrendinvestments.clickmeapp.screens.expertmode.bookingnote.ExpertBookingNoteKey] the
 * ViewModel built; it copies this summary's origin, so the editor lands on the same stack (Phase 16.4, D-05, D-06).
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
            onReschedule = {
                navigator.navigate(ExpertRescheduleKey(key.bookingId, key.origin, RescheduleOpener.ExpertSummary))
            },
            onOpenNote = { navigator.navigate(it) },
        )
    }
}
