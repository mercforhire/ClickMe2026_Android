package com.uptrendinvestments.clickmeapp.screens.settings.feedback

import com.uptrendinvestments.clickmeapp.model.requests.SubmitFeedbackRequest
import com.uptrendinvestments.clickmeapp.model.responses.FeedbackTypeItem
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Feedback source for Send Feedback (PARITY-AUDIT #6). Stateless; throws
 * [com.uptrendinvestments.clickmeapp.network.ApiException] only; never logs the request, which holds user text.
 */
@Singleton
class FeedbackRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
    ) {
        /** `GET /feedback/types`: the server's feedback categories, in its order. */
        suspend fun types(): List<FeedbackTypeItem> = apiCall { api.getFeedbackTypes() }.data.types

        /** `POST /feedback/submit`: the stored submission is not needed, so it is discarded. */
        suspend fun submit(request: SubmitFeedbackRequest) {
            apiCall { api.submitFeedback(request) }
        }
    }
