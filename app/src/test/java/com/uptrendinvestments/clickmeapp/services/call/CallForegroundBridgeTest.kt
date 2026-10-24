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

            bridge.onServiceStarted(true)
            assertTrue(result.await())
            assertTrue(bridge.active.value)
        }

    @Test
    fun serviceRefusal_returnsFalseAndDeactivates() =
        runTest {
            val bridge = bridge()
            val result = async(start = CoroutineStart.UNDISPATCHED) { bridge.startAndAwait(info) }
            bridge.onServiceStarted(false)
            assertFalse(result.await())
            assertFalse(bridge.active.value)
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

    private class ThrowingContext(
        base: Context,
        private val error: RuntimeException,
    ) : ContextWrapper(base) {
        override fun startForegroundService(service: Intent?): ComponentName? = throw error
    }
}
