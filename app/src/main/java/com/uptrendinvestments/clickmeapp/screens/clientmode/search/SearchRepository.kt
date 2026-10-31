package com.uptrendinvestments.clickmeapp.screens.clientmode.search

import com.uptrendinvestments.clickmeapp.model.responses.ExpertSearchResultItem
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Searches experts (`GET /experts/search`). Stateless and caches nothing. Never logs the query (it is user input).
 * `apiCall` rethrows coroutine cancellation untouched, which is what lets the ViewModel's latest search win (D-10).
 */
@Singleton
class SearchRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
    ) {
        /**
         * [q] is sent even when empty (the default browse set). A null [categorySlug] omits `category` (the All chip).
         * [sort] is a [SearchSort.apiValue]. Only page 1 of [PAGE_SIZE] rows is ever loaded (D-04).
         */
        suspend fun search(
            q: String,
            categorySlug: String?,
            sort: String,
        ): List<ExpertSearchResultItem> =
            apiCall {
                api.searchExperts(q, categorySlug, sort, PAGE, PAGE_SIZE)
            }.data.experts

        companion object {
            const val PAGE = 1
            const val PAGE_SIZE = 20
        }
    }
