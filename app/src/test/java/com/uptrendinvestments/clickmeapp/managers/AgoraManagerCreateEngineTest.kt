package com.uptrendinvestments.clickmeapp.managers

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.uptrendinvestments.clickmeapp.BuildConfig
import io.agora.rtc2.RtcEngineConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog

/**
 * Drives [AgoraManager.join] through `createEngine` to the SDK factory boundary (G-11-1). The pre-fix code threw a
 * NullPointerException while building the config, so the factory was never reached.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AgoraManagerCreateEngineTest {
    private val app: Context = ApplicationProvider.getApplicationContext()

    private fun logs() = ShadowLog.getLogsForTag(AgoraManager.TAG)

    private fun assertNoSentinelsLogged() {
        val sentinels = listOf(TOKEN, CHANNEL, UID.toString(), MESSAGE)
        logs().forEach { entry ->
            sentinels.forEach { sentinel ->
                assertFalse("log leaked $sentinel: ${entry.msg}", entry.msg.contains(sentinel))
            }
        }
    }

    @Test
    fun joinReachesFactoryWithApplicationContextAndNullCreateReturnsFalse() =
        runTest {
            var calls = 0
            var captured: RtcEngineConfig? = null
            val manager =
                AgoraManager(app, UnconfinedTestDispatcher(testScheduler)) { config ->
                    calls++
                    captured = config
                    null
                }

            val joined = manager.join(TOKEN, CHANNEL, UID)

            assertFalse(joined)
            assertEquals(1, calls)
            val config = captured
            assertNotNull(config)
            assertSame(app.applicationContext, config!!.mContext)
            assertEquals(BuildConfig.AGORA_APP_ID, config.mAppId)
            assertNotNull(config.mEventHandler)

            val warnings = logs().filter { it.type == Log.WARN }
            assertEquals(listOf("engine create returned null"), warnings.map { it.msg })
            assertTrue(logs().none { it.msg.contains("NullPointerException") })
            assertNoSentinelsLogged()
        }

    @Test
    fun thrownExceptionReturnsFalseWithRedactedLog() =
        runTest {
            val manager =
                AgoraManager(app, UnconfinedTestDispatcher(testScheduler)) { _ ->
                    throw IllegalStateException(MESSAGE)
                }

            assertFalse(manager.join(TOKEN, CHANNEL, UID))

            val warnings = logs().filter { it.type == Log.WARN }
            assertEquals(1, warnings.size)
            val message = warnings.single().msg
            assertTrue(message, message.startsWith("engine create failed IllegalStateException at "))
            assertNoSentinelsLogged()
        }

    @Test
    fun thrownLinkageErrorReturnsFalseWithRedactedLog() =
        runTest {
            val manager =
                AgoraManager(app, UnconfinedTestDispatcher(testScheduler)) { _ ->
                    throw UnsatisfiedLinkError(MESSAGE)
                }

            assertFalse(manager.join(TOKEN, CHANNEL, UID))

            val warnings = logs().filter { it.type == Log.WARN }
            assertEquals(1, warnings.size)
            val message = warnings.single().msg
            assertTrue(message, message.startsWith("engine create failed UnsatisfiedLinkError at "))
            assertNoSentinelsLogged()
        }

    private companion object {
        const val TOKEN = "sentinel-rtc-token-7f3a"
        const val CHANNEL = "sentinel-channel-b2c9"
        const val UID = 918273
        const val MESSAGE = "sentinel-message"
    }
}
