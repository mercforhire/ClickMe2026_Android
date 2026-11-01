package com.uptrendinvestments.clickmeapp.screens.clientmode.favorites

import com.uptrendinvestments.clickmeapp.model.responses.FavoritesPayload
import com.uptrendinvestments.clickmeapp.model.responses.ToggleFavoriteData
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The client's saved experts (`/client/favorites`). Ports iOS `ClickMeAPI+Favorites`.
 *
 * Stateless: it caches nothing and nothing here is cleared on logout (there is no shared favorites flow, so each
 * screen owns its own state). Throws ApiException only. The list is one request of up to [LIST_LIMIT] rows with no
 * paging (D-12), so favorites beyond 100 are not shown.
 */
@Singleton
class FavoritesRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
    ) {
        /** `GET /client/favorites?page=1&limit=100`: the saved experts, newest first as the server orders them. */
        suspend fun list(): FavoritesPayload = apiCall { api.getClientFavorites(1, LIST_LIMIT) }.data

        /** `PUT /client/favorites/{expertId}`: idempotent; answers `is_favorite = true`. */
        suspend fun add(expertId: String): ToggleFavoriteData = apiCall { api.addFavorite(expertId) }.data

        /** `DELETE /client/favorites/{expertId}`: idempotent; answers `is_favorite = false`. */
        suspend fun remove(expertId: String): ToggleFavoriteData = apiCall { api.removeFavorite(expertId) }.data

        companion object {
            /** The backend cap for `limit` (its default is 20). */
            const val LIST_LIMIT = 100
        }
    }
