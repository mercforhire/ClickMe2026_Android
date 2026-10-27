package com.uptrendinvestments.clickmeapp.screens.chat

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.InitiateChatData
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.ChatThreadKey
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.initiateData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

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

    @Test
    fun secondOpenForSamePeerWhileInFlightReturnsAlreadyOpening() =
        runTest {
            val gate = CompletableDeferred<InitiateChatData>()
            api.initiateChatHandler = { gate.await() }
            val first = launch { launcher.open(ChatPeer("p1", "Ada", null, true)) }
            runCurrent()

            val second = launcher.open(ChatPeer("P1", "Ada", null, true))

            assertEquals(ChatOpenResult.AlreadyOpening, second)
            assertEquals(1, api.initiateChatRequests.size)
            gate.complete(initiateData("t1"))
            first.join()
            assertEquals("t1", launchRequest.thread.consume()?.threadId)
        }

    @Test
    fun errorsMapToEntryCopy() =
        runTest {
            val peer = ChatPeer("p1", null, null, null)

            api.initiateChatHandler = {
                throw api.httpError(404, """{"status":"error","code":"NOT_FOUND","message":"missing"}""")
            }
            assertEquals(
                ChatOpenResult.Failed(FieldError.Res(R.string.chat_entry_error_unavailable)),
                launcher.open(peer),
            )

            api.initiateChatHandler = {
                throw api.httpError(422, """{"status":"error","code":"VALIDATION_ERROR","message":"x"}""")
            }
            assertEquals(ChatOpenResult.Failed(FieldError.Text("x")), launcher.open(peer))

            api.initiateChatHandler = { throw api.httpError(500, "{}") }
            assertEquals(ChatOpenResult.Failed(FieldError.Res(R.string.login_error_unknown)), launcher.open(peer))

            api.initiateChatHandler = { throw IOException("offline") }
            assertEquals(ChatOpenResult.Failed(FieldError.Res(R.string.login_error_network)), launcher.open(peer))

            assertNull(launchRequest.thread.consume())
        }

    @Test
    fun cancelledOpenPostsNothing() =
        runTest {
            val gate = CompletableDeferred<InitiateChatData>()
            api.initiateChatHandler = { gate.await() }
            val job = launch { launcher.open(ChatPeer("p1", null, null, null)) }
            runCurrent()

            job.cancelAndJoin()
            gate.complete(initiateData("t1"))

            assertNull(launchRequest.thread.consume())
            api.initiateChatHandler = { initiateData("t2") }
            val later = launcher.open(ChatPeer("p1", null, null, null))
            assertEquals("t2", (later as ChatOpenResult.Opened).key.threadId)
        }
}
