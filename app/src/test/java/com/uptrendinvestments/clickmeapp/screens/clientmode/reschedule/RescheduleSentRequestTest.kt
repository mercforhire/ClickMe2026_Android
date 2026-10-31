package com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RescheduleSentRequestTest {
    @Test
    fun postsOnlyTheOpenersSlot() {
        val store = RescheduleSentRequest()
        store.onSent(RescheduleOpener.MyBookings, "b1")
        assertEquals(RescheduleSent("b1"), store.myBookings.pending.value)
        assertNull(store.upcomingDetails.pending.value)
        assertNull(store.expertSummary.pending.value)
        assertNull(store.expertBookings.pending.value)
    }

    @Test
    fun slotForMapsEachOpenerToItsOwnSlot() {
        val store = RescheduleSentRequest()
        assertTrue(store.slotFor(RescheduleOpener.UpcomingDetails) === store.upcomingDetails)
        assertTrue(store.slotFor(RescheduleOpener.MyBookings) === store.myBookings)
        assertTrue(store.slotFor(RescheduleOpener.ExpertSummary) === store.expertSummary)
        assertTrue(store.slotFor(RescheduleOpener.ExpertBookings) === store.expertBookings)
    }

    @Test
    fun consumeForMatchingBookingOnly() {
        val store = RescheduleSentRequest()
        store.onSent(RescheduleOpener.UpcomingDetails, "b1")
        assertFalse(store.consumeFor(RescheduleOpener.UpcomingDetails, "b2"))
        assertEquals(RescheduleSent("b1"), store.upcomingDetails.pending.value)
        assertTrue(store.consumeFor(RescheduleOpener.UpcomingDetails, "b1"))
        assertNull(store.upcomingDetails.pending.value)
        assertFalse(store.consumeFor(RescheduleOpener.UpcomingDetails, "b1"))
    }

    @Test
    fun consumeForAnyBookingWhenIdNull() {
        val store = RescheduleSentRequest()
        store.onSent(RescheduleOpener.ExpertBookings, "b7")
        assertTrue(store.consumeFor(RescheduleOpener.ExpertBookings, null))
        assertNull(store.expertBookings.pending.value)
    }

    @Test
    fun consumeForNothingPendingIsFalse() {
        assertFalse(RescheduleSentRequest().consumeFor(RescheduleOpener.MyBookings, null))
    }

    @Test
    fun clearSessionDataEmptiesAllSlots() =
        runTest {
            val store = RescheduleSentRequest()
            RescheduleOpener.entries.forEach { store.onSent(it, "b1") }
            store.clearSessionData()
            RescheduleOpener.entries.forEach { assertNull(store.slotFor(it).pending.value) }
        }
}
