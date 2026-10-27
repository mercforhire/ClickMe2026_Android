package com.uptrendinvestments.clickmeapp.screens.chat.conversations

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.screens.chat.ChatLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.chat.ChatSessionState
import com.uptrendinvestments.clickmeapp.screens.chat.ChatsRepository
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.ChatThreadKey
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakeRealtimeConnection
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.chatPartner
import com.uptrendinvestments.clickmeapp.testing.chatThreadItem
import com.uptrendinvestments.clickmeapp.testing.chatsPayload
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConversationsScreenTest {
    private val main = MainDispatcherRule(UnconfinedTestDispatcher())
    private val composeRule = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(main).around(composeRule)

    private val rule get() = composeRule

    private val api = FakeClickMeApi()
    private val realtime = FakeRealtimeConnection()
    private val now = Instant.parse("2026-10-27T15:00:00Z")

    private fun vm() =
        ConversationsViewModel(
            SavedStateHandle(),
            ChatsRepository(api),
            realtime,
            ChatSessionState(),
            { "me" },
            ChatLaunchRequest(),
        )

    private fun twoThreads() {
        api.chatsHandler = { _, _ ->
            chatsPayload(
                chatThreadItem(
                    threadId = "t1",
                    partner = chatPartner(id = "p1", name = "Ada Lovelace", roles = listOf("expert")),
                    lastMessage = "See you at 3",
                    lastMsgAt = Instant.parse("2026-10-27T14:00:00Z"),
                ),
                chatThreadItem(
                    threadId = "t2",
                    partner = chatPartner(id = "p2", name = "Grace Hopper", roles = listOf("client")),
                    lastMessage = null,
                    lastMsgAt = null,
                ),
            )
        }
    }

    @Test
    fun rowsRenderFromTheApi() {
        twoThreads()
        rule.setContent {
            ClickMeTheme {
                ConversationsScreen(
                    mode = AppMode.Client,
                    avatarUrl = null,
                    onAvatarClick = {},
                    onOpenThread = {},
                    viewModel = vm(),
                )
            }
        }
        rule.onNodeWithText("Messages").assertExists()
        rule.onNodeWithText("Ada Lovelace").assertExists()
        rule.onNodeWithText("Grace Hopper").assertExists()
        rule.onNodeWithText("See you at 3").assertExists()
        assertTrue(api.chatsQueries.isNotEmpty())
        assertTrue(api.chatsQueries.all { it == 1 to 50 })
    }

    @Test
    fun rowTapOpensThreadWithPeerSeed() {
        twoThreads()
        var opened: ChatThreadKey? = null
        rule.setContent {
            ClickMeTheme {
                ConversationsScreen(
                    mode = AppMode.Client,
                    avatarUrl = null,
                    onAvatarClick = {},
                    onOpenThread = { opened = it },
                    viewModel = vm(),
                )
            }
        }
        rule.onNodeWithText("Ada Lovelace").performClick()
        assertEquals(ChatThreadKey("t1", "p1", "Ada Lovelace", null, true), opened)
    }

    @Test
    fun todayShowsTimeYesterdayShowsWord() {
        val earlier =
            ConversationUi(
                threadId = "t1",
                peerId = "p1",
                name = "Ada Lovelace",
                avatarUrl = null,
                peerIsExpert = true,
                lastMessage = "Today message",
                lastMsgAt = Instant.parse("2026-10-27T09:30:00Z"),
                isOnline = false,
                serverUnread = 0,
                localUnread = 0,
            )
        val yesterday =
            earlier.copy(
                threadId = "t2",
                peerId = "p2",
                name = "Grace Hopper",
                lastMessage = "Old message",
                lastMsgAt = Instant.parse("2026-10-26T20:00:00Z"),
            )
        rule.setContent {
            ClickMeTheme {
                ConversationsContent(
                    state =
                        ConversationsUiState(
                            load = ConversationsLoad.Loaded,
                            conversations = listOf(earlier, yesterday),
                        ),
                    mode = AppMode.Client,
                    avatarUrl = null,
                    now = now,
                    zone = ZoneOffset.UTC,
                    locale = Locale.US,
                    timeFormat = SimpleDateFormat("h:mm a", Locale.US),
                    onAvatarClick = {},
                    onRetry = {},
                    onRefresh = {},
                    onQueryChange = {},
                    onOpen = {},
                    onNoticeConsumed = {},
                    onRefreshFailureConsumed = {},
                )
            }
        }
        assertTrue(rule.onAllNodesWithText("9:30", substring = true).fetchSemanticsNodes().isNotEmpty())
        rule.onNodeWithText("Yesterday").assertExists()
    }
}
