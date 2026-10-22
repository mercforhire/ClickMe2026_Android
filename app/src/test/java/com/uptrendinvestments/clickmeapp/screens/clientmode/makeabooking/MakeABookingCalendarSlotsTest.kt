package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.MakeABookingCalendarCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.MakeABookingTimeSlotsCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.calendarDayTestTag
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.slotChipTestTag
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

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

    // ---- Time slots card ----

    private val anySlotChip =
        SemanticsMatcher("is a slot chip") {
            it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("make_a_booking_slot_") == true
        }

    private val zone = ZoneId.of("America/Toronto")
    private val now = Instant.parse("2026-10-22T16:00:00Z")
    private val past = Instant.parse("2026-10-22T14:00:00Z")
    private val open = Instant.parse("2026-10-22T20:00:00Z")
    private val held = Instant.parse("2026-10-22T21:00:00Z")

    private val loaded =
        AvailabilityState.Loaded(
            mapOf(
                today to
                    listOf(
                        BookingTimeSlot(past, null, true),
                        BookingTimeSlot(open, null, true),
                        BookingTimeSlot(held, null, false),
                    ),
            ),
        )

    private fun setSlots(
        availability: AvailabilityState = loaded,
        selectedDate: LocalDate = today,
        selectedSlotStart: Instant? = null,
        expertTimezone: String? = null,
        selectedDateLoaded: Boolean = true,
        onSelectSlot: (Instant) -> Unit = {},
        onRetry: () -> Unit = {},
    ) {
        rule.setContent {
            ClickMeTheme {
                MakeABookingTimeSlotsCard(
                    selectedDate = selectedDate,
                    availability = availability,
                    now = now,
                    selectedSlotStart = selectedSlotStart,
                    expertTimezone = expertTimezone,
                    onSelectSlot = onSelectSlot,
                    onRetry = onRetry,
                    selectedDateLoaded = selectedDateLoaded,
                    zone = zone,
                    timeFormat = SimpleDateFormat("h:mm a", Locale.US),
                )
            }
        }
    }

    @Test
    fun slotsCardShowsTitleDateLineAndChipsInOrder() {
        setSlots()

        rule.onNodeWithText("Available Time Slots").assertExists()
        rule.onNodeWithText("Thursday, October 22").assertExists()
        rule.onNodeWithText("10:00 AM").assertExists()
        rule.onNodeWithText("4:00 PM").assertExists()
        rule.onNodeWithText("5:00 PM").assertExists()
        val tags = rule.onAllNodes(anySlotChip).fetchSemanticsNodes()
        assertEquals(3, tags.size)
    }

    @Test
    fun onlySelectableSlotCallsOnSelectSlot() {
        val picked = mutableListOf<Instant>()
        setSlots(onSelectSlot = { picked += it })

        listOf(past, held).forEach { start ->
            rule
                .onNodeWithTag(slotChipTestTag(start))
                .assertIsNotEnabled()
                .assert(stateIs("Unavailable"))
                .performClick()
        }
        assertEquals(emptyList<Instant>(), picked)

        rule
            .onNodeWithTag(slotChipTestTag(open))
            .assertIsEnabled()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
            .performClick()
        assertEquals(listOf(open), picked)
    }

    @Test
    fun selectedSlotIsMarkedSelected() {
        setSlots(selectedSlotStart = open)

        rule
            .onNodeWithTag(slotChipTestTag(open))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        rule
            .onNodeWithTag(slotChipTestTag(held))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, false))
    }

    @Test
    fun differentExpertZoneShowsCityAndAbbreviationWithoutIanaId() {
        setSlots(expertTimezone = "America/Los_Angeles")

        rule.onNodeWithText("Times are in your time zone (EDT). Expert is in Los Angeles (PDT).").assertExists()
        rule.onAllNodes(hasText("America/", substring = true)).assertCountEquals(0)
    }

    @Test
    fun sameOrUnknownExpertZoneShowsSingleLine() {
        setSlots(expertTimezone = "America/Toronto")
        rule.onNodeWithText("Times are in your time zone (EDT).").assertExists()
        rule.onAllNodes(hasText("America/", substring = true)).assertCountEquals(0)
    }

    @Test
    fun nullExpertZoneShowsSingleLine() {
        setSlots(expertTimezone = null)
        rule.onNodeWithText("Times are in your time zone (EDT).").assertExists()
    }

    @Test
    fun dayWithoutEntryShowsEmptyMessage() {
        setSlots(selectedDate = LocalDate.of(2026, 10, 23))

        rule.onNodeWithText("No available slots on this day.").assertExists()
        rule.onAllNodes(anySlotChip).assertCountEquals(0)
    }

    @Test
    fun dayOutsideTheLoadedMonthsAsksForADayInsteadOfClaimingNoSlots() {
        // IN-08: the day is kept when the month moves; two months away its slots are unknown, not empty.
        setSlots(selectedDate = LocalDate.of(2026, 10, 23), selectedDateLoaded = false)

        rule.onNodeWithText("Pick a day to see its available times.").assertExists()
        rule.onNodeWithText("No available slots on this day.").assertDoesNotExist()
        rule.onAllNodes(anySlotChip).assertCountEquals(0)
    }

    @Test
    fun loadingShowsSkeletonAndNoChips() {
        setSlots(availability = AvailabilityState.Loading)

        rule.onNodeWithContentDescription("Loading").assertExists()
        rule.onAllNodes(anySlotChip).assertCountEquals(0)
    }

    @Test
    fun failedShowsMessageAndRetryCallsOnRetryOnce() {
        var retries = 0
        setSlots(
            availability = AvailabilityState.Failed(FieldError.Res(R.string.login_error_network)),
            onRetry = { retries++ },
        )

        rule.onNodeWithText("Check your connection and try again.").assertExists()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }
}
