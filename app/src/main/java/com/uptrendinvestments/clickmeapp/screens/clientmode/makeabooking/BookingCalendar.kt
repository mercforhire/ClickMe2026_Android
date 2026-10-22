package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * The cells of [month] padded to whole weeks, starting on [firstDayOfWeek]. Leading and trailing out-of-month cells
 * are `null` and are rendered empty (UI-SPEC deviation from iOS, which shows dimmed numbers). The composable renders
 * this as a Column of 7-cell Rows, never a LazyVerticalGrid inside the scrolling screen (RESEARCH anti-pattern).
 */
fun monthCells(
    month: YearMonth,
    firstDayOfWeek: DayOfWeek,
): List<LocalDate?> {
    val lead = (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
    val trail = (7 - (lead + days.size) % 7) % 7
    return List<LocalDate?>(lead) { null } + days + List<LocalDate?>(trail) { null }
}

/** The seven weekdays in header order, beginning with [firstDayOfWeek]. */
fun weekdayOrder(firstDayOfWeek: DayOfWeek): List<DayOfWeek> = (0..6).map { firstDayOfWeek.plus(it.toLong()) }

/** First day of the week for [locale] (UI-SPEC: follow the locale; iOS uses Sunday). */
fun firstDayOfWeek(locale: Locale): DayOfWeek = WeekFields.of(locale).firstDayOfWeek

/** D-06: days before [today] cannot be selected. */
fun isPastDay(
    date: LocalDate,
    today: LocalDate,
): Boolean = date.isBefore(today)

/** D-06: no month before the current one; navigating forward has no limit. */
fun canGoToPreviousMonth(
    displayed: YearMonth,
    today: LocalDate,
): Boolean = displayed.isAfter(YearMonth.from(today))
