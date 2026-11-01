package com.uptrendinvestments.clickmeapp.screens.clientmode.reviews

import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.StackOrigin
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileKey
import kotlinx.serialization.Serializable

/**
 * Nav3 route to the full Reviews list (Phase 16.3, D-16). It takes the expert id and the name the profile currently
 * shows (for the "{FirstName}'s Reviews" title); the key holds no secrets.
 *
 * [origin] only keeps keys distinct across stacks and is ignored by the screen. The route is registered in both
 * shells because the expert self-view keeps "See all" (D-15).
 */
@Serializable
data class ExpertReviewsKey(
    val expertId: String,
    val expertName: String,
    val origin: StackOrigin = StackOrigin.Default,
) : NavKey

/** Builds the Reviews key for this profile, copying its [ExpertProfileKey.origin] so each stack's key stays distinct. */
fun ExpertProfileKey.reviewsKey(expertName: String): ExpertReviewsKey = ExpertReviewsKey(expertId, expertName, origin)
