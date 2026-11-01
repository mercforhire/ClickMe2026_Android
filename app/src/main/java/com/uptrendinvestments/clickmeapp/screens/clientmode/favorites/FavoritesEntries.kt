package com.uptrendinvestments.clickmeapp.screens.clientmode.favorites

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.BottomBar
import com.uptrendinvestments.clickmeapp.navigation.Navigator
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.showExplore
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.showMyBookings

/** What the Saved Experts route can do; the shell maps each callback to a destination. */
data class FavoritesRouteActions(
    val onBack: () -> Unit,
    val onOpenExpert: (ExpertProfileKey) -> Unit,
    val onExplore: () -> Unit,
    val onViewHistory: () -> Unit,
)

/**
 * Registers [FavoritesKey] once for the client shell, with the bottom bar hidden like every hub destination
 * (RESEARCH A2). Hilt-free so HomeShellsTest drives the real metadata and callbacks with stand-in content (the
 * `clientBookingManagementEntries` precedent).
 *
 * - A favorites row pushes the expert's profile on the current (Profile) stack; its key carries the Profile origin.
 * - D-05: "Explore Experts" calls [showExplore], the same tab switch as the bottom bar.
 * - D-06: "View My History" calls [showMyBookings]; the screen has already posted the Past launch.
 * - RESEARCH A3: neither switch resets the Profile stack, so Saved Experts is still open when the user returns.
 * - Runbook row D1.FV4 depends on A3. A finished booking is the one exit that does return the Profile tab to the hub,
 *   and it happens in the Request Sent callbacks, not here (Phase 16.3 CR-01).
 *
 * Client shell only: the expert shell registers no favorites route.
 */
fun EntryProviderScope<NavKey>.clientFavoritesEntry(
    navigator: Navigator,
    favorites: @Composable (FavoritesRouteActions) -> Unit,
) {
    entry<FavoritesKey>(metadata = BottomBar.hidden()) {
        favorites(
            FavoritesRouteActions(
                onBack = navigator::goBack,
                onOpenExpert = { navigator.navigate(it) },
                onExplore = navigator::showExplore,
                onViewHistory = navigator::showMyBookings,
            ),
        )
    }
}
