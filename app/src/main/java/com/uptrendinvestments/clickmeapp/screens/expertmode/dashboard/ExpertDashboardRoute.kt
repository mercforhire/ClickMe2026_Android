package com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard

import androidx.compose.runtime.Composable
import com.uptrendinvestments.clickmeapp.components.avatar.TopBarAvatarButton
import com.uptrendinvestments.clickmeapp.navigation.Navigator
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.ExpertTabRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.DashboardRoot

/**
 * The Dashboard tab root route.
 *
 * Plan 11-10 replaces the body with the Hilt-backed Dashboard screen and keeps this signature, so
 * `HomeExpertScreen` does not change again. Until then it renders the tab placeholder with the avatar action that
 * opens the mode switcher.
 */
@Composable
fun ExpertDashboardRoute(
    navigator: Navigator,
    avatarUrl: String?,
    onOpenModeSwitch: () -> Unit,
) {
    ExpertTabRoot(
        key = DashboardRoot,
        actions = { TopBarAvatarButton(avatarUrl, onClick = onOpenModeSwitch) },
    )
}
