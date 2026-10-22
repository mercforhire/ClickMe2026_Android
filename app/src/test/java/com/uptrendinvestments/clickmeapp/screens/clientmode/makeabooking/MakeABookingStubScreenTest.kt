package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MakeABookingStubScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var backCount = 0

    private fun setScreen(key: BookingKey) {
        composeRule.setContent {
            ClickMeTheme {
                MakeABookingStubScreen(key = key, onBack = { backCount++ })
            }
        }
    }

    @Test
    fun showsExpertAndComingSoonMessage() {
        setScreen(BookingKey("e1", "Ada Lovelace", "Engineer", null))
        composeRule.onNodeWithText("Book a Session").assertExists()
        composeRule.onNodeWithText("Engineer").assertExists()
        composeRule.onNodeWithText("Booking is coming soon").assertExists()
        composeRule
            .onNodeWithText("You'll be able to book Ada Lovelace here in an upcoming update.")
            .assertExists()
        // The name appears once as the heading line; the body sentence is a separate node.
        composeRule.onNodeWithText("Ada Lovelace").assertExists()
    }

    @Test
    fun blankTitleOmitsTheTitleLine() {
        setScreen(BookingKey("e1", "Ada Lovelace", "", null))
        composeRule.onNodeWithText("Ada Lovelace").assertExists()
        composeRule.onNodeWithText("Engineer").assertDoesNotExist()
    }

    @Test
    fun backIconInvokesOnBackOnce() {
        setScreen(BookingKey("e1", "Ada Lovelace", "Engineer", null))
        composeRule.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backCount)
    }

    @Test
    fun backIsTheOnlyClickableControl() {
        setScreen(BookingKey("e1", "Ada Lovelace", "Engineer", null))
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(1)
    }
}
