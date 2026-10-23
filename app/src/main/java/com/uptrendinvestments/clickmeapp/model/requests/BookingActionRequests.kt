@file:Suppress("ktlint:standard:filename")

package com.uptrendinvestments.clickmeapp.model.requests

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * `POST /client/bookings/{id}/cancel` body. The backend schema is `.strict()`: `reason` is the only accepted key,
 * so the comment is merged into it by the caller as "label: comment" (D-05), at most 500 UTF-16 units. Null is
 * omitted by the converter (no `withNullSerialization`).
 */
@JsonClass(generateAdapter = true)
data class CancelBookingRequest(
    val reason: String? = null,
)

/**
 * `POST /client/bookings/{id}/accept-reschedule` body. The backend schema is `.strict()`. Null is omitted by the
 * converter (no `withNullSerialization`), so the body is `{}`; with a card it is
 * `{"stripe_payment_method_id":"pm_..."}` (D-02).
 */
@JsonClass(generateAdapter = true)
data class AcceptRescheduleRequest(
    @Json(name = "stripe_payment_method_id") val stripePaymentMethodId: String? = null,
)

/**
 * `POST /client/bookings/{id}/decline-reschedule` body. The backend schema is `.strict()`. Null is omitted so the
 * body is `{}`; `reason` is trimmed and capped at 500 UTF-16 units by the caller (D-04).
 */
@JsonClass(generateAdapter = true)
data class DeclineRescheduleRequest(
    val reason: String? = null,
)
