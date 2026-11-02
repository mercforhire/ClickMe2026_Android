package com.uptrendinvestments.clickmeapp.screens.chat

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.navigation.NavigationState
import com.uptrendinvestments.clickmeapp.navigation.Navigator
import com.uptrendinvestments.clickmeapp.navigation.StackOrigin
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.ChatThreadKey
import com.uptrendinvestments.clickmeapp.screens.chat.reportandblock.ChatReportBlockKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingnote.ClientBookingNoteKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary.BookingSummaryKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ClientChatsRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ExploreRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.UpcomingBookingKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingSummaryKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.DashboardRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.ExpertChatsRoot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ChatNavigationTest {
    private val thread = ChatThreadKey("t1", "p1", "Ada", null, true)

    private fun clientNavigator(): Navigator {
        val state =
            NavigationState(
                startRoute = ExploreRoot,
                topLevelRoute = mutableStateOf<NavKey>(ExploreRoot),
                backStacks =
                    mapOf(
                        ExploreRoot to NavBackStack<NavKey>(ExploreRoot),
                        ClientChatsRoot to NavBackStack<NavKey>(ClientChatsRoot),
                    ),
            )
        return Navigator(state).also { it.openChatThread(ClientChatsRoot, thread) }
    }

    private fun expertNavigator(): Navigator {
        val state =
            NavigationState(
                startRoute = DashboardRoot,
                topLevelRoute = mutableStateOf<NavKey>(DashboardRoot),
                backStacks =
                    mapOf(
                        DashboardRoot to NavBackStack<NavKey>(DashboardRoot),
                        ExpertChatsRoot to NavBackStack<NavKey>(ExpertChatsRoot),
                    ),
            )
        return Navigator(state).also { it.openChatThread(ExpertChatsRoot, thread) }
    }

    private fun Navigator.stack(tab: NavKey): List<NavKey> = state.backStacks.getValue(tab).toList()

    @Test
    fun clientTerminalEventOpensSummaryWithChatsOrigin() {
        val navigator = clientNavigator()

        navigator.openChatBooking(AppMode.Client, "b1", isTerminal = true)

        assertEquals(
            listOf<NavKey>(ClientChatsRoot, thread, BookingSummaryKey("b1", StackOrigin.Chats)),
            navigator.stack(ClientChatsRoot),
        )
    }

    @Test
    fun clientActiveEventOpensUpcomingWithChatsOrigin() {
        val navigator = clientNavigator()

        navigator.openChatBooking(AppMode.Client, "b1", isTerminal = false)

        assertEquals(
            listOf<NavKey>(ClientChatsRoot, thread, UpcomingBookingKey("b1", StackOrigin.Chats)),
            navigator.stack(ClientChatsRoot),
        )
    }

    @Test
    fun expertEventsOpenExpertSummaryWithChatsOrigin() {
        listOf(true, false).forEach { terminal ->
            val navigator = expertNavigator()

            navigator.openChatBooking(AppMode.Expert, "b1", isTerminal = terminal)

            assertEquals(
                listOf<NavKey>(ExpertChatsRoot, thread, ExpertBookingSummaryKey("b1", ExpertBookingOrigin.Chats)),
                navigator.stack(ExpertChatsRoot),
            )
        }
    }

    @Test
    fun viewProfileUsesPeerSeedAndChatsOrigin() {
        val navigator = clientNavigator()

        navigator.openChatPeerProfile(thread)

        assertEquals(
            ExpertProfileKey(
                expertId = "p1",
                name = "Ada",
                title = "",
                imageUrl = null,
                origin = StackOrigin.Chats,
            ),
            navigator.stack(ClientChatsRoot).last(),
        )
    }

    @Test
    fun viewProfileWithoutPeerIdPushesNothing() {
        val navigator = clientNavigator()

        navigator.openChatPeerProfile(ChatThreadKey("t1", null, "Ada", null, true))

        assertEquals(listOf<NavKey>(ClientChatsRoot, thread), navigator.stack(ClientChatsRoot))
    }

    @Test
    fun chatsOriginKeyDiffersFromDefault() {
        assertNotEquals(
            ExpertProfileKey("p1", "Ada", "", null),
            ExpertProfileKey("p1", "Ada", "", null, origin = StackOrigin.Chats),
        )
        assertNotEquals(BookingSummaryKey("b1"), BookingSummaryKey("b1", StackOrigin.Chats))
        assertNotEquals(UpcomingBookingKey("b1"), UpcomingBookingKey("b1", StackOrigin.Chats))
        assertNotEquals(
            ExpertBookingSummaryKey("b1", ExpertBookingOrigin.Dashboard),
            ExpertBookingSummaryKey("b1", ExpertBookingOrigin.Chats),
        )
        assertNotEquals(
            ClientBookingNoteKey("b1", "Ada", "Algorithms", 0L, 1L, StackOrigin.Default),
            ClientBookingNoteKey("b1", "Ada", "Algorithms", 0L, 1L, StackOrigin.Chats),
        )
    }

    @Test
    fun reportBlockAndReturn() {
        val navigator = clientNavigator()

        navigator.openChatReportBlock(thread)

        assertEquals(ChatReportBlockKey("t1", "p1", "Ada"), navigator.stack(ClientChatsRoot).last())

        navigator.returnToConversations(AppMode.Client)

        assertEquals(listOf<NavKey>(ClientChatsRoot), navigator.stack(ClientChatsRoot))
    }

    @Test
    fun returnToConversationsPopsTheExpertChatsStack() {
        val navigator = expertNavigator()
        navigator.openChatReportBlock(thread)

        navigator.returnToConversations(AppMode.Expert)

        assertEquals(listOf<NavKey>(ExpertChatsRoot), navigator.stack(ExpertChatsRoot))
    }
}
