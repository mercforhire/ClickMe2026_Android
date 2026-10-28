package com.uptrendinvestments.clickmeapp.screens.notifications

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.EmptyStateAction
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.screens.notifications.subviews.NotificationRow
import com.uptrendinvestments.clickmeapp.screens.notifications.subviews.NotificationSectionHeader
import com.uptrendinvestments.clickmeapp.screens.notifications.subviews.NotificationsEndNote
import com.uptrendinvestments.clickmeapp.screens.notifications.subviews.NotificationsOffRow
import com.uptrendinvestments.clickmeapp.screens.notifications.subviews.NotificationsSkeleton
import com.uptrendinvestments.clickmeapp.screens.notifications.subviews.openNotificationSettings
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import kotlinx.coroutines.launch

/** Test tag on the notifications list. */
const val NOTIFICATIONS_LIST_TAG = "notifications_list"

/** Test tag on the Retry action of the failed state. */
const val NOTIFICATIONS_RETRY_TAG = "notifications_retry"

/**
 * Hilt-aware Notifications screen. The ViewModel is entry-scoped, so it lives as long as the Nav3 entry. The list
 * reloads quietly whenever the screen is shown again (ON_RESUME), and the permission-off row is re-evaluated on every
 * ON_RESUME so it clears after the user turns notifications on in system settings (D-02).
 */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Below API 33 this is the app-level toggle only; from 33 it also reflects the runtime permission.
    var notificationsEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        viewModel.onScreenShown()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.onScreenHidden() }
    NotificationsContent(
        state = state,
        notificationsEnabled = notificationsEnabled,
        onBack = onBack,
        onRetry = viewModel::retry,
        onRefresh = viewModel::refresh,
        onRowTap = viewModel::onRowTapped,
        onOpenSettings = { openNotificationSettings(context) },
        onRefreshFailureShown = viewModel::consumeRefreshFailure,
        modifier = modifier,
    )
}

/** What the body shows. The cross-fade is keyed by this so a quiet reload never replays it. */
private enum class BodyKind { Loading, Failed, Loaded }

private fun NotificationsLoad.kind(): BodyKind =
    when (this) {
        NotificationsLoad.Loading -> BodyKind.Loading
        is NotificationsLoad.Failed -> BodyKind.Failed
        is NotificationsLoad.Loaded -> BodyKind.Loaded
    }

/**
 * Stateless Notifications list: app bar, pull-to-refresh, and per state the skeleton, the error with Retry, the empty
 * state or the grouped rows with the end note. The permission-off row sits above the content in the Loaded and empty
 * states only.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsContent(
    state: NotificationsUiState,
    notificationsEnabled: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onRowTap: (NotificationRowModel) -> Unit,
    onOpenSettings: () -> Unit,
    onRefreshFailureShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val refreshFailedMessage = stringResource(R.string.notifications_refresh_failed)

    LaunchedEffect(state.refreshFailed) {
        if (state.refreshFailed) {
            // Launched on the composition scope so consuming the flag does not cancel the Snackbar.
            scope.launch { snackbarHostState.showSnackbar(refreshFailedMessage) }
            onRefreshFailureShown()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { AuthTopAppBar(title = stringResource(R.string.notifications_title), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            AnimatedContent(
                targetState = state.load.kind(),
                transitionSpec = {
                    fadeIn(tween(Motion.CONTROL_STATE_DURATION_MS)) togetherWith
                        fadeOut(tween(Motion.CONTROL_STATE_DURATION_MS))
                },
                modifier = Modifier.fillMaxSize(),
                label = "notificationsBody",
            ) { kind ->
                when (kind) {
                    BodyKind.Loading -> NotificationsSkeleton()
                    BodyKind.Failed ->
                        (state.load as? NotificationsLoad.Failed)?.let {
                            NotificationsFailed(
                                it,
                                onRetry,
                            )
                        }
                    BodyKind.Loaded ->
                        (state.load as? NotificationsLoad.Loaded)?.let {
                            if (it.sections.isEmpty()) {
                                NotificationsEmpty(notificationsEnabled, onOpenSettings)
                            } else {
                                NotificationsList(it.sections, notificationsEnabled, onRowTap, onOpenSettings)
                            }
                        }
                }
            }
        }
    }
}

@Composable
private fun NotificationsFailed(
    failed: NotificationsLoad.Failed,
    onRetry: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyState(
            icon = Icons.Outlined.ErrorOutline,
            heading = stringResource(R.string.notifications_error_heading),
            body = failed.message.asString(),
            modifier = Modifier.testTag(NOTIFICATIONS_RETRY_TAG),
            action = EmptyStateAction(stringResource(R.string.section_retry), onRetry),
        )
    }
}

/** The permission-off row with the 200 ms fade and expand / shrink. */
@Composable
private fun OffRowSlot(
    visible: Boolean,
    onOpenSettings: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter =
            fadeIn(tween(Motion.STRENGTH_METER_VISIBILITY_MS)) +
                expandVertically(tween(Motion.STRENGTH_METER_VISIBILITY_MS)),
        exit =
            fadeOut(tween(Motion.STRENGTH_METER_VISIBILITY_MS)) +
                shrinkVertically(tween(Motion.STRENGTH_METER_VISIBILITY_MS)),
    ) {
        Column(Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.lg)) {
            NotificationsOffRow(onClick = onOpenSettings)
        }
    }
}

@Composable
private fun NotificationsEmpty(
    notificationsEnabled: Boolean,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OffRowSlot(visible = !notificationsEnabled, onOpenSettings = onOpenSettings)
        EmptyState(
            icon = Icons.Outlined.NotificationsNone,
            heading = stringResource(R.string.notifications_empty_heading),
            body = stringResource(R.string.notifications_empty_body),
            modifier = Modifier.padding(top = Spacing.xxxl),
        )
    }
}

@Composable
private fun NotificationsList(
    sections: List<NotificationSection>,
    notificationsEnabled: Boolean,
    onRowTap: (NotificationRowModel) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val offRow = remember { MutableTransitionState(false) }
    offRow.targetState = !notificationsEnabled
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag(NOTIFICATIONS_LIST_TAG),
        contentPadding =
            PaddingValues(
                start = Spacing.md,
                end = Spacing.md,
                top = Spacing.lg,
                bottom = Spacing.xl + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // The item stays while the exit animation runs, then drops out of the list.
        if (offRow.currentState || offRow.targetState) {
            item(key = "off-row") {
                AnimatedVisibility(
                    visibleState = offRow,
                    enter =
                        fadeIn(tween(Motion.STRENGTH_METER_VISIBILITY_MS)) +
                            expandVertically(tween(Motion.STRENGTH_METER_VISIBILITY_MS)),
                    exit =
                        fadeOut(tween(Motion.STRENGTH_METER_VISIBILITY_MS)) +
                            shrinkVertically(tween(Motion.STRENGTH_METER_VISIBILITY_MS)),
                    modifier = Modifier.animateItem(),
                ) {
                    NotificationsOffRow(onClick = onOpenSettings)
                }
            }
        }
        sections.forEachIndexed { index, section ->
            item(key = "header-${section.day}") {
                NotificationSectionHeader(
                    day = section.day,
                    modifier =
                        Modifier.animateItem().then(
                            if (index >
                                0
                            ) {
                                Modifier.padding(top = Spacing.md)
                            } else {
                                Modifier
                            },
                        ),
                )
            }
            items(section.rows, key = { it.id }) { row ->
                NotificationRow(
                    row = row,
                    onClick = row.destination?.let { { onRowTap(row) } },
                    modifier = Modifier.animateItem(),
                )
            }
        }
        item(key = "end-note") { NotificationsEndNote(Modifier.animateItem()) }
    }
}
