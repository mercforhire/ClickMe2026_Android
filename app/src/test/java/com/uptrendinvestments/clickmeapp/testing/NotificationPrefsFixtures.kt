package com.uptrendinvestments.clickmeapp.testing

import com.uptrendinvestments.clickmeapp.model.responses.NotificationPreferenceRow
import com.uptrendinvestments.clickmeapp.screens.settings.notifications.NOTIFICATION_SECTIONS

/** One `/notifications/preferences` row as the server returns it. */
fun notificationRow(
    category: String = "booking",
    sub: String = "requests",
    push: Boolean = true,
    email: Boolean = true,
    inApp: Boolean = true,
): NotificationPreferenceRow =
    NotificationPreferenceRow(
        category = category,
        subCategory = sub,
        pushEnabled = push,
        emailEnabled = email,
        inAppEnabled = inApp,
    )

/** A server answer covering every grid row with all three channels set to [on]. */
fun allNotificationRows(on: Boolean = true): List<NotificationPreferenceRow> =
    NOTIFICATION_SECTIONS.flatMap { it.rows }.map {
        notificationRow(category = it.category, sub = it.subCategory, push = on, email = on, inApp = on)
    }
