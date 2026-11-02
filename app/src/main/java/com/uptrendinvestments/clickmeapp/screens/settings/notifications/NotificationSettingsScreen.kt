package com.uptrendinvestments.clickmeapp.screens.settings.notifications

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.AutoSaveStatus
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth
import com.uptrendinvestments.clickmeapp.ui.theme.extras

/** Test tag on each toggle row (the merged switch target). */
const val NOTIFICATION_ROW_TAG = "notification_row"

/** Test tag on the app-bar save status slot. */
const val NOTIFICATION_SAVE_STATUS_TAG = "notification_save_status"

/** Test tag on each section card. */
const val NOTIFICATION_SECTION_CARD_TAG = "notification_section_card"

/** Test tag on the divider between two rows of a card. */
const val NOTIFICATION_DIVIDER_TAG = "notification_divider"

/** Test tag on a row's trailing Switch. */
const val NOTIFICATION_SWITCH_TAG = "notification_switch"

private val StatusSlotSize = 48.dp
private val RowMinHeight = 56.dp

/**
 * Port of iOS `NotificationSettingsView` (PARITY-AUDIT #5). No quiet-hours card (D-01), no pull-to-refresh (UI-SPEC
 * A5) and no failure dialog (D-02): a failed save is an error icon with Retry in the app bar. Leaving, by the arrow or
 * by system back, hands a pending edit to the application scope first; [DisposableEffect] is the backstop for any
 * other way off the screen.
 */
@Composable
fun NotificationSettingsScreen(
    viewModel: NotificationSettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val latestOnBack by rememberUpdatedState(onBack)
    val leave = {
        viewModel.flushPendingSave()
        latestOnBack()
    }
    BackHandler(onBack = leave)
    DisposableEffect(Unit) { onDispose { viewModel.flushPendingSave() } }
    NotificationSettingsContent(
        state = state,
        onToggle = viewModel::onToggle,
        onRetry = viewModel::retry,
        onRetrySave = viewModel::retrySave,
        onBack = leave,
        modifier = modifier,
    )
}

/** Stateless Notification Settings: the bar with its save status, then the section cards. */
@Composable
fun NotificationSettingsContent(
    state: NotificationSettingsUiState,
    onToggle: (key: String, on: Boolean) -> Unit,
    onRetry: () -> Unit,
    onRetrySave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            AuthTopAppBar(
                title = stringResource(R.string.notification_settings_title),
                onBack = onBack,
                actions = {
                    NotificationSaveStatusAction(
                        status = if (state.load is NotificationLoad.Loaded) state.status else AutoSaveStatus.Idle,
                        onRetry = onRetrySave,
                    )
                },
            )
        },
    ) { inner ->
        if (state.load is NotificationLoad.Loaded) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(inner)
                        .verticalScroll(rememberScrollState())
                        .contentMaxWidth()
                        .padding(start = Spacing.md, end = Spacing.md, top = Spacing.md, bottom = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                for (section in NOTIFICATION_SECTIONS) {
                    NotificationSectionCard(section = section, values = state.values, onToggle = onToggle)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(inner))
        }
    }
}

/** A heading above a card of toggle rows; a one-row section has no divider. */
@Composable
fun NotificationSectionCard(
    section: NotificationSection,
    values: Map<String, Boolean>,
    onToggle: (key: String, on: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(section.title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.sm))
        Surface(
            modifier = Modifier.fillMaxWidth().testTag(NOTIFICATION_SECTION_CARD_TAG),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column {
                section.rows.forEachIndexed { index, row ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.testTag(NOTIFICATION_DIVIDER_TAG),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                    NotificationToggleRow(
                        title = stringResource(row.title),
                        subtitle = stringResource(R.string.notification_settings_row_subtitle),
                        checked = values[row.key] ?: true,
                        onCheckedChange = { onToggle(row.key, it) },
                    )
                }
            }
        }
    }
}

/**
 * One row, one switch target: the whole row is `toggleable(role = Role.Switch)`, so TalkBack reads the title, subtitle
 * and state once, and the trailing [Switch] (with `onCheckedChange = null`) is only a picture of the state.
 */
@Composable
fun NotificationToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = RowMinHeight)
                .testTag(NOTIFICATION_ROW_TAG)
                .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
                .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.extras.onSurfaceMuted,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.testTag(NOTIFICATION_SWITCH_TAG),
            colors =
                SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                ),
        )
    }
}

/** The save status in the app bar. Always a [StatusSlotSize] slot, so the bar never shifts between looks. */
@Composable
fun NotificationSaveStatusAction(
    status: AutoSaveStatus,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(StatusSlotSize).testTag(NOTIFICATION_SAVE_STATUS_TAG),
        contentAlignment = Alignment.Center,
    ) {
        // Filled in by the status task: Idle is an empty slot.
    }
}
