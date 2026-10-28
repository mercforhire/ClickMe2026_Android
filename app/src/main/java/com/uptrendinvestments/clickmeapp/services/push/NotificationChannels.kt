package com.uptrendinvestments.clickmeapp.services.push

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.uptrendinvestments.clickmeapp.R

/**
 * ClickMe push notification channels.
 *
 * The ids are a contract with the backend `push.js` `CHANNEL_BY_CATEGORY` (D-04) and are permanent once shipped:
 * renaming one orphans the user's per-channel settings. The ongoing-call channel is owned by
 * `CallNotificationFactory` and is never touched here.
 */
object NotificationChannels {
    const val BOOKINGS = "bookings"
    const val SESSIONS = "sessions"
    const val MESSAGES = "messages"
    const val ACCOUNT_PAYOUTS = "account_payouts"
    const val GENERAL = "general"

    val ALL_IDS = listOf(BOOKINGS, SESSIONS, MESSAGES, ACCOUNT_PAYOUTS, GENERAL)

    /** Idempotent. Runs at app start so the manifest default channel always exists. */
    fun ensureCreated(context: Context) {
        val channel =
            NotificationChannelCompat
                .Builder(GENERAL, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.push_channel_general_name))
                .setDescription(context.getString(R.string.push_channel_general_description))
                .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }
}
