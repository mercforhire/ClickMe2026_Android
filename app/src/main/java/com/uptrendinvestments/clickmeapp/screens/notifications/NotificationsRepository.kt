package com.uptrendinvestments.clickmeapp.screens.notifications

import com.uptrendinvestments.clickmeapp.model.responses.NotificationItem
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The notifications list (`GET /notifications`). Ports iOS `NotificationsListViewModel` load.
 *
 * Stateless: nothing here is cleared on logout, so it caches nothing; throws ApiException only; notification
 * titles and bodies are never logged.
 */
@Singleton
class NotificationsRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
    ) {
        /** The first [PAGE_LIMIT] notifications, newest first (D-16: no further paging). */
        suspend fun recent(): List<NotificationItem> =
            apiCall { api.getNotifications(page = 1, limit = PAGE_LIMIT) }.data.notifications

        companion object {
            const val PAGE_LIMIT = 50
        }
    }
