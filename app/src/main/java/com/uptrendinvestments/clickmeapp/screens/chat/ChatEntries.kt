package com.uptrendinvestments.clickmeapp.screens.chat

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.navigation.BottomBar
import com.uptrendinvestments.clickmeapp.navigation.Navigator
import com.uptrendinvestments.clickmeapp.navigation.StackOrigin
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.ChatThreadKey
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.ChatThreadRoute
import com.uptrendinvestments.clickmeapp.screens.chat.reportandblock.ChatReportBlockKey
import com.uptrendinvestments.clickmeapp.screens.chat.reportandblock.ReportAndBlockRoute
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary.BookingSummaryKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ClientChatsRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.UpcomingBookingKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingSummaryKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile.ExpertClientProfileKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.ExpertChatsRoot

/**
 * Registers the chat routes pushed on the Chats stack, all with the bottom bar hidden. Both shells call this from
 * their `detailEntries`, so a chat feature plan never edits a shell again.
 */
fun EntryProviderScope<NavKey>.chatEntries(
    navigator: Navigator,
    mode: AppMode,
) {
    entry<ChatThreadKey>(metadata = BottomBar.hidden()) { ChatThreadRoute(it, navigator, mode) }
    entry<ChatReportBlockKey>(metadata = BottomBar.hidden()) { ReportAndBlockRoute(it, navigator, mode) }
}

/** The Chats tab root key of [mode]'s shell. */
fun chatsRootFor(mode: AppMode): NavKey =
    when (mode) {
        AppMode.Client -> ClientChatsRoot
        AppMode.Expert -> ExpertChatsRoot
    }

/**
 * Opens the booking a system event in a thread refers to, on the Chats stack (UI-SPEC Navigation; iOS `isTerminal`
 * routing). Client: a terminal event opens Booking Summary, an active one opens Upcoming Booking Details. Expert: both
 * open the expert Booking Summary. Every key carries a Chats origin so it never equals a key on another stack.
 */
fun Navigator.openChatBooking(
    mode: AppMode,
    bookingId: String,
    isTerminal: Boolean,
) {
    val key: NavKey =
        when {
            mode == AppMode.Expert -> ExpertBookingSummaryKey(bookingId, ExpertBookingOrigin.Chats)
            isTerminal -> BookingSummaryKey(bookingId, StackOrigin.Chats)
            else -> UpcomingBookingKey(bookingId, StackOrigin.Chats)
        }
    navigate(key)
}

/**
 * "View profile" for the peer in [key], on the Chats stack of [mode]'s shell. Expert mode opens the read-only Client
 * Profile (iOS `HomeRoute.peerProfile` in the expert shell, D-13); client mode opens the peer's Expert Profile, seeded
 * from the thread key. A no-op without a [ChatThreadKey.peerId]. [mode] has no default on purpose: only expert mode may
 * reach the expert endpoints, because `ensureExpert` converts a client-only account on first touch, so a caller must
 * choose.
 */
fun Navigator.openChatPeerProfile(
    key: ChatThreadKey,
    mode: AppMode,
) {
    val peerId = key.peerId ?: return
    when (mode) {
        AppMode.Client ->
            // An empty title means unknown, per ExpertProfileKey.
            navigate(
                ExpertProfileKey(
                    expertId = peerId,
                    name = key.peerName.orEmpty(),
                    title = "",
                    imageUrl = key.peerAvatarUrl,
                    origin = StackOrigin.Chats,
                ),
            )
        AppMode.Expert -> navigate(ExpertClientProfileKey(peerId, ExpertBookingOrigin.Chats))
    }
}

/** Opens Report & Block for the thread in [key] on the Chats stack. */
fun Navigator.openChatReportBlock(key: ChatThreadKey) =
    navigate(ChatReportBlockKey(threadId = key.threadId, peerId = key.peerId, peerName = key.peerName))

/** D-09: a block success pops Report & Block and the thread, back to the Conversations root of [mode]'s shell. */
fun Navigator.returnToConversations(mode: AppMode) = reselect(chatsRootFor(mode))
