package com.uptrendinvestments.clickmeapp.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey

/** One bottom-navigation tab: its root route, label, and unselected / selected icons. */
data class TabDefinition(
    val key: NavKey,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)
