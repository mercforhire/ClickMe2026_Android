package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import com.uptrendinvestments.clickmeapp.realtime.ConnectionState
import com.uptrendinvestments.clickmeapp.realtime.RealtimeEvent
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.chatMessage
import com.uptrendinvestments.clickmeapp.testing.messagesPayload
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ChatThreadViewModelRealtimeTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val h = ChatThreadTestHarness()

    private fun messageNew(
        id: String,
        thread: String = "T1",
        sender: String = "p1",
        content: String = "Hi",
    ) = RealtimeEvent.MessageNew(
        messageId = id,
        threadId = thread,
        senderId = sender,
        type = "text",
        content = content,
        timestamp = Instant.parse("2026-11-20T11:00:00Z"),
        status = "sent",
    )

    private fun serveEmpty() {
        h.api.chatMessagesHandler = { _, _, _ -> messagesPayload(emptyList()) }
    }

    @Test
    fun bookingUpdateReloadsPageOne() =
        runTest(main.testDispatcher) {
            serveEmpty()
            val vm = h.viewModel()
            runCurrent()
            assertEquals(1, h.api.chatMessagesQueries.size)
            h.realtime.emit(RealtimeEvent.BookingUpdate(bookingId = "b1", status = "confirmed"))
            runCurrent()
            assertEquals(2, h.api.chatMessagesQueries.size)
            assertEquals(ThreadLoad.Loaded, vm.uiState.value.load)
        }

    @Test
    fun reconnectReloadsQuietly() =
        runTest(main.testDispatcher) {
            serveEmpty()
            val vm = h.viewModel()
            runCurrent()
            h.realtime.setState(ConnectionState.Connected)
            runCurrent()
            assertEquals(1, h.api.chatMessagesQueries.size)
            h.realtime.setState(ConnectionState.Disconnected)
            runCurrent()
            h.realtime.setState(ConnectionState.Connected)
            assertEquals(ThreadLoad.Loaded, vm.uiState.value.load)
            runCurrent()
            assertEquals(2, h.api.chatMessagesQueries.size)
            assertEquals(ThreadLoad.Loaded, vm.uiState.value.load)
        }

    @Test
    fun incomingPeerMessageAppendedOnce() =
        runTest(main.testDispatcher) {
            serveEmpty()
            val vm = h.viewModel()
            runCurrent()
            h.realtime.emit(messageNew("m1"))
            runCurrent()
            assertEquals(
                listOf("m1"),
                vm.uiState.value.items
                    .map { it.serverId },
            )
            h.realtime.emit(messageNew("m1"))
            h.realtime.emit(messageNew("m2", sender = "ME"))
            h.realtime.emit(messageNew("m3", thread = "t2"))
            runCurrent()
            assertEquals(
                listOf("m1"),
                vm.uiState.value.items
                    .map { it.serverId },
            )
            assertEquals(1, h.api.chatMessagesQueries.size)
        }

    @Test
    fun incomingMessageKeepsVerbatimText() =
        runTest(main.testDispatcher) {
            serveEmpty()
            val vm = h.viewModel()
            runCurrent()
            h.realtime.emit(messageNew("m1", content = " a\n<i>b</i> "))
            runCurrent()
            assertEquals(
                " a\n<i>b</i> ",
                (
                    vm.uiState.value.items
                        .single() as ThreadItem.Message
                ).text,
            )
        }

    @Test
    fun incomingReadFlipsMine() =
        runTest(main.testDispatcher) {
            h.api.chatMessagesHandler = { _, _, _ ->
                messagesPayload(listOf(chatMessage(id = "m5", senderId = "me", content = "Mine")))
            }
            val vm = h.viewModel()
            runCurrent()
            assertEquals(
                Delivery.Sent,
                (
                    vm.uiState.value.items
                        .single() as ThreadItem.Message
                ).delivery,
            )
            h.realtime.emit(RealtimeEvent.MessageRead("t2", listOf("M5"), "p1"))
            runCurrent()
            assertEquals(
                Delivery.Sent,
                (
                    vm.uiState.value.items
                        .single() as ThreadItem.Message
                ).delivery,
            )
            h.realtime.emit(RealtimeEvent.MessageRead("t1", listOf("M5"), "p1"))
            runCurrent()
            assertEquals(
                Delivery.Read,
                (
                    vm.uiState.value.items
                        .single() as ThreadItem.Message
                ).delivery,
            )
            assertFalse(vm.uiState.value.peerTyping)
        }
}
