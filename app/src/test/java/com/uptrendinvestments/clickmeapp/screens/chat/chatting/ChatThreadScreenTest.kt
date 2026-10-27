package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.screens.chat.BlockState
import com.uptrendinvestments.clickmeapp.screens.chat.ThreadNotice
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews.CHAT_INPUT_TAG
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews.CHAT_SEND_TAG
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class ChatThreadScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val zone = ZoneOffset.UTC
    private val timeFormat: DateFormat = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US)

    private fun incoming(
        id: String,
        text: String,
        at: String = "2026-10-27T09:00:00Z",
    ) = ThreadItem.Message(
        serverId = id,
        clientMsgId = null,
        senderId = "peer",
        text = text,
        createdAt = Instant.parse(at),
        mine = false,
        delivery = Delivery.Sent,
    )

    private fun own(
        id: String,
        text: String,
        delivery: Delivery = Delivery.Sent,
        at: String = "2026-10-27T09:01:00Z",
    ) = ThreadItem.Message(
        serverId = id,
        clientMsgId = null,
        senderId = "me",
        text = text,
        createdAt = Instant.parse(at),
        mine = true,
        delivery = delivery,
    )

    private fun loaded(
        items: List<ThreadItem>,
        peerName: String? = "Ada Lovelace",
    ) = ChatThreadUiState(
        threadId = "t1",
        peerId = "peer",
        peerName = peerName,
        peerIsExpert = true,
        load = ThreadLoad.Loaded,
        items = items,
    )

    private var lastBooking: Pair<String, Boolean>? = null
    private var loadEarlierCalls = 0
    private var retryCalls = 0
    private var reportBlockCalls = 0
    private var viewProfileCalls = 0
    private var refreshFailureConsumed = 0
    private val composerEdits = mutableListOf<String>()
    private var sendCalls = 0
    private var dismissCalls = 0
    private var noticeConsumed = 0
    private val retried = mutableListOf<String>()
    private val deleted = mutableListOf<String>()
    private var shown by mutableStateOf(ChatThreadUiState(threadId = "t1"))

    private fun show(
        state: ChatThreadUiState,
        mode: AppMode = AppMode.Client,
    ) {
        shown = state
        rule.setContent {
            ClickMeTheme {
                ChatThreadContent(
                    state = shown,
                    mode = mode,
                    zone = zone,
                    locale = Locale.US,
                    timeFormat = timeFormat,
                    onBack = {},
                    onRetry = { retryCalls++ },
                    onRefresh = {},
                    onLoadEarlier = { loadEarlierCalls++ },
                    onOpenBooking = { id, terminal -> lastBooking = id to terminal },
                    onViewProfile = { viewProfileCalls++ },
                    onOpenReportBlock = { reportBlockCalls++ },
                    onRefreshFailureConsumed = { refreshFailureConsumed++ },
                    onComposerChanged = { composerEdits += it },
                    onSend = { sendCalls++ },
                    onDismissSendError = { dismissCalls++ },
                    onNoticeConsumed = {
                        noticeConsumed++
                        shown = shown.copy(notice = null)
                    },
                    onRetryMessage = { retried += it },
                    onDeleteMessage = { deleted += it },
                )
            }
        }
    }

    private fun event(
        id: String = "e1",
        kind: SystemEventKind = SystemEventKind.BookingCancelled,
        bookingId: String? = "b1",
        topic: String? = "Algorithms",
        startTime: Instant? = Instant.parse("2026-10-28T14:00:00Z"),
        content: String? = null,
        at: String = "2026-10-27T09:02:00Z",
    ) = ThreadItem.SystemEvent(
        serverId = id,
        kind = kind,
        bookingId = bookingId,
        topic = topic,
        startTime = startTime,
        content = content,
        createdAt = Instant.parse(at),
    )

    @Test
    fun messagesRenderWithPeerInTopBar() {
        show(loaded(listOf(incoming("m1", "Hello there"), own("m2", "Hi Ada", Delivery.Read))))

        rule.onNodeWithText("Ada Lovelace").assertIsDisplayed()
        rule.onNodeWithText("Hello there").assertIsDisplayed()
        rule.onNodeWithText("Hi Ada").assertIsDisplayed()
        rule.onNodeWithText("Read").assertIsDisplayed()
    }

    @Test
    fun peerAndOwnRowsSharingAClientMsgIdBothRender() {
        show(
            loaded(
                listOf(
                    own("m1", "Mine").copy(clientMsgId = "dup"),
                    incoming("m2", "Theirs").copy(clientMsgId = "dup"),
                ),
            ),
        )

        rule.onNodeWithText("Mine").assertIsDisplayed()
        rule.onNodeWithText("Theirs").assertIsDisplayed()
    }

    @Test
    fun sendingBubbleShowsSendingInsteadOfTime() {
        show(loaded(listOf(own("m1", "On my way", Delivery.Sending))))

        rule.onNodeWithText("Sending…").assertIsDisplayed()
    }

    @Test
    fun listStartsAtNewest() {
        val items =
            (1..40).map { i ->
                incoming("m$i", "Message number $i", at = "2026-10-27T09:%02d:00Z".format(i))
            }
        show(loaded(items))

        rule.onNodeWithText("Message number 40").assertIsDisplayed()
        rule.onNodeWithText("Message number 1").assertDoesNotExist()
    }

    @Test
    fun blankNameFallsBackToChat() {
        show(loaded(listOf(incoming("m1", "Hello")), peerName = null))

        rule.onNodeWithText("Chat").assertIsDisplayed()
    }

    @Test
    fun systemEventWithBookingIsTappable() {
        show(
            loaded(
                listOf(
                    event(id = "e1", kind = SystemEventKind.BookingCancelled),
                    event(id = "e2", kind = SystemEventKind.BookingConfirmed, bookingId = "b2", topic = "Calculus"),
                ),
            ),
        )

        rule.onNodeWithText("Booking Cancelled").assertIsDisplayed()
        rule.onNodeWithText("Algorithms · ", substring = true).assertExists()
        rule.onNodeWithText("Booking Cancelled").performClick()
        assertEquals("b1" to true, lastBooking)
        rule.onNodeWithText("Booking Confirmed").performClick()
        assertEquals("b2" to false, lastBooking)
    }

    @Test
    fun legacyEventWithoutBookingIsNotTappable() {
        show(
            loaded(
                listOf(
                    event(
                        bookingId = null,
                        topic = null,
                        startTime = null,
                        content = "Tomorrow, 2:00 PM",
                        kind = SystemEventKind.BookingRequest,
                    ),
                ),
            ),
        )

        rule.onNodeWithText("Tomorrow, 2:00 PM").assertIsDisplayed()
        rule.onNodeWithText("Booking Request").assertHasNoClickAction()
    }

    @Test
    fun loadEarlierShownWhenMorePages() {
        show(loaded(listOf(incoming("m1", "Hello"))).copy(hasMoreEarlier = true))

        rule.onNodeWithText("Load earlier").performClick()
        assertEquals(1, loadEarlierCalls)
    }

    @Test
    fun loadingEarlierShowsLoadingAndIsDisabled() {
        show(loaded(listOf(incoming("m1", "Hello"))).copy(hasMoreEarlier = true, isLoadingEarlier = true))

        rule.onNodeWithText("Loading…").assertIsNotEnabled()
    }

    @Test
    fun dateSeparatorsShowTodayAndYesterday() {
        val now = Instant.now()
        show(
            loaded(
                listOf(
                    incoming("m0", "Old news", at = "2020-01-02T10:00:00Z"),
                    incoming("m1", "Yesterday text", at = now.minusSeconds(86_400).toString()),
                    incoming("m2", "Today text", at = now.toString()),
                ),
            ),
        )

        rule.onNodeWithText("Today").assertExists()
        rule.onNodeWithText("Yesterday").assertExists()
    }

    private fun manyMessages(count: Int) =
        (1..count).map { i ->
            incoming(
                "m$i",
                "Message number $i",
                at = Instant.parse("2026-10-27T10:00:00Z").plusSeconds(i * 120L).toString(),
            )
        }

    @Test
    fun loadingShowsSkeleton() {
        show(ChatThreadUiState(threadId = "t1", peerName = "Ada Lovelace", load = ThreadLoad.Loading))

        rule.onNodeWithContentDescription("Loading").assertExists()
        rule.onAllNodesWithTag("skeleton_block", useUnmergedTree = true).assertCountEquals(5)
    }

    @Test
    fun failedShowsErrorAndRetry() {
        show(
            ChatThreadUiState(
                threadId = "t1",
                load = ThreadLoad.Failed(FieldError.Text("Server is down")),
            ),
        )

        rule.onNodeWithText("Couldn't load messages").assertIsDisplayed()
        rule.onNodeWithText("Server is down").assertIsDisplayed()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, retryCalls)
    }

    @Test
    fun emptyThreadShowsHeading() {
        show(loaded(emptyList()))

        rule.onNodeWithText("Start your conversation here.").assertIsDisplayed()
        rule.onNodeWithText("Say hello!").assertIsDisplayed()
    }

    @Test
    fun typingStripShowsPeerName() {
        show(loaded(listOf(incoming("m1", "Hello"))).copy(peerTyping = true))

        rule.onNodeWithText("Ada Lovelace is typing…").assertIsDisplayed()
    }

    @Test
    fun menuItemsFollowBlockState() {
        val base = loaded(listOf(incoming("m1", "Hello")))
        show(base)
        rule.onNodeWithContentDescription("More options").performClick()
        rule.onNodeWithText("Block or report").assertIsDisplayed()
        rule.onNodeWithText("Unblock or report").assertDoesNotExist()
        rule.onNodeWithText("Block or report").performClick()
        assertEquals(1, reportBlockCalls)

        shown = base.copy(block = BlockState(isBlocked = true, blockedByMe = true))
        rule.onNodeWithContentDescription("More options").performClick()
        rule.onNodeWithText("Unblock or report").assertIsDisplayed()
        rule.onNodeWithText("Block or report").assertDoesNotExist()
        rule.onNodeWithText("Unblock or report").performClick()
        assertEquals(2, reportBlockCalls)

        shown = base.copy(block = BlockState(isBlocked = true, blockedByMe = false))
        rule.onNodeWithContentDescription("More options").performClick()
        rule.onNodeWithText("Report").assertIsDisplayed()
        rule.onNodeWithText("Block or report").assertDoesNotExist()
        rule.onNodeWithText("Unblock or report").assertDoesNotExist()
    }

    @Test
    fun viewProfileShowsForExpertPeerInClientMode() {
        show(loaded(listOf(incoming("m1", "Hello"))))
        rule.onNodeWithContentDescription("More options").performClick()

        rule.onNodeWithText("View profile").assertIsDisplayed()
        rule.onNodeWithText("View profile").performClick()
        assertEquals(1, viewProfileCalls)
    }

    @Test
    fun viewProfileIsDisabledUntilThePeerIdIsKnown() {
        show(loaded(listOf(incoming("m1", "Hello"))).copy(peerId = null))
        rule.onNodeWithContentDescription("More options").performClick()

        rule.onNodeWithText("View profile").assertIsNotEnabled()
    }

    @Test
    fun viewProfileIsAbsentInExpertMode() {
        show(loaded(listOf(incoming("m1", "Hello"))), mode = AppMode.Expert)
        rule.onNodeWithContentDescription("More options").performClick()

        rule.onNodeWithText("View profile").assertDoesNotExist()
        rule.onNodeWithText("Block or report").assertIsDisplayed()
    }

    @Test
    fun selfThreadHidesMenu() {
        show(loaded(listOf(incoming("m1", "Hello"))).copy(isSelfThread = true), mode = AppMode.Expert)

        rule.onNodeWithContentDescription("More options").assertDoesNotExist()
    }

    @Test
    fun jumpToLatestAppearsWhenScrolledAway() {
        show(loaded(manyMessages(60)))
        rule.onNodeWithContentDescription("Jump to latest message").assertDoesNotExist()

        rule.onNodeWithTag(CHAT_THREAD_LIST_TAG).performScrollToIndex(60)
        rule.onNodeWithContentDescription("Jump to latest message").assertIsDisplayed()
        rule.onNodeWithText("Message number 60").assertDoesNotExist()

        rule.onNodeWithContentDescription("Jump to latest message").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Message number 60").assertIsDisplayed()
    }

    @Test
    fun incomingMessageAutoScrollsWhenAtTheNewestEnd() {
        val base = loaded(manyMessages(20))
        show(base)
        rule.onNodeWithText("Message number 20").assertIsDisplayed()

        shown = base.copy(items = base.items + incoming("m21", "Fresh arrival", at = "2026-10-27T11:00:00Z"))
        rule.waitForIdle()

        rule.onNodeWithText("Fresh arrival").assertIsDisplayed()
    }

    @Test
    fun incomingMessageDoesNotMoveAReaderOfOldHistory() {
        val base = loaded(manyMessages(60))
        show(base)
        rule.onNodeWithTag(CHAT_THREAD_LIST_TAG).performScrollToIndex(60)

        shown = base.copy(items = base.items + incoming("m61", "Fresh arrival", at = "2026-10-27T13:00:00Z"))
        rule.waitForIdle()

        rule.onNodeWithText("Fresh arrival").assertDoesNotExist()
        rule.onNodeWithContentDescription("Jump to latest message").assertIsDisplayed()
    }

    @Test
    fun refreshFailureShowsSnackbarAndIsConsumed() {
        show(loaded(listOf(incoming("m1", "Hello"))).copy(refreshFailed = true))

        rule.onNodeWithText("Couldn't refresh. Check your connection and try again.").assertIsDisplayed()
        assertTrue(refreshFailureConsumed >= 1)
    }

    @Test
    fun composerShowsWhenLoadedAndBindsSend() {
        show(loaded(listOf(incoming("m1", "Hello"))).copy(composerText = "On my way"))

        rule.onNodeWithTag(CHAT_INPUT_TAG).assertIsEnabled()
        rule.onNodeWithTag(CHAT_INPUT_TAG).performTextInput("!")
        assertTrue(composerEdits.isNotEmpty())
        rule.onNodeWithTag(CHAT_SEND_TAG).performClick()
        assertEquals(1, sendCalls)
    }

    @Test
    fun composerShowsPlaceholderWhenEmpty() {
        show(loaded(listOf(incoming("m1", "Hello"))))

        rule.onNodeWithText("Message").assertIsDisplayed()
        rule.onNodeWithTag(CHAT_SEND_TAG).assertIsNotEnabled()
    }

    @Test
    fun composerDisabledWhileLoadingOrFailed() {
        show(ChatThreadUiState(threadId = "t1", load = ThreadLoad.Loading))
        rule.onNodeWithTag(CHAT_INPUT_TAG).assertIsNotEnabled()
        rule.onNodeWithTag(CHAT_SEND_TAG).assertIsNotEnabled()

        shown = ChatThreadUiState(threadId = "t1", load = ThreadLoad.Failed(FieldError.Text("Down")))
        rule.onNodeWithTag(CHAT_INPUT_TAG).assertIsNotEnabled()
    }

    @Test
    fun ownSendingBubbleVisibleAtBottom() {
        val base = loaded(manyMessages(20))
        show(base)

        shown = base.copy(items = base.items + own("c1", "Just sent", Delivery.Sending, at = "2026-10-27T12:00:00Z"))
        rule.waitForIdle()

        rule.onNodeWithText("Just sent").assertIsDisplayed()
        rule.onNodeWithText("Sending…").assertIsDisplayed()
    }

    private fun failed(id: String = "c1") =
        own("", "Not delivered", Delivery.Failed).copy(serverId = null, clientMsgId = id)

    @Test
    fun failedBubbleShowsRetryLineAndRetriesOnTap() {
        show(loaded(listOf(failed("c1"))))

        rule.onNodeWithText("Not sent. Tap to retry").assertIsDisplayed()
        rule.onNodeWithText("Not sent. Tap to retry").performClick()
        assertEquals(listOf("c1"), retried)
    }

    @Test
    fun longPressOffersDelete() {
        show(loaded(listOf(failed("c1"))))

        rule.onNodeWithText("Not delivered").performTouchInput { longClick() }
        rule.onNodeWithText("Delete").assertIsDisplayed()
        rule.onNodeWithText("Delete").performClick()
        assertEquals(listOf("c1"), deleted)
        assertTrue(retried.isEmpty())
    }

    @Test
    fun failedBubbleCustomActions() {
        show(loaded(listOf(failed("c1"))))

        val node = rule.onNodeWithText("Not sent. Tap to retry").fetchSemanticsNode()
        val labels = node.config.getOrNull(SemanticsActions.CustomActions)?.map { it.label }
        assertEquals(listOf("Retry sending", "Delete message"), labels)
        val delete = node.config.getOrNull(SemanticsActions.CustomActions)!!.last()
        rule.runOnUiThread { delete.action() }
        assertEquals(listOf("c1"), deleted)
    }

    @Test
    fun sentBubbleIsNotClickable() {
        show(loaded(listOf(own("m1", "All good"))))

        rule.onNodeWithText("All good").assertHasNoClickAction()
        rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions)).assertDoesNotExist()
    }

    @Test
    fun bannerForBlockedByMeReplacesComposer() {
        show(
            loaded(listOf(incoming("m1", "Earlier words")))
                .copy(block = BlockState(isBlocked = true, blockedByMe = true)),
        )

        rule.onNodeWithText("You blocked this conversation").assertIsDisplayed()
        rule.onNodeWithText("Open the menu to unblock.").assertIsDisplayed()
        rule.onNodeWithTag(CHAT_INPUT_TAG).assertDoesNotExist()
        rule.onNodeWithTag(CHAT_SEND_TAG).assertDoesNotExist()
        rule.onNodeWithText("Earlier words").assertIsDisplayed()
    }

    @Test
    fun bannerForBlockedByPeerHasNoHint() {
        show(
            loaded(listOf(incoming("m1", "Earlier words")))
                .copy(block = BlockState(isBlocked = true, blockedByMe = false)),
        )

        rule.onNodeWithText("You can't reply to this conversation").assertIsDisplayed()
        rule.onNodeWithText("Open the menu to unblock.").assertDoesNotExist()
        rule.onNodeWithText("Unblock", substring = true).assertDoesNotExist()
        rule.onNodeWithTag(CHAT_INPUT_TAG).assertDoesNotExist()
    }

    @Test
    fun bannerForUnavailableRecipient() {
        show(loaded(listOf(incoming("m1", "Earlier words"))).copy(recipientUnavailable = true))

        rule.onNodeWithText("This user is unavailable.").assertIsDisplayed()
        rule.onNodeWithTag(CHAT_INPUT_TAG).assertDoesNotExist()
        rule.onNodeWithText("Earlier words").assertIsDisplayed()
    }

    @Test
    fun composerReturnsWhenUnblocked() {
        val base = loaded(listOf(incoming("m1", "Hello")))
        show(base.copy(block = BlockState(isBlocked = true, blockedByMe = true)))
        rule.onNodeWithText("You blocked this conversation").assertIsDisplayed()

        shown = base
        rule.waitForIdle()

        rule.onNodeWithTag(CHAT_INPUT_TAG).assertIsDisplayed()
        rule.onNodeWithText("You blocked this conversation").assertDoesNotExist()
    }

    @Test
    fun sendErrorShowsDialog() {
        show(loaded(listOf(incoming("m1", "Hello"))).copy(sendError = FieldError.Text("Validation failed")))

        rule.onNodeWithText("Couldn't send message").assertIsDisplayed()
        rule.onNodeWithText("Validation failed").assertIsDisplayed()
        rule.onNodeWithText("OK").performClick()
        assertEquals(1, dismissCalls)
    }

    @Test
    fun unblockedNoticeShowsSnackbarOnce() {
        show(loaded(listOf(incoming("m1", "Hello"))).copy(notice = ThreadNotice.Unblocked("t1", "Ada")))
        rule.waitForIdle()

        rule.onNodeWithText("Unblocked Ada").assertIsDisplayed()
        assertEquals(1, noticeConsumed)
    }

    @Test
    fun emptyThreadSendMessageFocusesComposer() {
        show(loaded(emptyList()))

        rule.onNodeWithText("Send a message").performClick()
        rule.waitForIdle()

        rule.onNodeWithTag(CHAT_INPUT_TAG).assertIsFocused()
    }
}
