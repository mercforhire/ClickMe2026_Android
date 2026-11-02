package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** `GET /notifications/preferences` data: one row per category and sub-category. Ports iOS `NotificationPreferencesData`. */
@JsonClass(generateAdapter = true)
data class NotificationPreferencesData(
    val preferences: List<NotificationPreferenceRow> = emptyList(),
)

/**
 * One preference row. Category keys are singular (`booking`, not `bookings`). The backend stores only these three
 * per-row channel flags and nothing else (CONTEXT D-01), so there is deliberately no other field here.
 */
@JsonClass(generateAdapter = true)
data class NotificationPreferenceRow(
    val category: String,
    @Json(name = "sub_category") val subCategory: String,
    @Json(name = "push_enabled") val pushEnabled: Boolean = true,
    @Json(name = "email_enabled") val emailEnabled: Boolean = true,
    @Json(name = "in_app_enabled") val inAppEnabled: Boolean = true,
) {
    /** True when at least one channel is on for this row. */
    val isAnyChannelEnabled: Boolean get() = pushEnabled || emailEnabled || inAppEnabled
}
