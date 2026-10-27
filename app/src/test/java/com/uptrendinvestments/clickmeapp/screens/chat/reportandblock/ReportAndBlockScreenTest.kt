package com.uptrendinvestments.clickmeapp.screens.chat.reportandblock

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.components.safety.REPORT_DETAILS_FIELD_TAG
import com.uptrendinvestments.clickmeapp.components.safety.REPORT_REASON_ROW_TAG_PREFIX
import com.uptrendinvestments.clickmeapp.model.enums.UserReportReason
import com.uptrendinvestments.clickmeapp.model.requests.ChatActionRequest
import com.uptrendinvestments.clickmeapp.screens.chat.BlockState
import com.uptrendinvestments.clickmeapp.screens.chat.ChatLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.chat.ChatSessionState
import com.uptrendinvestments.clickmeapp.screens.chat.ChatsRepository
import com.uptrendinvestments.clickmeapp.screens.chat.capText
import com.uptrendinvestments.clickmeapp.screens.chat.reportandblock.subviews.BLOCK_BUTTON_TAG
import com.uptrendinvestments.clickmeapp.screens.chat.reportandblock.subviews.REPORT_SUBMIT_TAG
import com.uptrendinvestments.clickmeapp.screens.chat.reportandblock.subviews.REPORT_THANKS_TAG
import com.uptrendinvestments.clickmeapp.screens.chat.reportandblock.subviews.UNBLOCK_BUTTON_TAG
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.chatThreadItem
import com.uptrendinvestments.clickmeapp.testing.chatsPayload
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ReportAndBlockScreenTest {
    @get:Rule(order = 0)
    val main = MainDispatcherRule(UnconfinedTestDispatcher())

    @get:Rule(order = 1)
    val rule = createComposeRule()

    private val api = FakeClickMeApi()

    private fun showScreen() {
        api.chatsHandler = { _, _ -> chatsPayload(chatThreadItem(threadId = "t1")) }
        val viewModel =
            ReportAndBlockViewModel(
                ChatReportBlockKey(threadId = "t1", peerId = "p1", peerName = "Ada"),
                SavedStateHandle(),
                ChatsRepository(api),
                ChatSessionState(),
                { "me" },
                ChatLaunchRequest(),
            )
        rule.setContent {
            ClickMeTheme {
                ReportAndBlockScreen(
                    viewModel = viewModel,
                    onBack = {},
                    onBlocked = {},
                    onUnblocked = {},
                )
            }
        }
    }

    private fun stateDescription(value: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    @Test
    fun submitDisabledUntilReasonChosen() {
        showScreen()

        rule.onNodeWithTag(REPORT_SUBMIT_TAG).performScrollTo().assertIsNotEnabled()
        rule.onNode(stateDescription("Choose a reason first")).assertExists()
        UserReportReason.entries.forEach {
            rule.onNodeWithTag(REPORT_REASON_ROW_TAG_PREFIX + it.name).assertIsNotSelected()
        }
    }

    @Test
    fun reportSendsWireValueAndShowsThanks() {
        showScreen()

        rule.onNodeWithText("Scam or fraud").performScrollTo().performClick()
        rule.onNodeWithTag(REPORT_DETAILS_FIELD_TAG).performScrollTo().performTextInput("  he asked for my card  ")
        val submit = rule.onNodeWithTag(REPORT_SUBMIT_TAG).performScrollTo()
        submit.assertIsEnabled()
        submit.performClick()

        assertEquals(
            listOf("t1" to ChatActionRequest("report", "scam_or_fraud", "he asked for my card")),
            api.chatActions,
        )
        rule.onNodeWithText("Thanks, we'll review this.").performScrollTo().assertExists()
        rule.onNodeWithTag(REPORT_THANKS_TAG).assertExists()
    }

    @Test
    fun detailsCapAndCounter() {
        showScreen()
        val field = rule.onNodeWithTag(REPORT_DETAILS_FIELD_TAG).performScrollTo()

        field.performTextInput("a".repeat(799))
        rule.onNodeWithText("799/1000").assertDoesNotExist()
        field.performTextInput("a")
        rule.onNodeWithText("800/1000").assertExists()
        field.performTextInput("b".repeat(400))
        rule.onNodeWithText("1000/1000").assertExists()
    }

    private fun showContent(state: ReportAndBlockUiState) {
        rule.setContent {
            ClickMeTheme {
                ReportAndBlockContent(
                    state = state,
                    onBack = {},
                    onSelectReason = {},
                    onDetailsChange = {},
                    onSubmitReport = {},
                    onRequestBlock = {},
                    onDismissBlockConfirm = {},
                    onConfirmBlock = {},
                    onUnblock = {},
                )
            }
        }
    }

    @Test
    fun blockedByThePeerShowsTheBannerAndNoButton() {
        showContent(
            ReportAndBlockUiState(peerName = "Ada", block = BlockState(isBlocked = true, blockedByMe = false)),
        )

        rule.onNodeWithText("You can't reply to this conversation").assertExists()
        rule.onNodeWithText("Only the person who blocked", substring = true).assertExists()
        rule.onNodeWithTag(BLOCK_BUTTON_TAG).assertDoesNotExist()
        rule.onNodeWithTag(UNBLOCK_BUTTON_TAG).assertDoesNotExist()
    }

    @Test
    fun blockedByMeOffersUnblockWithTheName() {
        showContent(ReportAndBlockUiState(peerName = "Ada", block = BlockState(isBlocked = true, blockedByMe = true)))

        rule.onNodeWithText("You blocked this conversation").assertExists()
        rule.onNodeWithText("Unblocking lets you and Ada send messages in this conversation again.").assertExists()
        rule.onNodeWithTag(UNBLOCK_BUTTON_TAG).assertExists()
        rule.onNodeWithTag(BLOCK_BUTTON_TAG).assertDoesNotExist()
    }

    @Test
    fun notBlockedOffersBlock() {
        showContent(ReportAndBlockUiState(peerName = "Ada"))

        rule.onNodeWithTag(BLOCK_BUTTON_TAG).assertExists()
        rule.onNodeWithTag(UNBLOCK_BUTTON_TAG).assertDoesNotExist()
    }

    @Test
    fun capTextKeepsSurrogatePairs() {
        val text = "a".repeat(999) + "😀" + "tail"

        val capped = capText(text, 1000)

        assertEquals(999, capped.length)
        assertEquals("a".repeat(999), capped)
        assertEquals("abc", capText("abc", 1000))
    }
}
