package com.uptrendinvestments.clickmeapp.model.requests

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Body of `POST /feedback/submit`. The backend schema is `.strict()`: `type_id` and `details` (at least 10
 * characters) are required, and `email` is optional but must be valid when present. Moshi omits a null, so an empty
 * email is sent as null and never as an empty string. `metadata` is not sent. The text and the email are
 * user-written, so [toString] prints the length and a presence flag only.
 */
@JsonClass(generateAdapter = true)
data class SubmitFeedbackRequest(
    @Json(name = "type_id") val typeId: String,
    val details: String,
    val email: String? = null,
) {
    override fun toString(): String =
        "SubmitFeedbackRequest(typeId=$typeId, detailsLength=${details.length}, hasEmail=${email != null})"
}
