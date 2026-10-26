package com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation

import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.CANCEL_REASON_MAX_CHARS
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.capNotes

/**
 * Expert cancellation reasons in the iOS order (D-17). Nothing is preselected (UI-SPEC A10).
 *
 * [wireLabel] is the value sent to the server and is never localized; the screen shows matching string resources.
 * This is a separate enum from the client one so the client flow stays untouched.
 */
enum class ExpertCancelReason(
    val wireLabel: String,
) {
    PersonalEmergency("Personal Emergency"),
    SchedulingConflict("Scheduling Conflict"),
    TechnicalIssues("Technical Issues"),
    Other("Other"),
}

/**
 * The `reason` string sent on cancel: `"<label>: <comment>"` when the trimmed [comment] is non-blank, else just the
 * label, never longer than [CANCEL_REASON_MAX_CHARS] UTF-16 units (D-17). The comment input itself is capped at 450
 * by `capCancelComment`.
 */
fun expertCancelReason(
    reason: ExpertCancelReason,
    comment: String,
): String {
    val trimmed = comment.trim()
    val text = if (trimmed.isEmpty()) reason.wireLabel else "${reason.wireLabel}: $trimmed"
    return capNotes(text, CANCEL_REASON_MAX_CHARS)
}
