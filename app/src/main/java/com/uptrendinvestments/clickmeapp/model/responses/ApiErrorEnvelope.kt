package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * One DTO for all three backend error shapes: the standard `{status, code?, message}` envelope, the field
 * validation envelope with `errors[]`, and the status-only shape. Every field is optional so decoding never
 * fails on a missing key. `code` stays a raw string; map it with `ErrorCode.fromWire` so unknown codes survive.
 */
@JsonClass(generateAdapter = true)
data class ApiErrorEnvelope(
    val status: String? = null,
    val code: String? = null,
    val message: String? = null,
    val errors: List<FieldErrorDto>? = null,
    val detail: Map<String, Any?>? = null,
    /** Top-level list on the expert publish 422, outside `errors[]` (D-06). */
    @Json(name = "missing_fields") val missingFields: List<String>? = null,
)

@JsonClass(generateAdapter = true)
data class FieldErrorDto(
    val field: String,
    val message: String,
)
