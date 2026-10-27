package com.uptrendinvestments.clickmeapp.screens.chat

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.navigation.BottomBar
import com.uptrendinvestments.clickmeapp.navigation.Navigator
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.ChatThreadKey
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.ChatThreadRoute
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ClientChatsRoot
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
}

/** The Chats tab root key of [mode]'s shell. */
fun chatsRootFor(mode: AppMode): NavKey =
    when (mode) {
        AppMode.Client -> ClientChatsRoot
        AppMode.Expert -> ExpertChatsRoot
    }
