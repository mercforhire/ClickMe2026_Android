package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews.CHAT_INPUT_TAG
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews.CHAT_SEND_TAG
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews.ChatComposer
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChatComposerTest {
    @get:Rule
    val rule = createComposeRule()

    private var sends = 0
    private val edits = mutableListOf<String>()

    private fun show(
        text: String,
        enabled: Boolean = true,
        atCap: Boolean = false,
    ) {
        rule.setContent {
            ClickMeTheme {
                ChatComposer(
                    text = text,
                    enabled = enabled,
                    canSend = enabled && text.isNotBlank(),
                    atCap = atCap,
                    onTextChange = { edits += it },
                    onSend = { sends++ },
                    focusRequester = remember { FocusRequester() },
                )
            }
        }
    }

    @Test
    fun sendDisabledWhenBlank() {
        show("   ")

        rule.onNodeWithTag(CHAT_SEND_TAG).assertIsNotEnabled()
    }

    @Test
    fun sendEnabledWhenTextPresentAndClickSends() {
        show("hi")

        rule.onNodeWithTag(CHAT_SEND_TAG).assertIsEnabled()
        rule.onNodeWithTag(CHAT_SEND_TAG).performClick()
        assertEquals(1, sends)
    }

    @Test
    fun disabledComposerRejectsInput() {
        show("", enabled = false)

        rule.onNodeWithTag(CHAT_INPUT_TAG).assertIsNotEnabled()
    }

    @Test
    fun typingReportsEachEdit() {
        show("")

        rule.onNodeWithTag(CHAT_INPUT_TAG).performTextInput("hello")
        assertEquals("hello", edits.last())
    }

    @Test
    fun counterFromNineThousand() {
        show("a".repeat(8_999))
        rule.onNodeWithText("8999/10000").assertDoesNotExist()
    }

    @Test
    fun counterShowsAtNineThousand() {
        show("a".repeat(9_000))
        rule.onNodeWithText("9000/10000").assertExists()
    }

    @Test
    fun counterShowsFullCountAtCap() {
        show("a".repeat(10_000), atCap = true)
        rule.onNodeWithText("10000/10000").assertExists()
    }

    @Test
    fun sendDisabledExplainsWhy() {
        show("")

        rule.onNodeWithTag(CHAT_SEND_TAG).assertIsNotEnabled()
        rule.onNodeWithTag(CHAT_SEND_TAG).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Type a message to send"),
        )
    }

    @Test
    fun sendEnabledHasNoStateDescription() {
        show("hi")

        rule.onNodeWithTag(CHAT_SEND_TAG).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
    }

    @Test
    fun focusStaysAfterSend() {
        var draft by mutableStateOf("")
        rule.setContent {
            ClickMeTheme {
                ChatComposer(
                    text = draft,
                    enabled = true,
                    canSend = draft.isNotBlank(),
                    atCap = false,
                    onTextChange = { draft = it },
                    onSend = { draft = "" },
                    focusRequester = remember { FocusRequester() },
                )
            }
        }

        rule.onNodeWithTag(CHAT_INPUT_TAG).performTextInput("hi")
        rule.onNodeWithTag(CHAT_INPUT_TAG).assertIsFocused()
        rule.onNodeWithTag(CHAT_SEND_TAG).performClick()
        rule.waitForIdle()

        assertEquals("", draft)
        rule.onNodeWithTag(CHAT_INPUT_TAG).assertIsFocused()
    }

    private fun fieldName(): String {
        val config = rule.onNodeWithTag(CHAT_INPUT_TAG).fetchSemanticsNode().config
        val descriptions = config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
        val texts = config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
        return (descriptions + texts).joinToString(" ")
    }

    @Test
    fun fieldNamedMessage() {
        show("")

        assertTrue(fieldName(), fieldName().contains("Message"))
    }

    @Test
    fun fieldKeepsItsNameWithText() {
        show("hello there")

        assertTrue(fieldName(), fieldName().contains("Message"))
    }

    @Test
    fun counterIsNotLiveUntilLimit() {
        show("a".repeat(9_500))

        val below = rule.onNodeWithText("9500/10000").fetchSemanticsNode().config
        assertNull(below.getOrNull(SemanticsProperties.LiveRegion))
    }

    @Test
    fun counterAnnouncesLimitOnce() {
        show("a".repeat(10_000), atCap = true)

        val atCap = rule.onNodeWithText("10000/10000").fetchSemanticsNode().config
        assertEquals(LiveRegionMode.Polite, atCap.getOrNull(SemanticsProperties.LiveRegion))
        assertEquals(
            listOf("Message length limit reached"),
            atCap.getOrNull(SemanticsProperties.ContentDescription),
        )
    }
}
