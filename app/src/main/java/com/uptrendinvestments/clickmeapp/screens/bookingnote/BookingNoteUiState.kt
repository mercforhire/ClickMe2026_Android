package com.uptrendinvestments.clickmeapp.screens.bookingnote

import com.uptrendinvestments.clickmeapp.screens.login.FieldError

/** The most characters a note may hold: the backend zod `max(5000)`, counted in UTF-16 code units (D-02). */
const val NOTE_MAX_LENGTH = 5_000

/** The note length from which the character counter is shown (RESEARCH A7). */
const val NOTE_COUNTER_FROM = 4_500

/**
 * The text that is saved and compared: a whitespace-only note is an empty note, anything else is kept as typed (D-04).
 * Applied to the body sent and to both sides of the change check, so a note that is blank on the server never counts
 * as a change (RESEARCH Pitfall 4).
 */
fun normalizeNote(text: String): String = if (text.isBlank()) "" else text

/** Which note is open: [role] picks the endpoint pair, [bookingId] the booking. */
data class BookingNoteArgs(
    val role: NoteRole,
    val bookingId: String,
)

/**
 * What the editor header shows. Passed in by the route because the note endpoints return none of it (D-05). Times are
 * epoch millis, as in the navigation keys.
 */
data class BookingNoteHeader(
    val name: String,
    val topic: String?,
    val startMillis: Long,
    val endMillis: Long,
)

/** Where the note load stands. A 404 on load is [Loaded] with an empty note, never [Failed]. */
sealed interface BookingNoteLoad {
    data object Loading : BookingNoteLoad

    data class Failed(
        val message: FieldError,
    ) : BookingNoteLoad

    data object Loaded : BookingNoteLoad
}

/**
 * State of the note editor. [lastPersisted] is the normalized text the server holds, so [hasChanges] compares like
 * with like. [didSave] shows the saved mark for a moment after a successful save.
 */
data class BookingNoteUiState(
    val load: BookingNoteLoad = BookingNoteLoad.Loading,
    val noteText: String = "",
    val lastPersisted: String = "",
    val isSaving: Boolean = false,
    val didSave: Boolean = false,
    val saveError: FieldError? = null,
    val showDiscardPrompt: Boolean = false,
) {
    val hasChanges: Boolean get() = normalizeNote(noteText) != lastPersisted
    val canSave: Boolean get() = load == BookingNoteLoad.Loaded && hasChanges && !isSaving
    val showCounter: Boolean get() = noteText.length >= NOTE_COUNTER_FROM
    val atCap: Boolean get() = noteText.length >= NOTE_MAX_LENGTH

    /** Never prints the note, only its length (T-16.4-10). */
    override fun toString(): String =
        "BookingNoteUiState(load=$load, noteLength=${noteText.length}, isSaving=$isSaving, didSave=$didSave, " +
            "saveError=${saveError != null}, showDiscardPrompt=$showDiscardPrompt)"
}
