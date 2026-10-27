@file:Suppress("ktlint:standard:filename")

package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import java.time.LocalDate
import java.time.ZoneId

private const val SECONDS_PER_MINUTE = 60L

/** One row of the thread list, built from the oldest-to-newest [ThreadItem]s by [buildThreadRows]. */
sealed interface ThreadRow {
    /** Stable LazyColumn key. */
    val key: String

    /** A date separator, shown when the local calendar day changes. */
    data class DateHeader(
        val date: LocalDate,
    ) : ThreadRow {
        override val key: String
            get() = "d:$date"
    }

    /**
     * A message or a system event. [showAvatar] is true on the last incoming message of a run; [tightAbove] is true
     * on a message that continues the run of the (older) message above it.
     */
    data class Entry(
        val item: ThreadItem,
        val showAvatar: Boolean,
        val tightAbove: Boolean,
    ) : ThreadRow {
        override val key: String
            get() = item.key
    }
}

/**
 * Turns [items] (oldest to newest) into rows: a [ThreadRow.DateHeader] before the first item and whenever the local
 * date in [zone] changes, with run grouping. A run is consecutive messages from the same sender whose times fall in
 * the same minute; a system event or a date header breaks it.
 */
fun buildThreadRows(
    items: List<ThreadItem>,
    zone: ZoneId,
): List<ThreadRow> {
    val rows = ArrayList<ThreadRow>(items.size + 1)
    var previousDate: LocalDate? = null
    items.forEachIndexed { index, item ->
        val date = item.createdAt.atZone(zone).toLocalDate()
        if (date != previousDate) rows += ThreadRow.DateHeader(date)
        previousDate = date
        val above = items.getOrNull(index - 1)
        val below = items.getOrNull(index + 1)
        val tightAbove = continuesRun(above, item)
        val showAvatar = item is ThreadItem.Message && !item.mine && !continuesRun(item, below)
        rows += ThreadRow.Entry(item = item, showAvatar = showAvatar, tightAbove = tightAbove)
    }
    return rows
}

private fun continuesRun(
    earlier: ThreadItem?,
    later: ThreadItem?,
): Boolean =
    earlier is ThreadItem.Message &&
        later is ThreadItem.Message &&
        earlier.mine == later.mine &&
        earlier.senderId == later.senderId &&
        earlier.createdAt.epochSecond / SECONDS_PER_MINUTE == later.createdAt.epochSecond / SECONDS_PER_MINUTE
