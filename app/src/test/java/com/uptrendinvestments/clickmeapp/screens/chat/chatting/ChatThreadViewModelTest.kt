package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.ChatMessageItem
import com.uptrendinvestments.clickmeapp.screens.chat.ComposerMode
import com.uptrendinvestments.clickmeapp.screens.chat.ThreadNotice
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.chatEvent
import com.uptrendinvestments.clickmeapp.testing.chatMessage
import com.uptrendinvestments.clickmeapp.testing.chatPartner
import com.uptrendinvestments.clickmeapp.testing.chatThreadItem
import com.uptrendinvestments.clickmeapp.testing.chatsPayload
import com.uptrendinvestments.clickmeapp.testing.messagesPayload
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private fun pagedHandler(
        pages: Map<Int, List<ChatMessageItem>>,
        totalPages: Int = 3,
    ) {
        h.api.chatMessagesHandler = { _, page, _ ->
            messagesPayload(pages[page].orEmpty(), currentPage = page, totalPages = totalPages)
        }
    }

    @Test
    fun loadEarlierMergesAndUpdatesHasMore() =
        runTest(main.testDispatcher) {
            val m3 = chatMessage(id = "m3", createdAt = at(3))
            pagedHandler(
                mapOf(
                    1 to listOf(m3),
                    2 to listOf(chatMessage(id = "m2", createdAt = at(2)), m3),
                    3 to listOf(chatMessage(id = "m1", createdAt = at(1))),
                ),
            )
            val vm = h.viewModel()
            runCurrent()
            assertTrue(vm.uiState.value.hasMoreEarlier)
            vm.loadEarlier()
            runCurrent()
            assertEquals(
                listOf("m2", "m3"),
                vm.uiState.value.items
                    .map { it.serverId },
            )
            assertTrue(vm.uiState.value.hasMoreEarlier)
            vm.loadEarlier()
            runCurrent()
            assertEquals(
                listOf("m1", "m2", "m3"),
                vm.uiState.value.items
                    .map { it.serverId },
            )
            assertFalse(vm.uiState.value.hasMoreEarlier)
            assertEquals(listOf(1, 2, 3), h.api.chatMessagesQueries.map { it.second })
        }

    @Test
    fun anOlderPageWithZeroRowsEndsTheHistory() =
        runTest(main.testDispatcher) {
            pagedHandler(mapOf(1 to listOf(chatMessage(id = "m3", createdAt = at(3))), 2 to emptyList()))
            val vm = h.viewModel()
            runCurrent()
            vm.loadEarlier()
            runCurrent()
            assertFalse(vm.uiState.value.hasMoreEarlier)
        }

    @Test
    fun loadEarlierFailureIsSilentAndRetryable() =
        runTest(main.testDispatcher) {
            var failing = true
            h.api.chatMessagesHandler = { _, page, _ ->
                if (page == 2 && failing) throw IOException("offline")
                messagesPayload(
                    listOf(chatMessage(id = "m$page", createdAt = at(10 - page))),
                    currentPage = page,
                    totalPages = 3,
                )
            }
            val vm = h.viewModel()
            runCurrent()
            val before = vm.uiState.value
            vm.loadEarlier()
            runCurrent()
            val after = vm.uiState.value
            assertEquals(before.items, after.items)
            assertTrue(after.hasMoreEarlier)
            assertFalse(after.isLoadingEarlier)
            assertEquals(ThreadLoad.Loaded, after.load)
            failing = false
            vm.loadEarlier()
            runCurrent()
            assertEquals(
                listOf(2, 2),
                h.api.chatMessagesQueries
                    .map { it.second }
                    .filter { it == 2 },
            )
            assertEquals(2, vm.uiState.value.items.size)
        }

    @Test
    fun loadEarlierDoesNothingWithoutMorePages() =
        runTest(main.testDispatcher) {
            h.api.chatMessagesHandler = { _, _, _ -> messagesPayload(listOf(chatMessage())) }
            val vm = h.viewModel()
            runCurrent()
            vm.loadEarlier()
            runCurrent()
            assertEquals(1, h.api.chatMessagesQueries.size)
        }

    @Test
    fun screenShownReloadsPageOneQuietly() =
        runTest(main.testDispatcher) {
            h.api.chatMessagesHandler = { _, _, _ -> messagesPayload(listOf(chatMessage(id = "m1"))) }
            val vm = h.viewModel()
            vm.onScreenShown()
            runCurrent()
            assertEquals(1, h.api.chatMessagesQueries.size)
            h.api.chatMessagesHandler = { _, _, _ ->
                messagesPayload(listOf(chatMessage(id = "m1"), chatMessage(id = "m2", createdAt = at(30))))
            }
            vm.onScreenShown()
            assertEquals(ThreadLoad.Loaded, vm.uiState.value.load)
            runCurrent()
            assertEquals(2, h.api.chatMessagesQueries.size)
            assertEquals(
                listOf("m1", "m2"),
                vm.uiState.value.items
                    .map { it.serverId },
            )
        }

    @Test
    fun pullRefreshShowsIndicatorAndFailureFlagsRefreshFailed() =
        runTest(main.testDispatcher) {
            var failing = false
            h.api.chatMessagesHandler = { _, _, _ ->
                if (failing) throw IOException("offline")
                messagesPayload(listOf(chatMessage()))
            }
            val vm = h.viewModel()
            runCurrent()
            failing = true
            vm.refresh()
            assertTrue(vm.uiState.value.isRefreshing)
            runCurrent()
            assertFalse(vm.uiState.value.isRefreshing)
            assertTrue(vm.uiState.value.refreshFailed)
            assertEquals(1, vm.uiState.value.items.size)
            vm.consumeRefreshFailure()
            assertFalse(vm.uiState.value.refreshFailed)
        }

    @Test
    fun quietFailureKeepsItems() =
        runTest(main.testDispatcher) {
            var failing = false
            h.api.chatMessagesHandler = { _, _, _ ->
                if (failing) throw IOException("offline")
                messagesPayload(listOf(chatMessage()))
            }
            val vm = h.viewModel()
            runCurrent()
            failing = true
            vm.onScreenShown()
            runCurrent()
            assertEquals(ThreadLoad.Loaded, vm.uiState.value.load)
            assertFalse(vm.uiState.value.refreshFailed)
            assertEquals(1, vm.uiState.value.items.size)
        }

    @Test
    fun blockStateReadOnOpenAndOnShown() =
        runTest(main.testDispatcher) {
            h.api.chatMessagesHandler = { _, _, _ -> messagesPayload(emptyList()) }
            h.api.chatsHandler = { _, _ -> chatsPayload(chatThreadItem(isBlocked = true, blockedByMe = true)) }
            val vm = h.viewModel()
            runCurrent()
            assertEquals(ComposerMode.BlockedByMe, vm.uiState.value.composerMode)
            h.api.chatsHandler = { _, _ -> chatsPayload(chatThreadItem(isBlocked = false)) }
            vm.onScreenShown()
            runCurrent()
            assertEquals(ComposerMode.Normal, vm.uiState.value.composerMode)
            assertEquals(listOf(1 to 100, 1 to 100), h.api.chatsQueries)
        }

    @Test
    fun failedScanKeepsTheNormalComposer() =
        runTest(main.testDispatcher) {
            h.api.chatMessagesHandler = { _, _, _ -> messagesPayload(emptyList()) }
            h.api.chatsHandler = { _, _ -> throw IOException("offline") }
            val vm = h.viewModel()
            runCurrent()
            assertEquals(ComposerMode.Normal, vm.uiState.value.composerMode)
        }

    @Test
    fun threadMissingFromScanIsBlockedWhenThisSessionBlockedIt() =
        runTest(main.testDispatcher) {
            h.api.chatMessagesHandler = { _, _, _ -> messagesPayload(emptyList()) }
            h.session.markBlockedByMe("T1")
            val vm = h.viewModel()
            runCurrent()
            assertEquals(ComposerMode.BlockedByMe, vm.uiState.value.composerMode)
        }

    @Test
    fun missingSeedsFilledFromScan() =
        runTest(main.testDispatcher) {
            h.api.chatMessagesHandler = { _, _, _ -> messagesPayload(emptyList()) }
            h.api.chatsHandler = { _, _ ->
                chatsPayload(chatThreadItem(partner = chatPartner(id = "p9", name = "Grace", roles = listOf("expert"))))
            }
            val vm = h.viewModel(ChatThreadKey("t1"))
            runCurrent()
            val state = vm.uiState.value
            assertEquals("Grace", state.peerName)
            assertEquals("p9", state.peerId)
            assertEquals(true, state.peerIsExpert)
        }

    @Test
    fun keySeedsAreNotOverwrittenByTheScan() =
        runTest(main.testDispatcher) {
            h.api.chatMessagesHandler = { _, _, _ -> messagesPayload(emptyList()) }
            h.api.chatsHandler = { _, _ -> chatsPayload(chatThreadItem(partner = chatPartner(name = "Grace"))) }
            val vm = h.viewModel()
            runCurrent()
            assertEquals("Ada Lovelace", vm.uiState.value.peerName)
        }

    @Test
    fun unblockNoticeConsumedForThisThreadOnly() =
        runTest(main.testDispatcher) {
            h.api.chatMessagesHandler = { _, _, _ -> messagesPayload(emptyList()) }
            val vm = h.viewModel()
            runCurrent()
            h.launch.threadNotice.post(ThreadNotice.Unblocked("t2", "Ada"))
            runCurrent()
            vm.onScreenShown()
            runCurrent()
            assertNull(vm.uiState.value.notice)
            assertEquals(ThreadNotice.Unblocked("t2", "Ada"), h.launch.threadNotice.pending.value)
            h.launch.threadNotice.post(ThreadNotice.Unblocked("T1", "Ada"))
            runCurrent()
            assertEquals(ThreadNotice.Unblocked("T1", "Ada"), vm.uiState.value.notice)
            assertNull(h.launch.threadNotice.pending.value)
            vm.consumeNotice()
            assertNull(vm.uiState.value.notice)
        }

    @Test
    fun noticePostedBeforeTheViewModelExistsIsTaken() =
        runTest(main.testDispatcher) {
            h.api.chatMessagesHandler = { _, _, _ -> messagesPayload(emptyList()) }
            h.launch.threadNotice.post(ThreadNotice.Unblocked("t1", null))
            val vm = h.viewModel()
            runCurrent()
            assertEquals(ThreadNotice.Unblocked("t1", null), vm.uiState.value.notice)
        }

    @Test
    fun screenShownAndHiddenTrackTheOpenThread() =
        runTest(main.testDispatcher) {
            h.api.chatMessagesHandler = { _, _, _ -> messagesPayload(emptyList()) }
            val vm = h.viewModel()
            vm.onScreenShown()
            assertEquals("t1", h.session.openThreadId.value)
            vm.onScreenHidden()
            assertNull(h.session.openThreadId.value)
        }
}
