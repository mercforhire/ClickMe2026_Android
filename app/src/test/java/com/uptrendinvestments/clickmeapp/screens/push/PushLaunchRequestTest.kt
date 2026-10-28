package com.uptrendinvestments.clickmeapp.screens.push

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PushLaunchRequestTest {
    private val first = PushDestination.ChatThread("11111111-1111-1111-1111-111111111111")
    private val second = PushDestination.NotificationsList

    @Test
    fun newPostReplacesUnconsumed() {
        val request = PushLaunchRequest()
        request.destination.post(first)
        request.destination.post(second)
        assertEquals(second, request.destination.consume())
        assertNull(request.destination.consume())
    }

    @Test
    fun clearSessionDataDropsPending() =
        runTest {
            val request = PushLaunchRequest()
            request.destination.post(first)
            request.clearSessionData()
            assertNull(request.destination.pending.value)
        }

    @Test
    fun foregroundArrivalIncrementsCounter() {
        val request = PushLaunchRequest()
        assertEquals(0L, request.arrivals.value)
        request.onForegroundArrival()
        assertEquals(1L, request.arrivals.value)
        request.onForegroundArrival()
        assertEquals(2L, request.arrivals.value)
    }
}
