package com.uptrendinvestments.clickmeapp.screens.expertmode.confirmed

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Nav3 route to Booking Confirmed. It replaces Request Decision after an accept (D-04).
 *
 * It carries everything the screen shows, because `replaceTop` clears the decision ViewModel and the accept
 * response has none of these fields (Pitfall 8), so Confirmed never fetches. Times are epoch millis because
 * `Instant` is not `@Serializable`. [clientMessage] is untrusted plain text from the client; render it as text only.
 */
@Serializable
data class BookingConfirmedKey(
    val bookingId: String,
    val clientName: String?,
    val clientAvatarUrl: String?,
    val topic: String?,
    val startMillis: Long,
    val endMillis: Long,
    val meetingType: String?,
    val clientMessage: String?,
) : NavKey
