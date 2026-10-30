package com.uptrendinvestments.clickmeapp.screens.chat.conversations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.RefreshFailedSnackbarEffect
import com.uptrendinvestments.clickmeapp.components.avatar.TopBarAvatarButton
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.screens.chat.ConversationsNotice
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.ChatThreadKey
import com.uptrendinvestments.clickmeapp.screens.chat.conversations.subviews.ConversationRow
import com.uptrendinvestments.clickmeapp.screens.chat.conversations.subviews.ConversationsEmptyState
import com.uptrendinvestments.clickmeapp.screens.chat.conversations.subviews.ConversationsError
import com.uptrendinvestments.clickmeapp.screens.chat.conversations.subviews.ConversationsNoResults
import com.uptrendinvestments.clickmeapp.screens.chat.conversations.subviews.ConversationsSearchField
import com.uptrendinvestments.clickmeapp.screens.chat.conversations.subviews.ConversationsSkeleton
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** Test tag on the Conversations list. */
const val CHATS_LIST_TAG = "chats_list"

/**
 * Hilt-aware Conversations tab root (CHAT-01), shared by both shells.
 *
 * Reload triggers (D-17): the screen reports it was shown on resume and on every (re)entry to composition, and hidden
 * on pause and disposal, so the ViewModel can coalesce quiet reloads. The Nav3 entry may not receive ON_RESUME when it
 * is re-shown; it leaves and re-enters composition instead.
 */
@Composable
fun ConversationsScreen(
    mode: AppMode,
    avatarUrl: String?,
    onAvatarClick: () -> Unit,
    onOpenThread: (ChatThreadKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConversationsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onScreenShown() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.onScreenHidden() }
    DisposableEffect(viewModel) { onDispose { viewModel.onScreenHidden() } }
    LaunchedEffect(Unit) { viewModel.onScreenShown() }

    ConversationsContent(
        state = state,
        mode = mode,
        avatarUrl = avatarUrl,
        now = Instant.now(),
        zone = ZoneId.systemDefault(),
        locale = LocalConfiguration.current.locales[0],
        timeFormat = rememberDeviceTimeFormat(),
        onAvatarClick = onAvatarClick,
        onRetry = viewModel::retry,
        onRefresh = viewModel::refresh,
        onQueryChange = viewModel::onQueryChange,
        onOpen = {
            viewModel.onOpen(it)
            onOpenThread(it.toThreadKey())
        },
        onNoticeConsumed = viewModel::consumeNotice,
        onRefreshFailureConsumed = viewModel::consumeRefreshFailure,
        modifier = modifier,
    )
}

/**
 * Stateless Conversations list: tab-root app bar ("Messages" and the profile avatar), pull-to-refresh, and the rows
 * (iOS `ChatConversationsView`). [now], [zone], [locale] and [timeFormat] are parameters so tests pass fixed values.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
fun ConversationsContent(
    state: ConversationsUiState,
    mode: AppMode,
    avatarUrl: String?,
    now: Instant,
    zone: ZoneId,
    locale: Locale,
    timeFormat: DateFormat,
    onAvatarClick: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onQueryChange: (String) -> Unit,
    onOpen: (ConversationUi) -> Unit,
    onNoticeConsumed: () -> Unit,
    onRefreshFailureConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val yesterday = stringResource(R.string.chat_conversations_yesterday)
    val notice = state.notice
    val noticeMessage =
        when (notice) {
            null -> null
            is ConversationsNotice.Blocked ->
                notice.peerName?.takeIf { it.isNotBlank() }?.let {
                    stringResource(R.string.chat_conversations_blocked_snackbar, it)
                } ?: stringResource(R.string.chat_conversations_blocked_snackbar_unnamed)
        }

    LaunchedEffect(notice) {
        if (noticeMessage != null) {
            // Launched on the composition scope so consuming the notice does not cancel the Snackbar.
            scope.launch { snackbarHostState.showSnackbar(noticeMessage, duration = SnackbarDuration.Long) }
            onNoticeConsumed()
        }
    }
    RefreshFailedSnackbarEffect(
        refreshFailed = state.refreshFailed,
        snackbarHostState = snackbarHostState,
        onRetry = onRefresh,
        onShown = onRefreshFailureConsumed,
    )

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.chat_conversations_title),
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = { TopBarAvatarButton(avatarUrl, onClick = onAvatarClick) },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            )
        },
    ) { innerPadding ->
        // The snackbar lives in the content, not in the Scaffold slot: the slot is offset by the system bars only, so
        // while the search field has focus the refresh-failed Retry would sit under the keyboard.
        Box(Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding)) {
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when (val load = state.load) {
                    ConversationsLoad.Loading -> ConversationsSkeleton()
                    is ConversationsLoad.Failed ->
                        ScrollableCentered { ConversationsError(message = load.message, onRetry = onRetry) }
                    ConversationsLoad.Loaded ->
                        if (state.conversations.isEmpty()) {
                            ScrollableCentered { ConversationsEmptyState(mode = mode) }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                ConversationsSearchField(
                                    query = state.query,
                                    onQueryChange = onQueryChange,
                                    modifier =
                                        Modifier
                                            .contentMaxWidth()
                                            .padding(start = Spacing.md, end = Spacing.md, top = Spacing.md),
                                )
                                val visible = state.visible
                                if (visible.isEmpty()) {
                                    ScrollableCentered { ConversationsNoResults() }
                                } else {
                                    ConversationList(
                                        rows = visible,
                                        now = now,
                                        zone = zone,
                                        locale = locale,
                                        timeFormat = timeFormat,
                                        yesterday = yesterday,
                                        onOpen = onOpen,
                                    )
                                }
                            }
                        }
                }
            }
            SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).imePadding())
        }
    }
}

@Composable
private fun ConversationList(
    rows: List<ConversationUi>,
    now: Instant,
    zone: ZoneId,
    locale: Locale,
    timeFormat: DateFormat,
    yesterday: String,
    onOpen: (ConversationUi) -> Unit,
) {
    val navigationBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag(CHATS_LIST_TAG),
        contentPadding =
            PaddingValues(
                start = Spacing.md,
                end = Spacing.md,
                top = Spacing.md,
                bottom = Spacing.xl + navigationBottom,
            ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        items(items = rows, key = { it.threadId }) { row ->
            ConversationRow(
                row = row,
                timeLabel = relativeTimeLabel(row.lastMsgAt, now, zone, locale, timeFormat, yesterday),
                onClick = { onOpen(row) },
                modifier = Modifier.animateItem().contentMaxWidth(),
            )
        }
    }
}

/** Vertically scrollable, centered column so pull-to-refresh works from the empty, no-results and error states. */
@Composable
private fun ScrollableCentered(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}
