package com.uptrendinvestments.clickmeapp.screens.notifications

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Which tab stack holds a Notifications list: the client Explore stack or the expert Dashboard stack. It only keeps
 * the same screen distinct across the two shells' stacks, because `NavigationState.toDecoratedEntries` flattens every
 * stack in use and two equal keys would produce duplicate content keys (Phase 10/11 Pitfall 7).
 */
@Serializable
enum class NotificationsOrigin { ClientExplore, ExpertDashboard }

/**
 * Nav3 route to the Notifications list. The bell on Explore and on the expert Dashboard pushes it, and so does a push
 * with no specific destination (D-10, D-11). The screen ignores [origin].
 */
@Serializable
data class NotificationsKey(
    val origin: NotificationsOrigin,
) : NavKey
