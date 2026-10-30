package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews.CHAT_INPUT_TAG
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews.CHAT_SEND_TAG
import com.uptrendinvestments.clickmeapp.testing.dispatchInsets
import com.uptrendinvestments.clickmeapp.testing.imeInsets
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

/** The chat composer under the keyboard and on a large window (POL-03; CONTEXT D-02, D-17). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class ChatComposerInsetsTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var view: View

    private fun message(
        index: Int,
        mine: Boolean,
    ) = ThreadItem.Message(
        serverId = "m$index",
        clientMsgId = null,
        senderId = if (mine) "me" else "peer",
        text = "Message number $index",
        createdAt = Instant.parse("2026-10-27T10:00:00Z").plusSeconds(index * 120L),
        mine = mine,
        delivery = Delivery.Sent,
    )

    private fun show() {
        val state =
            ChatThreadUiState(
                threadId = "t1",
                peerId = "peer",
                peerName = "Ada Lovelace",
                peerIsExpert = true,
                load = ThreadLoad.Loaded,
                items = (1..40).map { message(it, mine = it % 2 == 0) },
            )
        rule.setContent {
            view = LocalView.current
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

    @Test
    fun composerAboveIme() {
        show()
        val before =
            rule
                .onNodeWithTag(CHAT_SEND_TAG)
                .getUnclippedBoundsInRoot()
                .bottom.value

        rule.dispatchInsets(view, imeInsets(bottomPx = 300))

        val send = rule.onNodeWithTag(CHAT_SEND_TAG).getUnclippedBoundsInRoot()
        val field = rule.onNodeWithTag(CHAT_INPUT_TAG).getUnclippedBoundsInRoot()
        assertTrue("Send bottom ${send.bottom.value} must clear the IME (was $before)", send.bottom.value <= IME_TOP)
        assertTrue("Field bottom ${field.bottom.value} must clear the IME", field.bottom.value <= IME_TOP)
        rule.onNodeWithTag(CHAT_THREAD_LIST_TAG).performScrollToIndex(0)
        rule.onNodeWithTag(CHAT_THREAD_LIST_TAG).assertExists()
    }

    @Test
    @Config(qualifiers = "w900dp-h600dp")
    fun threadCappedOnLargeWindow() {
        show()

        val field = rule.onNodeWithTag(CHAT_INPUT_TAG).getUnclippedBoundsInRoot()
        val fieldWidth = field.right.value - field.left.value
        assertTrue("Composer field is $fieldWidth dp wide", fieldWidth <= 600f)
        val own = rule.onNodeWithText("Message number 40").getUnclippedBoundsInRoot()
        assertTrue("Newest own bubble ends at ${own.right.value} dp", own.right.value <= COLUMN_END)
    }

    private companion object {
        /** 800 dp window less the 300 px IME, with half a pixel of tolerance. */
        const val IME_TOP = 500.5f

        /** The 600 dp column is centered in a 900 dp window, so it ends at 750 dp. */
        const val COLUMN_END = 750.5f
    }
}
