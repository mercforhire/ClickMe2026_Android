package com.uptrendinvestments.clickmeapp.screens.expertmode.payouts

import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.SettingsOrigin
import kotlinx.serialization.Serializable

/**
 * Nav3 route to the full earnings Activity list, pushed from "See all activity" on Payouts. The [origin] only keeps
 * content keys unique across the Profile and Dashboard stacks (see [SettingsOrigin]).
 */
@Serializable
data class TransactionsKey(
    val origin: SettingsOrigin,
) : NavKey
