package com.uptrendinvestments.clickmeapp.testing

import com.uptrendinvestments.clickmeapp.managers.CurrentUserIdProvider
import com.uptrendinvestments.clickmeapp.screens.chat.ChatEntryViewModel
import com.uptrendinvestments.clickmeapp.screens.chat.ChatLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.chat.ChatLauncher
import com.uptrendinvestments.clickmeapp.screens.chat.ChatsRepository

/**
 * Builds a [ChatEntryViewModel] over the real [ChatLauncher] and [ChatsRepository] without Hilt.
 *
 * Build it outside `setContent` (lint ViewModelConstructorInComposable) and pass it as `chatEntry =` to the
 * Hilt-aware screens in Robolectric tests. Read the opened thread from [launchRequest] and the initiate calls from
 * [api].
 */
fun testChatEntryViewModel(
    api: FakeClickMeApi = FakeClickMeApi(),
    launchRequest: ChatLaunchRequest = ChatLaunchRequest(),
    currentUserId: String? = "me",
): ChatEntryViewModel =
    ChatEntryViewModel(
        launcher = ChatLauncher(ChatsRepository(api), launchRequest),
        currentUser = CurrentUserIdProvider { currentUserId },
    )
