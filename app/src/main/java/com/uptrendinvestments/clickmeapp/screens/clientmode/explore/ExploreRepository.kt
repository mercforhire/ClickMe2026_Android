package com.uptrendinvestments.clickmeapp.screens.clientmode.explore

import com.uptrendinvestments.clickmeapp.model.responses.ClientHomeData
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton

/** Loads the client home feed (`GET /client/home`): trending categories, featured topics, recommended experts. */
@Singleton
class ExploreRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
    ) {
        /**
         * [selection] narrows the recommended experts (D-10). Slugs are sent sorted and comma-joined, as iOS does; an
         * empty selection sends no `category` at all.
         */
        suspend fun home(selection: Set<String>): ClientHomeData =
            apiCall {
                api.getClientHome(selection.takeIf { it.isNotEmpty() }?.sorted()?.joinToString(","))
            }.data
    }
