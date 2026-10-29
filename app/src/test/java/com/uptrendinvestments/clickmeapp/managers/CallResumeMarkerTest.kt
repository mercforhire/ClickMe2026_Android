package com.uptrendinvestments.clickmeapp.managers

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.screens.push.PushAudience
import com.uptrendinvestments.clickmeapp.screens.push.PushDestination
import com.uptrendinvestments.clickmeapp.screens.push.PushLaunchRequest
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.testPreferencesDataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class CallResumeMarkerTest {
    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private class Setup(
        val h: CallCenterHarness,
        val dataStore: DataStore<Preferences>,
        val push: PushLaunchRequest,
        val notice: CallResumeNotice,
        val marker: CallResumeMarker,
    )

    private fun TestScope.setup(): Setup {
        val h = CallCenterHarness(backgroundScope)
        val dataStore = testPreferencesDataStore(backgroundScope, tmp, "call_resume.preferences_pb")
        val push = PushLaunchRequest()
        val notice = CallResumeNotice()
        val marker = CallResumeMarker(dataStore, h.center, push, notice, h.clock, backgroundScope)
        return Setup(h, dataStore, push, notice, marker)
    }

    private suspend fun Setup.seed(
        bookingId: String,
        scheduledEnd: Instant,
    ) {
        dataStore.edit {
            it[CallResumeMarker.KEY_BOOKING_ID] = bookingId
            it[CallResumeMarker.KEY_SCHEDULED_END_MS] = scheduledEnd.toEpochMilli()
        }
    }

    private suspend fun Setup.stored(): Preferences = dataStore.data.first()

    private fun Setup.startInAppCall() {
        h.api.joinCallHandler = { h.inAppJoinResponse() }
        h.center.startCall(h.request())
    }

    @Test
    fun writesOnInAppSessionAndClearsOnEnd() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.marker.start()
            runCurrent()
            s.startInAppCall()
            advanceUntilIdle()
            val written = s.stored()
            assertEquals("b1", written[CallResumeMarker.KEY_BOOKING_ID])
            assertEquals(s.h.request().scheduledEnd!!.toEpochMilli(), written[CallResumeMarker.KEY_SCHEDULED_END_MS])

            s.h.center.endCall()
            advanceUntilIdle()
            assertNull(s.h.center.session.value)
            assertTrue(s.stored().asMap().isEmpty())
        }

    @Test
    fun externalMeetingIsNotMarked() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.marker.start()
            runCurrent()
            s.h.api.joinCallHandler = { s.h.externalJoinResponse(url = null) }
            s.h.center.startCall(s.h.request(meetingType = MeetingType.SkypeZoom))
            advanceUntilIdle()
            assertTrue(s.stored().asMap().isEmpty())
        }

    @Test
    fun storesNoPii() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.marker.start()
            runCurrent()
            s.startInAppCall()
            advanceUntilIdle()
            val prefs = s.stored().asMap()
            assertEquals(setOf("call_resume_booking_id", "call_resume_scheduled_end_ms"), prefs.keys.map { it.name }.toSet())
            val values = prefs.values.map { it.toString() }
            listOf("Pat", "p1", "tok", "chan", "Career").forEach { secret ->
                assertFalse("marker leaked $secret", values.any { it.contains(secret) })
            }
        }

    @Test
    fun resumePostsBookingDetailAndNotice() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.seed("b1", s.h.clock.instant().plusSeconds(600))
            s.marker.start()
            advanceUntilIdle()
            assertEquals(PushDestination.BookingDetail("b1", PushAudience.Either), s.push.destination.pending.value)
            assertTrue(s.notice.consumeFor("b1"))
            assertFalse(s.notice.consumeFor("b1"))
            assertTrue(s.stored().asMap().isEmpty())
        }

    @Test
    fun staleMarkerIsDropped() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.seed("b1", s.h.clock.instant().minusSeconds(CallResumeMarker.CUTOFF_SECONDS + 1))
            s.marker.start()
            advanceUntilIdle()
            assertNull(s.push.destination.pending.value)
            assertFalse(s.notice.consumeFor("b1"))
            assertTrue(s.stored().asMap().isEmpty())
        }

    @Test
    fun liveSessionNeverRoutes() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.startInAppCall()
            advanceUntilIdle()
            assertEquals("b1", s.h.center.session.value?.bookingId)
            s.seed("b1", s.h.clock.instant().plusSeconds(600))
            s.marker.start()
            advanceUntilIdle()
            assertNull(s.push.destination.pending.value)
            assertFalse(s.notice.consumeFor("b1"))
        }

    @Test
    fun neverJoins() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.seed("b1", s.h.clock.instant().plusSeconds(600))
            s.marker.start()
            advanceUntilIdle()
            assertEquals(PushDestination.BookingDetail("b1", PushAudience.Either), s.push.destination.pending.value)
            assertTrue(s.h.engine.joins.isEmpty())
            assertEquals(0, s.h.permission.calls)
            assertEquals(0, s.h.api.joinCallCalls)
            assertNull(s.h.center.session.value)
            assertFalse(s.h.center.isCallActive.value)
        }

    @Test
    fun logoutClears() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.seed("b1", s.h.clock.instant().plusSeconds(600))
            s.marker.clearSessionData()
            assertTrue(s.stored().asMap().isEmpty())
        }
}
