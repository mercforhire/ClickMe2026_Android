package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews.ChatComposer
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews.ChatEmptyThread
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews.ChatThreadError
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews.ChatThreadSkeleton
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews.DateSeparator
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews.JumpToLatestButton
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews.LoadEarlierButton
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews.MessageBubble
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews.SystemEventRow
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews.ThreadOverflowMenu
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews.TypingStrip
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatShortDate
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatSlotTime
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** Test tag on the thread message list. */
const val CHAT_THREAD_LIST_TAG = "chat_thread_list"

private val TopBarAvatarSize = 32.dp
private const val LOAD_EARLIER_KEY = "load_earlier"

/** The key this thread was opened with, rebuilt from the live state so View profile and Report use the filled-in peer. */
fun ChatThreadUiState.toThreadKey(): ChatThreadKey =
    ChatThreadKey(
        threadId = threadId,
        peerId = peerId,
        peerName = peerName,
        peerAvatarUrl = peerAvatarUrl,
        peerIsExpert = peerIsExpert,
    )

/**
 * Hilt-aware chat thread screen (CHAT-02, CHAT-04): binds [viewModel] and reports the screen shown and hidden on
 * resume, pause, entry to and disposal of composition (the Nav3 entry may not receive ON_RESUME when re-shown).
 */
@Composable
fun ChatThreadScreen(
    viewModel: ChatThreadViewModel,
    mode: AppMode,
    onBack: () -> Unit,
    onOpenBooking: (bookingId: String, isTerminal: Boolean) -> Unit,
    onViewProfile: () -> Unit,
    onOpenReportBlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onScreenShown() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.onScreenHidden() }
    DisposableEffect(viewModel) { onDispose { viewModel.onScreenHidden() } }
    LaunchedEffect(Unit) { viewModel.onScreenShown() }

    ChatThreadContent(
        state = state,
        mode = mode,
        zone = ZoneId.systemDefault(),
        locale = LocalConfiguration.current.locales[0],
        timeFormat = rememberDeviceTimeFormat(),
        onBack = onBack,
        onRetry = viewModel::retry,
        onRefresh = viewModel::refresh,
        onLoadEarlier = viewModel::loadEarlier,
        onOpenBooking = onOpenBooking,
        onViewProfile = onViewProfile,
        onOpenReportBlock = onOpenReportBlock,
        onRefreshFailureConsumed = viewModel::consumeRefreshFailure,
        onComposerChanged = viewModel::onComposerChanged,
        onSend = viewModel::send,
        modifier = modifier,
    )
}

/**
 * Stateless thread: top bar with the peer and the overflow menu, the message list (oldest to newest, starting at the
 * newest message via `reverseLayout`), the typing strip and the composer. The first load shows
 * a skeleton, a failed load an error with Retry, and a loaded empty thread the empty state. [zone], [locale] and
 * [timeFormat] are parameters so tests pass fixed values.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
fun ChatThreadContent(
    state: ChatThreadUiState,
    mode: AppMode,
    zone: ZoneId,
    locale: Locale,
    timeFormat: DateFormat,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onLoadEarlier: () -> Unit,
    onOpenBooking: (bookingId: String, isTerminal: Boolean) -> Unit,
    onViewProfile: () -> Unit,
    onOpenReportBlock: () -> Unit,
    onRefreshFailureConsumed: () -> Unit,
    onComposerChanged: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val refreshFailedMessage = stringResource(R.string.chat_thread_refresh_failed)
    val peerLabel = state.peerName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.chat_thread_title_fallback)
    LaunchedEffect(state.refreshFailed) {
        if (state.refreshFailed) {
            // Launched on the composition scope so consuming the flag does not cancel the Snackbar.
            scope.launch { snackbarHostState.showSnackbar(refreshFailedMessage, duration = SnackbarDuration.Long) }
            onRefreshFailureConsumed()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            ChatThreadTopBar(state = state, onBack = onBack) {
                ThreadOverflowMenu(
                    entry = state.menuEntry,
                    showViewProfile = mode == AppMode.Client && state.peerIsExpert == true,
                    viewProfileEnabled = state.peerId != null,
                    showSafetyItem = !state.isSelfThread,
                    onViewProfile = onViewProfile,
                    onReportBlock = onOpenReportBlock,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        // The Scaffold padding already holds the system-bar insets; consuming them stops imePadding adding them twice.
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding)) {
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                when (val load = state.load) {
                    ThreadLoad.Loading -> ChatThreadSkeleton()
                    is ThreadLoad.Failed -> ScrollableCentered { ChatThreadError(load.message, onRetry) }
                    ThreadLoad.Loaded ->
                        if (state.items.isEmpty()) {
                            ScrollableCentered { ChatEmptyThread() }
                        } else {
                            ThreadList(
                                state = state,
                                zone = zone,
                                locale = locale,
                                timeFormat = timeFormat,
                                onLoadEarlier = onLoadEarlier,
                                onOpenBooking = onOpenBooking,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                }
            }
            TypingStrip(
                visible = state.peerTyping,
                name = peerLabel,
            )
            ChatComposer(
                text = state.composerText,
                enabled = state.load is ThreadLoad.Loaded,
                canSend = state.canSend,
                atCap = state.isComposerAtCap,
                onTextChange = onComposerChanged,
                onSend = onSend,
                focusRequester = focusRequester,
            )
        }
    }
}

/** Vertically scrollable, centered column so pull-to-refresh works from the empty and error states. */
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

@Composable
@Suppress("LongParameterList")
private fun ThreadList(
    state: ChatThreadUiState,
    zone: ZoneId,
    locale: Locale,
    timeFormat: DateFormat,
    onLoadEarlier: () -> Unit,
    onOpenBooking: (bookingId: String, isTerminal: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = remember(state.items, zone) { buildThreadRows(state.items, zone).asReversed() }
    val today = LocalDate.now(zone)
    val todayLabel = stringResource(R.string.chat_thread_today)
    val yesterdayLabel = stringResource(R.string.chat_thread_yesterday)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val awayFromNewest by remember {
        derivedStateOf { listState.firstVisibleItemIndex > listState.layoutInfo.visibleItemsInfo.size }
    }
    AutoScrollToNewest(state = state, listState = listState)

    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag(CHAT_THREAD_LIST_TAG),
            state = listState,
            reverseLayout = true,
            contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.md),
        ) {
            items(items = rows, key = { it.key }) { row ->
                when (row) {
                    is ThreadRow.DateHeader ->
                        DateSeparator(
                            label = dateLabel(row.date, today, locale, todayLabel, yesterdayLabel),
                            modifier = Modifier.animateItem(),
                        )
                    is ThreadRow.Entry ->
                        Box(Modifier.padding(top = if (row.tightAbove) Spacing.xs else Spacing.sm).animateItem()) {
                            EntryContent(row, state, zone, locale, timeFormat, onOpenBooking)
                        }
                }
            }
            if (state.hasMoreEarlier) {
                item(key = LOAD_EARLIER_KEY) {
                    LoadEarlierButton(loading = state.isLoadingEarlier, onClick = onLoadEarlier)
                }
            }
        }
        JumpToLatestButton(
            visible = awayFromNewest,
            onClick = { scope.launch { listState.animateScrollToItem(0) } },
            modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.md),
        )
    }
}

/**
 * Scrolls to the newest message when a new one arrives and the reader is within one viewport of it, or the message is
 * mine. A reader scrolled up through history is never moved (the Jump to latest button shows instead). Prepending
 * older pages leaves the newest key unchanged, so it never triggers a scroll.
 */
@Composable
private fun AutoScrollToNewest(
    state: ChatThreadUiState,
    listState: LazyListState,
) {
    val newest = state.items.lastOrNull()
    var lastNewestKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(newest?.key) {
        val previous = lastNewestKey
        lastNewestKey = newest?.key
        if (previous == null || newest == null || previous == newest.key) return@LaunchedEffect
        val nearNewest = listState.firstVisibleItemIndex <= listState.layoutInfo.visibleItemsInfo.size
        val mine = newest is ThreadItem.Message && newest.mine
        if (nearNewest || mine) listState.animateScrollToItem(0)
    }
}

@Composable
@Suppress("LongParameterList")
private fun EntryContent(
    row: ThreadRow.Entry,
    state: ChatThreadUiState,
    zone: ZoneId,
    locale: Locale,
    timeFormat: DateFormat,
    onOpenBooking: (bookingId: String, isTerminal: Boolean) -> Unit,
) {
    when (val item = row.item) {
        is ThreadItem.Message ->
            MessageBubble(
                message = item,
                peerName = state.peerName,
                peerAvatarUrl = state.peerAvatarUrl,
                showAvatar = row.showAvatar,
                timeText = formatSlotTime(item.createdAt, zone, timeFormat),
            )
        is ThreadItem.SystemEvent ->
            SystemEventRow(
                event = item,
                zone = zone,
                locale = locale,
                timeFormat = timeFormat,
                onOpenBooking = onOpenBooking,
            )
    }
}

private fun dateLabel(
    date: LocalDate,
    today: LocalDate,
    locale: Locale,
    todayLabel: String,
    yesterdayLabel: String,
): String =
    when (date) {
        today -> todayLabel
        today.minusDays(1) -> yesterdayLabel
        else -> formatShortDate(date, locale)
    }

/** Back arrow, a 32 dp avatar and the peer name (fallback "Chat"), truncated to one line at large font scales. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadTopBar(
    state: ChatThreadUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val title = state.peerName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.chat_thread_title_fallback)
    TopAppBar(
        modifier = modifier,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(url = state.peerAvatarUrl, contentDescription = null, size = TopBarAvatarSize)
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = title,
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.a11y_back),
                )
            }
        },
        actions = actions,
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface,
            ),
    )
}
