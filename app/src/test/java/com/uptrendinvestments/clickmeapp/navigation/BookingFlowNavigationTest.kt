package com.uptrendinvestments.clickmeapp.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.ChatThreadKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary.BookingSummaryKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.CancelBookingKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.favorites.FavoritesKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.backToHomeFromRequestSent
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.openBookingSummary
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.openUpcomingBooking
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.showExplore
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.showMyBookings
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.showSearch
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ClientBookingsRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ClientChatsRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ClientProfileRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ExploreRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.SearchRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.viewBookingsFromRequestSent
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.requestsent.RequestSentKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.UpcomingBookingKey
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

    private fun newNavigator(): Navigator {
        val tabs = listOf<NavKey>(ExploreRoot, SearchRoot, ClientBookingsRoot, ClientChatsRoot, ClientProfileRoot)
        val state =
            NavigationState(
                startRoute = ExploreRoot,
                topLevelRoute = mutableStateOf<NavKey>(ExploreRoot),
                backStacks = tabs.associateWith { NavBackStack<NavKey>(it) },
            )
        return Navigator(state)
    }

    private fun navigatorAtBookingForm(): Navigator =
        newNavigator().also {
            it.navigate(profile)
            it.navigate(booking)
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
        navigator.showMyBookings()
        assertEquals(listOf<NavKey>(ExploreRoot), navigator.stack(ExploreRoot))
        assertEquals(listOf<NavKey>(SearchRoot), navigator.stack(SearchRoot))
        assertEquals(ClientBookingsRoot, navigator.state.topLevelRoute)
    }

    @Test
    fun backToHomePopsExploreToRoot() {
        val navigator = navigatorAtBookingForm()
        navigator.replaceTop(RequestSentKey("b1"))
        navigator.showExplore()
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
        navigator.showExplore()
        assertEquals(listOf<NavKey>(ExploreRoot), navigator.stack(ExploreRoot))
        assertEquals(ExploreRoot, navigator.state.topLevelRoute)
        assertEquals(NavTransition.TabSwitch, navigator.state.lastTransition)
    }

    // A booking started from Search ends on Request Sent in the Search stack. Unlike iOS (which leaves searchPath
    // alone), View My Bookings and Explore Experts also reset Search so nothing stays parked there (user decision
    // 2026-10-30); SearchRoot itself stays so its ViewModel and query survive (D-02).
    private fun navigatorAtRequestSentFromSearch(): Navigator =
        newNavigator().also {
            it.navigate(SearchRoot)
            it.navigate(profile)
            it.navigate(booking)
            it.replaceTop(RequestSentKey("b1"))
        }

    @Test
    fun bookingStartedFromSearchLeavesNothingOnTheSearchStackAfterViewMyBookings() {
        val navigator = navigatorAtRequestSentFromSearch()
        assertEquals(listOf<NavKey>(SearchRoot, profile, RequestSentKey("b1")), navigator.stack(SearchRoot))
        navigator.showMyBookings()
        assertEquals(listOf<NavKey>(SearchRoot), navigator.stack(SearchRoot))
        assertEquals(listOf<NavKey>(ExploreRoot), navigator.stack(ExploreRoot))
        assertEquals(ClientBookingsRoot, navigator.state.topLevelRoute)
    }

    @Test
    fun bookingStartedFromSearchBackToHomeResetsSearchAndSelectsExplore() {
        val navigator = navigatorAtRequestSentFromSearch()
        navigator.showExplore()
        assertEquals(listOf<NavKey>(SearchRoot), navigator.stack(SearchRoot))
        assertEquals(ExploreRoot, navigator.state.topLevelRoute)
    }

    @Test
    fun seeAllSelectsSearchWithoutResettingItsStack() {
        // D-01: See all is a pure tab switch, exactly like iOS `selectedTab = .search`.
        val navigator = newNavigator()
        navigator.navigateInTab(SearchRoot, profile)
        navigator.navigate(ExploreRoot)
        navigator.showSearch()
        assertEquals(SearchRoot, navigator.state.topLevelRoute)
        assertEquals(listOf<NavKey>(SearchRoot, profile), navigator.stack(SearchRoot))
        assertEquals(NavTransition.TabSwitch, navigator.state.lastTransition)
    }

    @Test
    fun openUpcomingBookingFromExplorePushesOnTheExploreStack() {
        val navigator = newNavigator()
        navigator.openUpcomingBooking("b1")
        assertEquals(listOf<NavKey>(ExploreRoot, UpcomingBookingKey("b1")), navigator.stack(ExploreRoot))
        assertEquals(listOf<NavKey>(ClientBookingsRoot), navigator.stack(ClientBookingsRoot))
    }

    @Test
    fun openUpcomingBookingFromBookingsPushesOnTheBookingsStack() {
        val navigator = newNavigator()
        navigator.navigate(ClientBookingsRoot)
        navigator.openUpcomingBooking("b1")
        assertEquals(listOf<NavKey>(ClientBookingsRoot, UpcomingBookingKey("b1")), navigator.stack(ClientBookingsRoot))
        assertEquals(listOf<NavKey>(ExploreRoot), navigator.stack(ExploreRoot))
    }

    @Test
    fun openBookingSummaryPushesOnTheCurrentStack() {
        val navigator = newNavigator()
        navigator.navigate(ClientBookingsRoot)
        navigator.openBookingSummary("b2")
        assertEquals(listOf<NavKey>(ClientBookingsRoot, BookingSummaryKey("b2")), navigator.stack(ClientBookingsRoot))
    }

    @Test
    fun showMyBookingsAfterACancelResetsBothStacksAndSelectsBookings() {
        val navigator = newNavigator()
        navigator.openUpcomingBooking("b1")
        navigator.navigate(CancelBookingKey("b1"))
        navigator.navigateInTab(ClientBookingsRoot, UpcomingBookingKey("b9"))
        navigator.showMyBookings()
        assertEquals(listOf<NavKey>(ExploreRoot), navigator.stack(ExploreRoot))
        assertEquals(listOf<NavKey>(SearchRoot), navigator.stack(SearchRoot))
        assertEquals(listOf<NavKey>(ClientBookingsRoot), navigator.stack(ClientBookingsRoot))
        assertEquals(ClientBookingsRoot, navigator.state.topLevelRoute)
    }

    @Test
    fun showExploreFromBookingsPopsExploreToItsRootAndSelectsIt() {
        val navigator = newNavigator()
        navigator.openUpcomingBooking("b1")
        navigator.navigate(ClientBookingsRoot)
        navigator.showExplore()
        assertEquals(listOf<NavKey>(ExploreRoot), navigator.stack(ExploreRoot))
        assertEquals(ExploreRoot, navigator.state.topLevelRoute)
    }

    // CR-01: a booking started from Saved Experts ends on Request Sent in the Profile stack. Neither shared reset set
    // holds the Profile tab (Saved Experts' own buttons keep the list open, RESEARCH A3, runbook row D1.FV4), so the
    // two Request Sent exits reset it themselves, and only when this booking ran there.
    private val savedProfile = profile.copy(origin = StackOrigin.Profile)

    private fun navigatorParkedOnSavedExperts(): Navigator =
        newNavigator().also {
            it.navigate(ClientProfileRoot)
            it.navigate(FavoritesKey)
            it.navigate(savedProfile)
        }

    private fun navigatorAtRequestSentFromSavedExperts(): Navigator =
        navigatorParkedOnSavedExperts().also {
            it.navigate(booking)
            it.replaceTop(RequestSentKey("b1"))
        }

    @Test
    fun viewBookingsFromRequestSentOnTheProfileStackPopsItToTheHub() {
        val navigator = navigatorAtRequestSentFromSavedExperts()
        navigator.viewBookingsFromRequestSent(RequestSentKey("b1"))
        assertEquals(listOf<NavKey>(ClientProfileRoot), navigator.stack(ClientProfileRoot))
        assertEquals(ClientBookingsRoot, navigator.state.topLevelRoute)
        assertEquals(NavTransition.TabSwitch, navigator.state.lastTransition)
    }

    @Test
    fun backToHomeFromRequestSentOnTheProfileStackPopsItToTheHub() {
        val navigator = navigatorAtRequestSentFromSavedExperts()
        navigator.backToHomeFromRequestSent(RequestSentKey("b1"))
        assertEquals(listOf<NavKey>(ClientProfileRoot), navigator.stack(ClientProfileRoot))
        assertEquals(ExploreRoot, navigator.state.topLevelRoute)
        assertEquals(NavTransition.TabSwitch, navigator.state.lastTransition)
    }

    @Test
    fun sharedSwitchesLeaveSavedExpertsOnTheProfileStack() {
        // RESEARCH A3 / D1.FV4: Saved Experts' Explore Experts and View My History keep the list open.
        val navigator = newNavigator()
        navigator.navigate(ClientProfileRoot)
        navigator.navigate(FavoritesKey)
        navigator.showExplore()
        assertEquals(listOf<NavKey>(ClientProfileRoot, FavoritesKey), navigator.stack(ClientProfileRoot))
        navigator.navigate(ClientProfileRoot)
        navigator.showMyBookings()
        assertEquals(listOf<NavKey>(ClientProfileRoot, FavoritesKey), navigator.stack(ClientProfileRoot))
    }

    private val parkedProfileStack = listOf<NavKey>(ClientProfileRoot, FavoritesKey, savedProfile)

    /** Every other stack that can host Request Sent, built on a navigator whose Profile stack is parked. */
    private val flowsOnAnotherStack: List<Navigator.() -> Unit> =
        listOf(
            {
                navigate(ExploreRoot)
                navigate(profile)
                navigate(booking)
                replaceTop(RequestSentKey("b1"))
            },
            {
                navigate(SearchRoot)
                navigate(profile.copy(origin = StackOrigin.Search))
                navigate(booking)
                replaceTop(RequestSentKey("b1"))
            },
            {
                navigate(ClientChatsRoot)
                navigate(ChatThreadKey("t1"))
                navigate(profile.copy(origin = StackOrigin.Chats))
                navigate(booking)
                replaceTop(RequestSentKey("b1"))
            },
            {
                navigate(ClientBookingsRoot)
                navigate(BookingSummaryKey("b0"))
                navigate(booking)
                replaceTop(RequestSentKey("b1"))
            },
        )

    private fun parkedNavigatorWith(flow: Navigator.() -> Unit): Navigator =
        navigatorParkedOnSavedExperts().also { it.flow() }

    private fun assertSameNavigationState(
        expected: Navigator,
        actual: Navigator,
    ) {
        for (tab in expected.state.backStacks.keys) {
            assertEquals("stack of $tab", expected.stack(tab), actual.stack(tab))
        }
        assertEquals(expected.state.topLevelRoute, actual.state.topLevelRoute)
        assertEquals(expected.state.lastTransition, actual.state.lastTransition)
    }

    @Test
    fun viewBookingsFromRequestSentMatchesShowMyBookingsForAFlowOnAnotherStack() {
        for (flow in flowsOnAnotherStack) {
            val withHelper = parkedNavigatorWith(flow)
            val withShared = parkedNavigatorWith(flow)
            withHelper.viewBookingsFromRequestSent(RequestSentKey("b1"))
            withShared.showMyBookings()
            assertSameNavigationState(withShared, withHelper)
            assertEquals(parkedProfileStack, withHelper.stack(ClientProfileRoot))
        }
    }

    @Test
    fun backToHomeFromRequestSentMatchesShowExploreForAFlowOnAnotherStack() {
        for (flow in flowsOnAnotherStack) {
            val withHelper = parkedNavigatorWith(flow)
            val withShared = parkedNavigatorWith(flow)
            withHelper.backToHomeFromRequestSent(RequestSentKey("b1"))
            withShared.showExplore()
            assertSameNavigationState(withShared, withHelper)
            assertEquals(parkedProfileStack, withHelper.stack(ClientProfileRoot))
        }
    }
}
