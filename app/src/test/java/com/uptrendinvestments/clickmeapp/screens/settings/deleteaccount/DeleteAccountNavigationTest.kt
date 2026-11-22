package com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.navigation.NavigationState
import com.uptrendinvestments.clickmeapp.navigation.Navigator
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ClientBookingsRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ClientChatsRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ClientProfileRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ExploreRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.SearchRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.DashboardRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.ExpertBookingsRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.ExpertChatsRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.ExpertProfileRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.TopicsRoot
import org.junit.Assert.assertEquals
import org.junit.Test

/** "View My Bookings" from the blocker dialog: Bookings of the active mode, Profile stack back at its root. */
class DeleteAccountNavigationTest {
    private fun navigator(
        start: NavKey,
        tabs: List<NavKey>,
        selected: NavKey,
    ): Navigator =
        Navigator(
            NavigationState(
                startRoute = start,
                topLevelRoute = mutableStateOf(selected),
                backStacks = tabs.associateWith { NavBackStack<NavKey>(it) },
            ),
        )

    private fun Navigator.stack(tab: NavKey): List<NavKey> = state.backStacks.getValue(tab).toList()

    @Test
    fun clientOpensBookingsAndResetsTheProfileStack() {
        val clientTabs = listOf<NavKey>(ExploreRoot, SearchRoot, ClientBookingsRoot, ClientChatsRoot, ClientProfileRoot)
        val navigator = navigator(ExploreRoot, clientTabs, selected = ClientProfileRoot)
        navigator.navigate(DeleteAccountKey)

        navigator.showBookingsFromDeleteAccount(AppMode.Client)

        assertEquals(ClientBookingsRoot, navigator.state.topLevelRoute)
        assertEquals(listOf<NavKey>(ClientProfileRoot), navigator.stack(ClientProfileRoot))
    }

    @Test
    fun expertOpensBookingsAndResetsTheProfileStack() {
        val expertTabs =
            listOf<NavKey>(DashboardRoot, ExpertBookingsRoot, TopicsRoot, ExpertChatsRoot, ExpertProfileRoot)
        val navigator = navigator(DashboardRoot, expertTabs, selected = ExpertProfileRoot)
        navigator.navigate(DeleteAccountKey)

        navigator.showBookingsFromDeleteAccount(AppMode.Expert)

        assertEquals(ExpertBookingsRoot, navigator.state.topLevelRoute)
        assertEquals(listOf<NavKey>(ExpertProfileRoot), navigator.stack(ExpertProfileRoot))
    }
}
