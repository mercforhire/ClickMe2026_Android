package com.uptrendinvestments.clickmeapp.model.requests

import com.squareup.moshi.JsonClass

/**
 * Body of both note POSTs (`/client/bookings/{id}/note` and `/bookings/{id}/note`). The backend schema is strict:
 * only `note`, at most 5,000 UTF-16 units, and an empty string clears the note. The text is user-written, so
 * [toString] prints the length only.
 */
@JsonClass(generateAdapter = true)
data class BookingNoteRequest(
    val note: String,
) {
    override fun toString(): String = "BookingNoteRequest(noteLength=${note.length})"
}
