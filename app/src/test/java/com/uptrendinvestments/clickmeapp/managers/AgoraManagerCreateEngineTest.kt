package com.uptrendinvestments.clickmeapp.managers

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.uptrendinvestments.clickmeapp.BuildConfig
import io.agora.rtc2.Constants
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
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
    fun joinHandsFactoryACommunicationProfileConfig() =
        runTest {
            var calls = 0
            var captured: RtcEngineConfig? = null
            val manager =
                AgoraManager(app, UnconfinedTestDispatcher(testScheduler)) { config ->
                    calls++
                    captured = config
                    null
                }

            assertFalse(manager.join(TOKEN, CHANNEL, UID))

            assertEquals(1, calls)
            assertEquals(Constants.CHANNEL_PROFILE_COMMUNICATION, captured!!.mChannelProfile)
        }

    @Test
    fun remotePresenceCallbacksLogCodesOnlyAndKeepEvents() =
        runTest {
            var captured: RtcEngineConfig? = null
            val manager =
                AgoraManager(app, UnconfinedTestDispatcher(testScheduler)) { config ->
                    captured = config
                    null
                }
            assertFalse(manager.join(TOKEN, CHANNEL, UID))
            val handler = captured!!.mEventHandler
            val events = mutableListOf<EngineEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { manager.events.toList(events) }
            runCurrent()

            handler.onUserJoined(UID, 1500)
            runCurrent()
            assertEquals(listOf<EngineEvent>(EngineEvent.RemoteJoined(UID)), events.toList())

            handler.onUserOffline(UID, Constants.USER_OFFLINE_DROPPED)
            runCurrent()
            assertEquals(
                listOf(EngineEvent.RemoteJoined(UID), EngineEvent.RemoteOffline(UID, 1)),
                events.toList(),
            )

            handler.onRemoteAudioStateChanged(
                UID,
                Constants.REMOTE_AUDIO_STATE_DECODING,
                Constants.REMOTE_AUDIO_REASON_REMOTE_UNMUTED,
                2500,
            )
            handler.onRemoteAudioStateChanged(
                UID,
                Constants.REMOTE_AUDIO_STATE_STOPPED,
                Constants.REMOTE_AUDIO_REASON_REMOTE_MUTED,
                2600,
            )
            handler.onClientRoleChanged(Constants.CLIENT_ROLE_AUDIENCE, Constants.CLIENT_ROLE_BROADCASTER, null)
            runCurrent()
            // BUG-09: decoding audio is now an event (the peer is back); stopped audio, role changes and logs are not.
            assertEquals(
                listOf(
                    EngineEvent.RemoteJoined(UID),
                    EngineEvent.RemoteOffline(UID, 1),
                    EngineEvent.RemoteJoined(UID),
                ),
                events.toList(),
            )

            assertEquals(
                listOf(
                    "remote joined elapsed=1500",
                    "remote offline reason=1",
                    "remote audio state=2 reason=6",
                    "remote audio state=0 reason=5",
                    "client role old=2 new=1",
                ),
                logs().filter { it.type == Log.INFO }.map { it.msg },
            )
            assertEquals(listOf("engine create returned null"), logs().filter { it.type == Log.WARN }.map { it.msg })
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

    @Test
    fun setupFailureAfterCreationDestroysEngineAndAllowsRetry() =
        runTest {
            var destroyed = 0
            var created = 0
            val failing = Mockito.mock(RtcEngine::class.java)
            Mockito
                .`when`(
                    failing.setAudioProfile(Constants.AUDIO_PROFILE_SPEECH_STANDARD),
                ).thenThrow(IllegalStateException(MESSAGE))
            val manager =
                AgoraManager(app, UnconfinedTestDispatcher(testScheduler), { destroyed++ }) { _ ->
                    created++
                    failing
                }

            assertFalse(manager.join(TOKEN, CHANNEL, UID))

            assertEquals(1, destroyed)
            assertEquals(1, created)
            Mockito.verify(failing, Mockito.never()).joinChannel(
                Mockito.any(),
                Mockito.any(),
                Mockito.anyInt(),
                Mockito.any(),
            )
            val warnings = logs().filter { it.type == Log.WARN }
            assertEquals(1, warnings.size)
            assertTrue(
                warnings.single().msg,
                warnings.single().msg.startsWith("engine create failed IllegalStateException at "),
            )
            assertNoSentinelsLogged()

            // The failed engine was never cached, so the next join builds a fresh one.
            assertFalse(manager.join(TOKEN, CHANNEL, UID))
            assertEquals(2, created)
            assertEquals(2, destroyed)
        }

    @Test
    fun successfulSetupDoesNotDestroyEngine() =
        runTest {
            var destroyed = 0
            val healthy = Mockito.mock(RtcEngine::class.java)
            val manager = AgoraManager(app, UnconfinedTestDispatcher(testScheduler), { destroyed++ }) { _ -> healthy }

            // The mock reports joinChannel code 0, so setup succeeded and join returns true.
            assertTrue(manager.join(TOKEN, CHANNEL, UID))

            assertEquals(0, destroyed)
        }

    @Test
    fun destroyThatThrowsStillResetsEngineSoNextJoinRecreatesIt() =
        runTest {
            var created = 0
            var destroyed = 0
            val engine = Mockito.mock(RtcEngine::class.java)
            val manager =
                AgoraManager(
                    app,
                    UnconfinedTestDispatcher(testScheduler),
                    {
                        destroyed++
                        throw IllegalStateException(MESSAGE)
                    },
                ) { _ ->
                    created++
                    engine
                }
            assertTrue(manager.join(TOKEN, CHANNEL, UID))
            assertEquals(1, created)

            manager.destroy()

            assertEquals(1, destroyed)
            val warnings = logs().filter { it.type == Log.WARN }
            assertEquals(1, warnings.size)
            assertTrue(
                warnings.single().msg,
                warnings.single().msg.startsWith("engine destroy failed IllegalStateException at "),
            )
            assertNoSentinelsLogged()

            // The dead engine was dropped, so the next join creates a new one instead of reusing it.
            assertTrue(manager.join(TOKEN, CHANNEL, UID))
            assertEquals(2, created)
        }

    private companion object {
        const val TOKEN = "sentinel-rtc-token-7f3a"
        const val CHANNEL = "sentinel-channel-b2c9"
        const val UID = 918273
        const val MESSAGE = "sentinel-message"
    }
}
