package com.uptrendinvestments.clickmeapp.screens.expertmode.availability.subviews

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.AvailabilityScheduleLogic
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.DaySchedule
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.TimeSlot
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import java.text.DateFormat
import java.time.DayOfWeek
import java.util.Locale

private val HeaderMinHeight = 56.dp
private val TouchTarget = 48.dp
private val RemoveIconSize = 24.dp
private val AddIconSize = 16.dp
private const val OVERLAP_BORDER_ALPHA = 0.5f
private const val REMOVE_ICON_ALPHA = 0.85f
private const val ADD_HOURS_DISABLED_ALPHA = 0.45f

/** Test tag of the day card header (toggle) row, e.g. `availability_day_MONDAY`. */
fun availabilityDayTag(day: DayOfWeek): String = "availability_day_${day.name}"

/** Test tag of the "Add hours" button of a day, e.g. `availability_add_hours_MONDAY`. */
fun availabilityAddHoursTag(day: DayOfWeek): String = "availability_add_hours_${day.name}"

/**
 * One weekday of the schedule (UI-SPEC "Day card"): a toggleable header with the day name and a Switch, and while
 * enabled the slot rows (start button, separator, end button, remove) plus "Add hours". An overlapping day gets an
 * error border. Stateless: [timeFormat] is the device 12/24 hour format.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AvailabilityDayCard(
    day: DaySchedule,
    dayName: String,
    overlaps: Boolean,
    timeFormat: DateFormat,
    onToggle: (Boolean) -> Unit,
    onEditSlot: (Int) -> Unit,
    onRemoveSlot: (Int) -> Unit,
    onAddHours: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor =
        if (overlaps) {
            MaterialTheme.colorScheme.error.copy(alpha = OVERLAP_BORDER_ALPHA)
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            DayHeader(day = day, dayName = dayName, onToggle = onToggle)
            AnimatedVisibility(
                visible = day.enabled,
                enter =
                    fadeIn(tween(Motion.STRENGTH_METER_VISIBILITY_MS)) +
                        expandVertically(tween(Motion.STRENGTH_METER_VISIBILITY_MS)),
                exit =
                    fadeOut(tween(Motion.STRENGTH_METER_VISIBILITY_MS)) +
                        shrinkVertically(tween(Motion.STRENGTH_METER_VISIBILITY_MS)),
            ) {
                Column {
                    day.slots.forEachIndexed { index, slot ->
                        val startText = AvailabilityScheduleLogic.formatMinutes(slot.start, timeFormat)
                        val endText = AvailabilityScheduleLogic.formatMinutes(slot.end, timeFormat)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(top = Spacing.md),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                            itemVerticalAlignment = Alignment.CenterVertically,
                        ) {
                            AvailabilityTimeButton(
                                text = startText,
                                contentDescription = stringResource(R.string.availability_start_time_a11y, startText),
                                onClick = { onEditSlot(index) },
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = stringResource(R.string.availability_range_separator),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            AvailabilityTimeButton(
                                text = endText,
                                contentDescription = stringResource(R.string.availability_end_time_a11y, endText),
                                onClick = { onEditSlot(index) },
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = { onRemoveSlot(index) },
                                modifier = Modifier.size(TouchTarget),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.RemoveCircleOutline,
                                    contentDescription = stringResource(R.string.availability_delete_slot),
                                    modifier = Modifier.size(RemoveIconSize),
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = REMOVE_ICON_ALPHA),
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.size(Spacing.md))
                    // No room left in the day (iOS 06-03): disabled and dimmed like iOS's 0.45 opacity.
                    val canAddHours = AvailabilityScheduleLogic.canAddHours(day.slots)
                    val addAlpha = if (canAddHours) 1f else ADD_HOURS_DISABLED_ALPHA
                    val addColor = MaterialTheme.colorScheme.primary.copy(alpha = addAlpha)
                    TextButton(
                        onClick = onAddHours,
                        enabled = canAddHours,
                        modifier =
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .heightIn(min = TouchTarget)
                                .testTag(availabilityAddHoursTag(day.day)),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(AddIconSize),
                            tint = addColor,
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(
                            text = stringResource(R.string.availability_add_hours),
                            style = MaterialTheme.typography.labelLarge,
                            color = addColor,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeader(
    day: DaySchedule,
    dayName: String,
    onToggle: (Boolean) -> Unit,
) {
    val stateText =
        stringResource(if (day.enabled) R.string.availability_available else R.string.availability_unavailable)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = HeaderMinHeight)
                .testTag(availabilityDayTag(day.day))
                .toggleable(value = day.enabled, role = Role.Switch, onValueChange = onToggle)
                .semantics { stateDescription = stateText },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = dayName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color =
                if (day.enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
        if (!day.enabled) {
            Text(
                text = stringResource(R.string.availability_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.extras.onSurfaceMuted,
            )
        }
        Switch(checked = day.enabled, onCheckedChange = null)
    }
}

@Preview(name = "AvailabilityDayCard")
@Composable
private fun AvailabilityDayCardPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                AvailabilityDayCard(
                    day = DaySchedule(DayOfWeek.MONDAY, true, listOf(TimeSlot(540, 1020))),
                    dayName = "Monday",
                    overlaps = false,
                    timeFormat = java.text.SimpleDateFormat("h:mm a", Locale.US),
                    onToggle = {},
                    onEditSlot = {},
                    onRemoveSlot = {},
                    onAddHours = {},
                )
                AvailabilityDayCard(
                    day = DaySchedule(DayOfWeek.TUESDAY, false, emptyList()),
                    dayName = "Tuesday",
                    overlaps = false,
                    timeFormat = java.text.SimpleDateFormat("h:mm a", Locale.US),
                    onToggle = {},
                    onEditSlot = {},
                    onRemoveSlot = {},
                    onAddHours = {},
                )
            }
        }
    }
}
