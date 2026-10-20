package com.uptrendinvestments.clickmeapp.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

@Serializable
private data object Explore : NavKey

@Serializable
private data object Bookings : NavKey

@Serializable
private data object Chats : NavKey

@Serializable
private data object Detail : NavKey

@Serializable
private data object A : NavKey

@Serializable
private data object B : NavKey

class NavigatorTest {
    private fun newNavigator(): Navigator {
        val state = NavigationState(
            startRoute = Explore,
            topLevelRoute = mutableStateOf<NavKey>(Explore),
            backStacks = mapOf(
                Explore to NavBackStack<NavKey>(Explore),
                Bookings to NavBackStack<NavKey>(Bookings),
                Chats to NavBackStack<NavKey>(Chats),
            ),
        )
        return Navigator(state)
    }

    private fun Navigator.stack(tab: NavKey): List<NavKey> = state.backStacks.getValue(tab).toList()

    @Test
    fun stackIsRetainedAcrossTabSwitches() {
        val navigator = newNavigator()
        navigator.navigate(Detail)
        navigator.navigate(Bookings)
        navigator.navigate(Explore)
        assertEquals(listOf<NavKey>(Explore, Detail), navigator.stack(Explore))
        assertEquals(Explore, navigator.state.topLevelRoute)
    }

    @Test
    fun backAtNonHomeRootSelectsHomeTab() {
        val navigator = newNavigator()
        navigator.navigate(Bookings)
        navigator.goBack()
        assertEquals(Explore, navigator.state.topLevelRoute)
        assertEquals(NavTransition.TabSwitch, navigator.state.lastTransition)
    }

    @Test
    fun backInsideTabPopsOneEntry() {
        val navigator = newNavigator()
        navigator.navigate(Detail)
        navigator.goBack()
        assertEquals(listOf<NavKey>(Explore), navigator.stack(Explore))
        assertEquals(NavTransition.Pop, navigator.state.lastTransition)
    }

    @Test
    fun backAtHomeRootIsNoOp() {
        val navigator = newNavigator()
        navigator.goBack()
        assertEquals(Explore, navigator.state.topLevelRoute)
        assertEquals(listOf<NavKey>(Explore), navigator.stack(Explore))
    }

    @Test
    fun homeRootUsesSingleStack() {
        val navigator = newNavigator()
        assertEquals(listOf<NavKey>(Explore), navigator.state.stacksInUse)
    }

    @Test
    fun nonHomeTabUsesHomeAndSelectedStacks() {
        val navigator = newNavigator()
        navigator.navigate(Bookings)
        assertEquals(listOf<NavKey>(Explore, Bookings), navigator.state.stacksInUse)
    }

    @Test
    fun reTapPopsTabToRoot() {
        val navigator = newNavigator()
        navigator.navigate(A)
        navigator.navigate(B)
        navigator.onTabSelected(Explore)
        assertEquals(listOf<NavKey>(Explore), navigator.stack(Explore))
        assertEquals(Explore, navigator.state.topLevelRoute)
        assertEquals(NavTransition.Pop, navigator.state.lastTransition)
    }

    @Test
    fun switchingTabDoesNotTouchOtherStack() {
        val navigator = newNavigator()
        navigator.navigate(Detail)
        navigator.onTabSelected(Bookings)
        assertEquals(Bookings, navigator.state.topLevelRoute)
        assertEquals(listOf<NavKey>(Explore, Detail), navigator.stack(Explore))
        assertEquals(NavTransition.TabSwitch, navigator.state.lastTransition)
    }

    @Test
    fun pushSetsPushTransition() {
        val navigator = newNavigator()
        navigator.navigate(Detail)
        assertEquals(NavTransition.Push, navigator.state.lastTransition)
        assertEquals(listOf<NavKey>(Explore, Detail), navigator.stack(Explore))
    }

    @Test
    fun navigateInTabSelectsTabAndPushes() {
        val navigator = newNavigator()
        navigator.navigateInTab(Chats, Detail)
        assertEquals(Chats, navigator.state.topLevelRoute)
        assertEquals(listOf<NavKey>(Chats, Detail), navigator.stack(Chats))
        assertEquals(NavTransition.TabSwitch, navigator.state.lastTransition)
    }

    @Test
    fun navigateInTabRejectsUnknownTab() {
        val navigator = newNavigator()
        assertThrows(IllegalArgumentException::class.java) { navigator.navigateInTab(Detail, A) }
    }

    @Test
    fun reselectRejectsUnknownTab() {
        val navigator = newNavigator()
        assertThrows(IllegalArgumentException::class.java) { navigator.reselect(Detail) }
    }
}
