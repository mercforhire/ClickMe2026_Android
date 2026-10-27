package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
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
                    onLoadEarlier = {},
                    onOpenBooking = { _, _ -> },
                    onViewProfile = {},
                    onOpenReportBlock = {},
                    onRefreshFailureConsumed = {},
                )
            }
        }
    }

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
}
