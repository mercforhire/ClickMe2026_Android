package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookingsLaunchRequestTest {
    @Test
    fun newHolderIsEmpty() {
        val holder = BookingsLaunchRequest()
        assertNull(holder.pending.value)
        assertNull(holder.consume())
    }

    @Test
    fun consumeReturnsThePostedLaunchOnce() {
        val holder = BookingsLaunchRequest()
        val launch = BookingsLaunch(BookingsTab.Past, BookingsNotice.Cancelled)
        holder.post(launch)
        assertEquals(launch, holder.pending.value)
        assertEquals(launch, holder.consume())
        assertNull(holder.pending.value)
        assertNull(holder.consume())
    }

    @Test
    fun latestPostWins() {
        val holder = BookingsLaunchRequest()
        holder.post(BookingsLaunch(BookingsTab.Upcoming))
        val second = BookingsLaunch(BookingsTab.Past, BookingsNotice.CancelledWithRefund("$25.00"))
        holder.post(second)
        assertEquals(second, holder.consume())
    }

    @Test
    fun launchHasNoNoticeByDefault() {
        assertNull(BookingsLaunch(BookingsTab.Upcoming).notice)
    }
}
