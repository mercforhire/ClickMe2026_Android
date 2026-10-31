package com.uptrendinvestments.clickmeapp.screens.clientmode.search

import com.uptrendinvestments.clickmeapp.navigation.StackOrigin
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileKey

/**
 * Maps a Search result row to the Expert Profile route. Ports iOS `PublicExpertProfile(from:)`, seeding only what the
 * search row knows (D-05).
 *
 * `reviewCount` stays null (unknown) so the hero shows Pending until the reviews load instead of "No reviews"
 * (user decision 2026-10-30; iOS seeds 0). `origin` keeps the key distinct from the same expert opened on the Explore
 * stack, so the flattened Nav3 entries never produce duplicate content keys.
 */
fun SearchExpertUi.toProfileKey(nameFallback: String): ExpertProfileKey =
    ExpertProfileKey(
        expertId = expertId,
        name = name ?: nameFallback,
        title = headline,
        imageUrl = avatarUrl,
        rating = rating,
        reviewCount = null,
        tags = tags,
        origin = StackOrigin.Search,
    )
