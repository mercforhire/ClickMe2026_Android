package com.uptrendinvestments.clickmeapp.services.call

import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.uptrendinvestments.clickmeapp.managers.CallNotificationInfo
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CallForegroundBridgeTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val info = CallNotificationInfo("b1", "Ada", Instant.parse("2026-12-01T15:00:00Z"))

    private fun bridge(ctx: Context = context) = CallForegroundBridge(ctx)

    @Test
    fun startAndAwait_startsServiceWithOnlyDisplayExtras_andReturnsTrueOnCallback() =
        runTest {
            val bridge = bridge()
            val result = async(start = CoroutineStart.UNDISPATCHED) { bridge.startAndAwait(info) }
            assertTrue(bridge.active.value)
            val intent = shadowOf(context as android.app.Application).nextStartedService
            assertNotNull(intent)
            assertEquals(CallService.ACTION_START, intent.action)
            assertEquals(info, CallService.infoFrom(intent))
            val keys = intent.extras!!.keySet().map { it.lowercase() }
            assertFalse(keys.any { "token" in it || "channel" in it || "uid" in it })

            val attempt = intent.attempt()
            assertTrue(bridge.isCurrentAttempt(attempt))
            bridge.onServiceStarted(attempt, true)
            assertTrue(result.await())
            assertTrue(bridge.active.value)
        }

    @Test
    fun serviceRefusal_returnsFalseAndDeactivates() =
        runTest {
            val bridge = bridge()
            val result = async(start = CoroutineStart.UNDISPATCHED) { bridge.startAndAwait(info) }
            bridge.onServiceStarted(nextAttempt(), false)
            assertFalse(result.await())
            assertFalse(bridge.active.value)
        }

    @Test
    fun lateCallbackFromAnEarlierAttempt_isIgnored() =
        runTest {
            val bridge = bridge()
            val first = async(start = CoroutineStart.UNDISPATCHED) { bridge.startAndAwait(info) }
            val firstAttempt = nextAttempt()
            advanceTimeBy(CallForegroundBridge.START_TIMEOUT_MS + 1)
            runCurrent()
            assertFalse(first.await())

            val second = async(start = CoroutineStart.UNDISPATCHED) { bridge.startAndAwait(info) }
            val secondAttempt = nextAttempt()
            assertTrue(secondAttempt > firstAttempt)
            assertFalse(bridge.isCurrentAttempt(firstAttempt))
            assertTrue(bridge.isCurrentAttempt(secondAttempt))
            // The first service command finally reports a refusal: it must not fail the second start (WR-05).
            bridge.onServiceStarted(firstAttempt, false)
            runCurrent()
            assertFalse(second.isCompleted)
            assertTrue(bridge.active.value)
            bridge.onServiceStarted(secondAttempt, true)
            assertTrue(second.await())
        }

    @Test
    fun stoppedAttempt_isNoLongerCurrent() =
        runTest {
            val bridge = bridge()
            val result = async(start = CoroutineStart.UNDISPATCHED) { bridge.startAndAwait(info) }
            val attempt = nextAttempt()
            bridge.onServiceStarted(attempt, true)
            assertTrue(result.await())
            bridge.stop()
            assertFalse(bridge.isCurrentAttempt(attempt))
        }

    @Test
    fun noCallback_timesOutAndDeactivates() =
        runTest {
            val bridge = bridge()
            val result = async(start = CoroutineStart.UNDISPATCHED) { bridge.startAndAwait(info) }
            advanceTimeBy(CallForegroundBridge.START_TIMEOUT_MS + 1)
            runCurrent()
            assertFalse(result.await())
            assertFalse(bridge.active.value)
        }

    @Test
    fun startForegroundServiceThrowing_returnsFalse() =
        runTest {
            val errors = listOf<RuntimeException>(IllegalStateException("not allowed"), SecurityException("denied"))
            errors.forEach { error ->
                val bridge = bridge(ThrowingContext(context, error))
                assertFalse(bridge.startAndAwait(info))
                assertFalse(bridge.active.value)
            }
        }

    @Test
    fun cancellationWhileWaiting_stopsAndPropagates() =
        runTest {
            val bridge = bridge()
            val job = launch(start = CoroutineStart.UNDISPATCHED) { bridge.startAndAwait(info) }
            assertTrue(bridge.active.value)
            job.cancelAndJoin()
            assertTrue(job.isCancelled)
            assertFalse(bridge.active.value)
        }

    @Test
    fun stop_isIdempotent() {
        val bridge = bridge()
        bridge.stop()
        bridge.stop()
        assertFalse(bridge.active.value)
    }

    @Test
    fun requestHangUp_emitsOnceToCollector() =
        runTest {
            val bridge = bridge()
            val probe = mutableListOf<Unit>()
            val job = launch(start = CoroutineStart.UNDISPATCHED) { bridge.hangUpRequests.collect { probe.add(it) } }
            bridge.requestHangUp()
            runCurrent()
            job.cancelAndJoin()
            assertEquals(1, probe.size)
        }

    private fun Intent.attempt(): Long = getLongExtra(CallService.EXTRA_ATTEMPT_ID, CallForegroundBridge.NO_ATTEMPT)

    /** Attempt id of the next start intent the bridge sent. */
    private fun nextAttempt(): Long {
        val intent = shadowOf(context as android.app.Application).nextStartedService
        assertNotNull(intent)
        return intent.attempt()
    }

    private class ThrowingContext(
        base: Context,
        private val error: RuntimeException,
    ) : ContextWrapper(base) {
        override fun startForegroundService(service: Intent?): ComponentName? = throw error
    }
}
