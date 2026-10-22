package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Nav3 route to Make a Booking (D-04). Mirrors the iOS MakeABooking inputs (expert id, name, title, image).
 * This is the real route Phase 6 keeps: Phase 6 replaces the body of `MakeABookingStubScreen`, not this key.
 * Display seed only; the server re-checks bookability when the booking is confirmed.
 */
@Serializable
data class BookingKey(
    val expertId: String,
    val expertName: String,
    val expertTitle: String,
    val expertImageUrl: String?,
) : NavKey
