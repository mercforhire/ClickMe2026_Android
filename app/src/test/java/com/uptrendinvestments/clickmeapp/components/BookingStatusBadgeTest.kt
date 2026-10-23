package com.uptrendinvestments.clickmeapp.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.components.buttonstyles.DestructiveOutlinedButton
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingStatusDisplay
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingStatusLabel
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.StatusTone
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.detailStatusDisplay
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.pastStatusDisplay
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookingStatusBadgeTest {
    @get:Rule
    val rule = createComposeRule()

    private fun setBadge(status: BookingStatusDisplay) {
        rule.setContent { ClickMeTheme { BookingStatusBadge(status = status) } }
    }

    @Test
    fun declinedShowsCancelled() {
        setBadge(pastStatusDisplay("declined")!!)
        rule.onNodeWithText("Cancelled").assertExists()
    }

    @Test
    fun completedShowsCompleted() {
        setBadge(pastStatusDisplay("completed")!!)
        rule.onNodeWithText("Completed").assertExists()
    }

    @Test
    fun expiredShowsMissed() {
        setBadge(pastStatusDisplay("expired")!!)
        rule.onNodeWithText("Missed").assertExists()
    }

    @Test
    fun unknownStatusShowsHumanizedRaw() {
        setBadge(BookingStatusDisplay(BookingStatusLabel.Other, StatusTone.Neutral, "no_show_both"))
        rule.onNodeWithText("No show both").assertExists()
    }

    @Test
    fun detailLabelsRender() {
        val expected =
            mapOf(
                "confirmed" to "Confirmed",
                "pending_approval" to "Pending approval",
                "pending_reschedule" to "New time proposed",
                "in_progress" to "In progress",
            )
        for ((raw, text) in expected) {
            setBadge(detailStatusDisplay(raw))
            rule.onNodeWithText(text).assertExists()
        }
    }

    @Test
    fun badgeCarriesTag() {
        setBadge(pastStatusDisplay("completed")!!)
        rule.onNodeWithTag(BOOKING_STATUS_BADGE_TAG).assertExists()
    }

    @Test
    fun destructiveButtonInvokesOnClickWhenEnabled() {
        var clicks = 0
        rule.setContent {
            ClickMeTheme {
                DestructiveOutlinedButton(onClick = { clicks++ }) { Text("Go") }
            }
        }
        rule.onNodeWithText("Go").assertIsEnabled().performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun destructiveButtonIsInertWhenDisabled() {
        var clicks = 0
        rule.setContent {
            ClickMeTheme {
                DestructiveOutlinedButton(onClick = { clicks++ }, enabled = false) { Text("Go") }
            }
        }
        rule.onNodeWithText("Go").assertIsNotEnabled().performClick()
        assertEquals(0, clicks)
    }
}
