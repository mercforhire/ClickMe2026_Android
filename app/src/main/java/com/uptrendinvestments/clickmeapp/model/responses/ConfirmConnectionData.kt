package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** `PATCH /bookings/{id}/confirm-connection` data. Every field is defaulted so a missing key never fails decode. */
@JsonClass(generateAdapter = true)
data class ConfirmConnectionData(
    @Json(name = "booking_id") val bookingId: String? = null,
    val status: String? = null,
    @Json(name = "connection_status") val connectionStatus: String? = null,
    val method: String? = null,
)
