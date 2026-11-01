package com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile

import com.uptrendinvestments.clickmeapp.model.responses.ExpertReviewsPayload
import com.uptrendinvestments.clickmeapp.model.responses.PublicProfileDetailsData
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton

/** Number of reviews previewed on the Expert Profile (page 1). */
const val REVIEWS_PREVIEW_LIMIT = 3

/**
 * Expert Profile reads. Both calls throw [com.uptrendinvestments.clickmeapp.network.ApiException]. The heart's start
 * state arrives as `is_favorite` on the details response (16.3 D-01), so no saved-experts list is requested here.
 */
@Singleton
class ExpertProfileRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
    ) {
        suspend fun details(expertId: String): PublicProfileDetailsData =
            apiCall { api.getExpertDetails(expertId) }.data

        suspend fun reviewsPreview(expertId: String): ExpertReviewsPayload =
            apiCall { api.getExpertReviews(expertId, 1, REVIEWS_PREVIEW_LIMIT) }.data
    }
