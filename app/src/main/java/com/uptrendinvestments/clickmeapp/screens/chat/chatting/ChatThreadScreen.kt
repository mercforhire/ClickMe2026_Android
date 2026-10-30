package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyStateAction
import com.uptrendinvestments.clickmeapp.components.RefreshFailedSnackbarEffect
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.screens.chat.ComposerMode
import com.uptrendinvestments.clickmeapp.screens.chat.ThreadNotice
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews.ChatComposer
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews.ChatEmptyThread
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews.ChatThreadError
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews.ChatThreadSkeleton
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews.ComposerBlockedBanner
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
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** Test tag on the thread message list. */
const val CHAT_THREAD_LIST_TAG = "chat_thread_list"

private val TopBarAvatarSize = 32.dp
private const val LOAD_EARLIER_KEY = "load_earlier"
private const val COMPOSER_SWAP_MS = 200
private const val ANNOUNCEMENT_MS = 4_000L

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
        onRetryMessage = viewModel::retry,
        onDeleteMessage = viewModel::delete,
        onRefresh = viewModel::refresh,
        onLoadEarlier = viewModel::loadEarlier,
        onOpenBooking = onOpenBooking,
        onViewProfile = onViewProfile,
        onOpenReportBlock = onOpenReportBlock,
        onRefreshFailureConsumed = viewModel::consumeRefreshFailure,
        onComposerChanged = viewModel::onComposerChanged,
        onSend = viewModel::send,
        onDismissSendError = viewModel::dismissSendError,
        onNoticeConsumed = viewModel::consumeNotice,
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
    onDismissSendError: () -> Unit,
    onNoticeConsumed: () -> Unit,
    onRetryMessage: (clientMsgId: String) -> Unit,
    onDeleteMessage: (clientMsgId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val peerLabel = state.peerName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.chat_thread_title_fallback)
    RefreshFailedSnackbarEffect(
        refreshFailed = state.refreshFailed,
        snackbarHostState = snackbarHostState,
        onRetry = onRefresh,
        onShown = onRefreshFailureConsumed,
    )

    val unblockedNotice = state.notice as? ThreadNotice.Unblocked
    val unblockedName =
        unblockedNotice?.peerName?.takeIf { it.isNotBlank() } ?: state.peerName?.takeIf { it.isNotBlank() }
    val unblockedMessage =
        if (unblockedName != null) {
            stringResource(R.string.chat_thread_unblocked_snackbar, unblockedName)
        } else {
            stringResource(R.string.chat_thread_unblocked_snackbar_nameless)
        }
    LaunchedEffect(state.notice) {
        if (state.notice is ThreadNotice.Unblocked) {
            scope.launch { snackbarHostState.showSnackbar(unblockedMessage, duration = SnackbarDuration.Long) }
            onNoticeConsumed()
        }
    }
    state.sendError?.let { error ->
        AuthErrorDialog(
            message = error.asString(),
            title = stringResource(R.string.chat_send_error_title),
            onDismiss = onDismissSendError,
        )
    }
    val sendMessageLabel = stringResource(R.string.chat_thread_empty_action)

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
                            ScrollableCentered {
                                ChatEmptyThread(
                                    action =
                                        EmptyStateAction(sendMessageLabel) {
                                            focusRequester.requestFocus()
                                            keyboardController?.show()
                                        },
                                )
                            }
                        } else {
                            ThreadList(
                                state = state,
                                zone = zone,
                                locale = locale,
                                timeFormat = timeFormat,
                                onLoadEarlier = onLoadEarlier,
                                onOpenBooking = onOpenBooking,
                                onRetryMessage = onRetryMessage,
                                onDeleteMessage = onDeleteMessage,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                }
            }
            PeerArrivalAnnouncer(state = state, peerLabel = peerLabel)
            // In the column, above the composer, and not in the Scaffold slot: a realtime refresh failure is raised
            // while the user types, and the slot sits under the keyboard where Retry could not be reached.
            SnackbarHost(snackbarHostState)
            TypingStrip(
                visible = state.peerTyping,
                name = peerLabel,
            )
            AnimatedContent(
                targetState = state.composerMode,
                transitionSpec = { fadeIn(tween(COMPOSER_SWAP_MS)) togetherWith fadeOut(tween(COMPOSER_SWAP_MS)) },
                label = "composerSwap",
            ) { composerMode ->
                if (composerMode == ComposerMode.Normal) {
                    ChatComposer(
                        text = state.composerText,
                        enabled = state.load is ThreadLoad.Loaded,
                        canSend = state.canSend,
                        atCap = state.isComposerAtCap,
                        onTextChange = onComposerChanged,
                        onSend = onSend,
                        focusRequester = focusRequester,
                    )
                } else {
                    ComposerBlockedBanner(mode = composerMode)
                }
            }
        }
    }
}

/**
 * Speaks a peer message that arrives while the thread is open, once, as "{name}: {text}" (UI-SPEC Flow 4). A 1 dp node
 * with a polite live region carries the text for [ANNOUNCEMENT_MS] and is then emptied, so an identical message later
 * is announced again. The first loaded page is only recorded (also across recreation), a message of mine never speaks,
 * and prepending an older page leaves the newest key unchanged, so history is never announced.
 */
@Composable
private fun PeerArrivalAnnouncer(
    state: ChatThreadUiState,
    peerLabel: String,
) {
    val resources = LocalResources.current
    var started by rememberSaveable { mutableStateOf(false) }
    var lastNewestKey by rememberSaveable { mutableStateOf<String?>(null) }
    var announcement by remember { mutableStateOf("") }
    val loaded = state.load is ThreadLoad.Loaded
    val newest = state.items.lastOrNull()
    LaunchedEffect(loaded, newest?.key) {
        if (!loaded) return@LaunchedEffect
        if (!started) {
            started = true
            lastNewestKey = newest?.key
            return@LaunchedEffect
        }
        if (newest?.key == lastNewestKey) return@LaunchedEffect
        lastNewestKey = newest?.key
        announcement = ""
        if (newest is ThreadItem.Message && !newest.mine) {
            announcement = resources.getString(R.string.chat_thread_bubble_a11y, peerLabel, newest.text)
            delay(ANNOUNCEMENT_MS)
            announcement = ""
        }
    }
    Box(
        Modifier.size(1.dp).semantics {
            liveRegion = LiveRegionMode.Polite
            if (announcement.isNotEmpty()) contentDescription = announcement
        },
    )
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
    onRetryMessage: (clientMsgId: String) -> Unit,
    onDeleteMessage: (clientMsgId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = remember(state.items, zone) { buildThreadRows(state.items, zone).asReversed() }
    // Only the newest message of mine speaks its status changes ("Sent", then "Read"); older ones stay silent.
    val newestOwnKey =
        remember(state.items) { state.items.lastOrNull { it is ThreadItem.Message && it.mine }?.key }
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
                            modifier = Modifier.animateItem().contentMaxWidth(),
                        )
                    is ThreadRow.Entry ->
                        Box(
                            Modifier
                                .padding(top = if (row.tightAbove) Spacing.xs else Spacing.sm)
                                .animateItem()
                                .contentMaxWidth(),
                        ) {
                            EntryContent(
                                row = row,
                                state = state,
                                zone = zone,
                                locale = locale,
                                timeFormat = timeFormat,
                                newestOwnKey = newestOwnKey,
                                onOpenBooking = onOpenBooking,
                                onRetryMessage = onRetryMessage,
                                onDeleteMessage = onDeleteMessage,
                            )
                        }
                }
            }
            if (state.hasMoreEarlier) {
                item(key = LOAD_EARLIER_KEY) {
                    LoadEarlierButton(
                        loading = state.isLoadingEarlier,
                        onClick = onLoadEarlier,
                        modifier = Modifier.contentMaxWidth(),
                    )
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
    newestOwnKey: String?,
    onOpenBooking: (bookingId: String, isTerminal: Boolean) -> Unit,
    onRetryMessage: (clientMsgId: String) -> Unit,
    onDeleteMessage: (clientMsgId: String) -> Unit,
) {
    when (val item = row.item) {
        is ThreadItem.Message ->
            MessageBubble(
                message = item,
                peerName = state.peerName,
                peerAvatarUrl = state.peerAvatarUrl,
                showAvatar = row.showAvatar,
                timeText = formatSlotTime(item.createdAt, zone, timeFormat),
                onRetry = { item.clientMsgId?.let(onRetryMessage) },
                onDelete = { item.clientMsgId?.let(onDeleteMessage) },
                announceStatus = item.key == newestOwnKey,
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
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
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
