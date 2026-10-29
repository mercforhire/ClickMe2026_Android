package com.uptrendinvestments.clickmeapp.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

@Serializable
private data object RtExplore : NavKey

@Serializable
private data object RtBookings : NavKey

@Serializable
private data object RtDashboard : NavKey

@Serializable
private data object RtOther : NavKey

@Serializable
private data object RtNotATab : NavKey

@Serializable
private data object RtList : NavKey

@Serializable
private data class RtDetail(
    val id: String,
) : NavKey

class NavigatorOpenRoutedTest {
    private val s1 = RtDetail("S1")
    private val u1 = RtDetail("U1")
    private val c1 = RtDetail("C1")
    private val n = RtDetail("N")
    private val x = RtDetail("X")
    private val clientTabs = listOf(RtExplore, RtBookings)

    private fun navigator(
        start: NavKey,
        tabs: List<NavKey>,
        selected: NavKey = start,
        stacks: Map<NavKey, List<NavKey>> = emptyMap(),
    ): Navigator {
        val state =
            NavigationState(
                startRoute = start,
                topLevelRoute = mutableStateOf(selected),
                backStacks =
                    tabs.associateWith { tab ->
                        NavBackStack<NavKey>(tab).also { stack -> stack.addAll(stacks[tab].orEmpty()) }
                    },
            )
        return Navigator(state)
    }

    private fun Navigator.stack(tab: NavKey): List<NavKey> = state.backStacks.getValue(tab).toList()

    @Test
    fun otherTabIsResetThenSelected() {
        val nav = navigator(RtExplore, clientTabs, stacks = mapOf(RtBookings to listOf(x)))

        nav.openRouted(RtBookings, listOf(s1))

        assertEquals(listOf<NavKey>(RtBookings, s1), nav.stack(RtBookings))
        assertEquals(RtBookings, nav.state.topLevelRoute)
        assertEquals(NavTransition.TabSwitch, nav.state.lastTransition)
    }

    @Test
    fun selectedTabPushesOnTop() {
        val nav = navigator(RtExplore, clientTabs, stacks = mapOf(RtExplore to listOf(n)))
        val k = RtDetail("K")

        nav.openRouted(RtExplore, listOf(k))

        assertEquals(listOf<NavKey>(RtExplore, n, k), nav.stack(RtExplore))
        assertEquals(RtExplore, nav.state.topLevelRoute)
        assertEquals(NavTransition.Push, nav.state.lastTransition)
    }

    @Test
    fun destinationInStartStackIsReused() {
        val nav =
            navigator(
                RtExplore,
                clientTabs,
                selected = RtBookings,
                stacks = mapOf(RtExplore to listOf(u1, c1)),
            )

        nav.openRouted(RtBookings, listOf(u1))

        assertEquals(listOf<NavKey>(RtExplore, u1), nav.stack(RtExplore))
        assertEquals(listOf<NavKey>(RtBookings), nav.stack(RtBookings))
        assertEquals(RtExplore, nav.state.topLevelRoute)
        assertEquals(NavTransition.TabSwitch, nav.state.lastTransition)
    }

    @Test
    fun destinationDeeperInTargetIsTruncated() {
        val nav =
            navigator(
                RtExplore,
                clientTabs,
                selected = RtBookings,
                stacks = mapOf(RtBookings to listOf(u1, c1)),
            )

        nav.openRouted(RtBookings, listOf(u1))

        assertEquals(listOf<NavKey>(RtBookings, u1), nav.stack(RtBookings))
        assertEquals(RtBookings, nav.state.topLevelRoute)
        assertEquals(NavTransition.Pop, nav.state.lastTransition)
    }

    @Test
    fun openExistingSelectsTheTabHoldingAMatchAndCutsBackToIt() {
        val nav =
            navigator(
                RtDashboard,
                listOf(RtDashboard, RtBookings),
                selected = RtDashboard,
                stacks = mapOf(RtBookings to listOf(u1, c1)),
            )

        val found = nav.openExisting { it == u1 }

        assertEquals(true, found)
        assertEquals(listOf<NavKey>(RtBookings, u1), nav.stack(RtBookings))
        assertEquals(RtBookings, nav.state.topLevelRoute)
        assertEquals(NavTransition.TabSwitch, nav.state.lastTransition)
    }

    @Test
    fun openExistingInTheSelectedTabIsAPop() {
        val nav = navigator(RtDashboard, listOf(RtDashboard, RtBookings), stacks = mapOf(RtDashboard to listOf(u1, c1)))

        assertEquals(true, nav.openExisting { it == u1 })

        assertEquals(listOf<NavKey>(RtDashboard, u1), nav.stack(RtDashboard))
        assertEquals(NavTransition.Pop, nav.state.lastTransition)
    }

    @Test
    fun openExistingWithoutAMatchLeavesTheStateUntouched() {
        val nav = navigator(RtDashboard, listOf(RtDashboard, RtBookings), stacks = mapOf(RtBookings to listOf(c1)))

        assertEquals(false, nav.openExisting { it == u1 })

        assertEquals(listOf<NavKey>(RtBookings, c1), nav.stack(RtBookings))
        assertEquals(RtDashboard, nav.state.topLevelRoute)
    }

    @Test
    fun requestChainKeepsBackOrder() {
        val decision = RtDetail("RD1")
        val nav = navigator(RtDashboard, listOf(RtDashboard, RtBookings))

        nav.openRouted(RtDashboard, listOf(RtList, decision))

        assertEquals(listOf<NavKey>(RtDashboard, RtList, decision), nav.stack(RtDashboard))
        nav.goBack()
        assertEquals(listOf<NavKey>(RtDashboard, RtList), nav.stack(RtDashboard))
        nav.goBack()
        assertEquals(listOf<NavKey>(RtDashboard), nav.stack(RtDashboard))
    }

    @Test
    fun repeatedChainKeyIsCutFirst() {
        val old = RtDetail("RD0")
        val decision = RtDetail("RD1")
        val nav =
            navigator(
                RtDashboard,
                listOf(RtDashboard, RtBookings),
                stacks = mapOf(RtDashboard to listOf(n, RtList, old)),
            )

        nav.openRouted(RtDashboard, listOf(RtList, decision))

        assertEquals(listOf<NavKey>(RtDashboard, n, RtList, decision), nav.stack(RtDashboard))
        assertEquals(NavTransition.Push, nav.state.lastTransition)
    }

    @Test
    fun intermediateKeyInStartStackIsDropped() {
        val a = RtDetail("A")
        val b = RtDetail("B")
        val nav = navigator(RtExplore, clientTabs, stacks = mapOf(RtExplore to listOf(a)))

        nav.openRouted(RtBookings, listOf(a, b))

        assertEquals(listOf<NavKey>(RtBookings, b), nav.stack(RtBookings))
        assertEquals(listOf<NavKey>(RtExplore, a), nav.stack(RtExplore))
        assertEquals(RtBookings, nav.state.topLevelRoute)
    }

    @Test
    fun noKeyIsEverInTwoStacksInUse() {
        val nav = navigator(RtExplore, clientTabs, stacks = mapOf(RtExplore to listOf(u1)))

        nav.openRouted(RtBookings, listOf(u1))

        val inUse = nav.state.stacksInUse.flatMap { nav.stack(it) }
        assertEquals(inUse.size, inUse.toSet().size)
    }

    @Test
    fun unknownTabLeavesStateUntouched() {
        val nav = navigator(RtExplore, clientTabs, stacks = mapOf(RtExplore to listOf(n)))

        assertThrows(IllegalArgumentException::class.java) { nav.openRouted(RtNotATab, listOf(s1)) }

        assertEquals(listOf<NavKey>(RtExplore, n), nav.stack(RtExplore))
        assertEquals(listOf<NavKey>(RtBookings), nav.stack(RtBookings))
        assertEquals(RtExplore, nav.state.topLevelRoute)
    }

    @Test
    fun emptyChainLeavesStateUntouched() {
        val nav = navigator(RtExplore, clientTabs, stacks = mapOf(RtBookings to listOf(x)))

        assertThrows(IllegalArgumentException::class.java) { nav.openRouted(RtBookings, emptyList()) }

        assertEquals(listOf<NavKey>(RtExplore), nav.stack(RtExplore))
        assertEquals(listOf<NavKey>(RtBookings, x), nav.stack(RtBookings))
        assertEquals(RtExplore, nav.state.topLevelRoute)
    }

    @Test
    fun aTabThatIsNeitherStartNorTargetIsNotInUseAfterwards() {
        // RtOther is selected and holds u1, but it is neither the start tab nor the target, so it is not in use after.
        val nav =
            navigator(
                RtExplore,
                listOf(RtExplore, RtBookings, RtOther),
                selected = RtOther,
                stacks = mapOf(RtOther to listOf(u1)),
            )

        nav.openRouted(RtBookings, listOf(u1))

        assertEquals(listOf<NavKey>(RtBookings, u1), nav.stack(RtBookings))
        assertEquals(RtBookings, nav.state.topLevelRoute)
    }
}
