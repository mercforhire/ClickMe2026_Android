package com.uptrendinvestments.clickmeapp.testing

import com.uptrendinvestments.clickmeapp.model.responses.NotificationItem
import com.uptrendinvestments.clickmeapp.model.responses.NotificationsPayload
import java.time.Instant

/** Notification DTO builders shared by every Phase 13 test. */
const val NOTIF_BOOKING_ID = "11111111-2222-4333-8444-555555555555"
const val NOTIF_THREAD_ID = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee"

fun notificationItem(
    id: String = "n1",
    category: String = "booking",
    subCategory: String? = "confirmations",
    title: String = "Booking confirmed",
    body: String = "Your session with **Ada** is confirmed.",
    metaData: Map<String, Any?>? = mapOf("booking_id" to NOTIF_BOOKING_ID),
    createdAt: Instant = Instant.parse("2026-10-27T15:00:00Z"),
) = NotificationItem(
    id = id,
    category = category,
    subCategory = subCategory,
    title = title,
    body = body,
    metaData = metaData,
    createdAt = createdAt,
)

fun notificationsPayload(vararg items: NotificationItem) = NotificationsPayload(notifications = items.toList())
