package com.uptrendinvestments.clickmeapp.screens

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.HomeClientShell
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.HomeExpertShell
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeShellsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var switchCount = 0

    private fun setClient(showDebug: Boolean) {
        composeRule.setContent {
            ClickMeTheme {
                HomeClientShell(
                    avatarUrl = null,
                    showDebugAffordances = showDebug,
                    onDebugSwitchScaffold = { switchCount++ },
                    onAvatarClick = {},
                )
            }
        }
    }

    private fun setExpert(showDebug: Boolean) {
        composeRule.setContent {
            ClickMeTheme {
                HomeExpertShell(
                    avatarUrl = null,
                    showDebugAffordances = showDebug,
                    onDebugSwitchScaffold = { switchCount++ },
                    onAvatarClick = {},
                )
            }
        }
    }

    // Tab names also appear as TopAppBar titles and empty-state headings, so match the bar item.
    private fun tab(label: String) = hasText(label) and isSelectable()

    private fun tapTab(label: String) = composeRule.onNode(tab(label)).performClick()

    private fun openGalleryFromProfile() {
        tapTab("Profile")
        composeRule.onNodeWithText("Design system gallery").performClick()
        composeRule.onNodeWithContentDescription("Back").assertExists()
    }

    @Test
    fun clientShellShowsFiveTabsWithExploreSelectedAndAvatarButton() {
        setClient(showDebug = true)
        listOf("Explore", "Search", "Bookings", "Chats", "Profile").forEach {
            composeRule.onNode(tab(it)).assertExists()
        }
        composeRule.onNode(tab("Explore")).assertIsSelected()
        composeRule.onNodeWithContentDescription("Your profile").assertExists()
        composeRule.onNodeWithText("Nothing here yet.").assertExists()
    }

    @Test
    fun profileTabOffersDebugSwitchAndGalleryAndSwitchInvokesCallback() {
        setClient(showDebug = true)
        tapTab("Profile")
        composeRule.onNodeWithText("Design system gallery").assertExists()
        composeRule.onNodeWithText("Switch to expert scaffold").performClick()
        assertEquals(1, switchCount)
    }

    @Test
    fun galleryShowsBackAndAllSections() {
        setClient(showDebug = true)
        openGalleryFromProfile()
        listOf("Typography", "AsyncSaveButton", "Avatar").forEach {
            composeRule.onNodeWithText(it).performScrollTo().assertExists()
        }
    }

    @Test
    fun galleryStaysOnProfileStackAcrossTabSwitch() {
        setClient(showDebug = true)
        openGalleryFromProfile()
        tapTab("Explore")
        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()
        tapTab("Profile")
        composeRule.onNodeWithContentDescription("Back").assertExists()
    }

    @Test
    fun retappingSelectedProfileTabPopsToRoot() {
        setClient(showDebug = true)
        openGalleryFromProfile()
        tapTab("Profile")
        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()
        composeRule.onNodeWithText("Switch to expert scaffold").assertExists()
    }

    @Test
    fun backNodeReturnsToProfileRoot() {
        setClient(showDebug = true)
        openGalleryFromProfile()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()
        composeRule.onNodeWithText("Switch to expert scaffold").assertExists()
    }

    @Test
    fun debugAffordancesAbsentWhenGateIsOff() {
        setClient(showDebug = false)
        tapTab("Profile")
        composeRule.onNodeWithText("Design system gallery").assertDoesNotExist()
        composeRule.onNodeWithText("Switch to expert scaffold").assertDoesNotExist()
    }

    @Test
    fun expertShellShowsFiveTabsWithDashboardSelectedAndSwitchToClient() {
        setExpert(showDebug = true)
        listOf("Dashboard", "Bookings", "Topics", "Chats", "Profile").forEach {
            composeRule.onNode(tab(it)).assertExists()
        }
        composeRule.onNode(tab("Dashboard")).assertIsSelected()
        composeRule.onNodeWithContentDescription("Your profile").assertExists()
        tapTab("Profile")
        composeRule.onNodeWithText("Switch to client scaffold").assertExists()
        composeRule.onNodeWithText("Design system gallery").assertExists()
    }

    @Test
    fun expertDebugAffordancesAbsentWhenGateIsOff() {
        setExpert(showDebug = false)
        tapTab("Profile")
        composeRule.onNodeWithText("Switch to client scaffold").assertDoesNotExist()
        composeRule.onNodeWithText("Design system gallery").assertDoesNotExist()
    }
}
