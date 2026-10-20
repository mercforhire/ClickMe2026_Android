package com.uptrendinvestments.clickmeapp.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ClientBookingsRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ExploreRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.clientStartTab
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.clientTabs
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.DashboardRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.expertStartTab
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.expertTabs
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@Serializable
data object PushedScreen : NavKey

@Serializable
data object HiddenBarScreen : NavKey

@RunWith(RobolectricTestRunner::class)
class HomeTabScaffoldTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var navigator: Navigator? = null

    private fun entry(key: NavKey): NavEntry<NavKey> = NavEntry(
        key = key,
        metadata = if (key == HiddenBarScreen) BottomBar.hidden() else emptyMap(),
    ) {
        Text("screen:$key")
    }

    private fun setClientContent() {
        composeRule.setContent {
            ClickMeTheme {
                val state = rememberNavigationState(clientStartTab, clientTabs.map { it.key }.toSet())
                val nav = remember { Navigator(state) }
                navigator = nav
                HomeTabScaffold(
                    tabs = clientTabs,
                    navigationState = state,
                    navigator = nav,
                    entryProvider = ::entry,
                )
            }
        }
    }

    private fun tab(label: String) = hasText(label) and isSelectable()

    @Test
    fun clientScaffoldShowsFiveTabsWithExploreSelected() {
        setClientContent()
        listOf("Explore", "Search", "Bookings", "Chats", "Profile").forEach {
            composeRule.onNode(tab(it)).assertExists()
        }
        composeRule.onNode(tab("Explore")).assertIsSelected()
    }

    @Test
    fun clickingBookingsSelectsItAndShowsRoot() {
        setClientContent()
        composeRule.onNode(tab("Bookings")).performClick()
        composeRule.waitForIdle()
        composeRule.onNode(tab("Bookings")).assertIsSelected()
        assertEquals(ClientBookingsRoot, navigator!!.state.topLevelRoute)
        composeRule.onNodeWithText("screen:$ClientBookingsRoot").assertExists()
    }

    @Test
    fun expertScaffoldShowsFiveTabsWithDashboardSelected() {
        composeRule.setContent {
            ClickMeTheme {
                val state = rememberNavigationState(expertStartTab, expertTabs.map { it.key }.toSet())
                val nav = remember { Navigator(state) }
                HomeTabScaffold(
                    tabs = expertTabs,
                    navigationState = state,
                    navigator = nav,
                    entryProvider = ::entry,
                )
            }
        }
        listOf("Dashboard", "Bookings", "Topics", "Chats", "Profile").forEach {
            composeRule.onNode(tab(it)).assertExists()
        }
        composeRule.onNode(tab("Dashboard")).assertIsSelected()
        assertEquals(DashboardRoot, expertStartTab)
    }

    @Test
    fun hiddenBarMetadataRemovesNavigationBar() {
        setClientContent()
        composeRule.runOnIdle { navigator!!.navigate(HiddenBarScreen) }
        composeRule.waitForIdle()
        composeRule.onNode(tab("Explore")).assertDoesNotExist()
        composeRule.runOnIdle { navigator!!.goBack() }
        composeRule.waitForIdle()
        composeRule.onNode(tab("Explore")).assertExists()
    }

    @Test
    fun pushedScreenSurvivesTabSwitch() {
        setClientContent()
        composeRule.runOnIdle { navigator!!.navigate(PushedScreen) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("screen:$PushedScreen").assertExists()
        composeRule.onNode(tab("Bookings")).performClick()
        composeRule.waitForIdle()
        composeRule.onNode(tab("Explore")).performClick()
        composeRule.waitForIdle()
        assertEquals(ExploreRoot, navigator!!.state.topLevelRoute)
        composeRule.onNodeWithText("screen:$PushedScreen").assertExists()
    }
}
