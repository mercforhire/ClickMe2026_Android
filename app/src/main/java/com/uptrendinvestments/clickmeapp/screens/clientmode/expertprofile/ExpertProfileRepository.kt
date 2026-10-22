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
 * Expert Profile reads. Both calls throw [com.uptrendinvestments.clickmeapp.network.ApiException]. The profile
 * hides the heart (D-05), so nothing about the viewer's saved experts is requested (D-15).
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
