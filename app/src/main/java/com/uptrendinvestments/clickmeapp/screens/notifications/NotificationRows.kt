package com.uptrendinvestments.clickmeapp.screens.notifications

import com.uptrendinvestments.clickmeapp.model.responses.NotificationItem
import com.uptrendinvestments.clickmeapp.screens.push.PushDestination
import com.uptrendinvestments.clickmeapp.screens.push.PushEvent
import com.uptrendinvestments.clickmeapp.screens.push.PushRouter
import com.uptrendinvestments.clickmeapp.screens.push.landsOnNotificationsList
import java.time.Instant
import java.time.ZoneId

/**
 * One notification row as the list shows it. [destination] is null for an inert row. [toString] prints the id and
 * category only: titles and bodies are user data.
 */
data class NotificationRowModel(
    val id: String,
    val category: String,
    val title: String,
    val body: String,
    val createdAt: Instant,
    val time: NotificationTime,
    val destination: PushDestination?,
) {
    override fun toString(): String = "NotificationRowModel(id=$id, category=$category)"
}

/**
 * Maps a server row to its list model, with the time label relative to [now] in [zone].
 *
 * The destination is the push router's, so a tap is the same as a push tap. A null destination makes the row inert
 * (UI-SPEC A2): the category is unknown, a required id is missing or invalid, or it is payout or account, which land
 * on this very list until Phase 14.
 */
fun NotificationItem.toRowModel(
    now: Instant,
    zone: ZoneId,
): NotificationRowModel =
    NotificationRowModel(
        id = id,
        category = category,
        title = title,
        body = body,
        createdAt = createdAt,
        time = notificationTime(createdAt, now, zone),
        destination =
            PushEvent
                .fromRow(category, subCategory, metaData)
                ?.let(PushRouter::destination)
                ?.takeUnless { it.landsOnNotificationsList },
    )
