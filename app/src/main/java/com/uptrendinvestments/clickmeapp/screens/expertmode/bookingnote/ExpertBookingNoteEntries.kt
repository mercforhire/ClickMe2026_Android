package com.uptrendinvestments.clickmeapp.screens.expertmode.bookingnote

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.BottomBar
import com.uptrendinvestments.clickmeapp.navigation.Navigator

/**
 * Registers [ExpertBookingNoteKey] once for the expert shell, with the bottom bar hidden like every editor route.
 * Hilt-free so HomeShellsTest drives the real metadata and back callback with a stand-in editor.
 *
 * One registration serves every expert stack (Bookings, Chats, push-opened); the key's origin keeps their content keys
 * distinct (D-06).
 *
 * Expert shell only: the client shell registers `clientBookingNoteEntry`.
 */
fun EntryProviderScope<NavKey>.expertBookingNoteEntry(
    navigator: Navigator,
    editor: @Composable (key: ExpertBookingNoteKey, onBack: () -> Unit) -> Unit,
) {
    entry<ExpertBookingNoteKey>(metadata = BottomBar.hidden()) { key -> editor(key, navigator::goBack) }
}
