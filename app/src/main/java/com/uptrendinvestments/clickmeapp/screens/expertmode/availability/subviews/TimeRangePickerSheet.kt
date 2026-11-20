package com.uptrendinvestments.clickmeapp.screens.expertmode.availability.subviews

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.AvailabilityScheduleLogic
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.TimeSlot
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

/** Test tag prefix of a Start row: `time_range_start_{minutes}`. */
internal const val TIME_RANGE_START_PREFIX = "time_range_start_"

/** Test tag prefix of an End row: `time_range_end_{minutes}`. */
internal const val TIME_RANGE_END_PREFIX = "time_range_end_"

/** Test tag of the Start column list. */
internal const val TIME_RANGE_START_LIST_TAG = "time_range_start_list"

/** Test tag of the End column list. */
internal const val TIME_RANGE_END_LIST_TAG = "time_range_end_list"

/** Test tag of the Cancel button. */
internal const val TIME_RANGE_CANCEL_TAG = "time_range_cancel"

/** Test tag of the Done button. */
internal const val TIME_RANGE_DONE_TAG = "time_range_done"

private val ListHeight = 240.dp
private val RowMinHeight = 48.dp
private val ActionMinHeight = 56.dp
private val SelectedBorderWidth = 2.dp
private val ActionBorderWidth = 1.dp
private const val CENTER_LEAD_ROWS = 2

/**
 * Select Time Range bottom sheet (D-04). Edits go to a draft seeded from [initial]; Done hands the draft to
 * [onDone] (the caller closes the sheet) while Cancel, scrim tap, Back and swipe-down call [onDismiss] and discard
 * it. Overlap with the day's other slots is not checked here; the screen's banner reports it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeRangePickerSheet(
    initial: TimeSlot,
    timeFormat: DateFormat,
    onDone: (TimeSlot) -> Unit,
    onDismiss: () -> Unit,
) {
    var start by rememberSaveable { mutableIntStateOf(initial.start) }
    var end by rememberSaveable { mutableIntStateOf(initial.end) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape =
            MaterialTheme.shapes.extraLarge.copy(
                bottomStart = ZeroCornerSize,
                bottomEnd = ZeroCornerSize,
            ),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        TimeRangePickerContent(
            draftStart = start,
            draftEnd = end,
            storedStart = initial.start,
            storedEnd = initial.end,
            timeFormat = timeFormat,
            onStartSelected = { start = it },
            onEndSelected = { end = it },
            onCancel = onDismiss,
            onDone = { onDone(TimeSlot(start, end)) },
        )
    }
}

/**
 * Stateless sheet body: title, Start and End columns in 30-minute steps (start strictly before end, end strictly
 * after start) and the Cancel / Done row. [storedStart] and [storedEnd] are the slot's values as the sheet opened;
 * an off-grid or 24:00 stored time stays a row while the sheet is open (iOS 06-03, D-08).
 */
@Composable
fun TimeRangePickerContent(
    draftStart: Int,
    draftEnd: Int,
    storedStart: Int,
    storedEnd: Int,
    timeFormat: DateFormat,
    onStartSelected: (Int) -> Unit,
    onEndSelected: (Int) -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md)
                .padding(bottom = Spacing.lg)
                .navigationBarsPadding(),
    ) {
        Text(
            text = stringResource(R.string.availability_time_sheet_title),
            modifier = Modifier.fillMaxWidth().semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
            verticalAlignment = Alignment.Bottom,
        ) {
            TimeColumn(
                label = stringResource(R.string.availability_start_time),
                options = AvailabilityScheduleLogic.pickerRows(isStart = true, draftStart, draftEnd, storedStart),
                selected = draftStart,
                timeFormat = timeFormat,
                listTag = TIME_RANGE_START_LIST_TAG,
                rowTagPrefix = TIME_RANGE_START_PREFIX,
                onSelected = onStartSelected,
                modifier = Modifier.weight(1f),
            )
            Box(modifier = Modifier.height(ListHeight), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.availability_range_separator),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TimeColumn(
                label = stringResource(R.string.availability_end_time),
                options = AvailabilityScheduleLogic.pickerRows(isStart = false, draftStart, draftEnd, storedEnd),
                selected = draftEnd,
                timeFormat = timeFormat,
                listTag = TIME_RANGE_END_LIST_TAG,
                rowTagPrefix = TIME_RANGE_END_PREFIX,
                onSelected = onEndSelected,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(Spacing.lg))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            PressScaleButton(
                onClick = onCancel,
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = ActionMinHeight)
                        .border(ActionBorderWidth, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .testTag(TIME_RANGE_CANCEL_TAG),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            ) {
                Text(text = stringResource(R.string.availability_cancel))
            }
            PressScaleButton(
                onClick = onDone,
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = ActionMinHeight)
                        .testTag(TIME_RANGE_DONE_TAG),
            ) {
                Text(text = stringResource(R.string.availability_done))
            }
        }
    }
}

/** One labelled column. Scrolls once on first composition so the selected row sits near the middle. */
@Composable
private fun TimeColumn(
    label: String,
    options: List<Int>,
    selected: Int,
    timeFormat: DateFormat,
    listTag: String,
    rowTagPrefix: String,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialIndex = remember { (options.indexOf(selected) - CENTER_LEAD_ROWS).coerceAtLeast(0) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    Column(modifier = modifier.semantics { contentDescription = label }) {
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        LazyColumn(
            modifier = Modifier.fillMaxWidth().height(ListHeight).testTag(listTag),
            state = listState,
        ) {
            items(options, key = { it }) { minutes ->
                TimeOptionRow(
                    text = AvailabilityScheduleLogic.formatMinutes(minutes, timeFormat),
                    selected = minutes == selected,
                    onClick = { onSelected(minutes) },
                    modifier = Modifier.testTag(rowTagPrefix + minutes),
                )
            }
        }
    }
}

@Composable
private fun TimeOptionRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val textColor by animateColorAsState(
        targetValue = if (selected) colorScheme.primary else colorScheme.onSurface,
        animationSpec = tween(Motion.CONTROL_STATE_DURATION_MS),
        label = "timeOptionText",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) colorScheme.primary else Color.Transparent,
        animationSpec = tween(Motion.CONTROL_STATE_DURATION_MS),
        label = "timeOptionBorder",
    )
    val shape = MaterialTheme.shapes.small
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = RowMinHeight)
                .border(SelectedBorderWidth, borderColor, shape)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style =
                (if (selected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge)
                    .copy(fontFeatureSettings = "tnum"),
            color = textColor,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(name = "TimeRangePickerContent")
@Composable
private fun TimeRangePickerContentPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            TimeRangePickerContent(
                draftStart = 540,
                draftEnd = 1020,
                storedStart = 540,
                storedEnd = 1020,
                timeFormat = SimpleDateFormat("h:mm a", Locale.US),
                onStartSelected = {},
                onEndSelected = {},
                onCancel = {},
                onDone = {},
            )
        }
    }
}
