package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.managers.CurrentUserIdProvider
import com.uptrendinvestments.clickmeapp.screens.chat.ChatLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.chat.ChatSessionState
import com.uptrendinvestments.clickmeapp.screens.chat.ChatsRepository
import com.uptrendinvestments.clickmeapp.screens.chat.ClientMsgIdGenerator
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakeRealtimeConnection
import com.uptrendinvestments.clickmeapp.testing.ME
import com.uptrendinvestments.clickmeapp.testing.chatsPayload
import kotlinx.coroutines.CoroutineScope

/**
 * Builds a [ChatThreadViewModel] over the fakes. The block-state scan answers an empty list by default so a test that
 * ignores blocking is not hit by an unstubbed endpoint. 12-08 extends this class with defaults only.
 */
class ChatThreadTestHarness(
    val scope: CoroutineScope? = null,
) {
    val api = FakeClickMeApi()
    val realtime = FakeRealtimeConnection()
    val session = ChatSessionState()
    val launch = ChatLaunchRequest()
    val myId: String = ME

    private var counter = 0

    /** Counting generator: "c1", "c2", ... */
    val ids = ClientMsgIdGenerator { "c${++counter}" }

    init {
        api.chatsHandler = { _, _ -> chatsPayload() }
    }

    fun viewModel(
        key: ChatThreadKey = ChatThreadKey("t1", "p1", "Ada Lovelace", null, true),
        saved: SavedStateHandle = SavedStateHandle(),
    ) = ChatThreadViewModel(
        key,
        saved,
        ChatsRepository(api),
        realtime,
        session,
        CurrentUserIdProvider { myId },
        launch,
        ids,
    )
}
