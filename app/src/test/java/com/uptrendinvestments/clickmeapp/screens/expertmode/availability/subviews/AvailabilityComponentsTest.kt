package com.uptrendinvestments.clickmeapp.screens.expertmode.availability.subviews

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.DaySchedule
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.TimeSlot
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AvailabilityComponentsTest {
    @get:Rule
    val rule = createComposeRule()

    private val editedSlots = mutableListOf<Int>()
    private val removedSlots = mutableListOf<Int>()
    private val toggles = mutableListOf<Boolean>()
    private var addHoursCount = 0
    private var saveCount = 0

    private val format24: DateFormat = SimpleDateFormat("HH:mm", Locale.US)
    private val format12: DateFormat = SimpleDateFormat("h:mm a", Locale.US)

    private fun showCard(
        day: DaySchedule,
        format: DateFormat = format24,
        overlaps: Boolean = false,
    ) {
        rule.setContent {
            ClickMeTheme {
                AvailabilityDayCard(
                    day = day,
                    dayName = "Monday",
                    overlaps = overlaps,
                    timeFormat = format,
                    onToggle = { toggles += it },
                    onEditSlot = { editedSlots += it },
                    onRemoveSlot = { removedSlots += it },
                    onAddHours = { addHoursCount++ },
                )
            }
        }
    }

    private fun monday(
        enabled: Boolean,
        vararg slots: TimeSlot,
    ) = DaySchedule(DayOfWeek.MONDAY, enabled, slots.toList())

    private fun stateDescription(value: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    @Test
    fun enabledDayShowsSlotButtonsAndActions() {
        showCard(monday(true, TimeSlot(540, 1020)))

        rule.onNodeWithText("Monday").assertIsDisplayed()
        rule.onNodeWithText("09:00").assertIsDisplayed()
        rule.onNodeWithText("17:00").assertIsDisplayed()
        rule.onNodeWithContentDescription("Start time 09:00").performClick()
        rule.onNodeWithContentDescription("End time 17:00").performClick()
        assertEquals(listOf(0, 0), editedSlots)

        rule.onNodeWithContentDescription("Delete time slot").performClick()
        assertEquals(listOf(0), removedSlots)

        rule.onNodeWithText("Add hours").performClick()
        assertEquals(1, addHoursCount)
    }

    @Test
    fun enabledHeaderReportsAvailableAndTogglesOff() {
        showCard(monday(true, TimeSlot(540, 1020)))

        rule.onNodeWithTag("availability_day_MONDAY").assert(stateDescription("Available"))
        rule.onNodeWithTag("availability_day_MONDAY").performClick()
        assertEquals(listOf(false), toggles)
    }

    @Test
    fun twelveHourFormatAndMidnightEnd() {
        showCard(monday(true, TimeSlot(540, 1440)), format = format12)

        rule.onNodeWithText("9:00 AM").assertIsDisplayed()
        rule.onNodeWithText("12:00 AM").assertIsDisplayed()
        rule.onNodeWithContentDescription("End time 12:00 AM").assertIsDisplayed()
    }

    @Test
    fun disabledDayHidesSlotsAndTogglesOn() {
        showCard(monday(false, TimeSlot(540, 1020)))

        rule.onNodeWithText("Unavailable").assertIsDisplayed()
        rule.onNodeWithTag("availability_day_MONDAY").assert(stateDescription("Unavailable"))
        rule.onAllNodesWithText("09:00").assertCountEquals(0)
        rule.onAllNodesWithText("Add hours").assertCountEquals(0)

        rule.onNodeWithTag("availability_day_MONDAY").performClick()
        assertEquals(listOf(true), toggles)
    }

    @Test
    fun overlapBannerShowsTitleAndBody() {
        rule.setContent { ClickMeTheme { AvailabilityOverlapBanner(daysText = "Monday and Tuesday") } }

        rule.onNodeWithText("Overlapping time slots").assertIsDisplayed()
        rule
            .onNodeWithText("Time slots on Monday and Tuesday overlap. Please adjust them before saving.")
            .assertIsDisplayed()
    }

    private fun showSave(
        state: SaveState,
        enabled: Boolean = true,
    ) {
        rule.setContent {
            ClickMeTheme {
                AvailabilitySaveAction(state = state, enabled = enabled, onSave = { saveCount++ })
            }
        }
    }

    @Test
    fun saveIdleEnabledClicks() {
        showSave(SaveState.Idle)

        rule.onNodeWithText("Save").assertIsDisplayed()
        rule.onNodeWithContentDescription("Save availability").assertIsEnabled().performClick()
        assertEquals(1, saveCount)
    }

    @Test
    fun saveIdleDisabledExplainsItself() {
        showSave(SaveState.Idle, enabled = false)

        rule.onNodeWithTag("availability_save").assertIsNotEnabled()
        rule
            .onNodeWithTag("availability_save")
            .assert(stateDescription("Fix overlapping time slots to save availability"))
    }

    @Test
    fun savingShowsStateAndNoSaveButton() {
        showSave(SaveState.Saving)

        rule.onNode(stateDescription("Saving availability")).assertIsDisplayed()
        rule.onAllNodesWithText("Save").assertCountEquals(0)
        assertEquals(0, saveCount)
    }

    @Test
    fun savedShowsCheck() {
        showSave(SaveState.Saved)

        rule.onNodeWithContentDescription("Availability saved").assertIsDisplayed()
    }

    @Test
    fun skeletonRenders() {
        rule.setContent { ClickMeTheme { AvailabilitySkeleton() } }

        rule.onNodeWithTag("availability_skeleton").assertIsDisplayed()
    }
}
