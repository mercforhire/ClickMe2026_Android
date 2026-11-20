package com.uptrendinvestments.clickmeapp.screens.expertmode.settings

import com.uptrendinvestments.clickmeapp.model.requests.MeetingUrlBody
import com.uptrendinvestments.clickmeapp.model.requests.UpdateExpertMeetingUrlRequest
import com.uptrendinvestments.clickmeapp.model.responses.ExpertProfileData
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `GET /expert/profile` and `PATCH /expert/profile` (the meeting link only) for Expert Settings. Everything else on the
 * screen goes through `ProfileSettingsRepository`. Throws [com.uptrendinvestments.clickmeapp.network.ApiException] only.
 */
@Singleton
class ExpertProfileSettingsRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
    ) {
        suspend fun load(): ExpertProfileData = apiCall { api.getExpertProfile() }.data

        /**
         * Sends [url] as `professional_details.meeting_url`. The caller has already checked a non-blank [url] is a valid
         * link. An empty [url] is sent as `""` (not dropped) and clears the saved link: the backend stores NULL.
         */
        suspend fun updateMeetingUrl(url: String) {
            apiCall { api.updateExpertMeetingUrl(UpdateExpertMeetingUrlRequest(MeetingUrlBody(url))) }
        }
    }
