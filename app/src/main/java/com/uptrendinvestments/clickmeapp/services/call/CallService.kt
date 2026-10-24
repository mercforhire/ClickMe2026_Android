package com.uptrendinvestments.clickmeapp.services.call

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.uptrendinvestments.clickmeapp.managers.CallNotificationInfo
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/**
 * Thin microphone foreground service for the in-app call (CALL-03). It holds no call logic: CallCenter starts
 * it through [CallForegroundBridge] after the microphone grant and before the engine, and stops it by flipping
 * [CallForegroundBridge.active]. It receives only the booking id, peer name and scheduled start, never any RTC
 * credential (T-09-11).
 *
 * Survives a task swipe on purpose (`stopWithTask="false"`, D-14) and is never revived without a session (D-15).
 */
@AndroidEntryPoint
class CallService : Service() {
    @Inject
    lateinit var bridge: CallForegroundBridge

    @Inject
    lateinit var clock: Clock

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var focusRequest: AudioFocusRequest? = null

    /**
     * Newest start id this service has received. [teardown] stops with it, so a start command that is still
     * queued (a newer id) keeps the service alive and reaches its own `startForeground` (WR-05).
     */
    private var lastStartId = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // The channel must exist before startForeground, otherwise "Bad notification for startForeground".
        CallNotificationFactory.createChannel(this)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        lastStartId = startId
        when (intent?.action) {
            ACTION_START -> start(intent)
            // CallCenter runs leave -> /end -> teardown, then stops the bridge.
            ACTION_END -> bridge.requestHangUp()
            else -> if (!bridge.active.value) teardown()
        }
        return START_NOT_STICKY
    }

    private fun start(intent: Intent) {
        val attempt = intent.getLongExtra(EXTRA_ATTEMPT_ID, CallForegroundBridge.NO_ATTEMPT)
        val info = infoFrom(intent)
        // startForeground goes first on every ACTION_START, even a malformed or stale one: a
        // startForegroundService() that never reaches it crashes the process (WR-05).
        val shown = info ?: CallNotificationInfo(bookingId = "", peerName = null, scheduledStart = clock.instant())
        if (!enterForeground(shown)) {
            bridge.onServiceStarted(attempt, false)
            teardown()
            return
        }
        if (info == null || !bridge.isCurrentAttempt(attempt)) {
            // Malformed, orphaned (the call already ended before the service came up) or superseded by a
            // newer start attempt.
            bridge.onServiceStarted(attempt, false)
            teardown()
            return
        }
        requestAudioFocus()
        bridge.onServiceStarted(attempt, true)
        Log.i(TAG, "started")
        scheduleChronometerSwitch(info)
        serviceScope.launch {
            bridge.active.first { !it }
            teardown()
        }
    }

    /** Posts the ongoing notification as a microphone foreground service; false when the system refuses. */
    private fun enterForeground(info: CallNotificationInfo): Boolean =
        try {
            ServiceCompat.startForeground(
                this,
                CallNotificationFactory.NOTIFICATION_ID,
                CallNotificationFactory.build(this, info, clock.instant(), ::formatTime),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            )
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "startForeground rejected ${e.javaClass.simpleName}")
            false
        } catch (e: IllegalStateException) {
            Log.w(TAG, "startForeground rejected ${e.javaClass.simpleName}")
            false
        }

    private fun requestAudioFocus() {
        val manager = getSystemService(AudioManager::class.java) ?: return
        val request =
            AudioFocusRequest
                .Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                ).setOnAudioFocusChangeListener { }
                .build()
        manager.requestAudioFocus(request)
        focusRequest = request
    }

    private fun abandonAudioFocus() {
        val request = focusRequest ?: return
        focusRequest = null
        getSystemService(AudioManager::class.java)?.abandonAudioFocusRequest(request)
    }

    /** One re-post at the scheduled start so the notification flips from "Starts at" to the chronometer. */
    private fun scheduleChronometerSwitch(info: CallNotificationInfo) {
        val remaining = Duration.between(clock.instant(), info.scheduledStart)
        if (remaining.isNegative || remaining.isZero) return
        serviceScope.launch {
            delay(remaining.toMillis())
            if (!canPostNotifications()) return@launch
            val service = this@CallService
            val notification = CallNotificationFactory.build(service, info, clock.instant(), ::formatTime)
            @Suppress("MissingPermission")
            NotificationManagerCompat.from(service).notify(CallNotificationFactory.NOTIFICATION_ID, notification)
        }
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun formatTime(time: Instant): String =
        android.text.format.DateFormat
            .getTimeFormat(this)
            .format(time.toEpochMilli())

    private fun teardown() {
        abandonAudioFocus()
        serviceScope.coroutineContext.cancelChildren()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        // Not stopSelf(): that would also drop a newer start command already queued, whose startForeground
        // would then never run (WR-05).
        stopSelfResult(lastStartId)
    }

    override fun onDestroy() {
        abandonAudioFocus()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.uptrendinvestments.clickmeapp.call.START"
        const val ACTION_END = "com.uptrendinvestments.clickmeapp.call.END"
        const val EXTRA_BOOKING_ID = "com.uptrendinvestments.clickmeapp.call.extra.BOOKING_ID"
        const val EXTRA_PEER_NAME = "com.uptrendinvestments.clickmeapp.call.extra.PEER_NAME"
        const val EXTRA_SCHEDULED_START_MS = "com.uptrendinvestments.clickmeapp.call.extra.SCHEDULED_START_MS"
        const val EXTRA_ATTEMPT_ID = "com.uptrendinvestments.clickmeapp.call.extra.ATTEMPT_ID"
        private const val TAG = "CallService"

        fun startIntent(
            context: Context,
            info: CallNotificationInfo,
            attempt: Long,
        ): Intent =
            Intent(context, CallService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_BOOKING_ID, info.bookingId)
                .putExtra(EXTRA_PEER_NAME, info.peerName)
                .putExtra(EXTRA_SCHEDULED_START_MS, info.scheduledStart.toEpochMilli())
                .putExtra(EXTRA_ATTEMPT_ID, attempt)

        internal fun infoFrom(intent: Intent): CallNotificationInfo? {
            val bookingId = intent.getStringExtra(EXTRA_BOOKING_ID) ?: return null
            if (!intent.hasExtra(EXTRA_SCHEDULED_START_MS)) return null
            val startMs = intent.getLongExtra(EXTRA_SCHEDULED_START_MS, 0L)
            return CallNotificationInfo(
                bookingId = bookingId,
                peerName = intent.getStringExtra(EXTRA_PEER_NAME),
                scheduledStart = Instant.ofEpochMilli(startMs),
            )
        }
    }
}
