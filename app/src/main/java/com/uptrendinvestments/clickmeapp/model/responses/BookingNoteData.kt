package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * One private booking note. Ports iOS `BookingNote`. [note] is null when the note is empty. The text is private
 * and user-written, so [toString] never prints it.
 */
@JsonClass(generateAdapter = true)
data class BookingNoteData(
    @Json(name = "booking_id") val bookingId: String,
    val note: String? = null,
) {
    override fun toString(): String = "BookingNoteData(bookingId=$bookingId, hasNote=${note != null})"
}
