package com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard

import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingItem
import com.uptrendinvestments.clickmeapp.model.responses.RESCHEDULE_INITIATED_BY_CLIENT
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.JOINABLE_BOOKING_STATUSES
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.STATUS_PENDING_RESCHEDULE
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

private val WHITESPACE = Regex("\\s+")

/**
 * The name the Dashboard hero greets: the first non-blank of [authFirstName] and [profileFirstName], trimmed, then
 * its first whitespace-delimited word ("Ada Marie" greets "Ada"). Null when both are blank, which the hero renders as
 * "Hello, there".
 */
fun dashboardFirstName(
    authFirstName: String?,
    profileFirstName: String?,
): String? =
    listOf(authFirstName, profileFirstName)
        .firstOrNull { !it.isNullOrBlank() }
        ?.trim()
        ?.split(WHITESPACE)
        ?.firstOrNull()

/**
 * Today's Sessions on the expert Dashboard (D-13, iOS parity): joinable bookings (confirmed, in progress or pending
 * reschedule) whose start falls on today's date in [zone], ascending by start. Today comes from [clock] so a
 * timezone change is honored. A session that already ended earlier today is kept, as on iOS and the client
 * Dashboard.
 */
fun List<ExpertBookingItem>.expertTodaysSessions(
    clock: Clock,
    zone: ZoneId,
): List<ExpertBookingItem> {
    val today = LocalDate.now(clock.withZone(zone))
    return filter { it.statusCode in JOINABLE_BOOKING_STATUSES && it.startTime.atZone(zone).toLocalDate() == today }
        .sortedBy { it.startTime }
}

/** True while the client's reschedule proposal waits for the expert's answer (D-12, D-18). */
fun ExpertBookingItem.hasClientProposedReschedule(): Boolean =
    statusCode == STATUS_PENDING_RESCHEDULE && reschedule?.initiatedBy == RESCHEDULE_INITIATED_BY_CLIENT

/**
 * The "N Reschedule Requests" banner count (D-12): only `pending_reschedule` bookings the client proposed. iOS counts
 * the expert's own proposals too, which the expert cannot act on, so they are excluded.
 */
fun List<ExpertBookingItem>.clientRescheduleCount(): Int = count { it.hasClientProposedReschedule() }
