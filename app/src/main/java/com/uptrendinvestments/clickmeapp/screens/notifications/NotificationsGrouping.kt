package com.uptrendinvestments.clickmeapp.screens.notifications

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** The date bucket of a notification row, in the order the list shows them. */
enum class NotificationDay {
    Today,
    Yesterday,
    Earlier,
}

/** One dated block of the list. Never empty. */
data class NotificationSection(
    val day: NotificationDay,
    val rows: List<NotificationRowModel>,
)

/** The relative time of a row as a typed value; the screen formats it (13-12). */
sealed interface NotificationTime {
    /** Under a minute old, or created in the future (clock skew). */
    data object JustNow : NotificationTime

    data class MinutesAgo(
        val minutes: Int,
    ) : NotificationTime

    data class HoursAgo(
        val hours: Int,
    ) : NotificationTime

    data class Yesterday(
        val time: LocalTime,
    ) : NotificationTime

    data class Earlier(
        val date: LocalDate,
        val time: LocalTime,
    ) : NotificationTime
}

/** The relative time of a row created at [createdAt], seen at [now] in [zone]. */
@Suppress("UNUSED_PARAMETER")
fun notificationTime(
    createdAt: Instant,
    now: Instant,
    zone: ZoneId,
): NotificationTime = NotificationTime.JustNow

/**
 * Groups [rows] by the local calendar day of `createdAt` into Today, Yesterday and Earlier, in that order.
 *
 * Input order (the server's, newest first) is kept inside each section: this never sorts, so rows with equal
 * timestamps keep their relative order. Yesterday is the previous calendar date, not now minus 24 hours, so a DST
 * change cannot move a row between sections. Empty sections are omitted.
 */
fun groupNotifications(
    rows: List<NotificationRowModel>,
    now: Instant,
    zone: ZoneId,
): List<NotificationSection> {
    val today = now.atZone(zone).toLocalDate()
    val yesterday = today.minusDays(1)
    val byDay =
        rows.groupBy { row ->
            when (row.createdAt.atZone(zone).toLocalDate()) {
                today -> NotificationDay.Today
                yesterday -> NotificationDay.Yesterday
                else -> NotificationDay.Earlier
            }
        }
    return NotificationDay.entries.mapNotNull { day ->
        byDay[day]?.let { NotificationSection(day, it) }
    }
}
