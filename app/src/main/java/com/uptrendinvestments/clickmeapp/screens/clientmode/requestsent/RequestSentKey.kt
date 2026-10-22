package com.uptrendinvestments.clickmeapp.screens.clientmode.requestsent

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Nav3 route to Request Sent (BOOK-04). Carries only the booking id; the screen fetches
 * `GET /client/bookings/:id` itself (D-14). It is pushed with `Navigator.replaceTop`, so it replaces
 * Make a Booking and the filled form cannot be re-submitted (D-12). The id is not a secret and lives
 * only in the saved back stack.
 */
@Serializable
data class RequestSentKey(
    val bookingId: String,
) : NavKey
