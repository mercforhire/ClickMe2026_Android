package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
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

    private fun show(state: ChatThreadUiState) {
        rule.setContent {
            ClickMeTheme {
                ChatThreadContent(
                    state = state,
                    mode = AppMode.Client,
                    zone = zone,
                    locale = Locale.US,
                    timeFormat = timeFormat,
                    onBack = {},
                    onRetry = {},
                    onRefresh = {},
                    onLoadEarlier = { loadEarlierCalls++ },
                    onOpenBooking = { id, terminal -> lastBooking = id to terminal },
                    onViewProfile = {},
                    onOpenReportBlock = {},
                    onRefreshFailureConsumed = {},
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
}
