package com.uptrendinvestments.clickmeapp.screens.chat

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.InitiateChatData
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.initiateData
import com.uptrendinvestments.clickmeapp.testing.testChatEntryViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatEntryViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private val launchRequest = ChatLaunchRequest()
    private val viewModel = testChatEntryViewModel(api, launchRequest, currentUserId = "ME")

    @Test
    fun busyWhileInFlightAndIgnoresRepeatTaps() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<InitiateChatData>()
            api.initiateChatHandler = { gate.await() }
            var opened = 0

            viewModel.open(ChatPeer("p1", "Ada", null, true)) { opened++ }
            runCurrent()
            assertEquals("p1", viewModel.state.value.busyPeerId)

            viewModel.open(ChatPeer("p2", "Bob", null, true)) { opened++ }
            viewModel.open(ChatPeer("p1", "Ada", null, true)) { opened++ }
            runCurrent()
            assertEquals(1, api.initiateChatRequests.size)

            gate.complete(initiateData("t1"))
            runCurrent()

            assertNull(viewModel.state.value.busyPeerId)
            assertEquals(1, opened)
            assertEquals("t1", launchRequest.thread.consume()?.threadId)
        }

    @Test
    fun failureSetsErrorAndDismissClearsIt() =
        runTest(main.testDispatcher) {
            api.initiateChatHandler = {
                throw api.httpError(404, """{"status":"error","code":"NOT_FOUND","message":"missing"}""")
            }
            var opened = 0

            viewModel.open(ChatPeer("p1", null, null, null)) { opened++ }
            runCurrent()

            assertEquals(FieldError.Res(R.string.chat_entry_error_unavailable), viewModel.state.value.error)
            assertNull(viewModel.state.value.busyPeerId)
            assertEquals(0, opened)
            assertNull(launchRequest.thread.consume())

            viewModel.dismissError()
            assertNull(viewModel.state.value.error)
        }

    @Test
    fun selfPeerIsIgnored() =
        runTest(main.testDispatcher) {
            val fresh = FakeClickMeApi()

            val vm = testChatEntryViewModel(fresh, launchRequest, currentUserId = "ME")
            vm.open(ChatPeer("me", "Me", null, true))
            runCurrent()

            assertTrue(vm.isSelf("me"))
            assertFalse(vm.isSelf("other"))
            assertFalse(vm.isSelf(null))
            assertTrue(fresh.initiateChatRequests.isEmpty())
            assertNull(vm.state.value.busyPeerId)
        }
}
