package com.uptrendinvestments.clickmeapp.model.requests

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Body of `PATCH /notifications/preferences`. The backend schema is `.strict()` at both levels, so an extra key is a
 * 422: each row carries exactly the five keys of [NotificationPreferenceInput].
 */
@JsonClass(generateAdapter = true)
data class UpdateNotificationPreferencesRequest(
    val preferences: List<NotificationPreferenceInput>,
)

/** One row of the PATCH body: the category pair and the three channel flags. */
@JsonClass(generateAdapter = true)
data class NotificationPreferenceInput(
    val category: String,
    @Json(name = "sub_category") val subCategory: String,
    @Json(name = "push_enabled") val pushEnabled: Boolean,
    @Json(name = "email_enabled") val emailEnabled: Boolean,
    @Json(name = "in_app_enabled") val inAppEnabled: Boolean,
)
