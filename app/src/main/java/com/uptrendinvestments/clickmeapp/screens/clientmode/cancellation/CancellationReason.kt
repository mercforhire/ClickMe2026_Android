package com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation

import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.capNotes

/**
 * Cancellation reasons, in the iOS `defaultReasons` order (the first entry is the default selection).
 *
 * [wireLabel] is the value sent to the server and is never localized. The screen shows matching string resources
 * (plan 08-04), and a Robolectric test (plan 08-11) keeps the two equal.
 */
enum class CancellationReason(
    val wireLabel: String,
) {
    ChangeOfPlans("Change of plans"),
    FoundAnotherExpert("Found another expert"),
    ScheduleConflict("Schedule conflict"),
    PersonalEmergency("Personal emergency"),
    Other("Other"),
}

/** Comment input cap. The longest label (20) plus ": " (2) keeps the merged reason at 472 units (D-05). */
const val CANCEL_COMMENT_MAX_CHARS = 450

/** Backend `cancelSchema` maximum for `reason` (strict schema, no other keys accepted). */
const val CANCEL_REASON_MAX_CHARS = 500

/** Caps the comment input at [CANCEL_COMMENT_MAX_CHARS] without leaving a lone high surrogate. */
fun capCancelComment(text: String): String = capNotes(text, CANCEL_COMMENT_MAX_CHARS)

/**
 * The `reason` string sent on cancel: `"<label>: <comment>"` when the trimmed [comment] is non-blank, else just the
 * label, never longer than [CANCEL_REASON_MAX_CHARS] UTF-16 units (D-05).
 *
 * This settles the Phase 8 open item: the comment travels inside `reason`, the only key the strict backend schema
 * accepts. iOS collects the comment but drops it (iOS follow-up, logged in plan 08-15), and the backend validates
 * `reason` but does not store it today (backend note).
 */
fun cancelReason(
    reason: CancellationReason,
    comment: String,
): String {
    val trimmed = comment.trim()
    val text = if (trimmed.isEmpty()) reason.wireLabel else "${reason.wireLabel}: $trimmed"
    return capNotes(text, CANCEL_REASON_MAX_CHARS)
}
