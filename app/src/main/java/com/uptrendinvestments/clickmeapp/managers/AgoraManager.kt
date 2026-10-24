package com.uptrendinvestments.clickmeapp.managers

import android.content.Context
import android.util.Log
import com.uptrendinvestments.clickmeapp.BuildConfig
import com.uptrendinvestments.clickmeapp.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import io.agora.rtc2.ChannelMediaOptions
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
 * binding. The constructor touches no native code. The engine is created lazily by [join], which
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
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @IoDispatcher private val io: CoroutineDispatcher,
    ) : CallEngine {
        private val mutex = Mutex()
        private var engine: RtcEngine? = null

        @Volatile private var isLeaving = false

        @Volatile private var leaveSignal: CompletableDeferred<Unit>? = null

        private val _events = MutableSharedFlow<EngineEvent>(extraBufferCapacity = EVENT_BUFFER)
        override val events = _events.asSharedFlow()

        // Callbacks run on SDK threads: they only tryEmit / complete and never touch other state.
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
                    _events.tryEmit(EngineEvent.RemoteJoined(uid))
                }

                override fun onUserOffline(
                    uid: Int,
                    reason: Int,
                ) {
                    _events.tryEmit(mapUserOffline(uid, reason))
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
                    val options =
                        ChannelMediaOptions().apply {
                            channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                            publishMicrophoneTrack = true
                            autoSubscribeAudio = true
                        }
                    rtc.joinChannel(token, channelName, uid, options) == 0
                }
            }

        /** Creates the engine with the iOS / spike audio defaults, or null if the SDK cannot start. */
        private fun createEngine(): RtcEngine? =
            try {
                val config =
                    RtcEngineConfig().apply {
                        mContext = context.applicationContext
                        mAppId = BuildConfig.AGORA_APP_ID
                        mEventHandler = handler
                    }
                RtcEngine.create(config).also { rtc ->
                    rtc.setAudioProfile(Constants.AUDIO_PROFILE_SPEECH_STANDARD)
                    rtc.setAudioScenario(Constants.AUDIO_SCENARIO_DEFAULT)
                    rtc.setDefaultAudioRoutetoSpeakerphone(true)
                    rtc.setEnableSpeakerphone(true)
                    engine = rtc
                }
            } catch (e: Exception) {
                Log.w(TAG, "engine create failed ${e.javaClass.simpleName}")
                null
            }

        override suspend fun leave() {
            withContext(io) {
                val rtc = engine ?: return@withContext
                isLeaving = true
                val signal = CompletableDeferred<Unit>()
                leaveSignal = signal
                rtc.leaveChannel()
                withTimeoutOrNull(LEAVE_TIMEOUT_MS) { signal.await() }
            }
        }

        override fun setMuted(muted: Boolean): Boolean = engine?.muteLocalAudioStream(muted) == 0

        override fun setSpeakerOn(on: Boolean): Boolean = engine?.setEnableSpeakerphone(on) == 0

        override suspend fun destroy() {
            withContext(io) {
                mutex.withLock {
                    if (engine != null) {
                        RtcEngine.destroy()
                        engine = null
                        isLeaving = false
                        leaveSignal = null
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
