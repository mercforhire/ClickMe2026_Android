package com.uptrendinvestments.clickmeapp.model.requests

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * `POST /expert/booking-requests/{id}/accept` body. The backend `acceptSchema` is `.strict()` and a missing body
 * is rejected, so a null [message] is omitted by the converter and the body is exactly `{}`. The backend validates
 * the message (at most 1000 characters) and currently discards it (RESEARCH C10; backend follow-up recorded by
 * plan 11-15).
 */
@JsonClass(generateAdapter = true)
data class AcceptBookingRequestBody(
    val message: String? = null,
)

/**
 * `POST /expert/booking-requests/{id}/decline` body. The backend `declineSchema` is `.strict()` and requires both
 * keys, so neither is nullable and both are always serialized (D-01, D-02). [reasonCode] is one of `CONFLICT`,
 * `OUT_OF_SCOPE`, `UNAVAILABLE`, `OTHER`; [reasonText] is 1 to 1000 characters.
 */
@JsonClass(generateAdapter = true)
data class DeclineBookingRequestBody(
    @Json(name = "reason_code") val reasonCode: String,
    @Json(name = "reason_text") val reasonText: String,
)
