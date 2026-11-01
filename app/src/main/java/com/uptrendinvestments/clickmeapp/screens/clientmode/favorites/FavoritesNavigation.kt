package com.uptrendinvestments.clickmeapp.screens.clientmode.favorites

import com.uptrendinvestments.clickmeapp.navigation.StackOrigin
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileKey

/**
 * Maps a Saved Experts row to the Expert Profile route, seeding only what the row knows.
 *
 * `rating` and `reviewCount` stay null (unknown) so the hero shows Pending until the reviews load (RESEARCH A4, the
 * 16.1 Search precedent; iOS seeds 0 and 0, which shows "No reviews yet" until loaded). `origin = Profile` keeps the
 * key distinct from the same expert on Explore, Search or Chats (RESEARCH Pitfall 1).
 */
fun FavoriteExpertUi.toProfileKey(nameFallback: String): ExpertProfileKey =
    ExpertProfileKey(
        expertId = expertId,
        name = name ?: nameFallback,
        title = headline,
        imageUrl = avatarUrl,
        rating = null,
        reviewCount = null,
        tags = tags,
        origin = StackOrigin.Profile,
    )
