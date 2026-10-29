package com.uptrendinvestments.clickmeapp.screens.expertmode.taxprofile

import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.SettingsOrigin
import kotlinx.serialization.Serializable

/**
 * Nav3 route to Tax & Payout Info (PAYOUT-04). The [origin] only keeps the key distinct between the Profile and
 * Dashboard stacks (`NavigationState.toDecoratedEntries` flattens both); the screen ignores it.
 */
@Serializable
data class TaxProfileKey(
    val origin: SettingsOrigin,
) : NavKey
