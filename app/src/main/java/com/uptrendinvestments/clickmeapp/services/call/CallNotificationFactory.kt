package com.uptrendinvestments.clickmeapp.services.call

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.uptrendinvestments.clickmeapp.MainActivity
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.CallNotificationInfo
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMePalette
import java.time.Instant

/**
 * Builds the ongoing-call notification and its channel (UI-SPEC surface 4).
 *
 * This is the approved plain notification. On Android 14+ a plain ongoing notification is user-dismissible and
 * only `NotificationCompat.CallStyle.forOngoingCall` is exempt, so this object is the single swap point if the
 * user picks CallStyle later (open item logged in the phase gate). The service never builds a notification
 * itself.
 */
object CallNotificationFactory {
    const val CHANNEL_ID = "ongoing_call"
    const val NOTIFICATION_ID = 9_001
    const val EXTRA_EXPAND_CALL = "com.uptrendinvestments.clickmeapp.extra.EXPAND_CALL"

    private const val REQUEST_OPEN = 1
    private const val REQUEST_END = 2

    /** Idempotent. Must run before the first `startForeground` or the platform rejects the notification. */
    fun createChannel(context: Context) {
        val channel =
            NotificationChannelCompat
                .Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(context.getString(R.string.call_notification_channel_name))
                .setDescription(context.getString(R.string.call_notification_channel_description))
                .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    /**
     * Before [CallNotificationInfo.scheduledStart] the notification reads "Starts at {time}"; from the start it
     * shows the system chronometer counting up from it. There is deliberately no Mute action (D-10).
     */
    fun build(
        context: Context,
        info: CallNotificationInfo,
        now: Instant,
        formatTime: (Instant) -> String,
    ): Notification {
        val name = info.peerName?.takeIf { it.isNotBlank() }
        val title =
            if (name != null) {
                context.getString(R.string.call_notification_title, name)
            } else {
                context.getString(R.string.call_notification_title_fallback)
            }
        val builder =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_call)
                .setColor(ClickMePalette.primary.toArgb())
                .setContentTitle(title)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(publicVersion(context))
                .setContentIntent(openIntent(context))
                .addAction(0, context.getString(R.string.call_notification_end_action), endIntent(context))
        if (now >= info.scheduledStart) {
            builder
                .setUsesChronometer(true)
                .setWhen(info.scheduledStart.toEpochMilli())
                .setShowWhen(true)
        } else {
            builder.setContentText(
                context.getString(R.string.call_notification_starts_at, formatTime(info.scheduledStart)),
            )
        }
        return builder.build()
    }

    private fun publicVersion(context: Context): Notification =
        NotificationCompat
            .Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setContentTitle(context.getString(R.string.call_notification_public_title))
            .build()

    /**
     * CLEAR_TOP with SINGLE_TOP routes the tap to the existing MainActivity through `onNewIntent`, even when a
     * Custom Tab opened from the app sits above it in the task; singleTop alone would stack a second instance
     * (WR-06).
     */
    private fun openIntent(context: Context): PendingIntent {
        val intent =
            Intent(context, MainActivity::class.java)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
                ).putExtra(EXTRA_EXPAND_CALL, true)
        return PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    // getService, not getForegroundService: the service is already foreground (RESEARCH Pitfall 13).
    private fun endIntent(context: Context): PendingIntent =
        PendingIntent.getService(
            context,
            REQUEST_END,
            Intent(context, CallService::class.java).setAction(CallService.ACTION_END),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
