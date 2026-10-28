package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

/**
 * One row of `GET /notifications` (`notificationPrefs.controller.js`). [category] stays a string so an unknown
 * server category never fails decoding (iOS uses an enum; Android is lenient, so an unknown category still opens
 * the list). [metaData] is free-form (numbers decode as Double). [title] and [body] are user-facing text, so
 * [toString] prints ids and categories only.
 */
@JsonClass(generateAdapter = true)
data class NotificationItem(
    val id: String,
    val category: String,
    @Json(name = "sub_category") val subCategory: String? = null,
    val title: String = "",
    val body: String = "",
    @Json(name = "meta_data") val metaData: Map<String, Any?>? = null,
    @Json(name = "created_at") val createdAt: Instant,
    @Json(name = "is_expert_only") val isExpertOnly: Boolean = false,
) {
    override fun toString(): String = "NotificationItem(id=$id, category=$category, subCategory=$subCategory)"
}

/** `data` of `GET /notifications`. */
@JsonClass(generateAdapter = true)
data class NotificationsPayload(
    val notifications: List<NotificationItem> = emptyList(),
    val pagination: PaginationMeta? = null,
)
