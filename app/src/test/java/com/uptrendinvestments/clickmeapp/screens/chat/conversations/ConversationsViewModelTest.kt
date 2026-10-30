package com.uptrendinvestments.clickmeapp.screens.chat.conversations

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.model.responses.ChatThreadItem
import com.uptrendinvestments.clickmeapp.realtime.ConnectionState
import com.uptrendinvestments.clickmeapp.realtime.RealtimeEvent
import com.uptrendinvestments.clickmeapp.screens.chat.ChatLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.chat.ChatSessionState
import com.uptrendinvestments.clickmeapp.screens.chat.ChatsRepository
import com.uptrendinvestments.clickmeapp.screens.chat.ConversationsNotice
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakeRealtimeConnection
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.chatPartner
import com.uptrendinvestments.clickmeapp.testing.chatThreadItem
import com.uptrendinvestments.clickmeapp.testing.chatsPayload
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private val realtime = FakeRealtimeConnection()
    private val session = ChatSessionState()
    private val launchRequest = ChatLaunchRequest()
    private val saved = SavedStateHandle()

    private val older = Instant.parse("2026-10-27T10:00:00Z")
    private val newer = Instant.parse("2026-10-27T12:00:00Z")
    private val arrival = Instant.parse("2026-10-27T13:00:00Z")

    private fun t1(
        unread: Int = 0,
        lastMessage: String? = "See you at 3",
    ) = chatThreadItem(
        threadId = "t1",
        partner = chatPartner(id = "p1", name = "Ada Lovelace"),
        lastMessage = lastMessage,
        lastMsgAt = older,
        unreadCount = unread,
    )

    private fun t2() =
        chatThreadItem(
            threadId = "t2",
            partner = chatPartner(id = "p2", name = "Grace Hopper", roles = listOf("client")),
            lastMessage = "Thanks",
            lastMsgAt = newer,
        )

    private fun serve(vararg rows: ChatThreadItem) {
        api.chatsHandler = { _, _ -> chatsPayload(*rows) }
    }

    private fun vm(handle: SavedStateHandle = saved) =
        ConversationsViewModel(handle, ChatsRepository(api), realtime, session, { "me" }, launchRequest)

    private fun message(
        threadId: String,
        senderId: String = "p1",
        content: String? = "new",
    ) = RealtimeEvent.MessageNew(
        messageId = "m-$threadId",
        threadId = threadId,
        senderId = senderId,
        type = "text",
        content = content,
        timestamp = arrival,
        status = "sent",
    )

    private val ConversationsViewModel.ids: List<String>
        get() = uiState.value.conversations.map { it.threadId }

    private val ConversationsViewModel.visibleIds: List<String>
        get() = uiState.value.visible.map { it.threadId }

    private fun ConversationsViewModel.row(threadId: String) =
        uiState.value.conversations.first { it.threadId == threadId }

    @Test
    fun messageNewMovesRowToTopAndUpdatesPreview() =
        runTest(main.testDispatcher) {
            serve(t1(), t2())
            val vm = vm()
            runCurrent()
            assertEquals(listOf("t2", "t1"), vm.ids)
            realtime.emit(message("T1"))
            runCurrent()
            assertEquals(listOf("t1", "t2"), vm.ids)
            assertEquals("new", vm.row("t1").lastMessage)
            assertEquals(arrival, vm.row("t1").lastMsgAt)
        }

    @Test
    fun messageNewIncrementsLocalUnreadUnlessThreadOpen() =
        runTest(main.testDispatcher) {
            serve(t1(), t2())
            val vm = vm()
            runCurrent()
            realtime.emit(message("t1"))
            runCurrent()
            realtime.emit(message("t1"))
            runCurrent()
            assertEquals(2, vm.row("t1").unread)

            // Opening the thread reads it.
            session.onThreadShown("t1")
            runCurrent()
            assertEquals(0, vm.row("t1").unread)

            // A message for the open thread adds nothing.
            realtime.emit(message("t1"))
            runCurrent()
            assertEquals(0, vm.row("t1").unread)

            // A message from the current user never counts.
            session.onThreadHidden("t1")
            realtime.emit(message("t1", senderId = "ME"))
            runCurrent()
            assertEquals(0, vm.row("t1").unread)
        }

    @Test
    fun serverUnreadWinsWhenLarger() =
        runTest(main.testDispatcher) {
            serve(t1(unread = 5))
            val vm = vm()
            runCurrent()
            realtime.emit(message("t1"))
            runCurrent()
            realtime.emit(message("t1"))
            runCurrent()
            assertEquals(2, vm.row("t1").localUnread)
            assertEquals(5, vm.row("t1").unread)
        }

    @Test
    fun localUnreadSurvivesAReloadAndOnOpenClearsIt() =
        runTest(main.testDispatcher) {
            serve(t1())
            val vm = vm()
            runCurrent()
            realtime.emit(message("t1"))
            runCurrent()
            vm.refresh()
            runCurrent()
            assertEquals(1, vm.row("t1").unread)
            vm.onOpen(vm.row("t1"))
            assertEquals(0, vm.row("t1").unread)
        }

    @Test
    fun unknownThreadTriggersQuietReload() =
        runTest(main.testDispatcher) {
            serve(t1())
            val vm = vm()
            runCurrent()
            val states = mutableListOf<ConversationsUiState>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { states += it } }
            serve(t1(), chatThreadItem(threadId = "t9", partner = chatPartner(id = "p9", name = "New Peer")))
            realtime.emit(message("t9", senderId = "p9"))
            runCurrent()
            assertEquals(2, api.chatsQueries.size)
            assertTrue(states.none { it.load is ConversationsLoad.Loading })
            assertEquals(setOf("t1", "t9"), vm.ids.toSet())
            assertEquals(1, vm.row("t9").unread)
        }

    @Test
    fun userStatusFlipsPresence() =
        runTest(main.testDispatcher) {
            serve(t1(), t2())
            val vm = vm()
            runCurrent()
            realtime.emit(RealtimeEvent.UserStatus("P1", "online"))
            runCurrent()
            assertTrue(vm.row("t1").isOnline)
            assertFalse(vm.row("t2").isOnline)
            realtime.emit(RealtimeEvent.UserStatus("p1", "offline"))
            runCurrent()
            assertFalse(vm.row("t1").isOnline)
        }

    @Test
    fun reconnectReloadsQuietly() =
        runTest(main.testDispatcher) {
            serve(t1())
            realtime.setState(ConnectionState.Connected)
            val vm = vm()
            runCurrent()
            assertEquals(1, api.chatsQueries.size)
            realtime.setState(ConnectionState.Disconnected)
            runCurrent()
            assertEquals(1, api.chatsQueries.size)
            realtime.setState(ConnectionState.Connected)
            runCurrent()
            assertEquals(2, api.chatsQueries.size)
            assertEquals(ConversationsLoad.Loaded, vm.uiState.value.load)
        }

    @Test
    fun screenShownReloadsQuietlyWhenLoaded() =
        runTest(main.testDispatcher) {
            serve(t1())
            val vm = vm()
            runCurrent()
            vm.onScreenShown()
            assertEquals(ConversationsLoad.Loaded, vm.uiState.value.load)
            runCurrent()
            assertEquals(listOf(1 to 50, 1 to 50), api.chatsQueries)
        }

    @Test
    fun screenShownDuringCurrentLoadDoesNothing() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            api.chatsHandler = { _, _ ->
                gate.await()
                chatsPayload(t1())
            }
            val vm = vm()
            runCurrent()
            vm.onScreenShown()
            runCurrent()
            assertEquals(1, api.chatsQueries.size)
            gate.complete(Unit)
            runCurrent()
            assertEquals(ConversationsLoad.Loaded, vm.uiState.value.load)

            // A hide makes the next show restart the load.
            vm.onScreenHidden()
            vm.onScreenShown()
            runCurrent()
            assertEquals(2, api.chatsQueries.size)
        }

    @Test
    fun pullFailureFlagsRefreshFailedAndKeepsRows() =
        runTest(main.testDispatcher) {
            serve(t1())
            val vm = vm()
            runCurrent()
            api.chatsHandler = { _, _ -> throw api.httpError(500, """{"status":"error","message":"Boom"}""") }
            vm.refresh()
            assertTrue(vm.uiState.value.isRefreshing)
            runCurrent()
            assertTrue(vm.uiState.value.refreshFailed)
            assertFalse(vm.uiState.value.isRefreshing)
            assertEquals(ConversationsLoad.Loaded, vm.uiState.value.load)
            assertEquals(1, vm.uiState.value.conversations.size)
            vm.consumeRefreshFailure()
            assertFalse(vm.uiState.value.refreshFailed)
        }

    @Test
    fun quietFailureKeepsRowsSilently() =
        runTest(main.testDispatcher) {
            serve(t1())
            val vm = vm()
            runCurrent()
            api.chatsHandler = { _, _ -> throw api.httpError(500, """{"status":"error","message":"Boom"}""") }
            vm.onScreenShown()
            runCurrent()
            assertFalse(vm.uiState.value.refreshFailed)
            assertEquals(ConversationsLoad.Loaded, vm.uiState.value.load)
            assertEquals(1, vm.uiState.value.conversations.size)
        }

    @Test
    fun reconnectReloadFailureFlags() =
        runTest(main.testDispatcher) {
            serve(t1())
            realtime.setState(ConnectionState.Connected)
            val vm = vm()
            runCurrent()
            api.chatsHandler = { _, _ -> throw api.httpError(500, """{"status":"error","message":"Boom"}""") }
            realtime.setState(ConnectionState.Disconnected)
            runCurrent()
            realtime.setState(ConnectionState.Connected)
            runCurrent()
            assertTrue(vm.uiState.value.refreshFailed)
            assertFalse(vm.uiState.value.isRefreshing)
            assertEquals(ConversationsLoad.Loaded, vm.uiState.value.load)
            assertEquals(listOf("t1"), vm.ids)
        }

    @Test
    fun unknownThreadReloadFailureFlags() =
        runTest(main.testDispatcher) {
            serve(t1())
            val vm = vm()
            runCurrent()
            api.chatsHandler = { _, _ -> throw api.httpError(500, """{"status":"error","message":"Boom"}""") }
            realtime.emit(message(threadId = "t9"))
            runCurrent()
            assertTrue(vm.uiState.value.refreshFailed)
            assertEquals(listOf("t1"), vm.ids)
        }

    @Test
    fun quietTabReturnFailureIsSilent() =
        runTest(main.testDispatcher) {
            serve(t1())
            val vm = vm()
            runCurrent()
            api.chatsHandler = { _, _ -> throw api.httpError(500, """{"status":"error","message":"Boom"}""") }
            vm.onScreenShown()
            runCurrent()
            assertFalse(vm.uiState.value.refreshFailed)
            assertEquals(listOf("t1"), vm.ids)
        }

    @Test
    fun pullFailureStillFlags() =
        runTest(main.testDispatcher) {
            serve(t1())
            val vm = vm()
            runCurrent()
            api.chatsHandler = { _, _ -> throw api.httpError(500, """{"status":"error","message":"Boom"}""") }
            vm.refresh()
            runCurrent()
            assertTrue(vm.uiState.value.refreshFailed)
            assertEquals(listOf("t1"), vm.ids)
        }

    @Test
    fun loudFailureShowsFailedAndRetryRecovers() =
        runTest(main.testDispatcher) {
            api.chatsHandler = { _, _ -> throw api.httpError(500, """{"status":"error","message":"Boom"}""") }
            val vm = vm()
            runCurrent()
            assertTrue(vm.uiState.value.load is ConversationsLoad.Failed)
            serve(t1())
            vm.retry()
            assertEquals(ConversationsLoad.Loading, vm.uiState.value.load)
            runCurrent()
            assertEquals(ConversationsLoad.Loaded, vm.uiState.value.load)
        }

    @Test
    fun searchFiltersByNameOrPreviewIgnoringCase() =
        runTest(main.testDispatcher) {
            serve(t1(), t2())
            val vm = vm()
            runCurrent()
            vm.onQueryChange("ADA")
            assertEquals(listOf("t1"), vm.visibleIds)
            vm.onQueryChange("see you")
            assertEquals(listOf("t1"), vm.visibleIds)
            vm.onQueryChange("  ")
            assertEquals(2, vm.visibleIds.size)
            vm.onQueryChange("zzz")
            assertTrue(vm.visibleIds.isEmpty())
            assertEquals("zzz", saved[ConversationsViewModel.KEY_QUERY])
        }

    @Test
    fun queryIsRestoredFromSavedState() =
        runTest(main.testDispatcher) {
            serve(t1())
            val handle = SavedStateHandle(mapOf(ConversationsViewModel.KEY_QUERY to "ada"))
            assertEquals("ada", vm(handle).uiState.value.query)
        }

    @Test
    fun sortIsStableWithNullsLast() =
        runTest(main.testDispatcher) {
            serve(
                chatThreadItem(threadId = "a", lastMsgAt = null),
                chatThreadItem(threadId = "b", lastMsgAt = older),
                chatThreadItem(threadId = "c", lastMsgAt = older),
                chatThreadItem(threadId = "d", lastMsgAt = newer),
            )
            val vm = vm()
            runCurrent()
            assertEquals(listOf("d", "b", "c", "a"), vm.ids)
        }

    @Test
    fun samePageTwiceKeepsOneRowPerThread() =
        runTest(main.testDispatcher) {
            serve(t1(), t2())
            val vm = vm()
            runCurrent()
            serve(t1(), t1(), t2())
            vm.refresh()
            runCurrent()
            assertEquals(1, vm.ids.count { it == "t1" })
            assertEquals(2, vm.uiState.value.conversations.size)
        }

    @Test
    fun nullLastMessageAndPartnerNameAreKept() =
        runTest(main.testDispatcher) {
            serve(chatThreadItem(threadId = "t1", partner = chatPartner(name = null), lastMessage = null))
            val vm = vm()
            runCurrent()
            assertNull(vm.row("t1").name)
            assertNull(vm.row("t1").lastMessage)
        }

    @Test
    fun blockedNoticeIsConsumedOnce() =
        runTest(main.testDispatcher) {
            serve(t1())
            launchRequest.conversationsNotice.post(ConversationsNotice.Blocked("Ada"))
            val vm = vm()
            assertEquals(ConversationsNotice.Blocked("Ada"), vm.uiState.value.notice)
            runCurrent()
            vm.consumeNotice()
            assertNull(vm.uiState.value.notice)

            launchRequest.conversationsNotice.post(ConversationsNotice.Blocked("Grace"))
            runCurrent()
            assertEquals(ConversationsNotice.Blocked("Grace"), vm.uiState.value.notice)
            vm.consumeNotice()
            runCurrent()
            assertNull(vm.uiState.value.notice)
            assertNull(launchRequest.conversationsNotice.pending.value)
        }
}
