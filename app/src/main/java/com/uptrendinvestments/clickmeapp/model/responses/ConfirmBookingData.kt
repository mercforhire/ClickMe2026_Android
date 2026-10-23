package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** `POST /bookings/confirm` 201 payload (paid path). */
@JsonClass(generateAdapter = true)
data class ConfirmBookingData(
    @Json(name = "booking_id") val bookingId: String,
    val status: String? = null,
    val message: String? = null,
    @Json(name = "estimated_confirmation_time") val estimatedConfirmationTime: String? = null,
)
