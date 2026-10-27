package com.uptrendinvestments.clickmeapp.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

@Serializable
private data object ExploreTab : NavKey

@Serializable
private data object ChatsTab : NavKey

@Serializable
private data object ExploreDetail : NavKey

@Serializable
private data object ChatDetail : NavKey

@Serializable
private data object ThreadA : NavKey

@Serializable
private data object ThreadB : NavKey

@Serializable
private data object NotATab : NavKey

class NavigatorOpenChatThreadTest {
    private fun newNavigator(): Navigator {
        val state =
            NavigationState(
                startRoute = ExploreTab,
                topLevelRoute = mutableStateOf<NavKey>(ExploreTab),
                backStacks =
                    mapOf(
                        ExploreTab to NavBackStack<NavKey>(ExploreTab),
                        ChatsTab to NavBackStack<NavKey>(ChatsTab),
                    ),
            )
        return Navigator(state)
    }

    private fun Navigator.stack(tab: NavKey): List<NavKey> = state.backStacks.getValue(tab).toList()

    @Test
    fun fromAnotherTabPushesOnChatsRoot() {
        val navigator = newNavigator()
        navigator.navigate(ExploreDetail)

        navigator.openChatThread(ChatsTab, ThreadA)

        assertEquals(listOf<NavKey>(ChatsTab, ThreadA), navigator.stack(ChatsTab))
        assertEquals(listOf<NavKey>(ExploreTab, ExploreDetail), navigator.stack(ExploreTab))
        assertEquals(ChatsTab, navigator.state.topLevelRoute)
        assertEquals(NavTransition.TabSwitch, navigator.state.lastTransition)
    }

    @Test
    fun differentThreadResetsChatsStackFirst() {
        val navigator = newNavigator()
        navigator.openChatThread(ChatsTab, ThreadA)
        navigator.navigate(ChatDetail)
        assertEquals(listOf<NavKey>(ChatsTab, ThreadA, ChatDetail), navigator.stack(ChatsTab))

        navigator.openChatThread(ChatsTab, ThreadB)

        assertEquals(listOf<NavKey>(ChatsTab, ThreadB), navigator.stack(ChatsTab))
    }

    @Test
    fun sameThreadOnTopOnlySelectsTab() {
        val navigator = newNavigator()
        navigator.openChatThread(ChatsTab, ThreadA)
        navigator.navigate(ExploreTab)

        navigator.openChatThread(ChatsTab, ThreadA)

        assertEquals(listOf<NavKey>(ChatsTab, ThreadA), navigator.stack(ChatsTab))
        assertEquals(ChatsTab, navigator.state.topLevelRoute)
        assertEquals(NavTransition.TabSwitch, navigator.state.lastTransition)
    }

    @Test
    fun alreadyOnChatsAnimatesAsPush() {
        val navigator = newNavigator()
        navigator.navigate(ChatsTab)

        navigator.openChatThread(ChatsTab, ThreadB)

        assertEquals(listOf<NavKey>(ChatsTab, ThreadB), navigator.stack(ChatsTab))
        assertEquals(NavTransition.Push, navigator.state.lastTransition)
    }

    @Test
    fun unknownTabThrows() {
        val navigator = newNavigator()

        assertThrows(IllegalArgumentException::class.java) { navigator.openChatThread(NotATab, ThreadA) }

        assertEquals(listOf<NavKey>(ExploreTab), navigator.stack(ExploreTab))
        assertEquals(listOf<NavKey>(ChatsTab), navigator.stack(ChatsTab))
        assertEquals(ExploreTab, navigator.state.topLevelRoute)
    }
}
