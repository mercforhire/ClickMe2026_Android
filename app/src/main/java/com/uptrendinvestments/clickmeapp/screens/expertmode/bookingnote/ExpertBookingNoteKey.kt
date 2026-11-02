package com.uptrendinvestments.clickmeapp.screens.expertmode.bookingnote

import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingItem
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingOrigin
import kotlinx.serialization.Serializable

/**
 * Nav3 route to the shared private note editor in the expert role (D-05).
 *
 * [clientName], [topic] and the session times seed the editor header, because the note endpoints do not return them.
 * Times are epoch millis because `Instant` is not `@Serializable`. Never add the note text to a key: keys are written
 * to saved navigation state, and the note is private (the editor fetches it by [bookingId]).
 *
 * [origin] is copied from the opener and only keeps the key distinct across stacks (D-06):
 * [ExpertBookingOrigin.Bookings] from a Bookings card, the summary's origin from Expert Booking Summary.
 */
@Serializable
data class ExpertBookingNoteKey(
    val bookingId: String,
    val clientName: String?,
    val topic: String?,
    val startMillis: Long,
    val endMillis: Long,
    val origin: ExpertBookingOrigin,
) : NavKey

/** The note key for a Bookings card; the origin is always `Bookings` because the cards live only on that tab. */
fun ExpertBookingItem.noteKey(): ExpertBookingNoteKey =
    ExpertBookingNoteKey(
        bookingId = bookingId,
        clientName = client.name,
        topic = topic,
        startMillis = startTime.toEpochMilli(),
        endMillis = endTime.toEpochMilli(),
        origin = ExpertBookingOrigin.Bookings,
    )
