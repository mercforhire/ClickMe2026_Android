package com.uptrendinvestments.clickmeapp.screens.expertmode.availability

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Where the Availability screen was opened from; it only decides which tab stack owns the entry. */
@Serializable
enum class AvailabilityOrigin { Topics, Profile }

/**
 * Nav3 route to Availability. The [origin] only distinguishes the tab stack: `NavigationState.toDecoratedEntries`
 * flattens every stack in use, so two stacks holding an equal key would produce duplicate content keys. The
 * screen itself ignores it.
 */
@Serializable
data class AvailabilityKey(
    val origin: AvailabilityOrigin,
) : NavKey
