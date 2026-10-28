package com.uptrendinvestments.clickmeapp.screens.notifications

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.notifications.subviews.NotificationRow
import com.uptrendinvestments.clickmeapp.screens.notifications.subviews.NotificationSectionHeader
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Test tag on the notifications list. */
const val NOTIFICATIONS_LIST_TAG = "notifications_list"

/**
 * Hilt-aware Notifications screen. The ViewModel is entry-scoped, so it lives as long as the Nav3 entry. The list
 * reloads quietly whenever the screen is shown again (ON_RESUME).
 */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onScreenShown() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.onScreenHidden() }
    NotificationsContent(
        state = state,
        onBack = onBack,
        onRowTap = viewModel::onRowTapped,
        modifier = modifier,
    )
}

/** Stateless Notifications list: app bar and the grouped rows. */
@Composable
fun NotificationsContent(
    state: NotificationsUiState,
    onBack: () -> Unit,
    onRowTap: (NotificationRowModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { AuthTopAppBar(title = stringResource(R.string.notifications_title), onBack = onBack) },
    ) { innerPadding ->
        val load = state.load
        if (load is NotificationsLoad.Loaded) {
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
            }
        }
    }
}
