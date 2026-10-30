package com.uptrendinvestments.clickmeapp.screens.expertmode.availability

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.EmptyStateAction
import com.uptrendinvestments.clickmeapp.components.PickerField
import com.uptrendinvestments.clickmeapp.components.RefreshFailedSnackbarEffect
import com.uptrendinvestments.clickmeapp.constants.TimezoneEntry
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.subviews.AvailabilityDayCard
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.subviews.AvailabilityOverlapBanner
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.subviews.AvailabilitySaveAction
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.subviews.AvailabilitySkeleton
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.subviews.TimeRangePickerSheet
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.subviews.TimezonePickerSheet
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth
import java.text.DateFormat
import java.time.DayOfWeek

/** Test tag on the timezone picker row. */
const val AVAILABILITY_TIMEZONE_ROW_TAG = "availability_timezone_row"

/** Test tag on the "Auto-detect from device" button. */
const val AVAILABILITY_AUTO_DETECT_TAG = "availability_auto_detect"

private val AutoDetectIconSize = 16.dp
private val AutoDetectMinHeight = 48.dp
private const val BANNER_ANIMATION_MS = 200

/** ViewModel callbacks of the Availability screen, bundled so the stateless content stays easy to drive in tests. */
data class AvailabilityActions(
    val retry: () -> Unit,
    val refresh: () -> Unit,
    val setDayEnabled: (DayOfWeek, Boolean) -> Unit,
    val addHours: (DayOfWeek) -> Unit,
    val removeSlot: (DayOfWeek, Int) -> Unit,
    val updateSlot: (DayOfWeek, Int, TimeSlot) -> Unit,
    val selectTimezone: (String) -> Unit,
    val autoDetectTimezone: () -> Unit,
    val save: () -> Unit,
    val dismissDialog: () -> Unit,
    val onRefreshFailedShown: () -> Unit,
)

/**
 * Hilt-aware Availability route (EXP-03, EXP-04). Reachable from the Topics tab and the Settings tab; the caller
 * decides which stack [onBack] pops (10-15).
 */
@Composable
fun AvailabilityScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AvailabilityViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AvailabilityContent(
        state = state,
        actions =
            AvailabilityActions(
                retry = viewModel::retry,
                refresh = viewModel::refresh,
                setDayEnabled = viewModel::setDayEnabled,
                addHours = viewModel::addHours,
                removeSlot = viewModel::removeSlot,
                updateSlot = viewModel::updateSlot,
                selectTimezone = viewModel::selectTimezone,
                autoDetectTimezone = viewModel::autoDetectTimezone,
                save = viewModel::save,
                dismissDialog = viewModel::dismissDialog,
                onRefreshFailedShown = viewModel::onRefreshFailedShown,
            ),
        onBack = onBack,
        modifier = modifier,
    )
}

/**
 * Stateless Availability screen: app bar with the Save action, then a skeleton, an error with Retry, or the timezone
 * row, Auto-detect, overlap banner and the seven day cards under pull-to-refresh.
 *
 * Back pops freely, also while saving and with unsaved edits: edits are discarded without a prompt (D-16) and the
 * ViewModel lets an in-flight save finish. The screen stays editable during a call; only Save is guarded (D-03).
 * The two sheets are slots so Robolectric tests can substitute stubs for the `ModalBottomSheet` windows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailabilityContent(
    state: AvailabilityUiState,
    actions: AvailabilityActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    timeFormat: DateFormat = rememberDeviceTimeFormat(),
    timeRangeSheet: @Composable (
        initial: TimeSlot,
        onDone: (TimeSlot) -> Unit,
        onDismiss: () -> Unit,
    ) -> Unit = { initial, onDone, onDismiss ->
        TimeRangePickerSheet(initial = initial, timeFormat = timeFormat, onDone = onDone, onDismiss = onDismiss)
    },
    timezoneSheet: @Composable (
        entries: List<TimezoneEntry>,
        selectedId: String,
        onSelect: (String) -> Unit,
        onDismiss: () -> Unit,
    ) -> Unit = { entries, selectedId, onSelect, onDismiss ->
        TimezonePickerSheet(entries = entries, selectedId = selectedId, onSelect = onSelect, onDismiss = onDismiss)
    },
) {
    val snackbarHostState = remember { SnackbarHostState() }
    // DayOfWeek value and slot index of the slot being edited; a Pair of Ints survives configuration changes.
    var editing by rememberSaveable { mutableStateOf<Pair<Int, Int>?>(null) }
    var showTimezoneSheet by rememberSaveable { mutableStateOf(false) }

    RefreshFailedSnackbarEffect(
        refreshFailed = state.refreshFailed,
        snackbarHostState = snackbarHostState,
        onRetry = actions.refresh,
        onShown = actions.onRefreshFailedShown,
        message = stringResource(R.string.expert_refresh_failed),
    )

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        // The scrolling body draws under the navigation bar and pads its own end (see LoadedBody).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AuthTopAppBar(
                title = stringResource(R.string.availability_title),
                onBack = onBack,
                actions = {
                    AvailabilitySaveAction(
                        state = state.saveState,
                        enabled = state.canSave,
                        onSave = actions.save,
                    )
                },
            )
        },
        // The Scaffold has zero content insets (R1 exception), so the host clears the bars and the keyboard itself (R9).
        snackbarHost = { SnackbarHost(snackbarHostState, Modifier.navigationBarsPadding().imePadding()) },
    ) { innerPadding ->
        when (val load = state.load) {
            AvailabilityLoad.Loading ->
                AvailabilitySkeleton(
                    modifier =
                        Modifier
                            .padding(innerPadding)
                            .padding(horizontal = Spacing.md)
                            .padding(top = Spacing.lg),
                )
            is AvailabilityLoad.Failed ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    EmptyState(
                        icon = Icons.Outlined.CloudOff,
                        heading = stringResource(R.string.availability_load_error_heading),
                        body = load.message.asString(),
                        action = EmptyStateAction(stringResource(R.string.expert_retry), actions.retry),
                    )
                }
            AvailabilityLoad.Loaded ->
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = actions.refresh,
                    modifier = Modifier.padding(innerPadding),
                ) {
                    LoadedBody(
                        state = state,
                        actions = actions,
                        timeFormat = timeFormat,
                        onEditSlot = { day, index -> editing = day.value to index },
                        onOpenTimezone = { showTimezoneSheet = true },
                    )
                }
        }
    }

    editing?.let { (dayValue, index) ->
        val day = DayOfWeek.of(dayValue)
        val slot = state.slotAt(day, index)
        if (slot == null) {
            // A refresh replaced the schedule and the slot is gone.
            LaunchedEffect(Unit) { editing = null }
        } else {
            timeRangeSheet(
                slot,
                { updated ->
                    actions.updateSlot(day, index, updated)
                    editing = null
                },
                { editing = null },
            )
        }
    }

    if (showTimezoneSheet) {
        timezoneSheet(
            state.timezoneEntries,
            state.timezoneId,
            { id ->
                actions.selectTimezone(id)
                showTimezoneSheet = false
            },
            { showTimezoneSheet = false },
        )
    }

    state.dialog?.let { dialog ->
        AuthErrorDialog(
            message = dialog.asString(),
            onDismiss = actions.dismissDialog,
            title = stringResource(R.string.availability_save_error_title),
        )
    }
}

@Composable
private fun LoadedBody(
    state: AvailabilityUiState,
    actions: AvailabilityActions,
    timeFormat: DateFormat,
    onEditSlot: (DayOfWeek, Int) -> Unit,
    onOpenTimezone: () -> Unit,
) {
    val overlapping = state.overlappingDays
    val separator = stringResource(R.string.availability_list_separator)
    val lastSeparator = stringResource(R.string.availability_list_last_separator)
    // Remembered so the banner keeps its text while it animates out after the overlap is fixed.
    var bannerDays by remember { mutableStateOf(overlapping) }
    if (overlapping.isNotEmpty()) bannerDays = overlapping
    val dayNames = DayOfWeek.entries.associateWith { dayName(it) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .contentMaxWidth()
                .padding(horizontal = Spacing.md)
                .padding(top = Spacing.lg),
    ) {
        PickerField(
            value = state.timezoneLabel,
            placeholder = stringResource(R.string.availability_timezone_placeholder),
            leadingIcon = Icons.Outlined.Public,
            onClick = onOpenTimezone,
            modifier = Modifier.testTag(AVAILABILITY_TIMEZONE_ROW_TAG),
        )
        Spacer(modifier = Modifier.size(Spacing.sm))
        TextButton(
            onClick = actions.autoDetectTimezone,
            modifier = Modifier.heightIn(min = AutoDetectMinHeight).testTag(AVAILABILITY_AUTO_DETECT_TAG),
        ) {
            Icon(
                imageVector = Icons.Outlined.MyLocation,
                contentDescription = null,
                modifier = Modifier.size(AutoDetectIconSize),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(Spacing.xs))
            Text(
                text = stringResource(R.string.availability_auto_detect),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.size(Spacing.lg))
        AnimatedVisibility(
            visible = overlapping.isNotEmpty(),
            enter = fadeIn(tween(BANNER_ANIMATION_MS)) + expandVertically(tween(BANNER_ANIMATION_MS)),
            exit = fadeOut(tween(BANNER_ANIMATION_MS)) + shrinkVertically(tween(BANNER_ANIMATION_MS)),
        ) {
            Column {
                AvailabilityOverlapBanner(
                    daysText =
                        AvailabilityScheduleLogic.joinNames(
                            names = bannerDays.map { dayNames.getValue(it) },
                            separator = separator,
                            lastSeparator = lastSeparator,
                        ),
                )
                Spacer(modifier = Modifier.size(Spacing.md))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            state.days.forEach { day ->
                AvailabilityDayCard(
                    day = day,
                    dayName = dayNames.getValue(day.day),
                    overlaps = day.day in overlapping,
                    timeFormat = timeFormat,
                    onToggle = { actions.setDayEnabled(day.day, it) },
                    onEditSlot = { index -> onEditSlot(day.day, index) },
                    onRemoveSlot = { index -> actions.removeSlot(day.day, index) },
                    onAddHours = { actions.addHours(day.day) },
                )
            }
        }
        Spacer(modifier = Modifier.navigationBarsPadding().padding(bottom = Spacing.xl))
    }
}

private fun AvailabilityUiState.slotAt(
    day: DayOfWeek,
    index: Int,
): TimeSlot? {
    val slots = days.firstOrNull { it.day == day }?.slots ?: return null
    return slots.getOrNull(index)
}

@Composable
private fun dayName(day: DayOfWeek): String =
    stringResource(
        when (day) {
            DayOfWeek.MONDAY -> R.string.availability_day_monday
            DayOfWeek.TUESDAY -> R.string.availability_day_tuesday
            DayOfWeek.WEDNESDAY -> R.string.availability_day_wednesday
            DayOfWeek.THURSDAY -> R.string.availability_day_thursday
            DayOfWeek.FRIDAY -> R.string.availability_day_friday
            DayOfWeek.SATURDAY -> R.string.availability_day_saturday
            DayOfWeek.SUNDAY -> R.string.availability_day_sunday
        },
    )
