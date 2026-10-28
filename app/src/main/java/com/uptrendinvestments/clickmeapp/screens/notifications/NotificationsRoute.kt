package com.uptrendinvestments.clickmeapp.screens.notifications

import androidx.compose.runtime.Composable
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.navigation.Navigator

/**
 * The Notifications route: the list screen with an entry-scoped ViewModel (it lives as long as the Nav3 entry).
 * [key] and [mode] stay in the signature the shells call.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun NotificationsRoute(
    key: NotificationsKey,
    navigator: Navigator,
    mode: AppMode,
) {
    NotificationsScreen(onBack = navigator::goBack)
}
