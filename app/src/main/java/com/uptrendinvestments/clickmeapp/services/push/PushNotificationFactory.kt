package com.uptrendinvestments.clickmeapp.services.push

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.uptrendinvestments.clickmeapp.MainActivity
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.push.PushEvent
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMePalette
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the system notification for a foreground push (UI-SPEC Surface 6).
 *
 * The tap intent targets `MainActivity` and carries only the push routing keys, so it has the same shape as the
 * launcher intent FCM builds for a system-displayed push and both paths meet in `MainActivity`.
 */
object PushNotificationFactory {
    /** Distinct from the ongoing call's 9_001. Notifications stay unique through their tag. */
    const val PUSH_NOTIFICATION_ID = 9_100

    fun tagFor(push: ForegroundPush): String = "push:" + UUID.randomUUID()

    fun channelFor(push: ForegroundPush): String = NotificationChannels.channelIdFor(push.data["category"])

    /**
     * CLEAR_TOP with SINGLE_TOP routes the tap to the running MainActivity through `onNewIntent`. The request code is
     * derived from [tag] so two notifications never share a PendingIntent and its extras.
     */
    fun contentIntent(
        context: Context,
        push: ForegroundPush,
        tag: String,
    ): PendingIntent {
        val intent =
            Intent(context, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
        push.data.forEach { (key, value) -> if (key in PushEvent.PUSH_KEYS) intent.putExtra(key, value) }
        return PendingIntent.getActivity(
            context,
            tag.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun build(
        context: Context,
        push: ForegroundPush,
        tag: String,
    ): Notification =
        NotificationCompat
            .Builder(context, channelFor(push))
            .setSmallIcon(R.drawable.ic_stat_clickme)
            .setColor(ClickMePalette.primary.toArgb())
            .setContentTitle(push.title)
            .setContentText(push.body)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context, push, tag))
            .build()
}

/** Posts foreground pushes through the notification manager. */
@Singleton
class SystemPushNotificationPoster
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : PushNotificationPoster {
        override fun post(push: ForegroundPush) {
            NotificationChannels.ensureCreated(context)
            val tag = PushNotificationFactory.tagFor(push)
            val notification = PushNotificationFactory.build(context, push, tag)
            val manager = NotificationManagerCompat.from(context)
            manager.notify(tag, PushNotificationFactory.PUSH_NOTIFICATION_ID, notification)
        }
    }
