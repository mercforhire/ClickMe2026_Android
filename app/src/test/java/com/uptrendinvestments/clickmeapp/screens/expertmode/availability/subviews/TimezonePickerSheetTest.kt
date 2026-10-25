package com.uptrendinvestments.clickmeapp.screens.expertmode.availability.subviews

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.AvailabilityTimezoneLogic
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TimezonePickerSheetTest {
    @get:Rule
    val rule = createComposeRule()

    private val selected = mutableListOf<String>()

    @Test
    fun unbundledServerZoneShowsFirstAndSelectedAndBundledRowSelects() {
        val entries =
            AvailabilityTimezoneLogic.pickerEntries(
                selectedId = "America/Toronto",
                serverId = "America/Toronto",
                serverDisplayName = "(GMT-05:00) America/Toronto",
            )
        rule.setContent {
            ClickMeTheme {
                TimezonePickerContent(
                    entries = entries,
                    selectedId = "America/Toronto",
                    onSelect = { selected += it },
                )
            }
        }

        rule.onNodeWithText("Select Timezone").assertExists()
        rule.onNodeWithText("(GMT-05:00) America/Toronto").assertIsSelected()

        rule.onNode(hasScrollAction()).performScrollToNode(hasText("(GMT-05:00) Eastern Time"))
        rule.onNodeWithText("(GMT-05:00) Eastern Time").performClick()

        assertEquals(listOf("America/New_York"), selected)
    }
}
