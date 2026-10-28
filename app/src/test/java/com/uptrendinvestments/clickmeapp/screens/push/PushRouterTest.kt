package com.uptrendinvestments.clickmeapp.screens.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PushRouterTest {
    private val b1 = "0a1b2c3d-1111-4222-8333-444455556666"
    private val t1 = "9f8e7d6c-aaaa-4bbb-8ccc-ddddeeeeffff"

    private fun event(
        category: String?,
        sub: String? = null,
        booking: String? = b1,
        thread: String? = null,
    ) = PushEvent(
        category = category,
        subCategory = sub,
        bookingId = booking,
        threadId = thread,
        reviewId = null,
        payoutId = null,
        eventType = null,
    )

    private fun route(
        category: String?,
        sub: String? = null,
        booking: String? = b1,
        thread: String? = null,
    ) = PushRouter.destination(event(category, sub, booking, thread))

    @Test
    fun bookingPushRoutesToBookingDetail() {
        val parsed =
            PushEvent.from(
                mapOf("category" to "booking", "sub_category" to "confirmations", "booking_id" to b1),
            )
        assertNotNull(parsed)
        assertEquals(
            PushDestination.BookingDetail(b1, PushAudience.Client),
            PushRouter.destination(parsed!!),
        )
    }

    @Test
    fun iosTableRowByRow() {
        assertEquals(PushDestination.IncomingRequests(b1), route("booking", "requests"))
        assertEquals(PushDestination.IncomingRequests(null), route("booking", "requests", booking = null))
        assertEquals(PushDestination.BookingDetail(b1, PushAudience.Either), route("booking", "cancellations"))
        assertEquals(PushDestination.BookingDetail(b1, PushAudience.Either), route("booking", "reschedules"))
        assertEquals(PushDestination.BookingDetail(b1, PushAudience.Client), route("booking", "declines"))
        assertEquals(PushDestination.BookingDetail(b1, PushAudience.Client), route("booking", "expirations"))
        assertEquals(PushDestination.BookingDetail(b1, PushAudience.Client), route("booking", "refunds"))
        assertEquals(PushDestination.BookingDetail(b1, PushAudience.Client), route("booking", "confirmations"))
        assertEquals(PushDestination.BookingDetail(b1, PushAudience.Either), route("booking", null))
        assertEquals(PushDestination.BookingDetail(b1, PushAudience.Either), route("session", "reminder"))
        assertEquals(PushDestination.BookingDetail(b1, PushAudience.Either), route("session", "no_show"))
        assertEquals(PushDestination.LeaveReview(b1, PushAudience.Either), route("session", "ended"))
        assertEquals(PushDestination.ChatThread(t1), route("message", "new_message", booking = null, thread = t1))
        assertEquals(PushDestination.LeaveReview(b1, PushAudience.Client), route("review", "nudge"))
        assertEquals(PushDestination.BookingDetail(b1, PushAudience.Expert), route("review", "received"))
        assertEquals(PushDestination.KycOnboarding, route("payout", "kyc_action", booking = null))
        assertEquals(PushDestination.Payouts, route("payout", "paid", booking = null))
        assertEquals(PushDestination.Payouts, route("payout", "failed", booking = null))
        assertEquals(PushDestination.AccountSecurity, route("account", "password_changed", booking = null))
        assertEquals(PushDestination.AccountSecurity, route("account", "deletion_requested", booking = null))
    }

    @Test
    fun missingIdsGiveNull() {
        assertNull(route("booking", "confirmations", booking = null))
        assertNull(route("session", "ended", booking = null))
        assertNull(route("review", "nudge", booking = null))
        assertNull(route("review", null, booking = null))
        assertNull(route("message", "new_message", booking = null, thread = null))
        assertNull(route("promo"))
    }

    @Test
    fun destinationOrListFallsBackToList() {
        assertEquals(PushDestination.NotificationsList, PushRouter.destinationOrList(null))
        assertEquals(PushDestination.NotificationsList, PushRouter.destinationOrList(event("promo")))
        assertEquals(
            PushDestination.BookingDetail(b1, PushAudience.Either),
            PushRouter.destinationOrList(event("booking")),
        )
    }

    @Test
    fun landsOnNotificationsList() {
        assertTrue(PushDestination.NotificationsList.landsOnNotificationsList)
        assertTrue(PushDestination.Payouts.landsOnNotificationsList)
        assertTrue(PushDestination.KycOnboarding.landsOnNotificationsList)
        assertTrue(PushDestination.AccountSecurity.landsOnNotificationsList)
        assertFalse(PushDestination.BookingDetail(b1, PushAudience.Either).landsOnNotificationsList)
        assertFalse(PushDestination.IncomingRequests(b1).landsOnNotificationsList)
        assertFalse(PushDestination.ChatThread(t1).landsOnNotificationsList)
        assertFalse(PushDestination.LeaveReview(b1, PushAudience.Client).landsOnNotificationsList)
    }

    @Test
    fun cronFallbackRoutesByBooking() {
        val parsed = PushEvent.from(mapOf("booking_id" to b1))!!
        assertEquals(
            PushDestination.BookingDetail(b1, PushAudience.Either),
            PushRouter.destination(parsed),
        )
    }
}
