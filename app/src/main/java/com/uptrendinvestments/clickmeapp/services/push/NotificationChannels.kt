package com.uptrendinvestments.clickmeapp.services.push

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.uptrendinvestments.clickmeapp.R
import java.util.Locale

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

    /**
     * Idempotent. Runs at app start so the manifest default channel always exists. The four contract channels are
     * High so a foreground post shows as a heads-up (UI-SPEC A3); General is Default and has no heads-up.
     */
    fun ensureCreated(context: Context) {
        val channels =
            listOf(
                channel(
                    context,
                    BOOKINGS,
                    NotificationManagerCompat.IMPORTANCE_HIGH,
                    R.string.push_channel_bookings_name,
                    R.string.push_channel_bookings_description,
                ),
                channel(
                    context,
                    SESSIONS,
                    NotificationManagerCompat.IMPORTANCE_HIGH,
                    R.string.push_channel_sessions_name,
                    R.string.push_channel_sessions_description,
                ),
                channel(
                    context,
                    MESSAGES,
                    NotificationManagerCompat.IMPORTANCE_HIGH,
                    R.string.push_channel_messages_name,
                    R.string.push_channel_messages_description,
                ),
                channel(
                    context,
                    ACCOUNT_PAYOUTS,
                    NotificationManagerCompat.IMPORTANCE_HIGH,
                    R.string.push_channel_account_payouts_name,
                    R.string.push_channel_account_payouts_description,
                ),
                channel(
                    context,
                    GENERAL,
                    NotificationManagerCompat.IMPORTANCE_DEFAULT,
                    R.string.push_channel_general_name,
                    R.string.push_channel_general_description,
                ),
            )
        NotificationManagerCompat.from(context).createNotificationChannelsCompat(channels)
    }

    /** The backend's `channelIdFor` mapping (D-04); anything unknown or missing is [GENERAL]. */
    fun channelIdFor(category: String?): String =
        when (category?.trim()?.lowercase(Locale.ROOT)) {
            "booking" -> BOOKINGS
            "session", "review" -> SESSIONS
            "message" -> MESSAGES
            "payout", "account" -> ACCOUNT_PAYOUTS
            else -> GENERAL
        }

    private fun channel(
        context: Context,
        id: String,
        importance: Int,
        nameRes: Int,
        descriptionRes: Int,
    ): NotificationChannelCompat =
        NotificationChannelCompat
            .Builder(id, importance)
            .setName(context.getString(nameRes))
            .setDescription(context.getString(descriptionRes))
            .build()
}
