package com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile

import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.StackOrigin
import kotlinx.serialization.Serializable

/**
 * Nav3 route to the Expert Profile. The fields are a display seed only (no tokens or secrets), so the
 * hero renders at once while the network loads (D-15).
 *
 * From an Explore expert card every field is known. From a Featured Topic only [expertId], [name] and
 * [imageUrl] are known: an empty [title] and null [rating] / [reviewCount] mean unknown, not zero.
 *
 * [origin] is [StackOrigin.Chats] when a chat thread pushed it; it only keeps the key distinct from the same
 * profile on the Explore stack and is ignored by the screen.
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
    val origin: StackOrigin = StackOrigin.Default,
) : NavKey
