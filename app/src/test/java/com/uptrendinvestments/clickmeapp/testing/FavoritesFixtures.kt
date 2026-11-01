package com.uptrendinvestments.clickmeapp.testing

import com.uptrendinvestments.clickmeapp.model.responses.FavoriteExpertItem
import com.uptrendinvestments.clickmeapp.model.responses.FavoritesPayload
import com.uptrendinvestments.clickmeapp.model.responses.ToggleFavoriteData

/** One `/client/favorites` row as the server returns it. */
fun favoriteItem(
    id: String = "e1",
    name: String? = "Ada Lovelace",
    headline: String? = "Engineer",
    avatarUrl: String? = null,
    tags: List<String>? = listOf("AI"),
): FavoriteExpertItem =
    FavoriteExpertItem(
        expertId = id,
        fullName = name,
        headline = headline,
        avatarUrl = avatarUrl,
        expertiseTags = tags,
    )

fun favoritesPayload(vararg rows: FavoriteExpertItem): FavoritesPayload = FavoritesPayload(rows.toList())

/** The result of a favorites add (`isFavorite = true`) or remove (`false`). */
fun toggleFavorite(
    id: String = "e1",
    isFavorite: Boolean,
): ToggleFavoriteData = ToggleFavoriteData(expertId = id, isFavorite = isFavorite)
