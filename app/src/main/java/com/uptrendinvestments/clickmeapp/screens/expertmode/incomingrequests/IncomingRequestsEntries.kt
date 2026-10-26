package com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.BottomBar
import com.uptrendinvestments.clickmeapp.navigation.Navigator

/**
 * Registers the Incoming Requests route in the expert shell (D-11: pushed on the Dashboard stack, bottom bar hidden).
 * A tapped card navigates straight to the [com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.RequestDecisionKey]
 * built from its row.
 */
fun EntryProviderScope<NavKey>.incomingRequestsEntries(navigator: Navigator) {
    entry<IncomingRequestsKey>(metadata = BottomBar.hidden()) {
        IncomingRequestsScreen(
            onBack = navigator::goBack,
            onOpenRequest = { navigator.navigate(it) },
        )
    }
}
