package com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile

import com.uptrendinvestments.clickmeapp.model.responses.ExpertClientProfileData
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The expert's view of a client (`GET /expert/clients/{id}`). Ports iOS `getClientProfile`.
 *
 * Stateless. Throws ApiException only: 404 for a missing or deleted client, 403 `NO_RELATIONSHIP` when this expert
 * has no booking with them. Expert shell only: the backend's `ensureExpert` permanently converts a client-only
 * account to expert on the first touch of any expert route (Phase 16.4 RESEARCH, D-13).
 */
@Singleton
class ExpertClientProfileRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
    ) {
        /** The client's profile as this expert may see it. */
        suspend fun profile(clientId: String): ExpertClientProfileData = apiCall { api.getExpertClient(clientId) }.data
    }
