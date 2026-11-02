package com.uptrendinvestments.clickmeapp.screens.settings.notifications

import com.uptrendinvestments.clickmeapp.model.requests.UpdateNotificationPreferencesRequest
import com.uptrendinvestments.clickmeapp.model.responses.NotificationPreferenceRow
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notification preferences source. Stateless: mirrors iOS `ClickMeAPI+Notifications`; throws
 * [com.uptrendinvestments.clickmeapp.network.ApiException] only.
 */
@Singleton
class NotificationSettingsRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
    ) {
        /** `GET /notifications/preferences`: one row per category and sub-category. */
        suspend fun preferences(): List<NotificationPreferenceRow> =
            apiCall { api.getNotificationPreferences() }.data.preferences

        /** `PATCH /notifications/preferences`; the response carries nothing the screen needs. */
        suspend fun update(request: UpdateNotificationPreferencesRequest) {
            apiCall { api.updateNotificationPreferences(request) }
        }
    }
