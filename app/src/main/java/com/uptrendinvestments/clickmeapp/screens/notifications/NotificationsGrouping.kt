package com.uptrendinvestments.clickmeapp.screens.notifications

import java.time.Duration
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

/**
 * The relative time of a row created at [createdAt], seen at [now] in [zone]: under 60 s JustNow, under 60 min
 * minutes, the rest of today hours, the previous calendar day Yesterday, else the date. A future [createdAt] (clock
 * skew) counts as JustNow.
 */
fun notificationTime(
    createdAt: Instant,
    now: Instant,
    zone: ZoneId,
): NotificationTime {
    val seconds = maxOf(0L, Duration.between(createdAt, now).seconds)
    if (seconds < SECONDS_PER_MINUTE) return NotificationTime.JustNow
    if (seconds < SECONDS_PER_HOUR) return NotificationTime.MinutesAgo((seconds / SECONDS_PER_MINUTE).toInt())
    val created = createdAt.atZone(zone)
    val today = now.atZone(zone).toLocalDate()
    return when (created.toLocalDate()) {
        today -> NotificationTime.HoursAgo(maxOf(1L, seconds / SECONDS_PER_HOUR).toInt())
        today.minusDays(1) -> NotificationTime.Yesterday(created.toLocalTime())
        else -> NotificationTime.Earlier(created.toLocalDate(), created.toLocalTime())
    }
}

private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3600L

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
