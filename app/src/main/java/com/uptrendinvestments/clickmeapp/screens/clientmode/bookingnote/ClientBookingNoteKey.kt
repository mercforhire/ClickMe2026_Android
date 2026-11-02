package com.uptrendinvestments.clickmeapp.screens.clientmode.bookingnote

import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingItem
import com.uptrendinvestments.clickmeapp.navigation.StackOrigin
import kotlinx.serialization.Serializable

/**
 * Nav3 route to the shared private note editor in the client role (D-05).
 *
 * [expertName], [topic] and the session times seed the editor header, because the note endpoints do not return them.
 * Times are epoch millis because `Instant` is not `@Serializable`. Never add the note text to a key: keys are written
 * to saved navigation state, and the note is private (the editor fetches it by [bookingId]).
 *
 * [origin] is copied from the opener and only keeps the key distinct across stacks, so the same booking's note open on
 * two stacks never shares a content key (D-06): [StackOrigin.Default] from a Bookings card, the summary's origin from
 * Booking Summary.
 */
@Serializable
data class ClientBookingNoteKey(
    val bookingId: String,
    val expertName: String?,
    val topic: String?,
    val startMillis: Long,
    val endMillis: Long,
    val origin: StackOrigin = StackOrigin.Default,
) : NavKey

/** The note key for a Bookings card; the origin stays [StackOrigin.Default] because the cards live on that tab. */
fun ClientBookingItem.noteKey(): ClientBookingNoteKey =
    ClientBookingNoteKey(
        bookingId = bookingId,
        expertName = expert.fullName,
        topic = topic,
        startMillis = startTime.toEpochMilli(),
        endMillis = endTime.toEpochMilli(),
    )
