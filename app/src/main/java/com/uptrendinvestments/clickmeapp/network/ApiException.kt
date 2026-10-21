package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.model.responses.FieldErrorDto
import java.io.IOException

/** The single exception type every network call surfaces (see [apiCall]). */
sealed class ApiException(
    message: String?,
    cause: Throwable?,
) : Exception(message, cause) {
    /**
     * Text to show the user, or null so the caller shows its own string resource. Follows iOS
     * Error+UserMessage: the server message when present, else the first field error message.
     */
    open val userMessage: String? get() = null

    /** A non-2xx response. The exception message carries status and code only, never body text (T-02-08). */
    class Http(
        val status: Int,
        val code: ErrorCode?,
        val rawCode: String?,
        val serverMessage: String?,
        val fieldErrors: List<FieldErrorDto>,
        val detail: Map<String, Any?>?,
        val missingFields: List<String> = emptyList(),
    ) : ApiException("HTTP $status ${rawCode ?: ""}".trimEnd(), null) {
        override val userMessage: String? get() = serverMessage ?: fieldErrors.firstOrNull()?.message
    }

    /** Connectivity failure (timeout, DNS, TLS, offline). Callers map this to a string resource. */
    class Network(
        cause: IOException,
    ) : ApiException("Network error", cause)

    /** The response arrived but could not be decoded, or an unexpected non-IO failure occurred. */
    class Decoding(
        cause: Throwable,
    ) : ApiException("Decoding error", cause)
}
