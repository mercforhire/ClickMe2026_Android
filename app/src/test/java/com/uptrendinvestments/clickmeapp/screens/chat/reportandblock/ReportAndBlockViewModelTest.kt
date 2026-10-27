package com.uptrendinvestments.clickmeapp.screens.chat.reportandblock

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.model.enums.UserReportReason
import com.uptrendinvestments.clickmeapp.model.requests.ChatActionRequest
import com.uptrendinvestments.clickmeapp.model.responses.StatusOnlyResponse
import com.uptrendinvestments.clickmeapp.screens.chat.BlockState
import com.uptrendinvestments.clickmeapp.screens.chat.ChatLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.chat.ChatSessionState
import com.uptrendinvestments.clickmeapp.screens.chat.ChatsRepository
import com.uptrendinvestments.clickmeapp.screens.chat.ConversationsNotice
import com.uptrendinvestments.clickmeapp.screens.chat.ThreadNotice
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.chatThreadItem
import com.uptrendinvestments.clickmeapp.testing.chatsPayload
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ReportAndBlockViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private val session = ChatSessionState()
    private val launchRequest = ChatLaunchRequest()

    private fun vm(
        peerId: String? = "p1",
        handle: SavedStateHandle = SavedStateHandle(),
        me: String? = "me",
    ) = ReportAndBlockViewModel(
        ChatReportBlockKey(threadId = "t1", peerId = peerId, peerName = "Ada"),
        handle,
        ChatsRepository(api),
        session,
        { me },
        launchRequest,
    )

    private fun scan(
        blocked: Boolean = false,
        byMe: Boolean = false,
    ) {
        api.chatsHandler = { _, _ ->
            chatsPayload(chatThreadItem(threadId = "t1", isBlocked = blocked, blockedByMe = byMe))
        }
    }

    private fun apiError(
        status: Int,
        code: String? = null,
        message: String = "msg",
    ) = api.httpError(
        status,
        if (code == null) {
            """{"status":"error","message":"$message"}"""
        } else {
            """{"status":"error","code":"$code","message":"$message"}"""
        },
    )

    private fun TestScope.collectEvents(vm: ReportAndBlockViewModel): List<ReportAndBlockEvent> {
        val events = mutableListOf<ReportAndBlockEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.collect { events += it } }
        return events
    }

    @Test
    fun scanOnOpenSetsBlockState() =
        runTest(main.testDispatcher) {
            scan(blocked = true, byMe = true)
            val mine = vm()
            runCurrent()
            assertTrue(mine.uiState.value.block.showsUnblock)
            assertEquals(1 to 100, api.chatsQueries.last())

            scan(blocked = true, byMe = false)
            val theirs = vm()
            runCurrent()
            assertFalse(theirs.uiState.value.block.showsBlock)
            assertFalse(theirs.uiState.value.block.showsUnblock)
        }

    @Test
    fun scanFailureKeepsTheBlockState() =
        runTest(main.testDispatcher) {
            api.chatsHandler = { _, _ -> throw apiError(500) }
            val vm = vm()
            runCurrent()

            assertTrue(vm.uiState.value.block.showsBlock)
        }

    @Test
    fun threadAbsentFromTheScanKeepsAnEarlierSessionBlock() =
        runTest(main.testDispatcher) {
            api.chatsHandler = { _, _ -> chatsPayload() }
            session.markBlockedByMe("t1")
            val vm = vm()
            runCurrent()

            assertTrue(vm.uiState.value.block.showsUnblock)
        }

    @Test
    fun blockConfirmsThenBlocksAndExits() =
        runTest(main.testDispatcher) {
            scan()
            val gate = CompletableDeferred<Unit>()
            api.chatActionHandler = { _, _ ->
                gate.await()
                StatusOnlyResponse("success")
            }
            val vm = vm()
            val events = collectEvents(vm)
            runCurrent()

            vm.requestBlock()
            assertTrue(vm.uiState.value.showBlockConfirm)
            vm.confirmBlock()
            vm.confirmBlock()
            runCurrent()
            assertTrue(vm.uiState.value.blockInFlight)
            assertEquals(1, api.chatActions.size)

            gate.complete(Unit)
            runCurrent()

            assertEquals(listOf("t1" to ChatActionRequest("block")), api.chatActions)
            assertEquals(BlockState(isBlocked = true, blockedByMe = true), vm.uiState.value.block)
            assertTrue(session.isBlockedByMe("t1"))
            assertEquals(ConversationsNotice.Blocked("Ada"), launchRequest.conversationsNotice.consume())
            assertEquals(listOf<ReportAndBlockEvent>(ReportAndBlockEvent.Blocked), events)
            assertFalse(vm.uiState.value.showBlockConfirm)
        }

    @Test
    fun blockFailureDoesNotMarkOrNotify() =
        runTest(main.testDispatcher) {
            scan()
            api.chatActionHandler = { _, _ -> throw IOException("offline") }
            val vm = vm()
            val events = collectEvents(vm)
            runCurrent()

            vm.requestBlock()
            vm.confirmBlock()
            runCurrent()

            assertTrue(vm.uiState.value.block.showsBlock)
            assertFalse(session.isBlockedByMe("t1"))
            assertNull(launchRequest.conversationsNotice.consume())
            assertTrue(events.isEmpty())
            assertFalse(vm.uiState.value.blockInFlight)
        }

    @Test
    fun unblockSucceedsAndReturns() =
        runTest(main.testDispatcher) {
            scan(blocked = true, byMe = true)
            session.markBlockedByMe("t1")
            val vm = vm()
            val events = collectEvents(vm)
            runCurrent()

            vm.unblock()
            runCurrent()

            assertEquals(listOf("t1" to ChatActionRequest("unblock")), api.chatActions)
            assertFalse(session.isBlockedByMe("t1"))
            assertEquals(ThreadNotice.Unblocked("t1", "Ada"), launchRequest.threadNotice.consume())
            assertEquals(listOf<ReportAndBlockEvent>(ReportAndBlockEvent.Unblocked), events)
            assertTrue(vm.uiState.value.block.showsBlock)
        }

    @Test
    fun unblockIsIgnoredWhenTheCallerIsNotTheBlocker() =
        runTest(main.testDispatcher) {
            scan(blocked = true, byMe = false)
            val vm = vm()
            runCurrent()

            vm.unblock()
            runCurrent()

            assertTrue(api.chatActions.isEmpty())
        }

    @Test
    fun notBlockerLatchesUnblockOff() =
        runTest(main.testDispatcher) {
            scan(blocked = true, byMe = true)
            api.chatActionHandler = { _, _ ->
                throw apiError(403, "NOT_BLOCKER", "Only the person who blocked this conversation can unblock it.")
            }
            val vm = vm()
            val events = collectEvents(vm)
            runCurrent()

            vm.unblock()
            runCurrent()

            val latched = BlockState(isBlocked = true, blockedByMe = false, unblockDenied = true)
            assertEquals(latched, vm.uiState.value.block)
            assertFalse(vm.uiState.value.block.showsUnblock)
            assertTrue(events.isEmpty())
            assertNull(launchRequest.threadNotice.consume())
        }

    @Test
    fun reportAfterBlockFailureStillWorks() =
        runTest(main.testDispatcher) {
            scan()
            var calls = 0
            api.chatActionHandler = { _, body ->
                calls++
                if (body.action == "block") throw apiError(500) else StatusOnlyResponse("success")
            }
            val vm = vm()
            runCurrent()

            vm.requestBlock()
            vm.confirmBlock()
            runCurrent()
            vm.selectReason(UserReportReason.Spam)
            vm.submitReport()
            runCurrent()

            assertEquals(2, calls)
            assertTrue(vm.uiState.value.reportSubmitted)
        }

    @Test
    fun reportKeepsReasonAndDetailsAcrossRecreation() =
        runTest(main.testDispatcher) {
            scan()
            val handle = SavedStateHandle()
            val first = vm(handle = handle)
            first.selectReason(UserReportReason.Impersonation)
            first.onDetailsChange("details")

            val second = vm(handle = handle)

            assertEquals(UserReportReason.Impersonation, second.uiState.value.reason)
            assertEquals("details", second.uiState.value.details)
        }
}
