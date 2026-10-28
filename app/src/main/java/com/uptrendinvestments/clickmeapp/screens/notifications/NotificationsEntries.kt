package com.uptrendinvestments.clickmeapp.screens.notifications

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.navigation.BottomBar
import com.uptrendinvestments.clickmeapp.navigation.Navigator
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ExploreRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.DashboardRoot

/**
 * Registers the Notifications route with the bottom bar hidden. Both shells call this from their `detailEntries`, so
 * the Notifications feature plan never edits a shell again.
 */
fun EntryProviderScope<NavKey>.notificationsEntries(
    navigator: Navigator,
    mode: AppMode,
) {
    entry<NotificationsKey>(metadata = BottomBar.hidden()) { NotificationsRoute(it, navigator, mode) }
}

/** The Notifications key for [mode]'s shell: its origin keeps it distinct on the Explore and Dashboard stacks. */
fun notificationsKeyFor(mode: AppMode): NotificationsKey =
    when (mode) {
        AppMode.Client -> NotificationsKey(NotificationsOrigin.ClientExplore)
        AppMode.Expert -> NotificationsKey(NotificationsOrigin.ExpertDashboard)
    }

/** The tab holding the Notifications list of [mode]'s shell: Explore for a client, Dashboard for an expert (D-11). */
fun notificationsRootFor(mode: AppMode): NavKey =
    when (mode) {
        AppMode.Client -> ExploreRoot
        AppMode.Expert -> DashboardRoot
    }
