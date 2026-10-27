package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.realtime.ConnectionState
import com.uptrendinvestments.clickmeapp.realtime.RealtimeEvent
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.chatMessage
import com.uptrendinvestments.clickmeapp.testing.messagesPayload
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun typingStartOnceThenStopAfterThreeSeconds() =
        runTest(main.testDispatcher) {
            serveEmpty()
            val vm = h.viewModel()
            runCurrent()
            vm.onComposerChanged("h")
            advanceTimeBy(1_000)
            vm.onComposerChanged("he")
            runCurrent()
            assertEquals(listOf("t1" to "p1"), h.realtime.typingStarts)
            advanceTimeBy(2_999)
            runCurrent()
            assertTrue(h.realtime.typingStops.isEmpty())
            advanceTimeBy(2)
            runCurrent()
            assertEquals(listOf("t1" to "p1"), h.realtime.typingStops)
            vm.onComposerChanged("hey")
            assertEquals(2, h.realtime.typingStarts.size)
        }

    @Test
    fun emptyingComposerStopsAtOnce() =
        runTest(main.testDispatcher) {
            serveEmpty()
            val vm = h.viewModel()
            runCurrent()
            vm.onComposerChanged("h")
            vm.onComposerChanged("")
            assertEquals(1, h.realtime.typingStops.size)
            advanceTimeBy(10_000)
            runCurrent()
            assertEquals(1, h.realtime.typingStops.size)
        }

    @Test
    fun blankEditsNeverStartTyping() =
        runTest(main.testDispatcher) {
            serveEmpty()
            val vm = h.viewModel()
            runCurrent()
            vm.onComposerChanged("   ")
            assertTrue(h.realtime.typingStarts.isEmpty())
            assertTrue(h.realtime.typingStops.isEmpty())
        }

    @Test
    fun screenHiddenStopsTyping() =
        runTest(main.testDispatcher) {
            serveEmpty()
            val vm = h.viewModel()
            vm.onScreenShown()
            runCurrent()
            vm.onComposerChanged("h")
            vm.onScreenHidden()
            assertEquals(listOf("t1" to "p1"), h.realtime.typingStops)
        }

    @Test
    fun noTypingWithoutPeerId() =
        runTest(main.testDispatcher) {
            serveEmpty()
            val vm = h.viewModel(ChatThreadKey("t1"))
            runCurrent()
            vm.onComposerChanged("h")
            advanceTimeBy(5_000)
            runCurrent()
            assertTrue(h.realtime.typingStarts.isEmpty())
            assertTrue(h.realtime.typingStops.isEmpty())
        }

    @Test
    fun composerTextSavedAndRestored() =
        runTest(main.testDispatcher) {
            serveEmpty()
            val saved = SavedStateHandle()
            val vm = h.viewModel(saved = saved)
            runCurrent()
            vm.onComposerChanged("draft")
            assertEquals("draft", saved.get<String>(ChatThreadViewModel.KEY_COMPOSER))
            val restored = h.viewModel(saved = saved)
            assertEquals("draft", restored.uiState.value.composerText)
        }

    @Test
    fun incomingTypingShowsAndClears() =
        runTest(main.testDispatcher) {
            serveEmpty()
            val vm = h.viewModel()
            runCurrent()
            h.realtime.emit(RealtimeEvent.TypingStart("T1", "p1"))
            runCurrent()
            assertTrue(vm.uiState.value.peerTyping)
            h.realtime.emit(RealtimeEvent.TypingStop("t1", "p1"))
            runCurrent()
            assertFalse(vm.uiState.value.peerTyping)
            h.realtime.emit(RealtimeEvent.TypingStart("t1", "p1"))
            runCurrent()
            advanceTimeBy(5_999)
            runCurrent()
            assertTrue(vm.uiState.value.peerTyping)
            advanceTimeBy(2)
            runCurrent()
            assertFalse(vm.uiState.value.peerTyping)
        }

    @Test
    fun typingEventsForOtherThreadsOrFromMeAreIgnored() =
        runTest(main.testDispatcher) {
            serveEmpty()
            val vm = h.viewModel()
            runCurrent()
            h.realtime.emit(RealtimeEvent.TypingStart("t2", "p1"))
            h.realtime.emit(RealtimeEvent.TypingStart("t1", "ME"))
            runCurrent()
            assertFalse(vm.uiState.value.peerTyping)
        }

    @Test
    fun peerMessageClearsTheTypingIndicator() =
        runTest(main.testDispatcher) {
            serveEmpty()
            val vm = h.viewModel()
            runCurrent()
            h.realtime.emit(RealtimeEvent.TypingStart("t1", "p1"))
            runCurrent()
            assertTrue(vm.uiState.value.peerTyping)
            h.realtime.emit(messageNew("m1"))
            runCurrent()
            assertFalse(vm.uiState.value.peerTyping)
        }

    @Test
    fun readReceiptsEmittedOnceWhileShownAndConnected() =
        runTest(main.testDispatcher) {
            h.realtime.setState(ConnectionState.Connected)
            h.api.chatMessagesHandler = { _, _, _ ->
                messagesPayload(
                    listOf(
                        chatMessage(id = "m1", senderId = "p1"),
                        chatMessage(id = "m2", senderId = "p1"),
                        chatMessage(id = "m0", senderId = "me"),
                    ),
                )
            }
            val vm = h.viewModel()
            vm.onScreenShown()
            runCurrent()
            assertEquals(listOf("t1" to listOf("m1", "m2")), h.realtime.messageReads)
            vm.onScreenShown()
            runCurrent()
            h.realtime.emit(RealtimeEvent.BookingUpdate(bookingId = "b1", status = "confirmed"))
            runCurrent()
            assertEquals(1, h.realtime.messageReads.size)
            h.realtime.emit(messageNew("m3"))
            runCurrent()
            assertEquals(listOf("t1" to listOf("m1", "m2"), "t1" to listOf("m3")), h.realtime.messageReads)
        }

    @Test
    fun noReadReceiptsWhileHidden() =
        runTest(main.testDispatcher) {
            h.realtime.setState(ConnectionState.Connected)
            h.api.chatMessagesHandler = { _, _, _ -> messagesPayload(listOf(chatMessage(id = "m1", senderId = "p1"))) }
            val vm = h.viewModel()
            runCurrent()
            h.realtime.emit(messageNew("m2"))
            runCurrent()
            assertTrue(h.realtime.messageReads.isEmpty())
            vm.onScreenShown()
            runCurrent()
            assertEquals(listOf("t1" to listOf("m1", "m2")), h.realtime.messageReads)
            vm.onScreenHidden()
            h.realtime.emit(messageNew("m3"))
            runCurrent()
            assertEquals(1, h.realtime.messageReads.size)
        }

    @Test
    fun pendingReadReceiptsGoOutAfterTheReconnect() =
        runTest(main.testDispatcher) {
            h.api.chatMessagesHandler = { _, _, _ -> messagesPayload(listOf(chatMessage(id = "m1", senderId = "p1"))) }
            val vm = h.viewModel()
            vm.onScreenShown()
            runCurrent()
            assertTrue(h.realtime.messageReads.isEmpty())
            h.realtime.setState(ConnectionState.Connected)
            runCurrent()
            assertEquals(listOf("t1" to listOf("m1")), h.realtime.messageReads)
            h.realtime.setState(ConnectionState.Disconnected)
            h.realtime.setState(ConnectionState.Connected)
            runCurrent()
            assertEquals(1, h.realtime.messageReads.size)
        }

    @Test
    fun peerMessagesAlreadyReadAreNotReEmitted() =
        runTest(main.testDispatcher) {
            h.realtime.setState(ConnectionState.Connected)
            h.api.chatMessagesHandler = { _, _, _ ->
                messagesPayload(listOf(chatMessage(id = "m1", senderId = "p1", status = "read")))
            }
            val vm = h.viewModel()
            vm.onScreenShown()
            runCurrent()
            assertTrue(h.realtime.messageReads.isEmpty())
        }
}
