package com.uptrendinvestments.clickmeapp.managers

import android.content.Context
import android.util.Log
import com.uptrendinvestments.clickmeapp.BuildConfig
import com.uptrendinvestments.clickmeapp.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import io.agora.rtc2.ClientRoleOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production [CallEngine] over the Agora [RtcEngine]; port of iOS `AgoraManager.swift`.
 *
 * Lives in an app singleton rather than the foreground service so the UI and the service share it without
 * binding. The constructors touch no native code. The internal primary constructor takes the SDK engine
 * factory and the static engine release so a JVM test can stop at the SDK boundary; Hilt injects through the
 * public secondary constructor, which passes the real `RtcEngine.create` and `RtcEngine.destroy`. The engine is created lazily by [join], which
 * `CallCenter` calls only after `CallForegroundController.startAndAwait` returned true (spike finding), and
 * is released by [destroy] off the main thread, never from a callback (RESEARCH Pitfall 10).
 *
 * `onUserJoined` is expected for a peer already in the channel right after the local join (RESEARCH A5,
 * covered by the D-16 UAT).
 *
 * Never logs the token, channel name or uid (T-09-02).
 */
@Singleton
class AgoraManager
    internal constructor(
        private val context: Context,
        private val io: CoroutineDispatcher,
        private val destroyRtcEngine: () -> Unit = { RtcEngine.destroy() },
        private val createRtcEngine: (RtcEngineConfig) -> RtcEngine?,
    ) : CallEngine {
        @Inject
        constructor(
            @ApplicationContext context: Context,
            @IoDispatcher io: CoroutineDispatcher,
        ) : this(context, io, { RtcEngine.destroy() }, { config -> RtcEngine.create(config) })

        /** Guards every engine lifecycle step (join, leave, destroy) and the controls (WR-04). */
        private val mutex = Mutex()

        @Volatile private var engine: RtcEngine? = null

        @Volatile private var isLeaving = false

        @Volatile private var leaveSignal: CompletableDeferred<Unit>? = null

        private val _events = MutableSharedFlow<EngineEvent>(extraBufferCapacity = EVENT_BUFFER)
        override val events = _events.asSharedFlow()

        // Callbacks run on SDK threads: they only log codes, tryEmit or complete, and never touch other state.
        // The remote-presence lines carry state, reason and role codes and elapsed milliseconds only, never the uid
        // (T-09-02). They exist so a device run shows whether the remote peer arrived (G-11-5).
        private val handler =
            object : IRtcEngineEventHandler() {
                override fun onJoinChannelSuccess(
                    channel: String?,
                    uid: Int,
                    elapsed: Int,
                ) {
                    _events.tryEmit(EngineEvent.LocalJoined)
                }

                override fun onRejoinChannelSuccess(
                    channel: String?,
                    uid: Int,
                    elapsed: Int,
                ) {
                    _events.tryEmit(EngineEvent.LocalJoined)
                }

                override fun onUserJoined(
                    uid: Int,
                    elapsed: Int,
                ) {
                    Log.i(TAG, "remote joined elapsed=$elapsed")
                    _events.tryEmit(EngineEvent.RemoteJoined(uid))
                }

                override fun onUserOffline(
                    uid: Int,
                    reason: Int,
                ) {
                    Log.i(TAG, "remote offline reason=$reason")
                    _events.tryEmit(mapUserOffline(uid, reason))
                }

                override fun onRemoteAudioStateChanged(
                    uid: Int,
                    state: Int,
                    reason: Int,
                    elapsed: Int,
                ) {
                    Log.i(TAG, "remote audio state=$state reason=$reason")
                    mapRemoteAudioStateChanged(uid, state)?.let(_events::tryEmit)
                }

                override fun onClientRoleChanged(
                    oldRole: Int,
                    newRole: Int,
                    newRoleOptions: ClientRoleOptions?,
                ) {
                    Log.i(TAG, "client role old=$oldRole new=$newRole")
                }

                override fun onConnectionStateChanged(
                    state: Int,
                    reason: Int,
                ) {
                    Log.i(TAG, "conn state=$state reason=$reason")
                    mapConnectionStateChanged(state, reason, isLeaving)?.let(_events::tryEmit)
                }

                override fun onLeaveChannel(stats: RtcStats?) {
                    leaveSignal?.complete(Unit)
                }

                override fun onError(err: Int) {
                    Log.w(TAG, "error code=$err")
                    _events.tryEmit(EngineEvent.Error(err))
                }
            }

        override suspend fun join(
            token: String,
            channelName: String,
            uid: Int,
        ): Boolean =
            withContext(io) {
                mutex.withLock {
                    val rtc = engine ?: createEngine() ?: return@withLock false
                    isLeaving = false
                    val options = buildChannelMediaOptions()
                    val code = rtc.joinChannel(token, channelName, uid, options)
                    if (code != 0) Log.w(TAG, "joinChannel failed code=$code")
                    code == 0
                }
            }

        /** Creates the engine with the iOS / spike audio defaults, or null if the SDK cannot start. */
        private fun createEngine(): RtcEngine? {
            var created: RtcEngine? = null
            try {
                // Read before any config scope: inside RtcEngineConfig a bare `context` is the SDK getter (G-11-1).
                val appContext = context.applicationContext
                val config = buildRtcEngineConfig(appContext, BuildConfig.AGORA_APP_ID, handler)
                val rtc: RtcEngine? = createRtcEngine(config)
                if (rtc == null) {
                    Log.w(TAG, "engine create returned null")
                    return null
                }
                created = rtc
                rtc.setAudioProfile(Constants.AUDIO_PROFILE_SPEECH_STANDARD)
                rtc.setAudioScenario(Constants.AUDIO_SCENARIO_DEFAULT)
                rtc.setDefaultAudioRoutetoSpeakerphone(true)
                rtc.setEnableSpeakerphone(true)
                engine = rtc
                return rtc
            } catch (e: Exception) {
                Log.w(TAG, "engine create failed ${describeEngineFailure(e)}")
            } catch (e: LinkageError) {
                // A native library that fails to load (UnsatisfiedLinkError) is an Error, not an Exception (WR-10).
                Log.w(TAG, "engine create failed ${describeEngineFailure(e)}")
            }
            // join holds the mutex on io, so this static destroy cannot interleave with destroy(). The engine field
            // was never assigned, so destroy() would otherwise skip the native engine and leave it alive for the
            // next join (11-16-REVIEW WR-01).
            if (created != null) runCatching { destroyRtcEngine() }
            return null
        }

        /** Holds the lock so a leave never interleaves with [destroy] or [join] (WR-04). */
        override suspend fun leave() {
            withContext(io) {
                mutex.withLock {
                    val rtc = engine ?: return@withLock
                    isLeaving = true
                    val signal = CompletableDeferred<Unit>()
                    leaveSignal = signal
                    rtc.leaveChannel()
                    withTimeoutOrNull(LEAVE_TIMEOUT_MS) { signal.await() }
                }
            }
        }

        override fun setMuted(muted: Boolean): Boolean = withIdleEngine { it.muteLocalAudioStream(muted) == 0 }

        override fun setSpeakerOn(on: Boolean): Boolean = withIdleEngine { it.setEnableSpeakerphone(on) == 0 }

        /**
         * Runs [block] on the engine only while no join, leave or destroy holds the lock, so a control called on
         * Main never reaches an engine that is being released on the IO dispatcher. Never blocks the caller:
         * a busy engine reports false and the UI flag stays unchanged (WR-04).
         */
        private inline fun withIdleEngine(block: (RtcEngine) -> Boolean): Boolean {
            if (!mutex.tryLock()) return false
            try {
                val rtc = engine ?: return false
                return block(rtc)
            } finally {
                mutex.unlock()
            }
        }

        override suspend fun destroy() {
            withContext(io) {
                mutex.withLock {
                    if (engine != null) {
                        // Reset in finally so a throwing release never leaves a dead engine for the next join, and
                        // swallow the failure (redacted) so CallCenter's remaining teardown still runs (11-REVIEW WR-02).
                        try {
                            destroyRtcEngine()
                        } catch (e: Exception) {
                            Log.w(TAG, "engine destroy failed ${describeEngineFailure(e)}")
                        } catch (e: LinkageError) {
                            Log.w(TAG, "engine destroy failed ${describeEngineFailure(e)}")
                        } finally {
                            engine = null
                            isLeaving = false
                            leaveSignal = null
                        }
                    }
                }
            }
        }

        companion object {
            const val LEAVE_TIMEOUT_MS = 2_000L
            const val TAG = "ClickMeCall"
            private const val EVENT_BUFFER = 64
        }
    }
