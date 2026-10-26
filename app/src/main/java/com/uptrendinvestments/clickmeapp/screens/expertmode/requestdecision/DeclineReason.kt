package com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision

import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.capNotes

/**
 * Longest accept message the backend `acceptSchema` allows (`message: z.string().max(1000).optional()`), measured in
 * Kotlin `String` length (UTF-16 units) as D-02 and D-08 specify.
 */
const val ACCEPT_MESSAGE_MAX_CHARS = 1000

/**
 * Longest decline note the backend `declineSchema` allows (`reason_text: z.string().min(1).max(1000)`), in Kotlin
 * `String` length. The edge text's 500 is the reschedule decline reason of EXP-09, not this field (D-02).
 */
const val DECLINE_NOTE_MAX_CHARS = 1000

/**
 * Why an expert declines a booking request, with the exact backend `reason_code` (D-01). The backend enum is
 * `CONFLICT | OUT_OF_SCOPE | UNAVAILABLE | OTHER`; iOS sends other codes and one extra pricing reason, which the strict
 * schema rejects with 422, so the iOS spellings are not used here (iOS follow-up is recorded by plan 11-15). Nothing
 * is preselected in the sheet.
 *
 * [wire] is the code sent in `reason_code`. [wireLabel] is the English label sent as `reason_text` when the expert
 * writes no note (D-02); it is never localized, and the screen shows the matching string resource.
 */
enum class DeclineReason(
    val wire: String,
    val wireLabel: String,
) {
    ScheduleConflict("CONFLICT", "Schedule conflict"),
    OutsideExpertise("OUT_OF_SCOPE", "Outside my expertise"),
    NotAvailable("UNAVAILABLE", "Not available at that time"),
    Other("OTHER", "Other reason"),
}

/**
 * The `reason_text` to send: the trimmed [note], or [DeclineReason.wireLabel] when the note is blank, capped at
 * [DECLINE_NOTE_MAX_CHARS]. The backend requires 1..1000 characters, while the UI keeps the note optional (D-02), so
 * the result is never empty.
 */
fun declineReasonText(
    reason: DeclineReason,
    note: String,
): String = capNotes(note.trim().ifEmpty { reason.wireLabel }, DECLINE_NOTE_MAX_CHARS)

/** The character counter under the message and note fields appears once the text reaches this length (D-08). */
internal const val NOTE_COUNTER_FROM = 800

/**
 * The accept `message` to send: the trimmed [raw] text capped at [ACCEPT_MESSAGE_MAX_CHARS], or null when it is blank
 * so the request body omits the key (`{}`), which the strict backend schema accepts.
 */
fun acceptMessage(raw: String): String? =
    raw
        .trim()
        .takeIf { it.isNotEmpty() }
        ?.let { capNotes(it, ACCEPT_MESSAGE_MAX_CHARS) }
