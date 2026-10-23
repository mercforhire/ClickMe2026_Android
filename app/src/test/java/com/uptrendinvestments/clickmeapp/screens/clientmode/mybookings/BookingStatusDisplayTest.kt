package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookingStatusDisplayTest {
    @Test
    fun pastStatusLabelsAndTones() {
        assertEquals(
            BookingStatusDisplay(BookingStatusLabel.Completed, StatusTone.Primary, "completed"),
            pastStatusDisplay("completed"),
        )
        listOf("cancelled", "declined").forEach {
            assertEquals(
                BookingStatusDisplay(BookingStatusLabel.Cancelled, StatusTone.Error, it),
                pastStatusDisplay(it),
            )
        }
        listOf("missed", "expired").forEach {
            assertEquals(
                BookingStatusDisplay(BookingStatusLabel.Missed, StatusTone.Tertiary, it),
                pastStatusDisplay(it),
            )
        }
    }

    @Test
    fun pastStatusDropsNonPastStatuses() {
        assertNull(pastStatusDisplay("confirmed"))
        assertNull(pastStatusDisplay("rescheduled"))
        assertNull(pastStatusDisplay("weird"))
    }

    @Test
    fun detailStatusShowsTheRealStatus() {
        assertEquals(
            BookingStatusDisplay(BookingStatusLabel.Confirmed, StatusTone.Primary, "confirmed"),
            detailStatusDisplay("confirmed"),
        )
        assertEquals(
            BookingStatusDisplay(BookingStatusLabel.PendingApproval, StatusTone.Tertiary, "pending_approval"),
            detailStatusDisplay("pending_approval"),
        )
        assertEquals(
            BookingStatusDisplay(BookingStatusLabel.NewTimeProposed, StatusTone.Tertiary, "pending_reschedule"),
            detailStatusDisplay("pending_reschedule"),
        )
        assertEquals(
            BookingStatusDisplay(BookingStatusLabel.InProgress, StatusTone.Primary, "in_progress"),
            detailStatusDisplay("in_progress"),
        )
        assertEquals(
            BookingStatusDisplay(BookingStatusLabel.Completed, StatusTone.Primary, "completed"),
            detailStatusDisplay("completed"),
        )
    }

    @Test
    fun detailStatusFallsBackToNeutralOther() {
        assertEquals(
            BookingStatusDisplay(BookingStatusLabel.Other, StatusTone.Neutral, "no_show_both"),
            detailStatusDisplay("no_show_both"),
        )
    }

    @Test
    fun summaryStatusUsesPastTableThenDetailTable() {
        assertEquals(BookingStatusLabel.Cancelled, summaryStatusDisplay("declined").label)
        assertEquals(BookingStatusLabel.NewTimeProposed, summaryStatusDisplay("pending_reschedule").label)
        assertEquals(BookingStatusLabel.Confirmed, summaryStatusDisplay("confirmed").label)
        assertEquals(BookingStatusLabel.Missed, summaryStatusDisplay("expired").label)
    }

    @Test
    fun humanizeStatusReplacesUnderscoresAndCapitalizes() {
        assertEquals("No show both", humanizeStatus("no_show_both"))
        assertEquals("Rescheduled", humanizeStatus("rescheduled"))
        assertEquals("", humanizeStatus(""))
    }

    @Test
    fun paymentPillOnlyForPaidBookingsWithAMoneyState() {
        assertEquals(PaymentPill.HoldPlaced, paymentPillFor(isFree = false, paymentStatus = "held"))
        assertEquals(PaymentPill.Paid, paymentPillFor(isFree = false, paymentStatus = "captured"))
        assertEquals(PaymentPill.Refunded, paymentPillFor(isFree = false, paymentStatus = "refunded"))
        assertNull(paymentPillFor(isFree = false, paymentStatus = "none"))
        assertNull(paymentPillFor(isFree = false, paymentStatus = null))
        assertNull(paymentPillFor(isFree = true, paymentStatus = "held"))
    }

    @Test
    fun cancellableStatusesAreTheBackendAllowList() {
        assertEquals(setOf("confirmed", "pending_approval", "pending_reschedule"), CANCELLABLE_BOOKING_STATUSES)
    }
}
