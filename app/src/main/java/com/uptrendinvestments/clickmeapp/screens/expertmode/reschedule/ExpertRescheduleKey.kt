package com.uptrendinvestments.clickmeapp.screens.expertmode.reschedule

import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RescheduleOpener
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingOrigin
import kotlinx.serialization.Serializable

/**
 * Nav3 route to the expert Reschedule picker (Phase 16.2), a full-screen push from Expert Booking Summary or the
 * expert Bookings list. The screen loads the booking by [bookingId]. [origin] only keeps content keys unique across
 * the Dashboard, Bookings and Chats stacks (the same trick as `ExpertCancelKey`). [opener] names the one-shot slot
 * that receives the success notice (D-10). Nothing here is a secret.
 */
@Serializable
data class ExpertRescheduleKey(
    val bookingId: String,
    val origin: ExpertBookingOrigin,
    val opener: RescheduleOpener,
) : NavKey
