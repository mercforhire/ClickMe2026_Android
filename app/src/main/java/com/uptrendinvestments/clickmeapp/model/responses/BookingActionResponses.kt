@file:Suppress("ktlint:standard:filename")

package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * `POST /client/bookings/{id}/cancel` data.
 *
 * @property refundAmount MAJOR units (`minor / 10^digits`, may be a decimal such as 12.5), unlike every other
 * amount in the app. Format only with `Money.formatMajor`. A lost concurrent-cancel race is a 200 with 0 (D-07).
 * @property currency upper-case ISO code.
 */
@JsonClass(generateAdapter = true)
data class CancelBookingData(
    @Json(name = "refund_amount") val refundAmount: Double = 0.0,
    val currency: String? = null,
)

/** `POST /client/bookings/{id}/accept-reschedule` and `.../decline-reschedule` data (shared). */
@JsonClass(generateAdapter = true)
data class RescheduleResponseData(
    @Json(name = "booking_id") val bookingId: String? = null,
    val status: String? = null,
)
