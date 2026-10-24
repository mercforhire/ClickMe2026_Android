package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * `POST /bookings/{id}/end` data.
 *
 * @property status always the unchanged booking status after the 2026-08-16 backend change, so callers must never
 * branch on it (CALL-END-SEMANTICS).
 */
@JsonClass(generateAdapter = true)
data class EndCallData(
    @Json(name = "booking_id") val bookingId: String? = null,
    val status: String? = null,
    @Json(name = "actual_duration_secs") val actualDurationSecs: Int? = null,
    @Json(name = "end_reason") val endReason: String? = null,
    @Json(name = "review_seam_triggered") val reviewSeamTriggered: Boolean = false,
)
