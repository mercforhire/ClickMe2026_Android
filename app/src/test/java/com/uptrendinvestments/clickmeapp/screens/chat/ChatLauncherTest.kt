package com.uptrendinvestments.clickmeapp.screens.chat

import com.uptrendinvestments.clickmeapp.screens.chat.chatting.ChatThreadKey
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.initiateData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatLauncherTest {
    private val api = FakeClickMeApi()
    private val launchRequest = ChatLaunchRequest()
    private val launcher = ChatLauncher(ChatsRepository(api), launchRequest)

    @Test
    fun openInitiatesAndPostsKey() =
        runTest {
            api.initiateChatHandler = { initiateData("t1") }

            val result = launcher.open(ChatPeer("p1", "Ada", "https://a", isExpert = true))

            val expected = ChatThreadKey("t1", "p1", "Ada", "https://a", true)
            assertEquals(ChatOpenResult.Opened(expected), result)
            assertEquals("p1", api.initiateChatRequests.single().peerId)
            assertEquals(expected, launchRequest.thread.consume())
        }
}
