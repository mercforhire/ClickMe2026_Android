package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.MakeABookingCalendarCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.calendarDayTestTag
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
class MakeABookingCalendarSlotsTest {
    @get:Rule
    val rule = createComposeRule()

    private val today = LocalDate.of(2026, 10, 22)

    private fun stateIs(expected: String) =
        SemanticsMatcher("stateDescription == $expected") {
            it.config.getOrNull(SemanticsProperties.StateDescription) == expected
        }

    private fun stateContains(part: String) =
        SemanticsMatcher("stateDescription contains $part") {
            it.config.getOrNull(SemanticsProperties.StateDescription)?.contains(part) == true
        }

    private fun setCalendar(
        openDays: Set<LocalDate> = emptySet(),
        canGoToPreviousMonth: Boolean = false,
        onPreviousMonth: () -> Unit = {},
        onNextMonth: () -> Unit = {},
        onSelectDate: (LocalDate) -> Unit = {},
    ) {
        rule.setContent {
            ClickMeTheme {
                MakeABookingCalendarCard(
                    displayedMonth = YearMonth.of(2026, 10),
                    today = today,
                    selectedDate = today,
                    openDays = openDays,
                    canGoToPreviousMonth = canGoToPreviousMonth,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onSelectDate = onSelectDate,
                )
            }
        }
    }

    @Test
    fun calendarShowsMonthWeekdayHeadersAndOnlyInMonthCells() {
        setCalendar()

        rule.onNodeWithText("October 2026").assertExists()
        rule.onNodeWithText("Sun").assertExists()
        rule.onNodeWithText("Sat").assertExists()
        rule.onNodeWithTag(calendarDayTestTag(LocalDate.of(2026, 10, 1))).assertExists()
        rule.onNodeWithTag(calendarDayTestTag(LocalDate.of(2026, 9, 30))).assertDoesNotExist()
        rule.onNodeWithTag(calendarDayTestTag(LocalDate.of(2026, 11, 1))).assertDoesNotExist()
    }

    @Test
    fun pastDayIsDisabledAndFutureDayCallsOnSelectDate() {
        val selected = mutableListOf<LocalDate>()
        setCalendar(onSelectDate = { selected += it })

        rule.onNodeWithTag(calendarDayTestTag(LocalDate.of(2026, 10, 21))).assertIsNotEnabled().performClick()
        assertEquals(emptyList<LocalDate>(), selected)

        rule.onNodeWithTag(calendarDayTestTag(LocalDate.of(2026, 10, 24))).assertIsEnabled().performClick()
        assertEquals(listOf(LocalDate.of(2026, 10, 24)), selected)
    }

    @Test
    fun selectedDayIsSelectedAndTodayIsAnnounced() {
        setCalendar()

        rule
            .onNodeWithTag(calendarDayTestTag(today))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
            .assert(stateContains("Today"))
        rule
            .onNodeWithTag(calendarDayTestTag(LocalDate.of(2026, 10, 23)))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, false))
    }

    @Test
    fun openDaysAreAnnouncedAsHavingOpenTimes() {
        setCalendar(openDays = setOf(LocalDate.of(2026, 10, 24)))

        rule.onNodeWithTag(calendarDayTestTag(LocalDate.of(2026, 10, 24))).assert(stateIs("Has open times"))
        rule
            .onNodeWithTag(calendarDayTestTag(LocalDate.of(2026, 10, 25)))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
    }

    @Test
    fun dayNodesCarryTheFullDateAsContentDescription() {
        setCalendar()

        rule.onNodeWithContentDescription("Saturday, October 24, 2026").assertExists()
        rule
            .onNodeWithTag(calendarDayTestTag(LocalDate.of(2026, 10, 24)))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("Saturday, October 24, 2026"),
                ),
            )
    }

    @Test
    fun monthArrowsRespectBoundsAndCallbacks() {
        var previous = 0
        var next = 0
        setCalendar(onPreviousMonth = { previous++ }, onNextMonth = { next++ })

        rule.onNodeWithContentDescription("Previous month").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Next month").assertIsEnabled().performClick()
        assertEquals(1, next)
        assertEquals(0, previous)
    }

    @Test
    fun previousMonthArrowCallsBackWhenAllowed() {
        var previous = 0
        setCalendar(canGoToPreviousMonth = true, onPreviousMonth = { previous++ })

        rule.onNodeWithContentDescription("Previous month").assertIsEnabled().performClick()
        assertEquals(1, previous)
    }
}
