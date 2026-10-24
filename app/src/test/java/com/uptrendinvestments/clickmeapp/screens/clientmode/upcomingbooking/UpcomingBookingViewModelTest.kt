package com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking

import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingStatusLabel
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.PaymentPill
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.bookingDetail
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.rescheduleDto
import com.uptrendinvestments.clickmeapp.util.PriceDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class UpcomingBookingViewModelTest {
    private val now = Instant.parse("2026-10-22T16:00:00Z")

    // ---- mapping ----

    @Test
    fun shortBookingId_uses_the_first_eight_characters_upper_cased() {
        assertEquals("#A1B2C3D4", shortBookingId("a1b2c3d4-e5f6-7890"))
        assertEquals("#B1", shortBookingId("b1"))
    }

    @Test
    fun pending_reschedule_maps_the_proposal_and_allows_cancel() {
        val details =
            bookingDetail(
                status = "pending_reschedule",
                reschedule = rescheduleDto(initiatedBy = "expert"),
            ).toUpcomingDetails(now)

        assertEquals(BookingStatusLabel.NewTimeProposed, details.status.label)
        assertTrue(details.canCancel)
        assertFalse(details.showsPendingBanner)
        assertNotNull(details.reschedule)
        assertTrue(details.reschedule!!.initiatedByExpert)
    }

    @Test
    fun pending_approval_shows_the_banner_and_allows_cancel() {
        val details = bookingDetail(status = "pending_approval").toUpcomingDetails(now)

        assertTrue(details.showsPendingBanner)
        assertTrue(details.canCancel)
        assertEquals(BookingStatusLabel.PendingApproval, details.status.label)
    }

    @Test
    fun confirmed_allows_cancel_and_has_no_proposal() {
        val details = bookingDetail(status = "confirmed").toUpcomingDetails(now)

        assertTrue(details.canCancel)
        assertFalse(details.showsPendingBanner)
        assertNull(details.reschedule)
        assertEquals(BookingStatusLabel.Confirmed, details.status.label)
    }

    @Test
    fun in_progress_and_completed_cannot_be_cancelled() {
        assertFalse(bookingDetail(status = "in_progress").toUpcomingDetails(now).canCancel)
        assertFalse(bookingDetail(status = "completed").toUpcomingDetails(now).canCancel)
    }

    @Test
    fun paid_held_booking_shows_money_and_the_hold_pill() {
        val details =
            bookingDetail(isFree = false, amount = 5000, currency = "CAD", paymentStatus = "held")
                .toUpcomingDetails(now)

        assertEquals(PriceDisplay.of(false, 5000, "CAD"), details.price)
        assertEquals(PaymentPill.HoldPlaced, details.paymentPill)
    }

    @Test
    fun free_booking_shows_free_and_no_pill() {
        val details = bookingDetail(isFree = true, paymentStatus = "held").toUpcomingDetails(now)

        assertEquals(PriceDisplay.Free, details.price)
        assertNull(details.paymentPill)
    }

    @Test
    fun paid_booking_with_no_payment_yet_has_no_pill() {
        assertNull(bookingDetail(isFree = false, paymentStatus = "none").toUpcomingDetails(now).paymentPill)
    }

    @Test
    fun blank_strings_map_to_null_and_link_and_type_are_copied() {
        val details =
            bookingDetail(
                expertTitle = "  ",
                clientNotes = "   ",
                meetingType = "skype_zoom",
                joinLink = "https://zoom.us/j/1",
            ).toUpcomingDetails(now)

        assertNull(details.expertTitle)
        assertNull(details.clientNotes)
        assertEquals("skype_zoom", details.meetingType)
        assertEquals("https://zoom.us/j/1", details.joinLink)
    }

    @Test
    fun blank_topic_title_maps_to_null() {
        val base = bookingDetail()
        val blank = base.copy(topic = base.topic.copy(title = " "))

        assertNull(blank.toUpcomingDetails(now).topicTitle)
        assertEquals("Algorithms", base.toUpcomingDetails(now).topicTitle)
    }

    @Test
    fun missing_end_time_falls_back_to_start_plus_duration() {
        val start = Instant.parse("2026-10-24T18:00:00Z")
        val details = bookingDetail(start = start, end = null).toUpcomingDetails(now)

        assertEquals(start, details.start)
        assertEquals(Instant.parse("2026-10-24T19:00:00Z"), details.end)
    }
}
