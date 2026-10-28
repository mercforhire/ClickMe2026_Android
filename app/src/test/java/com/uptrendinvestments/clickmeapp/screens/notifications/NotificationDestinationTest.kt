package com.uptrendinvestments.clickmeapp.screens.notifications

import com.uptrendinvestments.clickmeapp.screens.push.PushAudience
import com.uptrendinvestments.clickmeapp.screens.push.PushDestination
import com.uptrendinvestments.clickmeapp.testing.NOTIF_BOOKING_ID
import com.uptrendinvestments.clickmeapp.testing.notificationItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class NotificationDestinationTest {
    private fun destination(
        category: String,
        subCategory: String?,
        metaData: Map<String, Any?>?,
    ): PushDestination? =
        notificationItem(category = category, subCategory = subCategory, metaData = metaData)
            .toRowModel(Instant.parse("2026-10-28T19:00:00Z"), ZoneId.of("America/Toronto"))
            .destination

    private val withBooking = mapOf<String, Any?>("booking_id" to NOTIF_BOOKING_ID)

    @Test
    fun bookingConfirmationOpensTheBooking() {
        assertEquals(
            PushDestination.BookingDetail(NOTIF_BOOKING_ID, PushAudience.Client),
            destination("booking", "confirmations", withBooking),
        )
    }

    @Test
    fun sessionEndedOpensLeaveReview() {
        assertEquals(
            PushDestination.LeaveReview(NOTIF_BOOKING_ID, PushAudience.Either),
            destination("session", "ended", withBooking),
        )
    }

    @Test
    fun reviewNudgeReadsTheCamelCaseIdAlias() {
        assertEquals(
            PushDestination.LeaveReview(NOTIF_BOOKING_ID, PushAudience.Client),
            destination("review", "nudge", mapOf("bookingId" to NOTIF_BOOKING_ID)),
        )
    }

    @Test
    fun bookingRequestWithoutAnIdOpensIncomingRequests() {
        assertEquals(
            PushDestination.IncomingRequests(null),
            destination("booking", "requests", emptyMap()),
        )
    }

    @Test
    fun bookingRequestWithAnIdCarriesIt() {
        assertEquals(
            PushDestination.IncomingRequests(NOTIF_BOOKING_ID),
            destination("booking", "requests", withBooking),
        )
    }

    @Test
    fun payoutRowsAreInertUntilPhase14() {
        assertNull(destination("payout", "paid", mapOf("payout_id" to NOTIF_BOOKING_ID)))
        assertNull(destination("payout", "kyc_action", null))
    }

    @Test
    fun accountRowsAreInertUntilPhase14() {
        assertNull(destination("account", "password_changed", null))
    }

    @Test
    fun anUnknownCategoryIsInert() {
        assertNull(destination("promotions", "weekly", withBooking))
    }

    @Test
    fun aBookingRowWithNullMetaDataIsInert() {
        assertNull(destination("booking", "confirmations", null))
    }

    @Test
    fun aNonUuidBookingIdIsInert() {
        assertNull(destination("booking", "confirmations", mapOf("booking_id" to "../../etc")))
    }

    @Test
    fun aNonStringBookingIdIsInert() {
        assertNull(destination("booking", "confirmations", mapOf("booking_id" to 12345.0)))
    }

    @Test
    fun toStringHidesTitleAndBody() {
        val row =
            notificationItem(title = "Secret title", body = "Secret body")
                .toRowModel(Instant.parse("2026-10-28T19:00:00Z"), ZoneId.of("UTC"))
        assertEquals("NotificationRowModel(id=n1, category=booking)", row.toString())
    }
}
