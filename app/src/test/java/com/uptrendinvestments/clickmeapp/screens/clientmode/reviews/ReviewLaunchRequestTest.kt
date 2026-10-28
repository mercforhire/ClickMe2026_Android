package com.uptrendinvestments.clickmeapp.screens.clientmode.reviews

import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.ReviewOpener
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReviewLaunchRequestTest {
    private val request = ReviewLaunchRequest()

    @Test
    fun openerPicksTheSlot() {
        request.onSubmitted("B1", ReviewOpener.MyBookings)
        assertNull(request.summaryReviewed.pending.value)
        assertEquals(ReviewSubmitted("B1"), request.bookingsReviewed.pending.value)
        assertEquals(setOf("B1"), request.reviewedBookingIds.value)
    }

    @Test
    fun summaryOpenerFillsTheSummarySlotOnly() {
        request.onSubmitted("B2", ReviewOpener.BookingSummary)
        assertEquals(ReviewSubmitted("B2"), request.summaryReviewed.pending.value)
        assertNull(request.bookingsReviewed.pending.value)
    }

    @Test
    fun clearSessionDataClearsEverything() =
        runTest {
            request.onSubmitted("B1", ReviewOpener.BookingSummary)
            request.onSubmitted("B2", ReviewOpener.MyBookings)
            request.clearSessionData()
            assertNull(request.summaryReviewed.pending.value)
            assertNull(request.bookingsReviewed.pending.value)
            assertEquals(emptySet<String>(), request.reviewedBookingIds.value)
        }
}
