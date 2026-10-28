package com.uptrendinvestments.clickmeapp.screens.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PushRouterTest {
    private val b1 = "0a1b2c3d-1111-4222-8333-444455556666"

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
}
