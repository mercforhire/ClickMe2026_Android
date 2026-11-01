@file:Suppress("ktlint:standard:filename")

package com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule

import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingTimeSlot
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.NOTES_MAX_CHARS
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.PAID_BOOKING_MAX_LEAD
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.capNotes
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.isSelectable
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleDisclosure
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.STATUS_COMPLETED
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.STATUS_CONFIRMED
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.STATUS_IN_PROGRESS
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.STATUS_PENDING_APPROVAL
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.STATUS_PENDING_RESCHEDULE
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.rescheduleDisclosure
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/** iOS parity: a slot within this many milliseconds of the booking's start is the booking's own slot. */
private const val CURRENT_SLOT_TOLERANCE_MS = 1000L

/** Statuses whose Reschedule entry is offered at any time (server `NO_TIME_GATE_STATUSES`, minus pending_reschedule). */
private val RESCHEDULE_ANYTIME_STATUSES: Set<String> = setOf(STATUS_CONFIRMED, STATUS_PENDING_APPROVAL)

/** Statuses whose Reschedule entry is offered only until the session ends (server `TIME_GATED_STATUSES`). */
private val RESCHEDULE_UNTIL_END_STATUSES: Set<String> = setOf(STATUS_IN_PROGRESS, STATUS_COMPLETED)

/**
 * D-04 (as amended 2026-11-01, UAT G-16.2-1), RS-C1, WR-04: whether the client's "Reschedule" entry is shown for a
 * booking.
 *
 * (a) This is an allow-list mirroring the server's `canReschedule` (`src/utils/reschedule.js`). Confirmed and
 * pending_approval have no time gate. In_progress and completed apply only while [now] is before the session's [end].
 * Everything else fails closed: cancelled, declined, missed, expired, and any unknown or empty status.
 *
 * (b) Pending_reschedule ([STATUS_PENDING_RESCHEDULE]) is hidden although the server accepts it, because the
 * Accept/Decline card shows instead (D-04).
 *
 * (c) In_progress and completed are included because the Upcoming surfaces render them (the server Upcoming bucket
 * includes in_progress, and `isRejoinableCompleted` moves an unfinished completed session into Upcoming), and iOS shows
 * the button on every Upcoming card (`UpcomingBookingCard.swift` secondaryActions,
 * `UpcomingBookingSecondaryActions.swift`).
 *
 * (d) [end] is the session's own end, the server's refusal condition, not a start-based lead-time rule. There is still
 * no 24 h gate on the current session (D-04 amended). The only 24 h rule applies to the PROPOSED start of a
 * payment-conversion move (RESCHEDULE_OUTSIDE_AUTH_WINDOW).
 *
 * (e) The terminal hide matters because Upcoming Booking Details reloads on a realtime BookingUpdate, so an expert
 * cancel while the screen is open must not leave Reschedule as the only action on a dead booking.
 *
 * (f) [now] must come from the caller's ticking join clock so the end bound takes effect live.
 *
 * (g) The server stays the authority. Refusals surface through the D-13 propose error mapping (`ProposeErrors.kt`).
 */
fun canClientStartReschedule(
    status: String,
    end: Instant,
    now: Instant,
): Boolean =
    when (status) {
        in RESCHEDULE_ANYTIME_STATUSES -> true
        in RESCHEDULE_UNTIL_END_STATUSES -> now.isBefore(end)
        else -> false
    }

/**
 * Whether the booking is paid, which decides the 6-day pick cap (RESEARCH Pattern 3, Assumption A1).
 *
 * The client detail's `payment_status` is derived and reads `none` for a long-lead paid booking with no payment
 * intent yet, so the topic's free flag closes that gap. Over-capping is safe; a wrong "free" would hit the server's
 * `RESCHEDULE_TOO_FAR_AHEAD` backstop.
 */
fun clientBookingIsPaid(
    paymentStatus: String?,
    topicIsFree: Boolean,
): Boolean = (paymentStatus != null && paymentStatus != "none") || !topicIsFree

/**
 * D-01, D-02: the latest start the client may pick: [now] + [PAID_BOOKING_MAX_LEAD] for a paid booking (inclusive,
 * through [isSelectable]), null (no limit) for a free one.
 */
fun latestPaidStart(
    isPaid: Boolean,
    now: Instant,
): Instant? = if (isPaid) now.plus(PAID_BOOKING_MAX_LEAD) else null

/** D-12: this slot is the booking's own slot, within one second of [currentStart] (iOS `abs(delta) < 1`). */
fun BookingTimeSlot.isCurrent(currentStart: Instant?): Boolean =
    currentStart != null && abs(Duration.between(currentStart, start).toMillis()) < CURRENT_SLOT_TOLERANCE_MS

/**
 * D-12: the Make a Booking selectability rule plus the current-slot exclusion. `authorization_expires_at` is
 * deliberately not an input: the card-hold window never gates what can be picked (locked non-gating rule); it only
 * feeds the disclosure ([proposeDisclosure]).
 */
fun BookingTimeSlot.isPickable(
    now: Instant,
    latestStart: Instant?,
    currentStart: Instant?,
): Boolean = isSelectable(now, latestStart) && !isCurrent(currentStart)

/** The days with at least one pickable slot; a day whose only open slot is the current one is not open. */
fun Map<LocalDate, List<BookingTimeSlot>>.pickableDays(
    now: Instant,
    latestStart: Instant?,
    currentStart: Instant?,
): Set<LocalDate> = filterValues { slots -> slots.any { it.isPickable(now, latestStart, currentStart) } }.keys

/**
 * D-11 as amended 2026-10-31 by user override: the proposed session's end for the payment disclosure is
 * [proposedStart] + [originalDuration] first, the slot's [slotEnd] only when the duration is unavailable, else
 * [proposedStart].
 *
 * A slot's `end_utc` is start + 30 min for every slot because `topic_id` is never sent, so it is only a fallback;
 * the server's conversion check uses the original duration. This deliberately deviates from iOS
 * `RescheduleViewModel.sessionEnd(for:)` (end_utc first), which could skip the disclosure for a session longer than
 * 30 minutes whose true end lands after `authorization_expires_at`. Duration-first, not `max(...)`.
 */
fun disclosureSessionEnd(
    proposedStart: Instant,
    slotEnd: Instant?,
    originalDuration: Duration?,
): Instant =
    when {
        originalDuration != null -> proposedStart.plus(originalDuration)
        slotEnd != null -> slotEnd
        else -> proposedStart
    }

/**
 * D-11, RS-C5: reuses the accept-side comparison and its 48 hour `CHARGES_SOON_WINDOW` unchanged, over the amended
 * [disclosureSessionEnd]. Informational only; it never gates the send.
 */
fun proposeDisclosure(
    proposedStart: Instant,
    slotEnd: Instant?,
    originalDuration: Duration?,
    authorizationExpiresAt: Instant?,
    now: Instant,
): RescheduleDisclosure =
    rescheduleDisclosure(
        proposedStart,
        disclosureSessionEnd(proposedStart, slotEnd, originalDuration),
        authorizationExpiresAt,
        now,
    )

/** D-15: trimmed, capped at [NOTES_MAX_CHARS], and null when blank. */
fun normalizeRescheduleNote(raw: String): String? = capNotes(raw.trim(), NOTES_MAX_CHARS).takeIf { it.isNotBlank() }

/**
 * RESEARCH Pitfall 7: the day the picker opens on, the later of the booking's local date in [zone] and [today], so an
 * in-progress booking does not open on a past day.
 */
fun pickerSeedDate(
    bookingStart: Instant,
    zone: ZoneId,
    today: LocalDate,
): LocalDate {
    val bookingDate = bookingStart.atZone(zone).toLocalDate()
    return if (bookingDate.isBefore(today)) today else bookingDate
}

/** D-15: the UTC ISO-8601 instant at seconds precision the propose request carries (iOS parity). */
fun Instant.toProposedStartWire(): String = truncatedTo(ChronoUnit.SECONDS).toString()
