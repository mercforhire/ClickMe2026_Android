package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.testing.assertActionablesNamed
import com.uptrendinvestments.clickmeapp.testing.assertIsHeading
import com.uptrendinvestments.clickmeapp.testing.assertTouchTargets
import com.uptrendinvestments.clickmeapp.testing.assertTraversalOrder
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

/** SC-4 flow 4 (send a chat message): TalkBack names, order, states and live regions of the thread (POL-04). */
@RunWith(RobolectricTestRunner::class)
class ChatTalkBackFlowTest {
    @get:Rule
    val rule = createComposeRule()

    private fun peerMessage(
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

    private fun ownMessage(
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

    private fun show(vararg items: ThreadItem) {
        val state =
            ChatThreadUiState(
                threadId = "t1",
                peerId = "peer",
                peerName = "Ada",
                peerIsExpert = true,
                load = ThreadLoad.Loaded,
                items = items.toList(),
            )
        rule.setContent {
            ClickMeTheme {
                ChatThreadContent(
                    state = state,
                    mode = AppMode.Client,
                    zone = ZoneOffset.UTC,
                    locale = Locale.US,
                    timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US),
                    onBack = {},
                    onRetry = {},
                    onRefresh = {},
                    onLoadEarlier = {},
                    onOpenBooking = { _, _ -> },
                    onViewProfile = {},
                    onOpenReportBlock = {},
                    onRefreshFailureConsumed = {},
                    onComposerChanged = {},
                    onSend = {},
                    onDismissSendError = {},
                    onNoticeConsumed = {},
                    onRetryMessage = {},
                    onDeleteMessage = {},
                )
            }
        }
    }

    private fun bubble(speakerAndText: String): SemanticsNode =
        rule.onNode(hasContentDescription(speakerAndText, substring = true)).fetchSemanticsNode()

    private fun SemanticsNode.description(): String =
        config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().joinToString(" ")

    @Test
    fun threadReadingOrder() {
        show(peerMessage("m1", "Hi"), ownMessage("m2", "Hello", Delivery.Read))

        rule.assertIsHeading("Ada")
        rule.assertTraversalOrder("Ada", "Ada: Hi", "You: Hello", "Message", "Send message")
    }

    @Test
    fun bubbleIsOneNodeWithStatus() {
        show(
            peerMessage("m1", "Hi"),
            ownMessage("m2", "Hello", Delivery.Read),
            ownMessage("c1", "Not delivered", Delivery.Failed, at = "2026-10-27T09:02:00Z")
                .copy(serverId = null, clientMsgId = "c1"),
        )

        val read = bubble("You: Hello").description()
        assertTrue(read, read.contains("You: Hello") && read.contains("Read"))

        val failed = bubble("You: Not delivered")
        assertTrue(failed.description(), failed.description().contains("Failed to send"))
        val custom = failed.config.getOrNull(SemanticsActions.CustomActions).orEmpty()
        val labels = custom.map { it.label }
        assertEquals("Retry sending", labels.first())
        assertTrue(labels.last().startsWith("Delete"))
    }

    @Test
    fun sentStatusIsSpoken() {
        show(ownMessage("m1", "On it", Delivery.Sent))

        assertTrue(bubble("You: On it").description().contains("Sent"))
    }

    @Test
    fun newestOwnStatusIsPolite() {
        show(
            ownMessage("m1", "First"),
            peerMessage("m2", "Reply", at = "2026-10-27T09:02:00Z"),
            ownMessage("m3", "Second", Delivery.Read, at = "2026-10-27T09:03:00Z"),
        )

        assertNull(bubble("You: First").config.getOrNull(SemanticsProperties.LiveRegion))
        assertNull(bubble("Ada: Reply").config.getOrNull(SemanticsProperties.LiveRegion))
        assertEquals(LiveRegionMode.Polite, bubble("You: Second").config.getOrNull(SemanticsProperties.LiveRegion))
    }

    @Test
    fun controlsNamedAndLargeEnough() {
        show(peerMessage("m1", "Hi"), ownMessage("m2", "Hello", Delivery.Read))

        rule.onNodeWithText("Hello").assertExists()
        rule.assertActionablesNamed()
        rule.assertTouchTargets()
    }
}
