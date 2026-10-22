package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

class BookingCalendarTest {
    @Test
    fun october2026SundayStartHasFiveRows() {
        val cells = monthCells(YearMonth.of(2026, 10), DayOfWeek.SUNDAY)
        assertEquals(35, cells.size)
        (0..3).forEach { assertNull(cells[it]) }
        assertEquals(LocalDate.of(2026, 10, 1), cells[4])
        assertEquals(LocalDate.of(2026, 10, 31), cells[34])
    }

    @Test
    fun october2026MondayStartPadsTrailingCell() {
        val cells = monthCells(YearMonth.of(2026, 10), DayOfWeek.MONDAY)
        assertEquals(35, cells.size)
        (0..2).forEach { assertNull(cells[it]) }
        assertEquals(LocalDate.of(2026, 10, 1), cells[3])
        assertNull(cells[34])
    }

    @Test
    fun february2026SundayStartIsFourFullRows() {
        val cells = monthCells(YearMonth.of(2026, 2), DayOfWeek.SUNDAY)
        assertEquals(28, cells.size)
        assertEquals(LocalDate.of(2026, 2, 1), cells[0])
        assertTrue(cells.none { it == null })
    }

    @Test
    fun august2026SundayStartIsSixRows() {
        val cells = monthCells(YearMonth.of(2026, 8), DayOfWeek.SUNDAY)
        assertEquals(42, cells.size)
        (0..5).forEach { assertNull(cells[it]) }
        assertEquals(LocalDate.of(2026, 8, 1), cells[6])
        (37..41).forEach { assertNull(cells[it]) }
    }

    @Test
    fun leapFebruary2028IncludesFeb29() {
        val cells = monthCells(YearMonth.of(2028, 2), DayOfWeek.SUNDAY)
        assertEquals(35, cells.size)
        (0..1).forEach { assertNull(cells[it]) }
        assertTrue(cells.contains(LocalDate.of(2028, 2, 29)))
    }

    @Test
    fun everyMonthOf2026And2027IsWholeWeeksWithOrderedDays() {
        for (year in 2026..2027) {
            for (monthNumber in 1..12) {
                val month = YearMonth.of(year, monthNumber)
                for (first in listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY)) {
                    val cells = monthCells(month, first)
                    assertEquals(0, cells.size % 7)
                    assertTrue(cells.size <= 42)
                    val days = cells.filterNotNull()
                    assertEquals((1..month.lengthOfMonth()).map { month.atDay(it) }, days)
                }
            }
        }
    }

    @Test
    fun weekdayOrderStartsAtFirstDay() {
        assertEquals(
            listOf(
                DayOfWeek.SUNDAY,
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
            ),
            weekdayOrder(DayOfWeek.SUNDAY),
        )
        val monday = weekdayOrder(DayOfWeek.MONDAY)
        assertEquals(DayOfWeek.MONDAY, monday.first())
        assertEquals(DayOfWeek.SUNDAY, monday.last())
    }

    @Test
    fun firstDayOfWeekFollowsLocale() {
        assertEquals(DayOfWeek.SUNDAY, firstDayOfWeek(Locale.US))
        assertEquals(DayOfWeek.MONDAY, firstDayOfWeek(Locale.UK))
    }

    @Test
    fun isPastDayIsTrueOnlyBeforeToday() {
        val today = LocalDate.of(2026, 10, 22)
        assertTrue(isPastDay(LocalDate.of(2026, 10, 21), today))
        assertFalse(isPastDay(LocalDate.of(2026, 10, 22), today))
        assertFalse(isPastDay(LocalDate.of(2026, 10, 23), today))
    }

    @Test
    fun cannotGoBeforeCurrentMonth() {
        val today = LocalDate.of(2026, 10, 22)
        assertFalse(canGoToPreviousMonth(YearMonth.of(2026, 10), today))
        assertTrue(canGoToPreviousMonth(YearMonth.of(2026, 11), today))
        assertFalse(canGoToPreviousMonth(YearMonth.of(2026, 9), today))
    }
}
