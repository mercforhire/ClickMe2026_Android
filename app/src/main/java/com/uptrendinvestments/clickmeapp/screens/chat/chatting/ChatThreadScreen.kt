package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews.MessageBubble
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatSlotTime
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import java.text.DateFormat
import java.time.ZoneId
import java.util.Locale

/** Test tag on the thread message list. */
const val CHAT_THREAD_LIST_TAG = "chat_thread_list"

private val TopBarAvatarSize = 32.dp

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
        modifier = modifier,
    )
}

/**
 * Stateless thread: top bar with the peer and the message list, oldest to newest with the list starting at the
 * newest message (`reverseLayout`, so opening needs no scroll). [zone], [locale] and [timeFormat] are parameters so
 * tests pass fixed values.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList", "UNUSED_PARAMETER")
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
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { ChatThreadTopBar(state = state, onBack = onBack) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).testTag(CHAT_THREAD_LIST_TAG),
            reverseLayout = true,
            contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            items(items = state.items.asReversed(), key = { it.key }) { item ->
                if (item is ThreadItem.Message) {
                    MessageBubble(
                        message = item,
                        peerName = state.peerName,
                        peerAvatarUrl = state.peerAvatarUrl,
                        showAvatar = true,
                        timeText = formatSlotTime(item.createdAt, zone, timeFormat),
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
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
