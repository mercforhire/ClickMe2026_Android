package com.uptrendinvestments.clickmeapp.screens.expertmode.availability.subviews

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TimeRangePickerSheetTest {
    @get:Rule
    val rule = createComposeRule()

    private val format24: DateFormat = SimpleDateFormat("HH:mm", Locale.US)

    private val starts = mutableListOf<Int>()
    private val ends = mutableListOf<Int>()
    private var cancelCount = 0
    private var doneCount = 0

    private fun show(
        start: Int,
        end: Int,
    ) {
        rule.setContent {
            ClickMeTheme {
                TimeRangePickerContent(
                    draftStart = start,
                    draftEnd = end,
                    storedStart = start,
                    storedEnd = end,
                    timeFormat = format24,
                    onStartSelected = { starts += it },
                    onEndSelected = { ends += it },
                    onCancel = { cancelCount++ },
                    onDone = { doneCount++ },
                )
            }
        }
    }

    private fun scrollStartTo(minutes: Int) {
        rule
            .onNodeWithTag("time_range_start_list")
            .performScrollToNode(hasTestTag("time_range_start_$minutes"))
    }

    private fun scrollEndTo(minutes: Int) {
        rule
            .onNodeWithTag("time_range_end_list")
            .performScrollToNode(hasTestTag("time_range_end_$minutes"))
    }

    @Test
    fun rendersTitleLabelsAndCurrentSelection() {
        show(540, 1020)

        rule.onNodeWithText("Select Time Range").assertExists()
        rule.onNodeWithText("Start Time").assertExists()
        rule.onNodeWithText("End Time").assertExists()
        rule.onNodeWithTag("time_range_start_540").assertIsSelected()
        rule.onNodeWithTag("time_range_end_1020").assertIsSelected()
    }

    @Test
    fun startColumnStopsStrictlyBeforeEnd() {
        show(540, 1020)

        scrollStartTo(990)
        rule.onNodeWithTag("time_range_start_990").assertExists()
        rule.onAllNodesWithTag("time_range_start_1020").assertCountEquals(0)
    }

    @Test
    fun endColumnStartsStrictlyAfterStartAndReachesTheCap() {
        show(540, 1020)

        rule.onNodeWithTag("time_range_end_list").performScrollToIndex(0)
        rule.onNodeWithTag("time_range_end_570").assertExists()
        rule.onAllNodesWithTag("time_range_end_540").assertCountEquals(0)
        scrollEndTo(1410)
        rule.onNodeWithTag("time_range_end_1410").assertExists()
    }

    @Test
    fun tappingRowsReportsTheirValues() {
        show(540, 1020)

        scrollStartTo(600)
        rule.onNodeWithTag("time_range_start_600").performClick()
        scrollEndTo(1080)
        rule.onNodeWithTag("time_range_end_1080").performClick()

        assertEquals(listOf(600), starts)
        assertEquals(listOf(1080), ends)
    }

    @Test
    fun cancelAndDoneCallTheirHandlers() {
        show(540, 1020)

        rule.onNodeWithTag("time_range_cancel").performClick()
        rule.onNodeWithTag("time_range_done").performClick()

        assertEquals(1, cancelCount)
        assertEquals(1, doneCount)
    }

    @Test
    fun midnightEndIsKeptAndSelected() {
        show(1320, 1440)

        scrollEndTo(1440)
        rule.onNodeWithTag("time_range_end_1440").assertIsSelected().assertTextEquals("00:00")
    }

    @Test
    fun offGridStartStaysSelectable() {
        show(545, 1020)

        scrollStartTo(545)
        rule.onNodeWithTag("time_range_start_545").assertIsSelected()
    }

    @Test
    fun draftSelectionsRefilterTheOtherColumn() {
        rule.setContent {
            ClickMeTheme {
                var start by remember { mutableIntStateOf(540) }
                var end by remember { mutableIntStateOf(1020) }
                TimeRangePickerContent(
                    draftStart = start,
                    draftEnd = end,
                    storedStart = 540,
                    storedEnd = 1020,
                    timeFormat = format24,
                    onStartSelected = { start = it },
                    onEndSelected = { end = it },
                    onCancel = {},
                    onDone = {},
                )
            }
        }

        scrollStartTo(990)
        rule.onAllNodesWithTag("time_range_start_1020").assertCountEquals(0)

        scrollEndTo(600)
        rule.onNodeWithTag("time_range_end_600").performClick()
        rule.onNodeWithTag("time_range_end_600").assertIsSelected()

        scrollStartTo(570)
        rule.onNodeWithTag("time_range_start_570").assertExists()
        rule.onAllNodesWithTag("time_range_start_600").assertCountEquals(0)
        rule.onNodeWithTag("time_range_start_540").assertIsSelected()
    }

    /** Stateful sheet body seeded with a stored off-grid start and a stored 24:00 end (09:15 to 24:00). */
    private fun showStoredOffGridAndMidnight() {
        rule.setContent {
            ClickMeTheme {
                var start by remember { mutableIntStateOf(555) }
                var end by remember { mutableIntStateOf(1440) }
                TimeRangePickerContent(
                    draftStart = start,
                    draftEnd = end,
                    storedStart = 555,
                    storedEnd = 1440,
                    timeFormat = format24,
                    onStartSelected = { start = it },
                    onEndSelected = { end = it },
                    onCancel = {},
                    onDone = {},
                )
            }
        }
    }

    @Test
    fun storedOffGridStartStaysARowAfterAnotherStartIsTapped() {
        showStoredOffGridAndMidnight()

        scrollStartTo(600)
        rule.onNodeWithTag("time_range_start_600").performClick()
        rule.onNodeWithTag("time_range_start_600").assertIsSelected()

        scrollStartTo(555)
        rule.onNodeWithTag("time_range_start_555").assertExists().assertIsNotSelected()

        rule.onNodeWithTag("time_range_start_555").performClick()
        rule.onNodeWithTag("time_range_start_555").assertIsSelected()
    }

    @Test
    fun storedMidnightEndStaysARowAfterAnotherEndIsTapped() {
        showStoredOffGridAndMidnight()

        scrollEndTo(1410)
        rule.onNodeWithTag("time_range_end_1410").performClick()
        rule.onNodeWithTag("time_range_end_1410").assertIsSelected()

        scrollEndTo(1440)
        rule
            .onNodeWithTag("time_range_end_1440")
            .assertExists()
            .assertIsNotSelected()
            .assertTextEquals("00:00")
    }
}
