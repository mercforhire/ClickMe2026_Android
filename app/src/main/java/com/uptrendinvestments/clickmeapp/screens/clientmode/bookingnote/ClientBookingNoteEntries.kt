package com.uptrendinvestments.clickmeapp.screens.clientmode.bookingnote

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.BottomBar
import com.uptrendinvestments.clickmeapp.navigation.Navigator

/**
 * Registers [ClientBookingNoteKey] once for the client shell, with the bottom bar hidden like every editor route.
 * Hilt-free so HomeShellsTest drives the real metadata and back callback with a stand-in editor (the
 * `clientFavoritesEntry` precedent).
 *
 * One registration serves the Bookings, Chats and push-opened stacks, because the shell has a single entry provider
 * (D-06 finding); the key's origin keeps the stacks' content keys distinct.
 *
 * Client shell only: the expert shell registers `expertBookingNoteEntry`.
 */
fun EntryProviderScope<NavKey>.clientBookingNoteEntry(
    navigator: Navigator,
    editor: @Composable (key: ClientBookingNoteKey, onBack: () -> Unit) -> Unit,
) {
    entry<ClientBookingNoteKey>(metadata = BottomBar.hidden()) { key -> editor(key, navigator::goBack) }
}
