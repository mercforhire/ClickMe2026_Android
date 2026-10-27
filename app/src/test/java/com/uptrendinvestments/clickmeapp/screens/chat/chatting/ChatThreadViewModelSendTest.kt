package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import com.uptrendinvestments.clickmeapp.model.requests.SendMessageRequest
import com.uptrendinvestments.clickmeapp.testing.ME
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.chatMessage
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
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

    private fun ThreadItem.message() = this as ThreadItem.Message

    @Test
    fun networkFailureMarksFailed() =
        runTest(main.testDispatcher) {
            h.api.sendChatMessageHandler = { _, _ -> throw IOException("offline") }
            val vm = loadedViewModel()
            vm.onComposerChanged("hi")
            vm.send()
            runCurrent()
            val failed =
                vm.state.items
                    .single()
                    .message()
            assertEquals(Delivery.Failed, failed.delivery)
            assertEquals("hi", failed.text)
            assertNull(failed.serverId)
            assertEquals("", vm.state.composerText)
            assertNull(vm.state.sendError)
        }

    @Test
    fun serverErrorMarksFailed() =
        runTest(main.testDispatcher) {
            h.api.sendChatMessageHandler = { _, _ -> throw h.api.httpError(500, "{}") }
            val vm = loadedViewModel()
            vm.onComposerChanged("hi")
            vm.send()
            runCurrent()
            assertEquals(
                Delivery.Failed,
                vm.state.items
                    .single()
                    .message()
                    .delivery,
            )
            assertNull(vm.state.sendError)
        }

    @Test
    fun decodingFailureMarksFailed() =
        runTest(main.testDispatcher) {
            h.api.sendChatMessageHandler = { _, _ -> throw IllegalStateException("bad json") }
            val vm = loadedViewModel()
            vm.onComposerChanged("hi")
            vm.send()
            runCurrent()
            assertEquals(
                Delivery.Failed,
                vm.state.items
                    .single()
                    .message()
                    .delivery,
            )
        }

    @Test
    fun retryReusesSameIdAndSucceeds() =
        runTest(main.testDispatcher) {
            h.api.sendChatMessageHandler = { _, _ -> throw IOException("offline") }
            val vm = loadedViewModel()
            vm.onComposerChanged("hi")
            vm.send()
            runCurrent()
            val gate = CompletableDeferred<Unit>()
            h.api.sendChatMessageHandler = { _, _ ->
                gate.await()
                sendAck("m9", at(5))
            }
            vm.retry("c1")
            assertEquals(
                Delivery.Sending,
                vm.state.items
                    .single()
                    .message()
                    .delivery,
            )
            runCurrent()
            vm.retry("c1")
            runCurrent()
            assertEquals(2, h.api.sentChatMessages.size)
            assertEquals(listOf("c1", "c1"), h.api.sentChatMessages.map { it.second.clientMsgId })
            gate.complete(Unit)
            runCurrent()
            val sent =
                vm.state.items
                    .single()
                    .message()
            assertEquals(Delivery.Sent, sent.delivery)
            assertEquals("m9", sent.serverId)
            assertEquals(2, h.api.sentChatMessages.size)
        }

    @Test
    fun retryIgnoresABubbleThatIsNotFailed() =
        runTest(main.testDispatcher) {
            val vm = loadedViewModel()
            vm.onComposerChanged("hi")
            vm.send()
            runCurrent()
            vm.retry("c1")
            runCurrent()
            assertEquals(1, h.api.sentChatMessages.size)
        }

    @Test
    fun deleteRemovesFailedLocally() =
        runTest(main.testDispatcher) {
            h.api.sendChatMessageHandler = { _, _ -> throw IOException("offline") }
            val vm = loadedViewModel()
            vm.onComposerChanged("hi")
            vm.send()
            runCurrent()
            vm.delete("c1")
            runCurrent()
            assertTrue(vm.state.items.isEmpty())
            assertEquals(1, h.api.sentChatMessages.size)
        }

    @Test
    fun lostResponseThenReloadConverges() =
        runTest(main.testDispatcher) {
            h.api.sendChatMessageHandler = { _, _ -> throw IOException("response lost") }
            val vm = loadedViewModel()
            vm.onComposerChanged("hi")
            vm.send()
            runCurrent()
            assertEquals(
                Delivery.Failed,
                vm.state.items
                    .single()
                    .message()
                    .delivery,
            )
            h.api.chatMessagesHandler = { _, _, _ ->
                messagesPayload(
                    listOf(
                        chatMessage(id = "m9", senderId = ME, content = "hi", createdAt = at(5), clientMsgId = "c1"),
                    ),
                )
            }
            vm.onScreenShown()
            runCurrent()
            val row =
                vm.state.items
                    .single()
                    .message()
            assertEquals("m9", row.serverId)
            assertEquals(Delivery.Sent, row.delivery)
            assertEquals("c:c1", row.key)
        }

    @Test
    fun concurrentSendsReconcileIndependently() =
        runTest(main.testDispatcher) {
            val gates = mutableMapOf<String, CompletableDeferred<Instant>>()
            h.api.sendChatMessageHandler = { _, body ->
                val id = body.clientMsgId
                val at = gates.getOrPut(id) { CompletableDeferred() }.await()
                sendAck("srv-$id", at)
            }
            val vm = loadedViewModel()
            vm.onComposerChanged("first")
            vm.send()
            vm.onComposerChanged("second")
            vm.send()
            runCurrent()
            assertEquals(listOf("c1", "c2"), h.api.sentChatMessages.map { it.second.clientMsgId })
            assertTrue(vm.state.items.all { it.message().delivery == Delivery.Sending })
            gates.getValue("c2").complete(at(1))
            runCurrent()
            assertEquals(
                Delivery.Sending,
                vm.state.items
                    .first { it.key == "c:c1" }
                    .message()
                    .delivery,
            )
            assertEquals(
                Delivery.Sent,
                vm.state.items
                    .first { it.key == "c:c2" }
                    .message()
                    .delivery,
            )
            gates.getValue("c1").complete(at(2))
            runCurrent()
            assertEquals(listOf("srv-c2", "srv-c1"), vm.state.items.map { it.serverId })
            assertTrue(vm.state.items.all { it.message().delivery == Delivery.Sent })
        }

    @Test
    fun composerCapTruncatesPaste() =
        runTest(main.testDispatcher) {
            val vm = loadedViewModel()
            vm.onComposerChanged("a".repeat(10_500))
            assertEquals(10_000, vm.state.composerText.length)
            assertTrue(vm.state.isComposerAtCap)
            vm.onComposerChanged("a".repeat(9_999) + "\uD83D\uDE00" + "tail")
            assertEquals(9_999, vm.state.composerText.length)
            assertFalse(vm.state.isComposerAtCap)
        }

    @Test
    fun sentContentIsTheTrimmedText() =
        runTest(main.testDispatcher) {
            val vm = loadedViewModel()
            vm.onComposerChanged("\n  \uD83D\uDE00 hi \n")
            vm.send()
            runCurrent()
            assertEquals(
                "\uD83D\uDE00 hi",
                h.api.sentChatMessages
                    .single()
                    .second.content,
            )
        }
}
