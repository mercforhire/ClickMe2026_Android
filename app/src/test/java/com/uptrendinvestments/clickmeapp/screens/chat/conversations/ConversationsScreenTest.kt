package com.uptrendinvestments.clickmeapp.screens.chat.conversations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.screens.chat.ChatLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.chat.ChatSessionState
import com.uptrendinvestments.clickmeapp.screens.chat.ChatsRepository
import com.uptrendinvestments.clickmeapp.screens.chat.ConversationsNotice
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.ChatThreadKey
import com.uptrendinvestments.clickmeapp.screens.chat.conversations.subviews.CHATS_SEARCH_TAG
import com.uptrendinvestments.clickmeapp.screens.chat.conversations.subviews.CONVERSATION_BADGE_TAG
import com.uptrendinvestments.clickmeapp.screens.chat.conversations.subviews.CONVERSATION_PRESENCE_TAG
import com.uptrendinvestments.clickmeapp.screens.chat.conversations.subviews.CONVERSATION_ROW_TAG
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakeRealtimeConnection
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.chatPartner
import com.uptrendinvestments.clickmeapp.testing.chatThreadItem
import com.uptrendinvestments.clickmeapp.testing.chatsPayload
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
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

    private fun showScreen(onOpenThread: (ChatThreadKey) -> Unit = {}) {
        rule.setContent {
            ClickMeTheme {
                ConversationsScreen(
                    mode = AppMode.Client,
                    avatarUrl = null,
                    onAvatarClick = {},
                    onOpenThread = onOpenThread,
                    viewModel = vm(),
                )
            }
        }
    }

    private fun row(
        threadId: String = "t1",
        name: String? = "Ada Lovelace",
        lastMessage: String? = "Hello there",
        lastMsgAt: Instant? = Instant.parse("2026-10-27T09:30:00Z"),
        isOnline: Boolean = false,
        unread: Int = 0,
    ) = ConversationUi(
        threadId = threadId,
        peerId = "p-$threadId",
        name = name,
        avatarUrl = null,
        peerIsExpert = true,
        lastMessage = lastMessage,
        lastMsgAt = lastMsgAt,
        isOnline = isOnline,
        serverUnread = unread,
        localUnread = 0,
    )

    private fun loaded(
        vararg rows: ConversationUi,
        query: String = "",
    ) = ConversationsUiState(load = ConversationsLoad.Loaded, conversations = rows.toList(), query = query)

    private fun show(
        state: ConversationsUiState,
        mode: AppMode = AppMode.Client,
        onQueryChange: (String) -> Unit = {},
        onNoticeConsumed: () -> Unit = {},
        onRefreshFailureConsumed: () -> Unit = {},
        onRetry: () -> Unit = {},
        onRefresh: () -> Unit = {},
    ) {
        rule.setContent {
            ClickMeTheme {
                Content(state, mode, onQueryChange, onNoticeConsumed, onRefreshFailureConsumed, onRetry, onRefresh)
            }
        }
    }

    @Composable
    private fun Content(
        state: ConversationsUiState,
        mode: AppMode,
        onQueryChange: (String) -> Unit,
        onNoticeConsumed: () -> Unit,
        onRefreshFailureConsumed: () -> Unit,
        onRetry: () -> Unit = {},
        onRefresh: () -> Unit = {},
    ) = ConversationsContent(
        state = state,
        mode = mode,
        avatarUrl = null,
        now = now,
        zone = ZoneOffset.UTC,
        locale = Locale.US,
        timeFormat = SimpleDateFormat("h:mm a", Locale.US),
        onAvatarClick = {},
        onRetry = onRetry,
        onRefresh = onRefresh,
        onQueryChange = onQueryChange,
        onOpen = {},
        onNoticeConsumed = onNoticeConsumed,
        onRefreshFailureConsumed = onRefreshFailureConsumed,
    )

    private fun exists(text: String) = rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    private fun existsTag(tag: String) = rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun existsUnmergedTag(tag: String) =
        rule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun rowsRenderFromTheApi() {
        twoThreads()
        showScreen()
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
        showScreen(onOpenThread = { opened = it })
        rule.onNodeWithText("Ada Lovelace").performClick()
        assertEquals(ChatThreadKey("t1", "p1", "Ada Lovelace", null, true), opened)
    }

    @Test
    fun todayShowsTimeYesterdayShowsWord() {
        show(
            loaded(
                row(lastMessage = "Today message", lastMsgAt = Instant.parse("2026-10-27T09:30:00Z")),
                row(threadId = "t2", name = "Grace Hopper", lastMsgAt = Instant.parse("2026-10-26T20:00:00Z")),
            ),
        )
        assertTrue(rule.onAllNodesWithText("9:30", substring = true).fetchSemanticsNodes().isNotEmpty())
        rule.onNodeWithText("Yesterday").assertExists()
    }

    @Test
    fun firstLoadShowsSkeleton() {
        api.chatsHandler = { _, _ -> awaitCancellation() }
        showScreen()
        rule.onNodeWithContentDescription("Loading").assertExists()
        assertFalse(exists("Ada Lovelace"))
        assertFalse(existsTag(CHATS_SEARCH_TAG))
    }

    @Test
    fun failureShowsErrorWithRetry() {
        api.chatsHandler = { _, _ -> throw api.httpError(500, """{"status":"error","message":"Boom"}""") }
        showScreen()
        rule.onNodeWithText("Couldn't load conversations").assertExists()
        rule.onNodeWithText("Boom").assertExists()
        val before = api.chatsQueries.size
        rule.onNodeWithText("Retry").performClick()
        assertTrue(api.chatsQueries.size > before)
    }

    @Test
    fun emptyClientBodyHidesTheSearchField() {
        show(loaded(), mode = AppMode.Client)
        rule.onNodeWithText("No messages yet").assertExists()
        rule
            .onNodeWithText("Message an expert from their profile or one of your bookings to start a conversation.")
            .assertExists()
        assertFalse(existsTag(CHATS_SEARCH_TAG))
    }

    @Test
    fun emptyExpertBodyHidesTheSearchField() {
        show(loaded(), mode = AppMode.Expert)
        rule.onNodeWithText("No messages yet").assertExists()
        rule.onNodeWithText("Message a client from a booking or request to start a conversation.").assertExists()
        assertFalse(existsTag(CHATS_SEARCH_TAG))
    }

    @Test
    fun noResultsKeepsSearchField() {
        show(loaded(row(), query = "zzz"))
        rule.onNodeWithText("No conversations found").assertExists()
        rule.onNodeWithText("Try a different name or keyword.").assertExists()
        assertTrue(existsTag(CHATS_SEARCH_TAG))
        assertFalse(exists("Ada Lovelace"))
    }

    @Test
    fun clearSearchButtonClearsQuery() {
        var state by mutableStateOf(loaded(row()))
        rule.setContent {
            ClickMeTheme {
                Content(
                    state = state,
                    mode = AppMode.Client,
                    onQueryChange = { state = state.copy(query = it) },
                    onNoticeConsumed = {},
                    onRefreshFailureConsumed = {},
                )
            }
        }
        assertFalse(rule.onAllNodesWithContentDescription("Clear search").fetchSemanticsNodes().isNotEmpty())
        rule.onNodeWithTag(CHATS_SEARCH_TAG).performTextInput("ada")
        assertEquals("ada", state.query)
        rule.onNodeWithContentDescription("Clear search").performClick()
        assertEquals("", state.query)
    }

    @Test
    fun badgeHiddenAtZeroAndCapped() {
        show(
            loaded(
                row(threadId = "t1", name = "Zero", unread = 0),
                row(threadId = "t2", name = "Three", unread = 3),
                row(threadId = "t3", name = "Many", unread = 120),
            ),
        )
        val badges = rule.onAllNodesWithTag(CONVERSATION_BADGE_TAG, useUnmergedTree = true)
        assertEquals(2, badges.fetchSemanticsNodes().size)
        assertTrue(exists("3"))
        assertTrue(exists("99+"))
        assertFalse(exists("0"))
    }

    @Test
    fun rowSemanticsIncludeUnreadAndOnline() {
        show(loaded(row(lastMessage = "See you at 3", unread = 3, isOnline = true)))
        val description =
            rule
                .onNodeWithTag(CONVERSATION_ROW_TAG)
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.ContentDescription)
                .orEmpty()
                .joinToString()
        listOf("Ada Lovelace", "3 unread", "See you at 3", "9:30 AM", "online").forEach {
            assertTrue("missing $it in: $description", description.contains(it))
        }
        assertTrue(existsUnmergedTag(CONVERSATION_PRESENCE_TAG))
    }

    @Test
    fun missingPartsAreHandled() {
        show(loaded(row(name = null, lastMessage = null, lastMsgAt = null)))
        rule.onNodeWithText("Chat").assertExists()
        assertFalse(exists("Hello there"))
        assertFalse(existsUnmergedTag(CONVERSATION_BADGE_TAG))
    }

    @Test
    fun refreshFailureSnackbarShowsAndIsConsumed() {
        var consumed = 0
        show(loaded(row()).copy(refreshFailed = true), onRefreshFailureConsumed = { consumed++ })
        rule.onNodeWithText("Couldn't refresh. Check your connection and try again.").assertExists()
        assertEquals(1, consumed)
    }

    @Test
    fun blockedSnackbarShowsOnce() {
        var consumed = 0
        show(loaded(row()).copy(notice = ConversationsNotice.Blocked("Ada")), onNoticeConsumed = { consumed++ })
        rule.onNodeWithText("Blocked Ada").assertExists()
        assertEquals(1, consumed)
    }

    @Test
    fun refreshFailedShowsRetryThatRefreshes() {
        var refreshes = 0
        var retries = 0
        show(
            loaded(row()).copy(refreshFailed = true),
            onRetry = { retries++ },
            onRefresh = { refreshes++ },
        )
        rule.onNodeWithText("Couldn't refresh. Check your connection and try again.").assertExists()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, refreshes)
        assertEquals(0, retries)
        rule.onNodeWithText("Ada Lovelace").assertExists()
    }

    @Test
    fun firstLoadErrorNamedWithRetry() {
        var retries = 0
        show(
            ConversationsUiState(load = ConversationsLoad.Failed(FieldError.Text("Boom"))),
            onRetry = { retries++ },
        )
        rule.onNodeWithText("Couldn't load conversations").assertExists()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }

    @Test
    @Config(qualifiers = "w900dp-h600dp")
    fun listCapped() {
        show(loaded(row()))
        val bounds = rule.onNodeWithTag(CONVERSATION_ROW_TAG).getUnclippedBoundsInRoot()
        val width = bounds.right.value - bounds.left.value
        assertTrue("row is $width dp wide", width <= LIST_COLUMN_MAX)
    }

    private companion object {
        /** The 600 dp column, with half a dp of tolerance. */
        const val LIST_COLUMN_MAX = 600.5f
    }
}
