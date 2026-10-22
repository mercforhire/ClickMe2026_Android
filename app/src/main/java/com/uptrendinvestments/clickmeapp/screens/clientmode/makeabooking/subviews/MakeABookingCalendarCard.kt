package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.firstDayOfWeek
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatDayLabel
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatMonthYear
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.isPastDay
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.monthCells
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.weekdayOrder
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle

private const val DISABLED_ALPHA = 0.38f
private const val DAYS_PER_WEEK = 7
private val DayCellHeight = 48.dp
private val DayCircleSize = 36.dp
private val OpenDotSize = 4.dp

/** Test tag of the calendar cell for [date]. Out-of-month cells have no node. */
fun calendarDayTestTag(date: LocalDate): String = "make_a_booking_day_$date"

/**
 * The month calendar card (D-03, D-06, BOOK-01).
 *
 * The grid is a Column of 7-cell Rows built from [monthCells], never a lazy grid, because the screen already scrolls.
 * Days before [today] are dimmed and not selectable, a 4 dp dot marks days in [openDays], and the previous-month arrow
 * is controlled by [canGoToPreviousMonth]. Forward navigation has no limit. While [enabled] is false (a booking is in
 * flight) neither the arrows nor the days respond; nothing is restyled.
 */
@Composable
fun MakeABookingCalendarCard(
    displayedMonth: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate,
    openDays: Set<LocalDate>,
    canGoToPreviousMonth: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val locale = LocalConfiguration.current.locales[0]
    val firstDay = remember(locale) { firstDayOfWeek(locale) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.sm)) {
            MonthRow(
                label = formatMonthYear(displayedMonth, locale),
                canGoToPreviousMonth = canGoToPreviousMonth,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                enabled = enabled,
            )
            WeekdayHeader(firstDay = firstDay, locale = locale)
            Spacer(Modifier.height(Spacing.sm))
            Crossfade(
                targetState = displayedMonth,
                animationSpec = tween(Motion.CONTROL_STATE_DURATION_MS),
                label = "bookingMonth",
            ) { month ->
                Column(Modifier.fillMaxWidth().selectableGroup()) {
                    monthCells(month, firstDay).chunked(DAYS_PER_WEEK).forEach { week ->
                        Row(Modifier.fillMaxWidth()) {
                            week.forEach { date ->
                                Box(Modifier.weight(1f).height(DayCellHeight), contentAlignment = Alignment.Center) {
                                    if (date != null) {
                                        CalendarDay(
                                            date = date,
                                            today = today,
                                            isSelected = date == selectedDate,
                                            isOpen = date in openDays,
                                            locale = locale,
                                            interactive = enabled,
                                            onSelect = { onSelectDate(date) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthRow(
    label: String,
    canGoToPreviousMonth: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    enabled: Boolean,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPreviousMonth, enabled = enabled && canGoToPreviousMonth) {
            Icon(
                imageVector = Icons.Outlined.ChevronLeft,
                contentDescription = stringResource(R.string.make_a_booking_previous_month),
                modifier = Modifier.size(24.dp),
                tint = if (canGoToPreviousMonth) onSurface else onSurface.copy(alpha = DISABLED_ALPHA),
            )
        }
        Text(
            text = label,
            modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.titleMedium,
            color = onSurface,
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onNextMonth, enabled = enabled) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = stringResource(R.string.make_a_booking_next_month),
                modifier = Modifier.size(24.dp),
                tint = onSurface,
            )
        }
    }
}

@Composable
private fun WeekdayHeader(
    firstDay: java.time.DayOfWeek,
    locale: java.util.Locale,
) {
    Row(Modifier.fillMaxWidth()) {
        weekdayOrder(firstDay).forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.SHORT, locale),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.extras.onSurfaceMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate,
    today: LocalDate,
    isSelected: Boolean,
    isOpen: Boolean,
    locale: java.util.Locale,
    interactive: Boolean,
    onSelect: () -> Unit,
) {
    val isPast = isPastDay(date, today)
    val isToday = date == today
    val colors = MaterialTheme.colorScheme
    val spec = tween<Color>(Motion.CONTROL_STATE_DURATION_MS)
    val fill by animateColorAsState(if (isSelected) colors.primary else Color.Transparent, spec, label = "dayFill")
    val numberColor by animateColorAsState(
        targetValue =
            when {
                isSelected -> colors.onPrimary
                isPast -> colors.onSurface.copy(alpha = DISABLED_ALPHA)
                else -> colors.onSurface
            },
        animationSpec = spec,
        label = "dayNumber",
    )
    val dayLabel = formatDayLabel(date, locale)
    val todayText = stringResource(R.string.make_a_booking_day_today)
    val openText = stringResource(R.string.make_a_booking_day_open)
    val state = listOfNotNull(todayText.takeIf { isToday }, openText.takeIf { isOpen }).joinToString(", ")
    Column(
        modifier =
            Modifier
                .testTag(calendarDayTestTag(date))
                .selectable(
                    selected = isSelected,
                    enabled = interactive && !isPast,
                    role = Role.RadioButton,
                    onClick = onSelect,
                ).semantics {
                    contentDescription = dayLabel
                    if (state.isNotEmpty()) stateDescription = state
                },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.xs))
        Box(
            modifier =
                Modifier
                    .size(DayCircleSize)
                    .background(fill, CircleShape)
                    .then(
                        if (isToday && !isSelected) {
                            Modifier.border(1.dp, colors.outline, CircleShape)
                        } else {
                            Modifier
                        },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = if (isSelected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                color = numberColor,
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        Box(
            Modifier
                .size(OpenDotSize)
                .background(if (isOpen) colors.primary else Color.Transparent, CircleShape),
        )
    }
}

@Preview(name = "MakeABookingCalendarCard")
@Composable
private fun MakeABookingCalendarCardPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            MakeABookingCalendarCard(
                displayedMonth = YearMonth.of(2026, 10),
                today = LocalDate.of(2026, 10, 22),
                selectedDate = LocalDate.of(2026, 10, 22),
                openDays = setOf(LocalDate.of(2026, 10, 24)),
                canGoToPreviousMonth = false,
                onPreviousMonth = {},
                onNextMonth = {},
                onSelectDate = {},
                modifier = Modifier.padding(Spacing.md),
            )
        }
    }
}
