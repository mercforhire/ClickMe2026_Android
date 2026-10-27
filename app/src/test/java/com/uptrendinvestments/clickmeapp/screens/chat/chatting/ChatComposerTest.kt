package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews.CHAT_INPUT_TAG
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews.CHAT_SEND_TAG
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews.ChatComposer
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
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
                    focusRequester = FocusRequester(),
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
}
