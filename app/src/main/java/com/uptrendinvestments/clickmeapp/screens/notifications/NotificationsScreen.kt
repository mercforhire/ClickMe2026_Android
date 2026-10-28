package com.uptrendinvestments.clickmeapp.screens.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.notifications.subviews.NotificationRow
import com.uptrendinvestments.clickmeapp.screens.notifications.subviews.NotificationSectionHeader
import com.uptrendinvestments.clickmeapp.screens.notifications.subviews.NotificationsEndNote
import com.uptrendinvestments.clickmeapp.screens.notifications.subviews.NotificationsOffRow
import com.uptrendinvestments.clickmeapp.screens.notifications.subviews.openNotificationSettings
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Test tag on the notifications list. */
const val NOTIFICATIONS_LIST_TAG = "notifications_list"

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
        onRowTap = viewModel::onRowTapped,
        onOpenSettings = { openNotificationSettings(context) },
        modifier = modifier,
    )
}

/** Stateless Notifications list: app bar, the optional permission-off row, the grouped rows and the end note. */
@Composable
fun NotificationsContent(
    state: NotificationsUiState,
    notificationsEnabled: Boolean,
    onBack: () -> Unit,
    onRowTap: (NotificationRowModel) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { AuthTopAppBar(title = stringResource(R.string.notifications_title), onBack = onBack) },
    ) { innerPadding ->
        val load = state.load
        if (load is NotificationsLoad.Loaded) {
            val offRow = remember { MutableTransitionState(false) }
            offRow.targetState = !notificationsEnabled
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).testTag(NOTIFICATIONS_LIST_TAG),
                contentPadding =
                    PaddingValues(
                        start = Spacing.md,
                        end = Spacing.md,
                        top = Spacing.lg,
                        bottom = Spacing.xl + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                    ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
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
                        ) {
                            NotificationsOffRow(onClick = onOpenSettings)
                        }
                    }
                }
                load.sections.forEachIndexed { index, section ->
                    item(key = "header-${section.day}") {
                        NotificationSectionHeader(
                            day = section.day,
                            modifier = if (index > 0) Modifier.padding(top = Spacing.md) else Modifier,
                        )
                    }
                    items(section.rows, key = { it.id }) { row ->
                        NotificationRow(row = row, onClick = row.destination?.let { { onRowTap(row) } })
                    }
                }
                item(key = "end-note") { NotificationsEndNote() }
            }
        }
    }
}
