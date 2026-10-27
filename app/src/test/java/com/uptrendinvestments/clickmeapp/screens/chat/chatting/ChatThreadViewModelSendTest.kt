package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import com.uptrendinvestments.clickmeapp.model.requests.SendMessageRequest
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.chatThreadItem
import com.uptrendinvestments.clickmeapp.testing.chatsPayload
import com.uptrendinvestments.clickmeapp.testing.messagesPayload
import com.uptrendinvestments.clickmeapp.testing.sendAck
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ChatThreadViewModelSendTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val h = ChatThreadTestHarness()

    private fun at(minute: Int): Instant = Instant.parse("2026-11-20T10:%02d:00Z".format(minute))

    private val ChatThreadViewModel.state: ChatThreadUiState get() = uiState.value

    private fun TestScope.loadedViewModel(): ChatThreadViewModel {
        h.api.chatMessagesHandler = { _, _, _ -> messagesPayload(emptyList()) }
        return h.viewModel().also { runCurrent() }
    }

    @Test
    fun sendAppendsSendingThenAckMakesSent() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            h.api.sendChatMessageHandler = { _, _ ->
                gate.await()
                sendAck("m9", at(5))
            }
            val vm = loadedViewModel()
            vm.onComposerChanged("  hi  ")
            vm.send()
            val sending = vm.state.items.last() as ThreadItem.Message
            assertEquals("c1", sending.clientMsgId)
            assertEquals("hi", sending.text)
            assertTrue(sending.mine)
            assertEquals(Delivery.Sending, sending.delivery)
            assertNull(sending.serverId)
            assertEquals("", vm.state.composerText)
            runCurrent()
            assertEquals(listOf("t1" to SendMessageRequest("text", "hi", "c1")), h.api.sentChatMessages)
            gate.complete(Unit)
            runCurrent()
            val sent = vm.state.items.single() as ThreadItem.Message
            assertEquals("c:c1", sent.key)
            assertEquals("m9", sent.serverId)
            assertEquals(at(5), sent.createdAt)
            assertEquals(Delivery.Sent, sent.delivery)
        }

    @Test
    fun sendStopsTyping() =
        runTest(main.testDispatcher) {
            val vm = loadedViewModel()
            vm.onComposerChanged("hi")
            assertEquals(1, h.realtime.typingStarts.size)
            vm.send()
            assertEquals(listOf("t1" to "p1"), h.realtime.typingStops)
        }

    @Test
    fun blankDoesNotSend() =
        runTest(main.testDispatcher) {
            val vm = loadedViewModel()
            vm.onComposerChanged("   ")
            vm.send()
            runCurrent()
            assertTrue(vm.state.items.isEmpty())
            assertTrue(h.api.sentChatMessages.isEmpty())
        }

    @Test
    fun cannotSendWhileLoading() =
        runTest(main.testDispatcher) {
            h.api.chatMessagesHandler = { _, _, _ -> messagesPayload(emptyList()) }
            val vm = h.viewModel()
            vm.onComposerChanged("hi")
            vm.send()
            runCurrent()
            assertTrue(h.api.sentChatMessages.isEmpty())
            assertEquals("hi", vm.state.composerText)
        }

    @Test
    fun cannotSendWhileBlocked() =
        runTest(main.testDispatcher) {
            h.api.chatsHandler = { _, _ -> chatsPayload(chatThreadItem(isBlocked = true, blockedByMe = true)) }
            val vm = loadedViewModel()
            vm.onComposerChanged("hi")
            vm.send()
            runCurrent()
            assertTrue(h.api.sentChatMessages.isEmpty())
            assertEquals("hi", vm.state.composerText)
        }
}
