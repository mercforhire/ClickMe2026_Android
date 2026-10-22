package com.uptrendinvestments.clickmeapp.screens.clientmode.explore

import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileKey

/** Maps an Explore expert card to the profile route; the key carries everything the card knows (D-15). */
fun ExpertCardUi.toProfileKey(nameFallback: String): ExpertProfileKey =
    ExpertProfileKey(
        expertId = expertId,
        name = name ?: nameFallback,
        title = title.orEmpty(),
        imageUrl = imageUrl,
        rating = rating,
        reviewCount = reviewCount,
        tags = tags,
    )

/**
 * Maps a Featured Topic card to the profile route. Only the expert id, name and image are known, so the title is
 * empty and rating / review count are null (unknown, not zero) until the profile loads (D-15).
 */
fun FeaturedTopicUi.toProfileKey(nameFallback: String): ExpertProfileKey =
    ExpertProfileKey(
        expertId = expertId,
        name = expertName ?: nameFallback,
        title = "",
        imageUrl = expertImageUrl,
        rating = null,
        reviewCount = null,
        tags = emptyList(),
    )
