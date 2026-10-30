package com.uptrendinvestments.clickmeapp.screens.settings.modeswitching

import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ModeSwitchScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val events = mutableListOf<String>()

    private fun setScreen(currentMode: AppMode) {
        rule.setContent {
            ClickMeTheme {
                ModeSwitchScreen(
                    currentMode = currentMode,
                    onSwitchMode = { events += "switch:${it.wire}" },
                    onBack = { events += "back" },
                )
            }
        }
    }

    private fun clientCard() = rule.onNode(hasText("Client Mode"))

    private fun expertCard() = rule.onNode(hasText("Expert Mode"))

    @Test
    @Config(qualifiers = "w900dp-h1200dp")
    fun capped() {
        setScreen(AppMode.Client)

        val card = clientCard().getUnclippedBoundsInRoot()
        assertTrue("card width ${card.right - card.left}", card.right - card.left <= 600.dp)
        assertTrue("card left ${card.left}", card.left >= 150.dp)
    }

    @Test
    fun rendersTitleAndBothCardsCopy() {
        setScreen(AppMode.Client)
        rule.onNodeWithText("Mode Switcher").assertExists()
        rule.onNodeWithText("Client Mode").assertExists()
        rule.onNodeWithText("Find and book experts").assertExists()
        rule
            .onNodeWithText("Browse a wide range of experts and book sessions that fit your needs.")
            .assertExists()
        rule.onNodeWithText("Expert Mode").assertExists()
        rule.onNodeWithText("Manage bookings and profile").assertExists()
        rule
            .onNodeWithText("Manage your bookings, update your profile, and connect with clients.")
            .assertExists()
    }

    @Test
    fun clientActive_clientSelectedWithBadge_expertNot() {
        setScreen(AppMode.Client)
        clientCard().assertIsSelected()
        expertCard().assertIsNotSelected()
        rule.onNode(hasText("Client Mode") and hasText("Active")).assertExists()
        rule.onNode(hasText("Expert Mode") and hasText("Active")).assertDoesNotExist()
    }

    @Test
    fun expertActive_expertSelectedWithBadge() {
        setScreen(AppMode.Expert)
        expertCard().assertIsSelected()
        clientCard().assertIsNotSelected()
        rule.onNode(hasText("Expert Mode") and hasText("Active")).assertExists()
        rule.onNode(hasText("Client Mode") and hasText("Active")).assertDoesNotExist()
    }

    @Test
    fun tappingInactiveCard_switchesThenGoesBack() {
        setScreen(AppMode.Client)
        expertCard().performClick()
        assertEquals(listOf("switch:expert", "back"), events)
    }

    @Test
    fun tappingActiveCard_onlyGoesBack() {
        setScreen(AppMode.Client)
        clientCard().performClick()
        assertEquals(listOf("back"), events)
    }

    @Test
    fun topBarBack_callsOnBack() {
        setScreen(AppMode.Client)
        rule.onNodeWithContentDescription("Back").performClick()
        assertEquals(listOf("back"), events)
    }
}
