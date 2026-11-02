package com.uptrendinvestments.clickmeapp.screens.settings.notifications

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.ErrorState
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.AutoSaveStatus
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
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
private val StatusGlyphSize = 24.dp
private val StatusSpinnerStroke = 2.dp
private val RowMinHeight = 56.dp
private val SkeletonHeadingHeight = 24.dp
private val SkeletonHeadingWidth = 120.dp
private val SkeletonRowCounts = listOf(7, 3, 1, 2)

/**
 * Port of iOS `NotificationSettingsView` (PARITY-AUDIT #5). Nothing beyond the 13-row grid (D-01), no pull-to-refresh (UI-SPEC
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
        Crossfade(
            targetState = state.load.kind(),
            animationSpec = tween(Motion.CONTROL_STATE_DURATION_MS),
            modifier = Modifier.fillMaxSize().padding(inner),
            label = "notificationLoad",
        ) { kind ->
            when (kind) {
                LoadKind.Loading -> NotificationSkeleton()
                LoadKind.Failed -> {
                    val message = (state.load as? NotificationLoad.Failed)?.message
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ErrorState(
                            onRetry = onRetry,
                            heading = stringResource(R.string.notification_settings_load_error_title),
                            body = message?.asString().orEmpty(),
                            retryLabel = stringResource(R.string.section_retry),
                        )
                    }
                }
                LoadKind.Loaded ->
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .contentMaxWidth()
                                .padding(start = Spacing.md, end = Spacing.md, top = Spacing.md, bottom = Spacing.xl),
                        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                    ) {
                        for (section in NOTIFICATION_SECTIONS) {
                            NotificationSectionCard(section = section, values = state.values, onToggle = onToggle)
                        }
                    }
            }
        }
    }
}

private enum class LoadKind { Loading, Failed, Loaded }

private fun NotificationLoad.kind(): LoadKind =
    when (this) {
        NotificationLoad.Loading -> LoadKind.Loading
        is NotificationLoad.Failed -> LoadKind.Failed
        NotificationLoad.Loaded -> LoadKind.Loaded
    }

/** First-load placeholder shaped like the four section cards (7, 3, 1 and 2 rows). */
@Composable
private fun NotificationSkeleton() {
    SkeletonGroup(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .contentMaxWidth()
                    .padding(start = Spacing.md, end = Spacing.md, top = Spacing.md, bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            for (count in SkeletonRowCounts) {
                Column {
                    SkeletonBlock(modifier = Modifier.width(SkeletonHeadingWidth).height(SkeletonHeadingHeight))
                    Spacer(Modifier.height(Spacing.sm))
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        repeat(count) {
                            SkeletonBlock(
                                modifier = Modifier.fillMaxWidth().height(RowMinHeight),
                                shape = RectangleShape,
                            )
                        }
                    }
                }
            }
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

/**
 * The save status in the app bar (D-02). Always at least a [StatusSlotSize] slot, so the bar never shifts between
 * looks; the looks cross-fade over 150 ms. A failed save is an error icon and Retry here, never a dialog.
 */
@Composable
fun NotificationSaveStatusAction(
    status: AutoSaveStatus,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val saving = stringResource(R.string.a11y_state_saving)
    val saved = stringResource(R.string.a11y_state_saved)
    val errorDescription = stringResource(R.string.notification_settings_save_error_a11y)
    Box(
        modifier = modifier.defaultMinSize(StatusSlotSize, StatusSlotSize).testTag(NOTIFICATION_SAVE_STATUS_TAG),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(
            targetState = status,
            animationSpec = tween(Motion.CONTROL_STATE_DURATION_MS),
            label = "notificationSaveStatus",
        ) { shown ->
            when (shown) {
                AutoSaveStatus.Idle -> Unit
                AutoSaveStatus.Saving ->
                    CircularProgressIndicator(
                        modifier = Modifier.size(StatusGlyphSize).semantics { contentDescription = saving },
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = StatusSpinnerStroke,
                    )
                AutoSaveStatus.Saved ->
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = saved,
                        modifier = Modifier.size(StatusGlyphSize).semantics { liveRegion = LiveRegionMode.Polite },
                        tint = MaterialTheme.colorScheme.primary,
                    )
                AutoSaveStatus.Error ->
                    // Wider than the idle slot: the Retry label needs room. The slot only grows toward the title, so the
                    // back arrow and the title's start stay where they are.
                    Row(
                        modifier =
                            Modifier.semantics(mergeDescendants = true) {
                                contentDescription = errorDescription
                                liveRegion = LiveRegionMode.Polite
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(StatusGlyphSize),
                            tint = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = StatusSlotSize)) {
                            Text(
                                text = stringResource(R.string.section_retry),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
            }
        }
    }
}
