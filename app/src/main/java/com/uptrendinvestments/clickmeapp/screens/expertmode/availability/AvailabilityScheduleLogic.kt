package com.uptrendinvestments.clickmeapp.screens.expertmode.availability

import java.text.DateFormat
import java.time.DayOfWeek
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** A time range within one day, in minutes since midnight (0..1440). 1440 is the "24:00" end-of-day value. */
data class TimeSlot(
    val start: Int,
    val end: Int,
)

/** One weekday of the weekly schedule. Slots are kept in the model even while the day is switched off. */
data class DaySchedule(
    val day: DayOfWeek,
    val enabled: Boolean,
    val slots: List<TimeSlot>,
)

/**
 * Pure schedule logic for Expert availability (EXP-03).
 *
 * Port of the logic in iOS `AvailabilitySettingsViewModel.swift` (defaultSchedule, hydrate, overlappingDayNames,
 * add slot, request building) and `Subviews/AvailabilityDayRow.swift` (toggle seeding, Add hours).
 *
 * Decision (research open question 1): the End picker keeps the iOS 23:30 cap, but the model, hydration and the
 * request builder round-trip 1440 minutes ("24:00") losslessly, and nothing here calls `LocalTime.of(24, 0)`
 * (it throws). All wire strings use [Locale.ROOT] so locale digits (e.g. Arabic) can never reach the server.
 *
 * This file must stay free of Android types so it runs on the plain JVM.
 */
object AvailabilityScheduleLogic {
    const val STEP_MINUTES = 30
    const val MINUTES_PER_DAY = 1440

    /** Latest selectable Start (23:00). */
    const val LATEST_START = 1380

    /** Latest selectable End (23:30), matching the iOS cap. */
    const val LATEST_END = 1410

    val WEEK: List<DayOfWeek> =
        listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY,
        )

    /** Shown before the first load: 17:00-21:00. */
    val DEFAULT_SLOT = TimeSlot(1020, 1260)

    /** Seeded when a day with no slots is switched on, or "Add hours" is used on an empty day: 09:00-17:00. */
    val SEED_SLOT = TimeSlot(540, 1020)

    private const val ADD_HOURS_GAP = 30
    private const val ADD_HOURS_LENGTH = 60
    private const val MINUTES_PER_HOUR = 60
    private const val MAX_HOUR = 24
    private val HHMM = Regex("^\\d{2}:\\d{2}$")

    fun defaultSchedule(): List<DaySchedule> = WEEK.map { DaySchedule(it, true, listOf(DEFAULT_SLOT)) }

    /** Strict `HH:mm` (two digits each) to minutes; hours 0..24, minutes 0..59, at most 24:00. Otherwise null. */
    fun parseHhmm(value: String?): Int? {
        if (value == null || !HHMM.matches(value)) return null
        val hours = value.substring(0, 2).toInt()
        val minutes = value.substring(3, 5).toInt()
        if (hours > MAX_HOUR || minutes >= MINUTES_PER_HOUR) return null
        val total = hours * MINUTES_PER_HOUR + minutes
        return if (total > MINUTES_PER_DAY) null else total
    }

    /** Minutes to `HH:mm` with ASCII digits. 1440 becomes "24:00". */
    fun minutesToHhmm(minutes: Int): String =
        String.format(Locale.ROOT, "%02d:%02d", minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR)

    /**
     * Builds the seven-day model from server ranges. Malformed values and slots with `end <= start` are dropped; a
     * day is enabled exactly when it keeps at least one slot. Absent days are disabled.
     */
    fun hydrate(raw: Map<DayOfWeek, List<Pair<String?, String?>>>): List<DaySchedule> =
        WEEK.map { day ->
            val slots = raw[day].orEmpty().mapNotNull { parseRange(it) }
            DaySchedule(day, slots.isNotEmpty(), slots)
        }

    /**
     * True when [hydrate] keeps every server range of [raw]. The silent D-15 timezone PUT is only safe then: the
     * hydrated schedule equals the server's, whereas otherwise the PUT would delete the dropped slots server-side
     * (WR-04).
     */
    fun isLossless(raw: Map<DayOfWeek, List<Pair<String?, String?>>>): Boolean =
        raw.values.all { ranges -> ranges.all { parseRange(it) != null } }

    /** The one rule shared by [hydrate] and [isLossless]: both ends parse and `end > start`, else null. */
    private fun parseRange(range: Pair<String?, String?>): TimeSlot? {
        val start = parseHhmm(range.first)
        val end = parseHhmm(range.second)
        return if (start != null && end != null && end > start) TimeSlot(start, end) else null
    }

    /**
     * True when any slot starts strictly before the running maximum end of the earlier slots, which matches the
     * server: touching slots are valid and a long slot containing two short ones overlaps.
     */
    fun dayOverlaps(day: DaySchedule): Boolean {
        var maxEnd = Int.MIN_VALUE
        for (slot in day.slots.sortedBy { it.start }) {
            if (maxEnd != Int.MIN_VALUE && slot.start < maxEnd) return true
            maxEnd = maxOf(maxEnd, slot.end)
        }
        return false
    }

    /** Enabled days that contain overlapping slots, in [WEEK] order. */
    fun overlappingDays(days: List<DaySchedule>): List<DayOfWeek> =
        days.filter { it.enabled && dayOverlaps(it) }.map { it.day }.sortedBy { WEEK.indexOf(it) }

    private fun update(
        days: List<DaySchedule>,
        day: DayOfWeek,
        transform: (DaySchedule) -> DaySchedule,
    ): List<DaySchedule> = days.map { if (it.day == day) transform(it) else it }

    /** Switching on a day with no slots seeds [SEED_SLOT]; switching off keeps the slots in the model. */
    fun setEnabled(
        days: List<DaySchedule>,
        day: DayOfWeek,
        enabled: Boolean,
    ): List<DaySchedule> =
        update(days, day) {
            val slots = if (enabled && it.slots.isEmpty()) listOf(SEED_SLOT) else it.slots
            it.copy(enabled = enabled, slots = slots)
        }

    /**
     * Whether Add hours has room (iOS plan 06-03, user decision U3): true for a day with no slots, otherwise the new
     * slot's start, the latest end across all slots plus [ADD_HOURS_GAP], must be before 23:00 ([LATEST_START]). The
     * latest end is the largest end, not the last slot's, since an earlier slot may have been edited to run later.
     */
    fun canAddHours(slots: List<TimeSlot>): Boolean {
        val maxEnd = slots.maxOfOrNull { it.end } ?: return true
        return maxEnd + ADD_HOURS_GAP < LATEST_START
    }

    /**
     * Appends a one-hour slot [ADD_HOURS_GAP] minutes after the latest end, its end clamped to 23:30 ([LATEST_END]),
     * as iOS `AvailabilitySlotRules.addHours`. A day with no room ([canAddHours] false) is returned unchanged, so a
     * new slot never starts before an existing end and the overlap banner is not needed for it.
     */
    fun addHours(
        days: List<DaySchedule>,
        day: DayOfWeek,
    ): List<DaySchedule> =
        update(days, day) { d ->
            val maxEnd = d.slots.maxOfOrNull { it.end }
            when {
                maxEnd == null -> d.copy(slots = d.slots + SEED_SLOT)
                !canAddHours(d.slots) -> d
                else -> {
                    val start = maxEnd + ADD_HOURS_GAP
                    d.copy(slots = d.slots + TimeSlot(start, minOf(start + ADD_HOURS_LENGTH, LATEST_END)))
                }
            }
        }

    fun removeSlot(
        days: List<DaySchedule>,
        day: DayOfWeek,
        index: Int,
    ): List<DaySchedule> =
        update(days, day) { d ->
            d.copy(slots = d.slots.filterIndexed { i, _ -> i != index })
        }

    fun replaceSlot(
        days: List<DaySchedule>,
        day: DayOfWeek,
        index: Int,
        slot: TimeSlot,
    ): List<DaySchedule> =
        update(days, day) { d ->
            d.copy(slots = d.slots.mapIndexed { i, s -> if (i == index) slot else s })
        }

    /** The request ranges for one day: `[]` when disabled, else the slots sorted by start as `HH:mm` pairs. */
    fun wireRanges(day: DaySchedule): List<Pair<String, String>> =
        if (!day.enabled) {
            emptyList()
        } else {
            day.slots.sortedBy { it.start }.map { minutesToHhmm(it.start) to minutesToHhmm(it.end) }
        }

    /**
     * The rows for the Start or End list of the time-range sheet (port of iOS `AvailabilitySlotRules.pickerRows`,
     * EXP-07, D-08): the 30-minute grid from 00:00 to 23:30 ([LATEST_END], iOS `lastGridTime`) for both lists, kept
     * when earlier than [draftEnd] (Start list) or later than [draftStart] (End list), plus [storedValue] when it
     * passes the same test. Strictly ascending, no duplicate. 24:00 is never a new End choice; it is a row only when
     * it is the stored end.
     *
     * [storedValue] is the slot's start (Start list) or end (End list) as the sheet opened, so an off-grid time
     * (09:15) or a stored 24:00 end stays a row for as long as the sheet is open, even after another row is tapped.
     *
     * A Start row of 23:30 can only appear with a 24:00 end, because every other end is at most 23:30.
     */
    fun pickerRows(
        isStart: Boolean,
        draftStart: Int,
        draftEnd: Int,
        storedValue: Int,
    ): List<Int> {
        val allowed: (Int) -> Boolean = if (isStart) { it -> it < draftEnd } else { it -> it > draftStart }
        val grid = (0..LATEST_END step STEP_MINUTES).filter(allowed)
        return (if (allowed(storedValue)) grid + storedValue else grid).distinct().sorted()
    }

    /**
     * [minutes] formatted with the device time format. The format is cloned and pinned to UTC so a DST gap in the
     * device zone cannot shift the clock face; 1440 renders as midnight. The passed [deviceFormat] is not mutated.
     */
    fun formatMinutes(
        minutes: Int,
        deviceFormat: DateFormat,
    ): String {
        val clone = deviceFormat.clone() as DateFormat
        clone.timeZone = TimeZone.getTimeZone("UTC")
        return clone.format(Date(minutes * MILLIS_PER_MINUTE))
    }

    /** Compact versioned string for SavedStateHandle: `v1|<timezoneId>|<day>;...` with `1:540-600,...` days. */
    fun encode(
        days: List<DaySchedule>,
        timezoneId: String,
    ): String {
        val body =
            WEEK.joinToString(";") { weekday ->
                val d = days.firstOrNull { it.day == weekday }
                val flag = if (d?.enabled == true) "1" else "0"
                flag + ":" + d?.slots.orEmpty().joinToString(",") { "${it.start}-${it.end}" }
            }
        return "$CODEC_VERSION|$timezoneId|$body"
    }

    /** Inverse of [encode]; null for anything malformed, not exactly seven days, or with an invalid slot. */
    fun decode(value: String?): Pair<List<DaySchedule>, String>? {
        if (value.isNullOrEmpty()) return null
        val parts = value.split("|")
        if (parts.size != CODEC_PARTS || parts[0] != CODEC_VERSION || parts[1].isBlank()) return null
        val dayStrings = parts[2].split(";")
        if (dayStrings.size != WEEK.size) return null
        val days =
            WEEK.mapIndexed { i, weekday ->
                val enabled =
                    when (dayStrings[i].substringBefore(":", "")) {
                        "1" -> true
                        "0" -> false
                        else -> return null
                    }
                val slotText = dayStrings[i].substringAfter(":", "")
                val slots =
                    if (slotText.isEmpty()) {
                        emptyList()
                    } else {
                        slotText.split(",").map { decodeSlot(it) ?: return null }
                    }
                DaySchedule(weekday, enabled, slots)
            }
        return days to parts[1]
    }

    private fun decodeSlot(text: String): TimeSlot? {
        val bits = text.split("-")
        if (bits.size != 2 || bits.any { it.isEmpty() || !it.all(Char::isDigit) }) return null
        val start = bits[0].toIntOrNull() ?: return null
        val end = bits[1].toIntOrNull() ?: return null
        return if (start in 0 until end && end <= MINUTES_PER_DAY) TimeSlot(start, end) else null
    }

    /** English list join without an Oxford comma: "A", "A and B", "A, B and C". */
    fun joinNames(
        names: List<String>,
        separator: String,
        lastSeparator: String,
    ): String =
        when (names.size) {
            0 -> ""
            1 -> names[0]
            else -> names.dropLast(1).joinToString(separator) + lastSeparator + names.last()
        }

    private const val MILLIS_PER_MINUTE = 60_000L
    private const val CODEC_VERSION = "v1"
    private const val CODEC_PARTS = 3
}
