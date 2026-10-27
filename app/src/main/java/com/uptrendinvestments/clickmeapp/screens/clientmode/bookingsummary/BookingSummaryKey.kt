package com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary

import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.StackOrigin
import kotlinx.serialization.Serializable

/**
 * Nav3 route to Booking Summary (BOOK-08). Opened from a Past booking card; Phase 13 push routing reuses it. Carries
 * only the booking id, so the screen fetches `GET /client/bookings/:id` itself and re-fetches after process death.
 * The id is not a secret. [origin] is [StackOrigin.Chats] when a chat thread pushed it and only keeps the key
 * distinct across stacks.
 */
@Serializable
data class BookingSummaryKey(
    val bookingId: String,
    val origin: StackOrigin = StackOrigin.Default,
) : NavKey
