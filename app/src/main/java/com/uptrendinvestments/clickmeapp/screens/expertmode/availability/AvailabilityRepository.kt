package com.uptrendinvestments.clickmeapp.screens.expertmode.availability

import com.uptrendinvestments.clickmeapp.model.requests.UpdateMyAvailabilityRequest
import com.uptrendinvestments.clickmeapp.model.responses.ExpertAvailabilityScheduleData
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The signed-in expert's own weekly schedule (`/expert/availability`), distinct from the public per-month
 * availability a client sees. Every call throws [com.uptrendinvestments.clickmeapp.network.ApiException] only.
 *
 * Stateless by design: no per-user cache, since nothing clears singletons on logout (threat T-10-16).
 */
@Singleton
class AvailabilityRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
    ) {
        /** `GET /expert/availability`: the saved timezone and recurring weekly schedule. */
        suspend fun get(): ExpertAvailabilityScheduleData = apiCall { api.getMyAvailability() }.data

        /** `PUT /expert/availability`: replaces the whole weekly schedule. */
        suspend fun put(body: UpdateMyAvailabilityRequest) {
            apiCall { api.updateMyAvailability(body) }
        }
    }
