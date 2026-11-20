package com.uptrendinvestments.clickmeapp.screens.expertmode.availability

import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.constants.TimezoneEntry
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.subviews.AVAILABILITY_SAVE_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.subviews.AVAILABILITY_SKELETON_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.subviews.TIME_RANGE_DONE_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.subviews.TIME_RANGE_END_PREFIX
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.subviews.TIME_RANGE_START_PREFIX
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.subviews.availabilityAddHoursTag
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.subviews.availabilityDayTag
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.combinedInsets
import com.uptrendinvestments.clickmeapp.testing.dispatchInsets
import com.uptrendinvestments.clickmeapp.testing.imeInsets
import com.uptrendinvestments.clickmeapp.testing.navigationBarInsets
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AvailabilityScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val format24: DateFormat = SimpleDateFormat("HH:mm", Locale.US)

    private var retryCount = 0
    private var autoDetectCount = 0
    private var saveCount = 0
    private var backCount = 0
    private var dismissDialogCount = 0
    private var refreshFailedShownCount = 0
    private var refreshCount = 0
    private lateinit var view: View
    private val toggles = mutableListOf<Pair<DayOfWeek, Boolean>>()
    private val addedHours = mutableListOf<DayOfWeek>()
    private val updatedSlots = mutableListOf<Triple<DayOfWeek, Int, TimeSlot>>()
    private val selectedZones = mutableListOf<String>()

    private val actions =
        AvailabilityActions(
            retry = { retryCount++ },
            refresh = { refreshCount++ },
            setDayEnabled = { day, enabled -> toggles += day to enabled },
            addHours = { addedHours += it },
            removeSlot = { _, _ -> },
            updateSlot = { day, index, slot -> updatedSlots += Triple(day, index, slot) },
            selectTimezone = { selectedZones += it },
            autoDetectTimezone = { autoDetectCount++ },
            save = { saveCount++ },
            dismissDialog = { dismissDialogCount++ },
            onRefreshFailedShown = { refreshFailedShownCount++ },
        )

    private var lastTimeRangeInitial: TimeSlot? = null
    private var lastTimezoneEntries: List<TimezoneEntry> = emptyList()
    private var lastTimezoneSelected: String? = null

    private fun show(state: AvailabilityUiState) {
        rule.setContent {
            view = LocalView.current
            ClickMeTheme {
                AvailabilityContent(
                    state = state,
                    actions = actions,
                    onBack = { backCount++ },
                    timeFormat = format24,
                    timeRangeSheet = { initial, onDone, onDismiss ->
                        lastTimeRangeInitial = initial
                        Column {
                            Text("TimeRangeStub ${initial.start}-${initial.end}")
                            Button(onClick = { onDone(TimeSlot(600, 1020)) }) { Text("StubDone") }
                            Button(onClick = onDismiss) { Text("StubDismiss") }
                        }
                    },
                    timezoneSheet = { entries, selectedId, onSelect, onDismiss ->
                        lastTimezoneEntries = entries
                        lastTimezoneSelected = selectedId
                        Column {
                            Text("TimezoneStub $selectedId")
                            Button(onClick = { onSelect("Europe/Paris") }) { Text("StubPickParis") }
                            Button(onClick = onDismiss) { Text("StubTzDismiss") }
                        }
                    },
                )
            }
        }
    }

    private fun loaded(
        days: List<DaySchedule> = AvailabilityScheduleLogic.defaultSchedule(),
        timezoneId: String = "America/Toronto",
        saveState: SaveState = SaveState.Idle,
        dialog: FieldError? = null,
        refreshFailed: Boolean = false,
    ) = AvailabilityUiState(
        load = AvailabilityLoad.Loaded,
        days = days,
        timezoneId = timezoneId,
        serverTimezoneId = timezoneId,
        saveState = saveState,
        dialog = dialog,
        refreshFailed = refreshFailed,
    )

    private fun mondayNineToFive(): List<DaySchedule> =
        AvailabilityScheduleLogic.defaultSchedule().map {
            if (it.day == DayOfWeek.MONDAY) it.copy(slots = listOf(TimeSlot(540, 1020))) else it
        }

    private fun overlapping(): List<DaySchedule> =
        AvailabilityScheduleLogic.defaultSchedule().map {
            if (it.day == DayOfWeek.MONDAY || it.day == DayOfWeek.FRIDAY) {
                it.copy(slots = listOf(TimeSlot(540, 720), TimeSlot(600, 780)))
            } else {
                it
            }
        }

    private fun stateDescription(value: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    @Test
    fun loadingShowsSkeleton() {
        show(AvailabilityUiState())

        rule.onNodeWithTag(AVAILABILITY_SKELETON_TAG).assertExists()
        rule.onNodeWithText("Availability").assertExists()
    }

    @Test
    fun failedShowsMessageAndRetry() {
        show(AvailabilityUiState(load = AvailabilityLoad.Failed(FieldError.Text("Server down"))))

        rule.onNodeWithText("Couldn't load availability").assertExists()
        rule.onNodeWithText("Server down").assertExists()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, retryCount)
    }

    @Test
    fun loadedShowsTitleTimezoneAutoDetectAndDaysInOrder() {
        val state = loaded()
        show(state)

        rule.onNodeWithText("Availability").assertExists()
        rule.onNodeWithText(state.timezoneLabel).assertExists()
        rule.onNodeWithText("Auto-detect from device").performClick()
        assertEquals(1, autoDetectCount)

        val tops =
            AvailabilityScheduleLogic.WEEK.map {
                rule.onNodeWithTag(availabilityDayTag(it)).getUnclippedBoundsInRoot().top
            }
        assertEquals(tops.sorted(), tops)
        assertEquals(7, tops.toSet().size)
        listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday").forEach {
            rule.onAllNodesWithText(it).assertCountEquals(1)
        }
    }

    @Test
    fun backArrowCallsOnBack() {
        show(loaded())

        rule.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backCount)
    }

    @Test
    fun overlapShowsBannerAndDisablesSave() {
        show(loaded(days = overlapping()))

        rule
            .onNodeWithText("Time slots on Monday and Friday overlap. Please adjust them before saving.")
            .assertExists()
        rule.onNodeWithTag(AVAILABILITY_SAVE_TAG).assertIsNotEnabled()
    }

    @Test
    fun noOverlapShowsNoBannerAndSaveCallsAction() {
        show(loaded())

        rule.onAllNodesWithText("overlap", substring = true).assertCountEquals(0)
        rule.onNodeWithTag(AVAILABILITY_SAVE_TAG).assertIsEnabled().performClick()
        assertEquals(1, saveCount)
    }

    @Test
    fun savingShowsStateDescription() {
        show(loaded(saveState = SaveState.Saving))

        rule.onNode(stateDescription("Saving availability")).assertExists()
    }

    @Test
    fun savedShowsContentDescription() {
        show(loaded(saveState = SaveState.Saved))

        rule.onNodeWithContentDescription("Availability saved").assertExists()
    }

    @Test
    fun slotEditorOpensWithCurrentSlotAndDoneWritesIt() {
        show(loaded(days = mondayNineToFive()))

        rule.onNodeWithContentDescription("Start time 09:00").performScrollTo().performClick()
        rule.onNodeWithText("TimeRangeStub 540-1020").assertExists()
        assertEquals(TimeSlot(540, 1020), lastTimeRangeInitial)

        rule.onNodeWithText("StubDone").performClick()
        assertEquals(listOf(Triple(DayOfWeek.MONDAY, 0, TimeSlot(600, 1020))), updatedSlots)
        rule.onAllNodesWithText("TimeRangeStub", substring = true).assertCountEquals(0)
    }

    @Test
    fun slotEditorDismissDiscards() {
        show(loaded(days = mondayNineToFive()))

        rule.onNodeWithContentDescription("End time 17:00").performScrollTo().performClick()
        rule.onNodeWithText("TimeRangeStub 540-1020").assertExists()

        rule.onNodeWithText("StubDismiss").performClick()
        assertTrue(updatedSlots.isEmpty())
        rule.onAllNodesWithText("TimeRangeStub", substring = true).assertCountEquals(0)
    }

    @Test
    fun timezoneRowOpensSheetAndSelectionClosesIt() {
        val state = loaded(timezoneId = "America/Toronto")
        show(state)

        rule.onNodeWithTag(AVAILABILITY_TIMEZONE_ROW_TAG).performClick()
        rule.onNodeWithText("TimezoneStub America/Toronto").assertExists()
        assertEquals(state.timezoneEntries, lastTimezoneEntries)
        assertEquals("America/Toronto", lastTimezoneSelected)

        rule.onNodeWithText("StubPickParis").performClick()
        assertEquals(listOf("Europe/Paris"), selectedZones)
        rule.onAllNodesWithText("TimezoneStub", substring = true).assertCountEquals(0)
    }

    @Test
    fun timezoneSheetDismissKeepsZone() {
        show(loaded())

        rule.onNodeWithTag(AVAILABILITY_TIMEZONE_ROW_TAG).performClick()
        rule.onNodeWithText("StubTzDismiss").performClick()

        assertTrue(selectedZones.isEmpty())
        rule.onAllNodesWithText("TimezoneStub", substring = true).assertCountEquals(0)
    }

    @Test
    fun disabledDayHeaderTogglesOn() {
        val days =
            AvailabilityScheduleLogic.defaultSchedule().map {
                if (it.day == DayOfWeek.TUESDAY) it.copy(enabled = false, slots = emptyList()) else it
            }
        show(loaded(days = days))

        rule.onNodeWithTag(availabilityDayTag(DayOfWeek.TUESDAY)).performScrollTo().performClick()
        assertEquals(listOf(DayOfWeek.TUESDAY to true), toggles)
    }

    @Test
    fun addHoursCallsAction() {
        show(loaded())

        rule.onNodeWithTag(availabilityAddHoursTag(DayOfWeek.MONDAY)).performScrollTo().performClick()
        assertEquals(listOf(DayOfWeek.MONDAY), addedHours)
    }

    @Test
    fun addHoursIsDisabledWhenTheLatestEndLeavesNoRoom() {
        val days =
            AvailabilityScheduleLogic.defaultSchedule().map {
                if (it.day == DayOfWeek.MONDAY) it.copy(slots = listOf(TimeSlot(540, 1350))) else it
            }
        show(loaded(days = days))

        rule.onNodeWithTag(availabilityAddHoursTag(DayOfWeek.MONDAY)).performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag(availabilityAddHoursTag(DayOfWeek.TUESDAY)).performScrollTo().assertIsEnabled()
    }

    @Test
    fun saveErrorDialogShowsTitleAndMessage() {
        show(loaded(dialog = FieldError.Text("Boom")))

        rule.onNodeWithText("Couldn't save availability").assertExists()
        rule.onNodeWithText("Boom").assertExists()
        rule.onNodeWithText("OK").performClick()
        assertEquals(1, dismissDialogCount)
    }

    @Test
    fun refreshFailureShowsSnackbarOnce() {
        show(loaded(refreshFailed = true))

        rule.onNodeWithText("Couldn't refresh. Check your connection and try again.").assertExists()
        assertEquals(1, refreshFailedShownCount)
    }

    // Phase 15: D-05 Retry, UI-SPEC R9 snackbar insets, D-12 sheet restoration, D-02 width cap.

    @Test
    fun refreshFailedShowsRetry() {
        show(loaded(refreshFailed = true))

        rule.onNodeWithText("Couldn't refresh. Check your connection and try again.").assertExists()
        // Long duration: still up after the Short 4 s would have dismissed it.
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(5_000)
        rule.onNodeWithText("Retry").assertExists()
        rule.onNodeWithText("Retry").performClick()
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        assertEquals(1, refreshCount)
        assertEquals(0, retryCount)
        assertEquals(1, refreshFailedShownCount)
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun snackbarAboveNavBar() {
        show(loaded(refreshFailed = true))
        rule.dispatchInsets(view, navigationBarInsets(48))

        val withNavBar = rule.onNodeWithText("Retry").getUnclippedBoundsInRoot()
        assertTrue("bottom ${withNavBar.bottom.value}", withNavBar.bottom.value <= 752f)

        rule.dispatchInsets(view, combinedInsets(navigationBarInsets(48), imeInsets(300)))
        val withIme = rule.onNodeWithText("Retry").getUnclippedBoundsInRoot()
        assertTrue("bottom ${withIme.bottom.value}", withIme.bottom.value <= 500f)
    }

    @Test
    fun timeRangeSheetSurvivesRestore() {
        val tester = StateRestorationTester(rule)
        tester.setContent {
            ClickMeTheme {
                AvailabilityContent(
                    state = loaded(days = mondayNineToFive()),
                    actions = actions,
                    onBack = {},
                    timeFormat = format24,
                )
            }
        }

        rule.onNodeWithContentDescription("Start time 09:00").performScrollTo().performClick()
        rule.onNodeWithTag(TIME_RANGE_DONE_TAG).assertExists()
        rule.onNodeWithTag("${TIME_RANGE_START_PREFIX}600").performClick()
        rule.onNodeWithTag("${TIME_RANGE_END_PREFIX}1080").performClick()
        rule.onNodeWithTag("${TIME_RANGE_START_PREFIX}600").assertIsSelected()
        rule.onNodeWithTag("${TIME_RANGE_END_PREFIX}1080").assertIsSelected()

        tester.emulateSavedInstanceStateRestore()
        rule.waitForIdle()

        rule.onNodeWithTag(TIME_RANGE_DONE_TAG).assertExists()
        rule.onNodeWithTag("${TIME_RANGE_START_PREFIX}600").assertIsSelected()
        rule.onNodeWithTag("${TIME_RANGE_END_PREFIX}1080").assertIsSelected()
    }

    @Test
    @Config(qualifiers = "w900dp-h600dp")
    fun capped() {
        show(loaded())

        val bounds = rule.onNodeWithTag(availabilityDayTag(DayOfWeek.MONDAY)).getUnclippedBoundsInRoot()
        assertTrue("width ${bounds.right - bounds.left}", (bounds.right - bounds.left).value <= 600f)
    }
}
