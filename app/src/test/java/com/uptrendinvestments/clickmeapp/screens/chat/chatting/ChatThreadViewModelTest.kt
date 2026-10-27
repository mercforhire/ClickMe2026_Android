package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.chatEvent
import com.uptrendinvestments.clickmeapp.testing.chatMessage
import com.uptrendinvestments.clickmeapp.testing.messagesPayload
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ChatThreadViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val h = ChatThreadTestHarness()

    private fun at(minute: Int): Instant = Instant.parse("2026-11-20T10:%02d:00Z".format(minute))

    @Test
    fun opensWithPageOneLimitThirtyAndOrderedItems() =
        runTest(main.testDispatcher) {
            h.api.chatMessagesHandler = { _, _, _ ->
                messagesPayload(
                    listOf(
                        chatMessage(id = "m2", senderId = "me", content = "Mine", createdAt = at(3)),
                        chatEvent(id = "e1", createdAt = at(1)),
                        chatMessage(id = "m1", senderId = "p1", content = "Theirs", createdAt = at(2)),
                    ),
                )
            }
            val vm = h.viewModel()
            assertEquals(ThreadLoad.Loading, vm.uiState.value.load)
            runCurrent()
            assertEquals(listOf(Triple("t1", 1, 30)), h.api.chatMessagesQueries)
            val state = vm.uiState.value
            assertEquals(ThreadLoad.Loaded, state.load)
            assertEquals(listOf("e1", "m1", "m2"), state.items.map { it.serverId })
            assertTrue(state.items[0] is ThreadItem.SystemEvent)
            assertTrue((state.items[2] as ThreadItem.Message).mine)
            assertEquals("Ada Lovelace", state.peerName)
            assertFalse(state.hasMoreEarlier)
        }

    @Test
    fun zeroMessagesIsLoadedEmpty() =
        runTest(main.testDispatcher) {
            h.api.chatMessagesHandler = { _, _, _ -> messagesPayload(emptyList()) }
            val vm = h.viewModel()
            runCurrent()
            assertEquals(ThreadLoad.Loaded, vm.uiState.value.load)
            assertTrue(
                vm.uiState.value.items
                    .isEmpty(),
            )
            assertFalse(vm.uiState.value.hasMoreEarlier)
        }

    @Test
    fun morePagesSetHasMoreEarlier() =
        runTest(main.testDispatcher) {
            h.api.chatMessagesHandler = { _, _, _ ->
                messagesPayload(listOf(chatMessage()), currentPage = 1, totalPages = 3)
            }
            val vm = h.viewModel()
            runCurrent()
            assertTrue(vm.uiState.value.hasMoreEarlier)
        }

    @Test
    fun loudFailureShowsFailedAndRetryReloads() =
        runTest(main.testDispatcher) {
            var failing = true
            h.api.chatMessagesHandler = { _, _, _ ->
                if (failing) throw IOException("offline")
                messagesPayload(listOf(chatMessage()))
            }
            val vm = h.viewModel()
            runCurrent()
            assertEquals(ThreadLoad.Failed(FieldError.Res(R.string.login_error_network)), vm.uiState.value.load)
            failing = false
            vm.retry()
            assertEquals(ThreadLoad.Loading, vm.uiState.value.load)
            runCurrent()
            assertEquals(ThreadLoad.Loaded, vm.uiState.value.load)
            assertEquals(1, vm.uiState.value.items.size)
        }

    @Test
    fun peerIdFallsBackToFirstPeerSenderWhenKeyHasNone() =
        runTest(main.testDispatcher) {
            h.api.chatMessagesHandler = { _, _, _ ->
                messagesPayload(
                    listOf(
                        chatMessage(id = "m1", senderId = "me", createdAt = at(1)),
                        chatMessage(id = "m2", senderId = "p1", createdAt = at(2)),
                    ),
                )
            }
            val vm = h.viewModel(ChatThreadKey("t1"))
            runCurrent()
            assertEquals("p1", vm.uiState.value.peerId)
        }

    @Test
    fun keyPeerIdWinsOverTheFallback() =
        runTest(main.testDispatcher) {
            h.api.chatMessagesHandler = { _, _, _ ->
                messagesPayload(listOf(chatMessage(id = "m2", senderId = "other")))
            }
            val vm = h.viewModel(ChatThreadKey("t1", "p1"))
            runCurrent()
            assertEquals("p1", vm.uiState.value.peerId)
        }

    @Test
    fun selfThreadDetected() =
        runTest(main.testDispatcher) {
            h.api.chatMessagesHandler = { _, _, _ -> messagesPayload(emptyList()) }
            val vm = h.viewModel(ChatThreadKey("t1", "ME"), SavedStateHandle())
            runCurrent()
            assertTrue(vm.uiState.value.isSelfThread)
        }

    @Test
    fun receivedTextIsNotTrimmedOrParsed() =
        runTest(main.testDispatcher) {
            h.api.chatMessagesHandler = { _, _, _ ->
                messagesPayload(listOf(chatMessage(content = "  <b>hi</b>\nhttps://x.y  ")))
            }
            val vm = h.viewModel()
            runCurrent()
            val message =
                vm.uiState.value.items
                    .single() as ThreadItem.Message
            assertEquals("  <b>hi</b>\nhttps://x.y  ", message.text)
        }
}
