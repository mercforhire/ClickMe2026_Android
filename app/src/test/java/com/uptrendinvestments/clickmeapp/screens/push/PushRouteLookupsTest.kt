package com.uptrendinvestments.clickmeapp.screens.push

import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.ClientBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.bookingDetail
import com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests.BookingRequestsRepository
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import com.uptrendinvestments.clickmeapp.testing.bookingRequestItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Instant

class PushRouteLookupsTest {
    private val api = FakeClickMeApi()
    private val clock = MutableClock(Instant.parse("2026-10-22T16:00:00Z"))
    private val probe = RepositoryClientBookingProbe(ClientBookingsRepository(api))
    private val pending = RepositoryPendingRequestLookup(BookingRequestsRepository(api), clock)

    private fun httpError(status: Int) =
        api.httpError(status, """{"status":"error","code":"NOT_FOUND","message":"msg"}""")

    @Test
    fun probeMapsStatusCodes() =
        runTest {
            api.clientBookingDetailHandler = { bookingDetail(id = it, status = "confirmed") }
            assertEquals(ClientBookingProbeResult.Client("confirmed"), probe.probe("b1"))

            api.clientBookingDetailHandler = { throw httpError(404) }
            assertEquals(ClientBookingProbeResult.NotClient, probe.probe("b1"))

            api.clientBookingDetailHandler = { throw httpError(403) }
            assertEquals(ClientBookingProbeResult.NotClient, probe.probe("b1"))

            api.clientBookingDetailHandler = { throw httpError(500) }
            assertEquals(ClientBookingProbeResult.Unknown, probe.probe("b1"))

            api.clientBookingDetailHandler = { throw IOException("offline") }
            assertEquals(ClientBookingProbeResult.Unknown, probe.probe("b1"))
        }

    @Test
    fun pendingLookupBuildsKeyWithExpiry() =
        runTest {
            val expiry = clock.instant().plusSeconds(7_200)
            api.bookingRequestsHandler = { listOf(bookingRequestItem(id = "R1", expiresAt = expiry)) }

            val key = pending.decisionKeyFor("r1")

            assertNotNull(key)
            assertEquals("R1", key?.requestId)
            assertFalse(key?.isExpired ?: true)
            assertEquals(expiry.toEpochMilli(), key?.expiresAtMillis)
        }

    @Test
    fun pendingLookupMarksAnElapsedExpiryAsExpired() =
        runTest {
            api.bookingRequestsHandler =
                { listOf(bookingRequestItem(id = "R1", expiresAt = clock.instant().minusSeconds(60))) }

            assertTrue(pending.decisionKeyFor("R1")?.isExpired == true)
        }

    @Test
    fun pendingLookupAnswersNullForAnAbsentIdAndAFailedList() =
        runTest {
            api.bookingRequestsHandler = { listOf(bookingRequestItem(id = "other")) }
            assertNull(pending.decisionKeyFor("R1"))

            api.bookingRequestsHandler = { throw IOException("offline") }
            assertNull(pending.decisionKeyFor("R1"))
        }
}
