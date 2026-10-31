package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.MakeABookingTimeSlotsCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.PAID_WINDOW_NOTE_TEST_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.slotChipTestTag
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/**
 * The Make a Booking cards reused by the Phase 16.2 reschedule picker: the new optional parameters behave as the
 * picker needs and their defaults keep Make a Booking unchanged.
 */
@RunWith(RobolectricTestRunner::class)
class SharedPickerCardsTest {
    @get:Rule
    val rule = createComposeRule()

    private val zone = ZoneId.of("America/Toronto")
    private val day = LocalDate.of(2026, 10, 22)
    private val early = Instant.parse("2026-10-22T14:00:00Z")
    private val middle = Instant.parse("2026-10-22T15:00:00Z")
    private val late = Instant.parse("2026-10-22T16:00:00Z")
    private val morning = Instant.parse("2026-10-22T12:00:00Z")
    private val pickerPrefix = "reschedule_slot_"

    private val loaded =
        AvailabilityState.Loaded(
            mapOf(
                day to
                    listOf(
                        BookingTimeSlot(early, null, true),
                        BookingTimeSlot(middle, null, true),
                        BookingTimeSlot(late, null, true),
                    ),
            ),
        )

    private fun stateIs(expected: String) =
        SemanticsMatcher("stateDescription == $expected") {
            it.config.getOrNull(SemanticsProperties.StateDescription) == expected
        }

    private fun setSlots(
        content: @Composable (now: Instant, onSelect: (Instant) -> Unit) -> Unit,
        now: Instant = morning,
        onSelectSlot: (Instant) -> Unit = {},
    ) {
        rule.setContent { ClickMeTheme { content(now, onSelectSlot) } }
    }

    private fun setPicker(
        now: Instant = morning,
        onSelectSlot: (Instant) -> Unit = {},
        availability: AvailabilityState = loaded,
        hidePastSlots: Boolean = false,
        currentSlotStart: Instant? = null,
        latestSelectableStart: Instant? = null,
    ) {
        setSlots(
            content = { n, onSelect ->
                MakeABookingTimeSlotsCard(
                    selectedDate = day,
                    availability = availability,
                    now = n,
                    selectedSlotStart = null,
                    expertTimezone = null,
                    onSelectSlot = onSelect,
                    onRetry = {},
                    zone = zone,
                    timeFormat = SimpleDateFormat("h:mm a", Locale.US),
                    latestSelectableStart = latestSelectableStart,
                    paidWindowNote = "Paid sessions can be rescheduled up to 6 days ahead.",
                    paidWindowNoteTag = "reschedule_paid_window_note",
                    hidePastSlots = hidePastSlots,
                    currentSlotStart = currentSlotStart,
                    slotTagPrefix = pickerPrefix,
                )
            },
            now = now,
            onSelectSlot = onSelectSlot,
        )
    }

    @Test
    fun currentSlotRendersCurrentChipAndIsNotSelectable() {
        val picked = mutableListOf<Instant>()
        setPicker(currentSlotStart = middle, onSelectSlot = { picked += it })

        rule
            .onNodeWithTag(slotChipTestTag(middle, pickerPrefix))
            .assert(hasText("Current"))
            .assert(stateIs("Current session time"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, false))
            .performClick()
        assertEquals(emptyList<Instant>(), picked)

        rule.onNodeWithTag(slotChipTestTag(early, pickerPrefix)).assertIsEnabled().performClick()
        assertEquals(listOf(early), picked)
    }

    @Test
    fun currentSlotMatchesWithinOneSecond() {
        setPicker(currentSlotStart = Instant.parse("2026-10-22T15:00:00.400Z"))

        rule
            .onNodeWithTag(slotChipTestTag(middle, pickerPrefix))
            .assert(hasText("Current"))
            .assert(stateIs("Current session time"))
    }

    @Test
    fun hidePastSlotsRemovesStartedSlots() {
        val now = Instant.parse("2026-10-22T15:30:00Z")
        setPicker(now = now, hidePastSlots = true)

        rule.onNodeWithTag(slotChipTestTag(early, pickerPrefix)).assertDoesNotExist()
        rule.onNodeWithTag(slotChipTestTag(middle, pickerPrefix)).assertDoesNotExist()
        rule.onNodeWithTag(slotChipTestTag(late, pickerPrefix)).assertExists()
    }

    @Test
    fun pastSlotsAreDimmedNotHiddenByDefault() {
        val now = Instant.parse("2026-10-22T15:30:00Z")
        setPicker(now = now)

        rule.onNodeWithTag(slotChipTestTag(early, pickerPrefix)).assertIsNotEnabled()
        rule.onNodeWithTag(slotChipTestTag(middle, pickerPrefix)).assertIsNotEnabled()
        rule.onNodeWithTag(slotChipTestTag(late, pickerPrefix)).assertIsEnabled()
    }

    @Test
    fun hidingAllSlotsShowsEmptyLine() {
        val now = Instant.parse("2026-10-22T17:00:00Z")
        setPicker(now = now, hidePastSlots = true)

        rule.onNodeWithText("No available slots on this day.").assertExists()
        rule.onNodeWithTag(slotChipTestTag(late, pickerPrefix)).assertDoesNotExist()
    }

    @Test
    fun missingCurrentSlotIsNotSynthesized() {
        setPicker(currentSlotStart = Instant.parse("2026-10-22T18:00:00Z"))

        rule.onAllNodes(hasText("Current")).assertCountEquals(0)
        rule.onNodeWithTag(slotChipTestTag(Instant.parse("2026-10-22T18:00:00Z"), pickerPrefix)).assertDoesNotExist()
    }

    @Test
    fun paidNoteUsesGivenCopyAndTag() {
        setPicker(latestSelectableStart = middle)

        rule
            .onNodeWithTag("reschedule_paid_window_note")
            .assert(hasText("Paid sessions can be rescheduled up to 6 days ahead."))
        rule.onNodeWithTag(PAID_WINDOW_NOTE_TEST_TAG).assertDoesNotExist()
        rule
            .onNodeWithTag(slotChipTestTag(late, pickerPrefix))
            .assertIsNotEnabled()
            .assert(stateIs("Unavailable"))
        rule.onNodeWithTag(slotChipTestTag(early, pickerPrefix)).assertIsEnabled()
    }

    @Test
    fun defaultsKeepMakeABookingCopyAndTags() {
        setSlots(
            content = { n, onSelect ->
                MakeABookingTimeSlotsCard(
                    selectedDate = day,
                    availability = loaded,
                    now = n,
                    selectedSlotStart = null,
                    expertTimezone = null,
                    onSelectSlot = onSelect,
                    onRetry = {},
                    zone = zone,
                    timeFormat = SimpleDateFormat("h:mm a", Locale.US),
                    latestSelectableStart = middle,
                )
            },
        )

        rule
            .onNodeWithTag(PAID_WINDOW_NOTE_TEST_TAG)
            .assert(hasText("Paid sessions can be booked up to 6 days ahead."))
        rule.onNodeWithText("Available Time Slots").assertExists()
        rule.onNodeWithTag(slotChipTestTag(early)).assertExists()
        assertEquals("make_a_booking_slot_${early.toEpochMilli()}", slotChipTestTag(early))
    }
}
