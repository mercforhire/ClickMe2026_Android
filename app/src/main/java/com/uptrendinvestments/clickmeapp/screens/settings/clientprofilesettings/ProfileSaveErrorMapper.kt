package com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings

import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.login.FieldError

/**
 * The "Couldn't save changes" text for a failed Profile Settings save, shared by the client and expert view models
 * (iOS `ProfileSaveErrorMapper`, G-05-6).
 *
 * A `422 VALIDATION_ERROR` names the fields to check, using the on-screen labels in screen order. It never shows the
 * server's field paths or raw validation text (for example "Too small: ..."). The sentence is client-owned copy built
 * here, not a string resource, because the view models have no `Context` and `FieldError.Res` takes no arguments.
 */
object ProfileSaveErrorMapper {
    /** Shown when a 422 names no field this table knows; the same words the backend sends as its generic message. */
    private const val GENERIC = "Please check the information you entered and try again."

    /**
     * Backend field path to on-screen label, in screen order. A path matches a key when it equals it or continues it
     * after a dot, so array items such as `languages.language_ids.0` fall under `languages`.
     */
    private val labelTable =
        listOf(
            "basic.first_name" to "First Name",
            "basic.last_name" to "Last Name",
            "basic.bio" to "Bio",
            "professional.job_title" to "Job Title",
            "professional.company" to "Company",
            "location.city" to "City",
            "location.state_province" to "State/Province",
            "location.country_code" to "Country",
            "languages" to "Spoken Languages",
        )

    /** "Check A, B and C, then try again." for the known [fieldPaths], or the generic sentence when none is known. */
    fun message(fieldPaths: List<String>): String {
        val labels =
            labelTable
                .filter { (key, _) -> fieldPaths.any { it == key || it.startsWith("$key.") } }
                .map { (_, label) -> label }
        if (labels.isEmpty()) return GENERIC
        return "Check ${joined(labels)}, then try again."
    }

    /** "A", "A and B", "A, B and C". */
    private fun joined(labels: List<String>): String =
        if (labels.size == 1) labels.single() else labels.dropLast(1).joinToString(", ") + " and " + labels.last()

    /**
     * The alert text for a failed save, or null when [error] is not a `422 VALIDATION_ERROR` (every other failure keeps
     * the caller's existing handling). Branches on the status and [ErrorCode], never on message text.
     */
    internal fun messageFor(error: ApiException): String? {
        val http = error as? ApiException.Http ?: return null
        if (http.status != HTTP_UNPROCESSABLE || http.code != ErrorCode.ValidationError) return null
        return message(http.fieldErrors.map { it.field })
    }
}

/**
 * The dialog for a failed Profile Settings save: the field-naming sentence for a `422 VALIDATION_ERROR`, otherwise the
 * usual [toSaveDialogError]. Null for ACCOUNT_SUSPENDED, whose dialog is global.
 */
internal fun ApiException.toProfileSaveDialogError(): FieldError? =
    ProfileSaveErrorMapper.messageFor(this)?.let { FieldError.Text(it) } ?: toSaveDialogError()
