package com.uptrendinvestments.clickmeapp.navigation

import kotlinx.serialization.Serializable

/**
 * Which stack pushed a Phase 14 settings key: the [Profile] hub or the expert [Dashboard] (the expert start tab).
 *
 * It only keeps keys distinct, because `NavigationState.toDecoratedEntries` flattens the start stack with the selected
 * one and two equal keys would produce duplicate content keys (the same reason as [StackOrigin] and
 * `AvailabilityOrigin`). Screens ignore it except to pass it on to the keys they push.
 */
@Serializable
enum class SettingsOrigin { Profile, Dashboard }
