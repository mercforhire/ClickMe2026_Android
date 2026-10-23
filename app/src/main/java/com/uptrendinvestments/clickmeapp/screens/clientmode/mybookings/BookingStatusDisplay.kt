package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings

import java.util.Locale

const val STATUS_PENDING_APPROVAL = "pending_approval"
const val STATUS_PENDING_RESCHEDULE = "pending_reschedule"
const val STATUS_COMPLETED = "completed"

/**
 * Booking statuses the client may cancel: the backend cancel guard. Any other status is answered with
 * 409 INVALID_STATUS, so the UI offers Cancel only for these. The UI gate is convenience; the server is the authority.
 */
val CANCELLABLE_BOOKING_STATUSES: Set<String> = setOf("confirmed", STATUS_PENDING_APPROVAL, STATUS_PENDING_RESCHEDULE)

/**
 * Semantic colour of a status pill. The composable side maps these to `colorScheme.primary`, `tertiary`, `error` and
 * `onSurfaceVariant` respectively (plan 08-06), so this file stays free of Android and Compose types.
 */
enum class StatusTone { Primary, Tertiary, Error, Neutral }

/** Which wording a status pill shows. [Other] means the pill shows [humanizeStatus] of [BookingStatusDisplay.raw]. */
enum class BookingStatusLabel {
    Completed,
    Cancelled,
    Missed,
    Confirmed,
    PendingApproval,
    NewTimeProposed,
    InProgress,
    Other,
}

/** A status pill: [label] and [tone], plus the [raw] server string (used for the [BookingStatusLabel.Other] text). */
data class BookingStatusDisplay(
    val label: BookingStatusLabel,
    val tone: StatusTone,
    val raw: String,
)

/**
 * Badge for a row in the Past tab. Only statuses that belong in Past have a label; anything else (including
 * `confirmed` and unknown future server statuses) returns null and the row is dropped.
 */
fun pastStatusDisplay(status: String): BookingStatusDisplay? =
    when (status) {
        STATUS_COMPLETED -> BookingStatusDisplay(BookingStatusLabel.Completed, StatusTone.Primary, status)
        "cancelled", "declined" -> BookingStatusDisplay(BookingStatusLabel.Cancelled, StatusTone.Error, status)
        "missed", "expired" -> BookingStatusDisplay(BookingStatusLabel.Missed, StatusTone.Tertiary, status)
        else -> null
    }

/**
 * Status pill on the booking details screen. It shows the booking's real status (D-14). iOS hardcodes "Confirmed" in
 * `UpcomingBookingStatusRow`; that iOS defect is logged as a follow-up in plan 08-15. An unknown status gets a neutral
 * pill with [humanizeStatus] of the raw string rather than a wrong label.
 */
fun detailStatusDisplay(status: String): BookingStatusDisplay =
    when (status) {
        "confirmed" -> BookingStatusDisplay(BookingStatusLabel.Confirmed, StatusTone.Primary, status)
        STATUS_PENDING_APPROVAL -> BookingStatusDisplay(BookingStatusLabel.PendingApproval, StatusTone.Tertiary, status)
        STATUS_PENDING_RESCHEDULE ->
            BookingStatusDisplay(BookingStatusLabel.NewTimeProposed, StatusTone.Tertiary, status)
        "in_progress" -> BookingStatusDisplay(BookingStatusLabel.InProgress, StatusTone.Primary, status)
        STATUS_COMPLETED -> BookingStatusDisplay(BookingStatusLabel.Completed, StatusTone.Primary, status)
        else -> BookingStatusDisplay(BookingStatusLabel.Other, StatusTone.Neutral, status)
    }

/** Status for screens that can show a booking from either tab: the Past table first, then the details table. */
fun summaryStatusDisplay(status: String): BookingStatusDisplay =
    pastStatusDisplay(status) ?: detailStatusDisplay(status)

/** `no_show_both` becomes "No show both": underscores to spaces, first letter upper-cased. */
fun humanizeStatus(raw: String): String = raw.replace('_', ' ').replaceFirstChar { it.titlecase(Locale.ROOT) }

/** Money state of a paid booking, shown as a pill on the details screen (D-15). */
enum class PaymentPill { HoldPlaced, Paid, Refunded }

/**
 * Payment pill for a booking, or null when none applies (D-15). Free bookings never have one. The wording matches the
 * Phase 7 lifecycle card. A long-lead paid booking still in `scheduled` derives to `none`, so it shows no pill
 * (RESEARCH Open Question 2).
 */
fun paymentPillFor(
    isFree: Boolean,
    paymentStatus: String?,
): PaymentPill? =
    if (isFree) {
        null
    } else {
        when (paymentStatus) {
            "held" -> PaymentPill.HoldPlaced
            "captured" -> PaymentPill.Paid
            "refunded" -> PaymentPill.Refunded
            else -> null
        }
    }
