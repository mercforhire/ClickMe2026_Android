package com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.BottomBar
import com.uptrendinvestments.clickmeapp.navigation.Navigator

/**
 * Registers the Request Decision route in the expert shell, pushed on whichever tab stack started it with the bottom
 * bar hidden. The ViewModel is created per key, so each request gets its own load. A successful accept replaces this
 * screen with Booking Confirmed, so Back from Confirmed lands on Incoming Requests (D-04).
 */
fun EntryProviderScope<NavKey>.requestDecisionEntries(navigator: Navigator) {
    entry<RequestDecisionKey>(metadata = BottomBar.hidden()) { key ->
        RequestDecisionScreen(
            viewModel =
                hiltViewModel<RequestDecisionViewModel, RequestDecisionViewModel.Factory>(
                    creationCallback = { factory -> factory.create(key) },
                ),
            onBack = navigator::goBack,
            onAccepted = { navigator.replaceTop(it) },
        )
    }
}
