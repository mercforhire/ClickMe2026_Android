package com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Nav3 route to Booking Summary (BOOK-08). Opened from a Past booking card; Phase 13 push routing reuses it. Carries
 * only the booking id, so the screen fetches `GET /client/bookings/:id` itself and re-fetches after process death.
 * The id is not a secret.
 */
@Serializable
data class BookingSummaryKey(
    val bookingId: String,
) : NavKey
