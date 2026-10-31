@file:Suppress("ktlint:standard:filename")

package com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule

import com.uptrendinvestments.clickmeapp.managers.SessionDataCleaner
import com.uptrendinvestments.clickmeapp.screens.expertmode.OneShotSlot
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/** The screen that opened the reschedule picker, and so the one slot the success notice is handed to (D-09). */
@Serializable
enum class RescheduleOpener { UpcomingDetails, MyBookings, ExpertSummary, ExpertBookings }

/** "The reschedule request for [bookingId] was sent": the opener reloads and shows its notice (D-09, D-10). */
data class RescheduleSent(
    val bookingId: String,
)

/**
 * Hand-off from a sent reschedule request to the screen that opened the picker (D-09, D-10, RS-R1).
 *
 * A singleton and not a navigation result because the origin may be covered or recreated when the send completes
 * (RESEARCH Pattern 6). There is one slot per origin so one consumer never swallows another's notice. Memory only:
 * it holds a booking id, never a card id or a note, and is cleared on logout.
 */
@Singleton
class RescheduleSentRequest
    @Inject
    constructor() : SessionDataCleaner {
        val upcomingDetails = OneShotSlot<RescheduleSent>()
        val myBookings = OneShotSlot<RescheduleSent>()
        val expertSummary = OneShotSlot<RescheduleSent>()
        val expertBookings = OneShotSlot<RescheduleSent>()

        fun slotFor(opener: RescheduleOpener): OneShotSlot<RescheduleSent> =
            when (opener) {
                RescheduleOpener.UpcomingDetails -> upcomingDetails
                RescheduleOpener.MyBookings -> myBookings
                RescheduleOpener.ExpertSummary -> expertSummary
                RescheduleOpener.ExpertBookings -> expertBookings
            }

        /** Posts the sent notice for [bookingId] to the slot of the [opener] screen only. */
        fun onSent(
            opener: RescheduleOpener,
            bookingId: String,
        ) {
            slotFor(opener).post(RescheduleSent(bookingId))
        }

        /**
         * Consumes the [opener]'s pending notice and returns true when something is pending and either [bookingId] is
         * null (a list origin) or equals the pending booking id (a detail origin, so a details screen for another
         * booking never swallows it). Otherwise leaves the notice in place and returns false.
         */
        fun consumeFor(
            opener: RescheduleOpener,
            bookingId: String?,
        ): Boolean {
            val slot = slotFor(opener)
            val pending = slot.pending.value ?: return false
            if (bookingId != null && pending.bookingId != bookingId) return false
            return slot.consume() != null
        }

        override suspend fun clearSessionData() {
            upcomingDetails.consume()
            myBookings.consume()
            expertSummary.consume()
            expertBookings.consume()
        }
    }
