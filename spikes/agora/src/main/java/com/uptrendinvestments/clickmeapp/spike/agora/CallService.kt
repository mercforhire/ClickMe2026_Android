package com.uptrendinvestments.clickmeapp.spike.agora

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CallState { Idle, Joining, InCall, PeerJoined, Left, Error }

/**
 * Throwaway spike (Phase 2, FND-14, D-13). Hosts the Agora RtcEngine inside a microphone foreground service so audio
 * keeps flowing while the app is backgrounded. Started only from the visible activity after RECORD_AUDIO is granted.
 *
 * Evidence is logged under tag AgoraSpike. The RTC credential is never logged.
 */
class CallService : Service() {
    private var engine: RtcEngine? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var joinStartedAtMs = 0L
    private var joinedAtMs = 0L

    @Volatile private var localVolume = 0

    @Volatile private var remoteVolume = 0

    private val heartbeat =
        object : Runnable {
            override fun run() {
                val info = ActivityManager.RunningAppProcessInfo().also { ActivityManager.getMyMemoryState(it) }
                val seconds = (SystemClock.elapsedRealtime() - joinedAtMs) / 1000
                val line = "t=${seconds}s local=$localVolume remote=$remoteVolume importance=${info.importance}"
                Log.i(TAG, "heartbeat: $line")
                remoteVolume = 0
                mainHandler.postDelayed(this, HEARTBEAT_MS)
            }
        }

    private val handler =
        object : IRtcEngineEventHandler() {
            override fun onJoinChannelSuccess(
                channel: String?,
                uid: Int,
                elapsed: Int,
            ) {
                joinedAtMs = SystemClock.elapsedRealtime()
                val elapsedMs = joinedAtMs - joinStartedAtMs
                Log.i(TAG, "join: success uid=$uid elapsedMs=$elapsedMs")
                _state.value = CallState.InCall
                mainHandler.removeCallbacks(heartbeat)
                mainHandler.postDelayed(heartbeat, HEARTBEAT_MS)
            }

            override fun onUserJoined(
                uid: Int,
                elapsed: Int,
            ) {
                Log.i(TAG, "peer: joined uid=$uid")
                _state.value = CallState.PeerJoined
            }

            override fun onUserOffline(
                uid: Int,
                reason: Int,
            ) {
                Log.i(TAG, "peer: offline uid=$uid reason=$reason")
                _state.value = CallState.InCall
            }

            override fun onConnectionStateChanged(
                state: Int,
                reason: Int,
            ) {
                Log.i(TAG, "conn: state=$state reason=$reason")
            }

            override fun onError(err: Int) {
                Log.e(TAG, "error: code=$err")
                _state.value = CallState.Error
            }

            override fun onAudioVolumeIndication(
                speakers: Array<out AudioVolumeInfo>?,
                totalVolume: Int,
            ) {
                speakers?.forEach {
                    if (it.uid == 0) {
                        localVolume = it.volume
                    } else if (it.volume > remoteVolume) {
                        remoteVolume = it.volume
                    }
                }
            }
        }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // The channel must exist before startForeground, otherwise "Bad notification for startForeground".
        val channel = NotificationChannel(CHANNEL_ID, "Spike call", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_START -> start(intent)
            else -> teardown()
        }
        return START_NOT_STICKY
    }

    private fun start(intent: Intent) {
        if (engine != null) {
            Log.i(TAG, "join: ignored, already running")
            return
        }
        val rtcCredential = intent.getStringExtra(EXTRA_TOKEN)
        val channelName = intent.getStringExtra(EXTRA_CHANNEL)
        val uid = intent.getIntExtra(EXTRA_UID, 0)
        if (rtcCredential.isNullOrEmpty() || channelName.isNullOrEmpty()) {
            Log.e(TAG, "error: missing join extras")
            _state.value = CallState.Error
            stopSelf()
            return
        }
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "error: startForeground rejected ${e.javaClass.simpleName}")
            _state.value = CallState.Error
            stopSelf()
            return
        }
        _state.value = CallState.Joining
        joinStartedAtMs = SystemClock.elapsedRealtime()
        val config =
            RtcEngineConfig().apply {
                mContext = applicationContext
                mAppId = BuildConfig.AGORA_APP_ID
                mEventHandler = handler
            }
        val rtc = RtcEngine.create(config)
        engine = rtc
        // iOS AgoraManager defaults: speech standard profile, default scenario, speakerphone routing.
        rtc.setAudioProfile(Constants.AUDIO_PROFILE_SPEECH_STANDARD)
        rtc.setAudioScenario(Constants.AUDIO_SCENARIO_DEFAULT)
        rtc.setDefaultAudioRoutetoSpeakerphone(true)
        rtc.setEnableSpeakerphone(true)
        rtc.enableAudioVolumeIndication(VOLUME_INTERVAL_MS, 3, false)
        val options =
            ChannelMediaOptions().apply {
                channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                publishMicrophoneTrack = true
                autoSubscribeAudio = true
            }
        val result = rtc.joinChannel(rtcCredential, channelName, uid, options)
        Log.i(TAG, "join: requested result=$result")
    }

    private fun buildNotification(): Notification {
        val open =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )
        return NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_speakerphone)
            .setContentTitle("Spike call in progress")
            .setOngoing(true)
            .setContentIntent(open)
            .build()
    }

    private fun teardown() {
        mainHandler.removeCallbacks(heartbeat)
        val rtc = engine
        if (rtc != null) {
            rtc.leaveChannel()
            RtcEngine.destroy()
            engine = null
            _state.value = CallState.Left
            Log.i(TAG, "leave: done")
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        teardown()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "spike.agora.START"
        const val ACTION_STOP = "spike.agora.STOP"
        const val EXTRA_TOKEN = "token"
        const val EXTRA_CHANNEL = "channel"
        const val EXTRA_UID = "uid"
        const val CHANNEL_ID = "spike_call"
        const val NOTIFICATION_ID = 42
        private const val TAG = "AgoraSpike"
        private const val HEARTBEAT_MS = 10_000L
        private const val VOLUME_INTERVAL_MS = 2000

        private val _state = MutableStateFlow(CallState.Idle)
        val state: StateFlow<CallState> = _state.asStateFlow()
    }
}
