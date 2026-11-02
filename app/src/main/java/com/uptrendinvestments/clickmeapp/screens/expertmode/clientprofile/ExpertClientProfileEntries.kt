package com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.BottomBar
import com.uptrendinvestments.clickmeapp.navigation.Navigator

/**
 * Registers [ExpertClientProfileKey] once for the expert shell, with the bottom bar hidden like every hub destination.
 * Hilt-free so HomeShellsTest drives the real metadata and back callback with stand-in content.
 *
 * Expert shell only. Never register it in the client shell: any call to an expert endpoint (the `/expert` path
 * prefix) converts a client-only account (`ensureExpert`, D-13).
 */
fun EntryProviderScope<NavKey>.expertClientProfileEntry(
    navigator: Navigator,
    profile: @Composable (key: ExpertClientProfileKey, onBack: () -> Unit) -> Unit,
) {
    entry<ExpertClientProfileKey>(metadata = BottomBar.hidden()) { key -> profile(key, navigator::goBack) }
}
