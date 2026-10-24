@file:Suppress("ktlint:standard:filename")

package com.uptrendinvestments.clickmeapp.model.requests

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** The only `connection_status` value the client reports on a successful join (the backend also accepts `failed`). */
const val CONNECTION_STATUS_CONNECTED = "connected"

/**
 * `PATCH /bookings/{id}/confirm-connection` body. The backend schema is `.strict()`:
 * `{connection_status: 'connected' | 'failed', timestamp?: ISO datetime with offset, method?: 'in_app_voice' |
 * 'skype_zoom'}`. Null fields are omitted by the converter (no `withNullSerialization`).
 */
@JsonClass(generateAdapter = true)
data class ConfirmConnectionRequest(
    @Json(name = "connection_status") val connectionStatus: String = CONNECTION_STATUS_CONNECTED,
    val timestamp: String? = null,
    val method: String? = null,
)

/**
 * `POST /bookings/{id}/end` body. The backend schema is `.strict()`: `{actual_duration: positive int,
 * end_reason: 'completed' | 'dropped' | 'manual_exit'}`. Null fields are omitted by the converter.
 *
 * @property actualDuration whole seconds, and must be at least 1 because the backend schema is `.int().positive()`
 * (iOS sends `max(0, d)`, which a sub-second leg turns into a 400).
 */
@JsonClass(generateAdapter = true)
data class EndCallRequest(
    @Json(name = "actual_duration") val actualDuration: Int,
    @Json(name = "end_reason") val endReason: String,
)
