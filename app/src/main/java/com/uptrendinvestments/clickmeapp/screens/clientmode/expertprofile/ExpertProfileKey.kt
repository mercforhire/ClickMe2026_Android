package com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Nav3 route to the Expert Profile. The fields are a display seed only (no tokens or secrets), so the
 * hero renders at once while the network loads (D-15).
 *
 * From an Explore expert card every field is known. From a Featured Topic only [expertId], [name] and
 * [imageUrl] are known: an empty [title] and null [rating] / [reviewCount] mean unknown, not zero.
 */
@Serializable
data class ExpertProfileKey(
    val expertId: String,
    val name: String,
    val title: String,
    val imageUrl: String?,
    val rating: Double? = null,
    val reviewCount: Int? = null,
    val tags: List<String> = emptyList(),
) : NavKey
