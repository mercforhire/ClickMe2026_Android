package com.uptrendinvestments.clickmeapp.screens.clientmode.favorites

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Saved Experts (titled "Favorite Experts", iOS `FavoritesView`) on the client Profile tab (Phase 16.3, PARITY-AUDIT
 * #2). Registered once, in the client shell only, with the bottom bar hidden like every hub destination
 * (RESEARCH A2).
 */
@Serializable
data object FavoritesKey : NavKey
