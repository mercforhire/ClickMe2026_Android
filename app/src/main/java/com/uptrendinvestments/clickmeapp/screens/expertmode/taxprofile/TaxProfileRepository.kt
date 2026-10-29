package com.uptrendinvestments.clickmeapp.screens.expertmode.taxprofile

import com.uptrendinvestments.clickmeapp.model.requests.SaveTaxProfileRequest
import com.uptrendinvestments.clickmeapp.model.responses.SaveTaxProfileData
import com.uptrendinvestments.clickmeapp.model.responses.TaxProfileData
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The expert's masked tax profile (`/expert/tax-profile`). Every call throws
 * [com.uptrendinvestments.clickmeapp.network.ApiException] only.
 *
 * Stateless by design: nothing is cached, so the TIN, name, address and date of birth never outlive a request in a
 * singleton (threat T-14-31).
 */
@Singleton
class TaxProfileRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
    ) {
        /** `GET /expert/tax-profile`: everything but the TIN, which the server never returns. */
        suspend fun load(): TaxProfileData = apiCall { api.getTaxProfile() }.data

        /** `POST /expert/tax-profile`: `saved = false` with `tin_required` is an answer, not an error. */
        suspend fun save(body: SaveTaxProfileRequest): SaveTaxProfileData = apiCall { api.saveTaxProfile(body) }.data
    }
