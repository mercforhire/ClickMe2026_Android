@file:Suppress("ktlint:standard:filename")

package com.uptrendinvestments.clickmeapp.model.requests

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * `POST /bookings/request` body (free path). The backend schema is strict: exactly these keys, declared in
 * the order Moshi writes them.
 *
 * @property clientNotes never send an empty string. Null is omitted by the converter (no
 * `withNullSerialization`), which is required because the strict schema rejects an explicit `null`.
 */
@JsonClass(generateAdapter = true)
data class BookingRequestBody(
    @Json(name = "expert_id") val expertId: String,
    @Json(name = "topic_id") val topicId: String,
    @Json(name = "scheduled_start") val scheduledStart: String,
    @Json(name = "duration_minutes") val durationMinutes: Int,
    @Json(name = "meeting_type") val meetingType: String,
    val timezone: String,
    @Json(name = "client_notes") val clientNotes: String? = null,
)
