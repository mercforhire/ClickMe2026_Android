package com.uptrendinvestments.clickmeapp.screens.expertmode.settings

import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.SettingsOrigin
import kotlinx.serialization.Serializable

/**
 * Nav3 route to Expert Settings. The [origin] keeps the key distinct between the Profile and Dashboard stacks
 * (`NavigationState.toDecoratedEntries` flattens both) and is passed on to the photo crop route (see [SettingsOrigin]).
 */
@Serializable
data class ExpertProfileSettingsKey(
    val origin: SettingsOrigin,
) : NavKey
