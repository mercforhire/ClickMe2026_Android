package com.uptrendinvestments.clickmeapp.screens.expertmode.topics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.ExpertTopicItem
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.subviews.TopicCard
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.subviews.TopicDeleteDialog
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.subviews.TopicsEmptyContent
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.subviews.TopicsErrorContent
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.subviews.TopicsSkeleton
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import kotlinx.coroutines.launch

/** Test tag on the "add topic" floating action button (shown only when topics exist). */
const val TOPICS_ADD_FAB_TAG = "topics_add_fab"

/** Test tag on the Availability app-bar action. */
const val TOPICS_AVAILABILITY_ACTION_TAG = "topics_availability_action"

/** Keeps the last card clear of the floating action button. */
private val ListBottomPadding = 88.dp

/**
 * Hilt-aware Topics tab root (EXP-01, D-05). The screen reports it was shown from both `LaunchedEffect(Unit)` and
 * `ON_RESUME`: the tab can be re-entered and the editor closed without either one firing alone, and the ViewModel
 * ignores the second call while a load runs.
 */
@Composable
fun TopicsScreen(
    onAddTopic: () -> Unit,
    onEditTopic: (ExpertTopicItem) -> Unit,
    onOpenAvailability: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TopicsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onScreenShown() }
    LaunchedEffect(Unit) { viewModel.onScreenShown() }

    TopicsContent(
        state = state,
        onAddTopic = onAddTopic,
        onEditTopic = onEditTopic,
        onOpenAvailability = onOpenAvailability,
        onRetry = viewModel::retry,
        onRefresh = viewModel::refresh,
        onDelete = viewModel::delete,
        onDismissDialog = viewModel::dismissDialog,
        onSnackbarShown = viewModel::onSnackbarShown,
        modifier = modifier,
    )
}

/**
 * Stateless Topics tab: app bar with the Availability action, skeleton / list / empty / error body with
 * pull-to-refresh, an add FAB only when topics exist, the delete confirmation, and snackbars and dialog for results.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicsContent(
    state: TopicsUiState,
    onAddTopic: () -> Unit,
    onEditTopic: (ExpertTopicItem) -> Unit,
    onOpenAvailability: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onDelete: (String) -> Unit,
    onDismissDialog: () -> Unit,
    onSnackbarShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var deleteTargetId by rememberSaveable { mutableStateOf<String?>(null) }

    val snackbarMessage =
        when (state.snackbar) {
            null -> null
            TopicsSnackbar.Added -> stringResource(R.string.topics_added)
            TopicsSnackbar.Updated -> stringResource(R.string.topics_updated)
            TopicsSnackbar.Deleted -> stringResource(R.string.topics_deleted)
            TopicsSnackbar.RefreshFailed -> stringResource(R.string.expert_refresh_failed)
        }
    LaunchedEffect(state.snackbar) {
        if (snackbarMessage != null) {
            // Launched on the composition scope so consuming the message does not cancel the Snackbar.
            scope.launch { snackbarHostState.showSnackbar(snackbarMessage, duration = SnackbarDuration.Short) }
            onSnackbarShown()
        }
    }

    val loaded = state.list as? TopicsListState.Loaded
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            AuthTopAppBar(
                title = stringResource(R.string.tab_topics),
                onBack = null,
                actions = {
                    IconButton(
                        onClick = onOpenAvailability,
                        modifier = Modifier.testTag(TOPICS_AVAILABILITY_ACTION_TAG),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = stringResource(R.string.topics_availability_action),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (loaded != null && loaded.topics.isNotEmpty()) {
                FloatingActionButton(
                    onClick = onAddTopic,
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag(TOPICS_ADD_FAB_TAG),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.topics_add_topic_fab),
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (val list = state.list) {
            TopicsListState.Loading -> TopicsSkeleton(modifier = Modifier.padding(innerPadding))
            is TopicsListState.Failed ->
                TopicsErrorContent(
                    message = list.message.asString(),
                    onRetry = onRetry,
                    modifier = Modifier.padding(innerPadding),
                )
            is TopicsListState.Loaded ->
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.padding(innerPadding),
                ) {
                    if (list.topics.isEmpty()) {
                        TopicsEmptyContent(onAddTopic = onAddTopic)
                    } else {
                        LazyColumn(
                            contentPadding =
                                PaddingValues(
                                    start = Spacing.md,
                                    top = Spacing.lg,
                                    end = Spacing.md,
                                    bottom = ListBottomPadding,
                                ),
                            verticalArrangement = Arrangement.spacedBy(Spacing.md),
                        ) {
                            items(list.topics, key = { it.id }) { item ->
                                TopicCard(
                                    item = item,
                                    isDeleting = state.deletingId == item.id,
                                    menuEnabled = state.deletingId == null,
                                    onClick = { onEditTopic(item) },
                                    onEdit = { onEditTopic(item) },
                                    onDelete = { deleteTargetId = item.id },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }
        }
    }

    val deleteTarget = loaded?.topics?.firstOrNull { it.id == deleteTargetId }
    if (deleteTarget != null) {
        TopicDeleteDialog(
            topicTitle = deleteTarget.title,
            onConfirm = {
                deleteTargetId = null
                onDelete(deleteTarget.id)
            },
            onDismiss = { deleteTargetId = null },
        )
    }

    state.dialog?.let { dialog ->
        AuthErrorDialog(
            message = dialog.message.asString(),
            onDismiss = onDismissDialog,
            title = stringResource(dialog.title),
        )
    }
}
