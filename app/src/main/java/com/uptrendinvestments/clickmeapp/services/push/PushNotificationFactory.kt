package com.uptrendinvestments.clickmeapp.services.push

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.uptrendinvestments.clickmeapp.MainActivity
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.notifications.stripBoldMarkers
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
 * launcher intent FCM builds for a system-displayed push and both paths meet in `MainActivity`. Nothing here logs a
 * title, a body or a data value, and non-routing data keys (sender id, amounts) are never copied into the intent.
 */
object PushNotificationFactory {
    /** Distinct from the ongoing call's 9_001. Notifications stay unique through their tag. */
    const val PUSH_NOTIFICATION_ID = 9_100

    /**
     * Pushes for the same category and entity share one tag, so a repeat updates the notification in place. A push with
     * no entity id gets a fresh tag each time.
     */
    fun tagFor(push: ForegroundPush): String {
        val event = PushEvent.from(push.data)
        val entity =
            if (event?.category == "message") {
                event.threadId
            } else {
                event?.bookingId ?: event?.payoutId ?: event?.reviewId
            }
        return if (event?.category != null && entity != null) {
            "push:${event.category}:$entity"
        } else {
            "push:" + UUID.randomUUID()
        }
    }

    /** The FCM channel when it is one of the five contract ids, else the category's channel (RESEARCH Pattern 6). */
    fun channelFor(push: ForegroundPush): String =
        push.channelId?.takeIf { it in NotificationChannels.ALL_IDS }
            ?: NotificationChannels.channelIdFor(push.data["category"])

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

    /**
     * A missing title or body shows the other field; neither shows "ClickMe" (UI-SPEC E8). Bold markers are stripped
     * (the list renders them as bold, a system notification cannot). The lock screen shows a generic public version.
     */
    fun build(
        context: Context,
        push: ForegroundPush,
        tag: String,
    ): Notification {
        val title = push.title?.let(::stripBoldMarkers)?.takeIf { it.isNotBlank() }
        val text = push.body?.let(::stripBoldMarkers)?.takeIf { it.isNotBlank() }
        val builder =
            NotificationCompat
                .Builder(context, channelFor(push))
                .setSmallIcon(R.drawable.ic_stat_clickme)
                .setColor(ClickMePalette.primary.toArgb())
                .setContentTitle(title ?: if (text == null) context.getString(R.string.push_fallback_title) else null)
                .setCategory(categoryFor(push))
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(publicVersion(context, channelFor(push)))
                .setOnlyAlertOnce(false)
                .setAutoCancel(true)
                .setContentIntent(contentIntent(context, push, tag))
        if (text != null) {
            builder.setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text))
        }
        return builder.build()
    }

    private fun categoryFor(push: ForegroundPush): String =
        when (PushEvent.from(push.data)?.category) {
            "message" -> NotificationCompat.CATEGORY_MESSAGE
            "booking", "session" -> NotificationCompat.CATEGORY_EVENT
            else -> NotificationCompat.CATEGORY_STATUS
        }

    private fun publicVersion(
        context: Context,
        channelId: String,
    ): Notification =
        NotificationCompat
            .Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_clickme)
            .setColor(ClickMePalette.primary.toArgb())
            .setContentTitle(context.getString(R.string.push_public_title))
            .setContentText(context.getString(R.string.push_public_text))
            .build()
}

/** Posts foreground pushes through the notification manager, and nothing when the user has not allowed them. */
@Singleton
class SystemPushNotificationPoster
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : PushNotificationPoster {
        override fun post(push: ForegroundPush) {
            val manager = NotificationManagerCompat.from(context)
            if (!manager.areNotificationsEnabled()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            NotificationChannels.ensureCreated(context)
            val tag = PushNotificationFactory.tagFor(push)
            val notification = PushNotificationFactory.build(context, push, tag)
            manager.notify(tag, PushNotificationFactory.PUSH_NOTIFICATION_ID, notification)
        }
    }
