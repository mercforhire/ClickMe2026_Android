package com.uptrendinvestments.clickmeapp.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ClientBookingsRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ClientChatsRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ClientProfileRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ExploreRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.SearchRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.requestsent.RequestSentKey
import org.junit.Assert.assertEquals
import org.junit.Test

class BookingFlowNavigationTest {
    private val profile = ExpertProfileKey(expertId = "e1", name = "Ada", title = "Engineer", imageUrl = null)
    private val booking =
        BookingKey(
            expertId = "e1",
            expertName = "Ada",
            expertTitle = "Engineer",
            expertImageUrl = null,
        )

    private fun navigatorAtBookingForm(): Navigator {
        val tabs = listOf<NavKey>(ExploreRoot, SearchRoot, ClientBookingsRoot, ClientChatsRoot, ClientProfileRoot)
        val state =
            NavigationState(
                startRoute = ExploreRoot,
                topLevelRoute = mutableStateOf<NavKey>(ExploreRoot),
                backStacks = tabs.associateWith { NavBackStack<NavKey>(it) },
            )
        return Navigator(state).also {
            it.navigate(profile)
            it.navigate(booking)
        }
    }

    private fun Navigator.stack(tab: NavKey): List<NavKey> = state.backStacks.getValue(tab).toList()

    @Test
    fun requestSentReplacesTheBookingForm() {
        val navigator = navigatorAtBookingForm()
        navigator.replaceTop(RequestSentKey("b1"))
        assertEquals(listOf<NavKey>(ExploreRoot, profile, RequestSentKey("b1")), navigator.stack(ExploreRoot))
    }

    @Test
    fun backFromRequestSentLandsOnExpertProfile() {
        val navigator = navigatorAtBookingForm()
        navigator.replaceTop(RequestSentKey("b1"))
        navigator.goBack()
        assertEquals(listOf<NavKey>(ExploreRoot, profile), navigator.stack(ExploreRoot))
    }

    @Test
    fun viewMyBookingsClearsExploreAndSelectsBookings() {
        val navigator = navigatorAtBookingForm()
        navigator.replaceTop(RequestSentKey("b1"))
        navigator.resetAndSelect(ClientBookingsRoot, setOf(ExploreRoot, ClientBookingsRoot))
        assertEquals(listOf<NavKey>(ExploreRoot), navigator.stack(ExploreRoot))
        assertEquals(ClientBookingsRoot, navigator.state.topLevelRoute)
    }

    @Test
    fun backToHomePopsExploreToRoot() {
        val navigator = navigatorAtBookingForm()
        navigator.replaceTop(RequestSentKey("b1"))
        navigator.resetAndSelect(ExploreRoot, setOf(ExploreRoot))
        assertEquals(listOf<NavKey>(ExploreRoot), navigator.stack(ExploreRoot))
        assertEquals(ExploreRoot, navigator.state.topLevelRoute)
        assertEquals(NavTransition.Pop, navigator.state.lastTransition)
    }

    @Test
    fun backToHomeFromAnotherTabSelectsExplore() {
        // IN-10: iOS returnToHome also sets selectedTab = .explore, so a flow started from another tab ends on
        // Explore, not on that tab's Request Sent.
        val navigator = navigatorAtBookingForm()
        navigator.navigateInTab(ClientBookingsRoot, RequestSentKey("b1"))
        navigator.resetAndSelect(ExploreRoot, setOf(ExploreRoot))
        assertEquals(listOf<NavKey>(ExploreRoot), navigator.stack(ExploreRoot))
        assertEquals(ExploreRoot, navigator.state.topLevelRoute)
        assertEquals(NavTransition.TabSwitch, navigator.state.lastTransition)
    }

    @Test
    fun requestSentKeyCarriesOnlyTheBookingId() {
        assertEquals(RequestSentKey("b1"), RequestSentKey("b1"))
        assertEquals("b1", RequestSentKey("b1").bookingId)
    }
}
