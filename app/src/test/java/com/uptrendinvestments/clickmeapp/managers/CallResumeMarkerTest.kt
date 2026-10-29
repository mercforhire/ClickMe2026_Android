package com.uptrendinvestments.clickmeapp.managers

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.model.responses.JoinCallResponse
import com.uptrendinvestments.clickmeapp.screens.push.PushAudience
import com.uptrendinvestments.clickmeapp.screens.push.PushDestination
import com.uptrendinvestments.clickmeapp.screens.push.PushLaunchRequest
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.testPreferencesDataStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Instant

/**
 * DataStore does its file work off the test scheduler, so `advanceUntilIdle()` alone can return before a write has
 * landed. Positive cases therefore wait on the store or the push slot itself ([awaitStore]); only the cases that assert
 * "nothing happened" use [settle].
 */
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

    private suspend fun Setup.awaitStore(predicate: (Preferences) -> Boolean): Preferences =
        dataStore.data.first(predicate)

    private suspend fun Setup.awaitEmptyStore() {
        awaitStore { prefs -> prefs.asMap().isEmpty() }
    }

    private suspend fun Setup.awaitBookingStored(bookingId: String): Preferences =
        awaitStore { prefs -> prefs[CallResumeMarker.KEY_BOOKING_ID] == bookingId }

    private suspend fun Setup.awaitRouted(): PushDestination? = push.destination.pending.first { it != null }

    /** Lets the scheduler drain and real IO finish, for assertions that nothing happened. */
    private suspend fun TestScope.settle() {
        advanceUntilIdle()
        withContext(Dispatchers.Default) { delay(SETTLE_MS) }
        advanceUntilIdle()
    }

    /** The harness clock moved by [seconds] (negative for the past). */
    private fun Setup.clockPlus(seconds: Long): Instant = h.clock.instant().plusSeconds(seconds)

    private fun Setup.liveSession(): CallSession? = h.center.session.value

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
            val written = s.awaitBookingStored("b1")
            val expectedEnd = s.h.request().scheduledEnd
            assertEquals(expectedEnd?.toEpochMilli(), written[CallResumeMarker.KEY_SCHEDULED_END_MS])

            s.h.center.endCall()
            s.awaitEmptyStore()
            assertNull(s.liveSession())
        }

    @Test
    fun externalMeetingIsNotMarked() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.marker.start()
            runCurrent()
            // The join never returns, so the session stays at its SkypeZoom hint.
            val pendingJoin = CompletableDeferred<JoinCallResponse>()
            s.h.api.joinCallHandler = { pendingJoin.await() }
            s.h.center.startCall(s.h.request(meetingType = MeetingType.SkypeZoom))
            settle()
            assertEquals("b1", s.liveSession()?.bookingId)
            assertTrue(s.stored().asMap().isEmpty())
        }

    @Test
    fun storesNoPii() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.marker.start()
            runCurrent()
            s.startInAppCall()
            val prefs = s.awaitBookingStored("b1").asMap()
            val keyNames = prefs.keys.map { key -> key.name }.toSet()
            assertEquals(setOf("call_resume_booking_id", "call_resume_scheduled_end_ms"), keyNames)
            val values = prefs.values.map { value -> value.toString() }
            for (secret in listOf("Pat", "p1", "tok", "chan", "Career")) {
                assertFalse("marker leaked $secret", values.any { value -> value.contains(secret) })
            }
        }

    @Test
    fun resumePostsBookingDetailAndNotice() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.seed("b1", s.clockPlus(600))
            s.marker.start()
            assertEquals(PushDestination.BookingDetail("b1", PushAudience.Either), s.awaitRouted())
            assertTrue(s.notice.consumeFor("b1"))
            assertFalse(s.notice.consumeFor("b1"))
            s.awaitEmptyStore()
        }

    @Test
    fun staleMarkerIsDropped() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.seed("b1", s.clockPlus(-(CallResumeMarker.CUTOFF_SECONDS + 1)))
            s.marker.start()
            s.awaitEmptyStore()
            settle()
            assertNull(s.push.destination.pending.value)
            assertFalse(s.notice.consumeFor("b1"))
        }

    @Test
    fun liveSessionNeverRoutes() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.startInAppCall()
            advanceUntilIdle()
            assertEquals("b1", s.liveSession()?.bookingId)
            s.seed("b0", s.clockPlus(600))
            s.marker.start()
            // The stale "b0" is replaced by the live "b1" once the marker has run its start-up read.
            s.awaitBookingStored("b1")
            settle()
            assertNull(s.push.destination.pending.value)
            assertFalse(s.notice.consumeFor("b0"))
            assertFalse(s.notice.consumeFor("b1"))
        }

    @Test
    fun neverJoins() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.seed("b1", s.clockPlus(600))
            s.marker.start()
            assertEquals(PushDestination.BookingDetail("b1", PushAudience.Either), s.awaitRouted())
            s.awaitEmptyStore()
            settle()
            val joins = s.h.engine.joins
            assertTrue(joins.isEmpty())
            assertEquals(0, s.h.permission.calls)
            assertEquals(0, s.h.api.joinCallCalls)
            assertEquals(emptyList<String>(), s.h.log.toList())
            assertNull(s.liveSession())
        }

    @Test
    fun logoutClears() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.seed("b1", s.clockPlus(600))
            s.marker.clearSessionData()
            assertTrue(s.stored().asMap().isEmpty())
        }

    private companion object {
        const val SETTLE_MS = 300L
    }
}
