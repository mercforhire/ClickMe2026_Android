package com.uptrendinvestments.clickmeapp.screens.settings.help

import com.uptrendinvestments.clickmeapp.model.responses.FaqsPayload
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton

/** FAQ source for Help. Mirrors iOS `ClickMeAPI+Help`; throws [com.uptrendinvestments.clickmeapp.network.ApiException] only. */
@Singleton
class HelpRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
    ) {
        /** `GET /help/faqs`: categories with their featured articles. */
        suspend fun faqs(): FaqsPayload = apiCall { api.getFaqs() }.data
    }
