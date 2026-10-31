package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.SectionError
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.AvailabilityState
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingTimeSlot
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.TimezoneNote
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatSlotDateLine
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatSlotTime
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.isSelectable
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.timezoneNote
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val DISABLED_ALPHA = 0.38f
private const val CURRENT_BORDER_ALPHA = 0.55f
private const val SKELETON_CHIP_COUNT = 4
private val ChipMinHeight = 48.dp
private val SkeletonChipWidth = 96.dp

/** Test tag of the paid-window note (quick 261028-to1). */
const val PAID_WINDOW_NOTE_TEST_TAG = "make_a_booking_paid_window_note"

/** Default test tag prefix of the time slot chips (Make a Booking). */
const val SLOT_CHIP_TAG_PREFIX = "make_a_booking_slot_"

/** Test tag of the time slot chip that starts at [start]. */
fun slotChipTestTag(
    start: Instant,
    prefix: String = SLOT_CHIP_TAG_PREFIX,
): String = "$prefix${start.toEpochMilli()}"

/** A slot is the booking's current one when its start is within a second of it (iOS `abs(delta) < 1`). */
private fun isCurrentSlot(
    slot: BookingTimeSlot,
    currentSlotStart: Instant?,
): Boolean =
    currentSlotStart != null &&
        Duration.between(slot.start, currentSlotStart).abs() < Duration.ofSeconds(1)

/** The slots to draw: with [hidePastSlots] the ones that already started are dropped (Make a Booking dims them). */
private fun visibleSlots(
    slots: List<BookingTimeSlot>,
    now: Instant,
    hidePastSlots: Boolean,
): List<BookingTimeSlot> = if (hidePastSlots) slots.filter { it.start.isAfter(now) } else slots

/**
 * The time slots card (D-04, D-05, D-07, BOOK-02).
 *
 * Shows the selected date, the friendly timezone note (never the raw IANA id) and every slot of that day as a chip in
 * a wrapping [FlowRow]. Slots that are not selectable at [now] (unavailable, held, or already started) are dimmed and
 * cannot be tapped, but are never hidden. When [latestSelectableStart] is set (a paid topic, quick 261028-to1),
 * slots starting after it are dimmed the same way and a short note under the timezone note explains the 6 day window. Chip labels use [timeFormat] in [zone], so the device 12/24 hour setting
 * wins.
 *
 * While [enabled] is false (a booking is in flight) no chip responds, but nothing is dimmed or marked unavailable.
 *
 * When [selectedDateLoaded] is false the selected day lies outside the loaded months (the day is kept when the month
 * moves, D-07), so its slots are unknown: the card asks the user to pick a day instead of claiming it has no slots.
 *
 * After a 409 the ViewModel reloads with the data kept visible, so the list refreshes in place and the taken slot
 * turns dimmed with no skeleton (D-09). [AvailabilityState.Failed] only ever describes the center month (D-07).
 *
 * The trailing parameters exist for the Phase 16.2 reschedule picker, the second caller; their defaults keep Make a
 * Booking unchanged. [title] and [paidWindowNote] / [paidWindowNoteTag] swap the copy and tag. [hidePastSlots] drops
 * slots that already started instead of dimming them. [currentSlotStart] marks the booking's current time as a shown,
 * never selectable "Current" chip; it is never synthesized when the availability does not contain it. [slotTagPrefix]
 * replaces the chip test tag prefix.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MakeABookingTimeSlotsCard(
    selectedDate: LocalDate,
    availability: AvailabilityState,
    now: Instant,
    selectedSlotStart: Instant?,
    expertTimezone: String?,
    onSelectSlot: (Instant) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectedDateLoaded: Boolean = true,
    zone: ZoneId = ZoneId.systemDefault(),
    timeFormat: java.text.DateFormat = rememberDeviceTimeFormat(),
    latestSelectableStart: Instant? = null,
    title: String = stringResource(R.string.make_a_booking_slots_title),
    paidWindowNote: String = stringResource(R.string.make_a_booking_paid_window_note),
    paidWindowNoteTag: String = PAID_WINDOW_NOTE_TEST_TAG,
    hidePastSlots: Boolean = false,
    currentSlotStart: Instant? = null,
    slotTagPrefix: String = SLOT_CHIP_TAG_PREFIX,
) {
    val locale = LocalConfiguration.current.locales[0]
    val countText =
        slotsCountText(
            availability,
            selectedDate,
            selectedDateLoaded,
            now,
            latestSelectableStart,
            hidePastSlots,
            currentSlotStart,
        )
    val dateLabel = formatSlotDateLine(selectedDate, locale)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        // Its own traversal group, apart from the calendar.
        Column(
            Modifier.padding(Spacing.md).semantics { isTraversalGroup = true },
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = title,
                modifier =
                    Modifier.semantics {
                        heading()
                        // Choosing a day announces the result once: "{n} times available" or "No times this day".
                        // The date is part of the text so two days with the same count still change it.
                        if (countText != null) {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = "$title, $dateLabel, $countText"
                        }
                    },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.extras.onSurfaceMuted,
            )
            val note = timezoneNote(zone, expertTimezone, selectedDate, locale)
            Text(
                text =
                    when (note) {
                        is TimezoneNote.Same -> stringResource(R.string.make_a_booking_tz_same, note.clientAbbr)
                        is TimezoneNote.Different ->
                            stringResource(
                                R.string.make_a_booking_tz_different,
                                note.clientAbbr,
                                note.expertCity,
                                note.expertAbbr,
                            )
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.extras.onSurfaceMuted,
            )
            if (latestSelectableStart != null) {
                Text(
                    text = paidWindowNote,
                    modifier = Modifier.testTag(paidWindowNoteTag),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.extras.onSurfaceMuted,
                )
            }
            when (availability) {
                AvailabilityState.Loading -> SlotsSkeleton()
                is AvailabilityState.Failed ->
                    SectionError(message = availability.message.asString(), onRetry = onRetry)
                is AvailabilityState.Loaded -> {
                    val slots = visibleSlots(availability.slotsByDate[selectedDate].orEmpty(), now, hidePastSlots)
                    if (!selectedDateLoaded) {
                        Text(
                            text = stringResource(R.string.make_a_booking_pick_a_day),
                            modifier = Modifier.padding(vertical = Spacing.xs),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.extras.onSurfaceMuted,
                        )
                    } else if (slots.isEmpty()) {
                        Text(
                            text = stringResource(R.string.make_a_booking_no_slots),
                            modifier = Modifier.padding(vertical = Spacing.xs),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.extras.onSurfaceMuted,
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().selectableGroup(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            slots.forEach { slot ->
                                val isCurrent = isCurrentSlot(slot, currentSlotStart)
                                SlotChip(
                                    slot = slot,
                                    label = formatSlotTime(slot.start, zone, timeFormat),
                                    isSelected = slot.start == selectedSlotStart && !isCurrent,
                                    enabled = !isCurrent && slot.isSelectable(now, latestSelectableStart),
                                    interactive = enabled,
                                    isCurrent = isCurrent,
                                    tagPrefix = slotTagPrefix,
                                    onSelect = { onSelectSlot(slot.start) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The spoken result of choosing a day, or null while there is no result (loading, failed, day not loaded). */
@Composable
private fun slotsCountText(
    availability: AvailabilityState,
    selectedDate: LocalDate,
    selectedDateLoaded: Boolean,
    now: Instant,
    latestSelectableStart: Instant?,
    hidePastSlots: Boolean,
    currentSlotStart: Instant?,
): String? {
    if (availability !is AvailabilityState.Loaded || !selectedDateLoaded) return null
    // The clock ticks, but a slot expiring must not re-announce unprompted: count at the moment the day or the
    // availability last changed.
    val countedAt = remember(availability, selectedDate, latestSelectableStart) { now }
    val count =
        visibleSlots(availability.slotsByDate[selectedDate].orEmpty(), countedAt, hidePastSlots).count {
            !isCurrentSlot(it, currentSlotStart) && it.isSelectable(countedAt, latestSelectableStart)
        }
    return if (count == 0) {
        stringResource(R.string.make_a_booking_slots_none_a11y)
    } else {
        pluralStringResource(R.plurals.make_a_booking_slots_available_a11y, count, count)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SlotsSkeleton() {
    SkeletonGroup(Modifier.fillMaxWidth()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            repeat(SKELETON_CHIP_COUNT) {
                SkeletonBlock(Modifier.size(width = SkeletonChipWidth, height = ChipMinHeight), CircleShape)
            }
        }
    }
}

@Composable
private fun SlotChip(
    slot: BookingTimeSlot,
    label: String,
    isSelected: Boolean,
    enabled: Boolean,
    interactive: Boolean,
    isCurrent: Boolean,
    tagPrefix: String,
    onSelect: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val unavailable = stringResource(R.string.make_a_booking_slot_unavailable)
    val currentState = stringResource(R.string.reschedule_start_current_chip_a11y)
    val spec = tween<Color>(Motion.CONTROL_STATE_DURATION_MS)
    val fill by animateColorAsState(if (isSelected) colors.primary else Color.Transparent, spec, label = "slotFill")
    val textColor by animateColorAsState(
        targetValue =
            when {
                isSelected -> colors.onPrimary
                enabled || isCurrent -> colors.onSurface
                else -> colors.onSurface.copy(alpha = DISABLED_ALPHA)
            },
        animationSpec = spec,
        label = "slotText",
    )
    val borderColor by animateColorAsState(
        targetValue =
            when {
                isCurrent -> colors.tertiary.copy(alpha = CURRENT_BORDER_ALPHA)
                enabled -> colors.outline
                else -> colors.outline.copy(alpha = DISABLED_ALPHA)
            },
        animationSpec = spec,
        label = "slotBorder",
    )
    Box(
        modifier =
            Modifier
                .testTag(slotChipTestTag(slot.start, tagPrefix))
                .heightIn(min = ChipMinHeight)
                .widthIn(min = ChipMinHeight)
                .background(fill, CircleShape)
                .then(if (isSelected) Modifier else Modifier.border(1.dp, borderColor, CircleShape))
                .selectable(
                    selected = isSelected,
                    enabled = enabled && interactive,
                    role = Role.RadioButton,
                    onClick = onSelect,
                ).semantics {
                    if (isCurrent) {
                        stateDescription = currentState
                    } else if (!enabled) {
                        stateDescription = unavailable
                    }
                }.padding(horizontal = Spacing.md, vertical = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = if (isSelected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                color = textColor,
            )
            if (isCurrent) {
                Text(
                    text = stringResource(R.string.reschedule_start_current_chip),
                    modifier = Modifier.padding(top = Spacing.xs),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.tertiary,
                )
            }
        }
    }
}

@Preview(name = "MakeABookingTimeSlotsCard")
@Composable
private fun MakeABookingTimeSlotsCardPreview() {
    val date = LocalDate.of(2026, 10, 22)
    val start = Instant.parse("2026-10-22T20:00:00Z")
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            MakeABookingTimeSlotsCard(
                selectedDate = date,
                availability =
                    AvailabilityState.Loaded(
                        mapOf(
                            date to
                                listOf(
                                    BookingTimeSlot(start, null, true),
                                    BookingTimeSlot(start.plusSeconds(3600), null, false),
                                ),
                        ),
                    ),
                now = Instant.parse("2026-10-22T16:00:00Z"),
                selectedSlotStart = start,
                expertTimezone = "America/Los_Angeles",
                onSelectSlot = {},
                onRetry = {},
                modifier = Modifier.padding(Spacing.md),
            )
        }
    }
}

@Preview(name = "MakeABookingTimeSlotsCard failed")
@Composable
private fun MakeABookingTimeSlotsCardFailedPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            MakeABookingTimeSlotsCard(
                selectedDate = LocalDate.of(2026, 10, 22),
                availability = AvailabilityState.Failed(FieldError.Text("Check your connection and try again.")),
                now = Instant.parse("2026-10-22T16:00:00Z"),
                selectedSlotStart = null,
                expertTimezone = null,
                onSelectSlot = {},
                onRetry = {},
                modifier = Modifier.padding(Spacing.md),
            )
        }
    }
}
