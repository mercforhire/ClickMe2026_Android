package com.uptrendinvestments.clickmeapp.screens.expertmode

import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsLaunch
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsNotice
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpertLaunchRequestTest {
    @Test
    fun newStoreIsEmpty() {
        val store = ExpertLaunchRequest()
        assertNull(store.incomingRequests.pending.value)
        assertNull(store.dashboard.pending.value)
        assertNull(store.bookings.pending.value)
    }

    @Test
    fun slotsAreIndependent() {
        val store = ExpertLaunchRequest()
        store.incomingRequests.post(IncomingRequestsLaunch(IncomingRequestsNotice.Declined))
        assertNull(store.dashboard.consume())
        assertNull(store.bookings.consume())
        assertEquals(IncomingRequestsLaunch(IncomingRequestsNotice.Declined), store.incomingRequests.pending.value)
    }

    @Test
    fun consumeReturnsOnceThenNull() {
        val store = ExpertLaunchRequest()
        val launch = IncomingRequestsLaunch(IncomingRequestsNotice.Declined)
        store.incomingRequests.post(launch)
        assertEquals(launch, store.incomingRequests.pending.value)
        assertEquals(launch, store.incomingRequests.consume())
        assertNull(store.incomingRequests.pending.value)
        assertNull(store.incomingRequests.consume())
    }

    @Test
    fun laterPostReplacesUnconsumed() {
        val store = ExpertLaunchRequest()
        store.bookings.post(BookingsLaunch(BookingsTab.Upcoming))
        val second = BookingsLaunch(BookingsTab.Past, BookingsNotice.CancelledWithRefund("$25.00"))
        store.bookings.post(second)
        assertEquals(second, store.bookings.consume())
        assertNull(store.bookings.consume())
    }

    @Test
    fun launchWithoutNoticeJustMeansReload() {
        assertNull(IncomingRequestsLaunch().notice)
        val store = ExpertLaunchRequest()
        store.dashboard.post(DashboardLaunch)
        assertEquals(DashboardLaunch, store.dashboard.consume())
        assertNull(store.dashboard.consume())
    }
}
