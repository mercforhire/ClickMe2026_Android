package com.uptrendinvestments.clickmeapp.screens.settings.notifications

import androidx.annotation.StringRes
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.requests.NotificationPreferenceInput
import com.uptrendinvestments.clickmeapp.model.requests.UpdateNotificationPreferencesRequest
import com.uptrendinvestments.clickmeapp.model.responses.NotificationPreferenceRow

/** Quiet period after the last toggle flip before the PATCH is sent (iOS `autoSaveDebounce`, 0.8 s). */
const val NOTIFICATION_DEBOUNCE_MS = 800L

/** One toggle row: a `(category, sub_category)` pair and its title. */
data class NotificationRow(
    val category: String,
    val subCategory: String,
    @param:StringRes val title: Int,
) {
    /** The key of this row in the state map. */
    val key: String get() = "$category|$subCategory"
}

/** A headed group of rows that share one category. */
data class NotificationSection(
    @param:StringRes val title: Int,
    val rows: List<NotificationRow>,
)

/**
 * The grid, and the only source of what is rendered and what is sent. Ported from iOS `defaultSections`: category keys
 * are singular (`booking`, not `bookings`). Every other category and sub-category the server knows (payout, account
 * and promotions rows, and the booking payment-actions row) is absent on purpose, as on iOS: those are transactional
 * and never switched off from here. There is no quiet-hours state anywhere (the backend stores nothing for it).
 */
val NOTIFICATION_SECTIONS: List<NotificationSection> =
    listOf(
        NotificationSection(
            title = R.string.notification_settings_section_bookings,
            rows =
                listOf(
                    NotificationRow("booking", "requests", R.string.notification_settings_row_requests),
                    NotificationRow("booking", "confirmations", R.string.notification_settings_row_confirmations),
                    NotificationRow("booking", "declines", R.string.notification_settings_row_declines),
                    NotificationRow("booking", "cancellations", R.string.notification_settings_row_cancellations),
                    NotificationRow("booking", "reschedules", R.string.notification_settings_row_reschedules),
                    NotificationRow("booking", "expirations", R.string.notification_settings_row_expirations),
                    NotificationRow("booking", "refunds", R.string.notification_settings_row_refunds),
                ),
        ),
        NotificationSection(
            title = R.string.notification_settings_section_sessions,
            rows =
                listOf(
                    NotificationRow("session", "reminder", R.string.notification_settings_row_reminder),
                    NotificationRow("session", "no_show", R.string.notification_settings_row_no_show),
                    NotificationRow("session", "ended", R.string.notification_settings_row_ended),
                ),
        ),
        NotificationSection(
            title = R.string.notification_settings_section_messages,
            rows = listOf(NotificationRow("message", "new_message", R.string.notification_settings_row_new_message)),
        ),
        NotificationSection(
            title = R.string.notification_settings_section_reviews,
            rows =
                listOf(
                    NotificationRow("review", "received", R.string.notification_settings_row_received),
                    NotificationRow("review", "nudge", R.string.notification_settings_row_nudge),
                ),
        ),
    )

/** Every grid row on: the state before the server answers, and the default for a row the server omits. */
fun defaultNotificationValues(): Map<String, Boolean> =
    NOTIFICATION_SECTIONS.flatMap { it.rows }.associate { it.key to true }

/**
 * Overlays the server's rows onto the grid (port of iOS `apply(preferences:)`). A row is on if any channel is on; a
 * row the server omits stays on; a server row outside the grid is dropped, because the map is built from the grid's
 * keys only, so an unknown key can never enter the state or a later PATCH.
 */
fun hydrateNotificationValues(rows: List<NotificationPreferenceRow>): Map<String, Boolean> {
    val index = rows.associate { "${it.category}|${it.subCategory}" to it.isAnyChannelEnabled }
    return defaultNotificationValues().mapValues { (key, default) -> index[key] ?: default }
}

/**
 * The PATCH body: every grid row, in order, with all three channels set to the row's value (iOS
 * `buildPreferenceInputs`). A key missing from the map counts as on.
 */
fun Map<String, Boolean>.toPreferencesRequest(): UpdateNotificationPreferencesRequest =
    UpdateNotificationPreferencesRequest(
        preferences =
            NOTIFICATION_SECTIONS.flatMap { it.rows }.map { row ->
                val on = this[row.key] ?: true
                NotificationPreferenceInput(
                    category = row.category,
                    subCategory = row.subCategory,
                    pushEnabled = on,
                    emailEnabled = on,
                    inAppEnabled = on,
                )
            },
    )
