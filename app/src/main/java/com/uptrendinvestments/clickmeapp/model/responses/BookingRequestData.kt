package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** `POST /bookings/request` 201 payload (free path). */
@JsonClass(generateAdapter = true)
data class BookingRequestData(
    @Json(name = "booking_id") val bookingId: String,
    val status: String? = null,
    val message: String? = null,
)
