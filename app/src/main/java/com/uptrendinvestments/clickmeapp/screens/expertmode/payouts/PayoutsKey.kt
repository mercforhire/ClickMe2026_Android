package com.uptrendinvestments.clickmeapp.screens.expertmode.payouts

import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.SettingsOrigin
import kotlinx.serialization.Serializable

/**
 * Nav3 route to the expert Payouts screen (PAYOUT-01), pushed from the Profile hub and from the Dashboard.
 *
 * [origin] only keeps the Dashboard and Profile stack entries distinct (two equal keys would produce duplicate content
 * keys when the shell flattens the start stack with the selected one). The screen passes it on to the keys it pushes.
 */
@Serializable
data class PayoutsKey(
    val origin: SettingsOrigin,
) : NavKey
