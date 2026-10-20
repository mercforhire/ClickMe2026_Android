package com.uptrendinvestments.clickmeapp.network

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.time.Instant
import java.time.OffsetDateTime

/**
 * Moshi adapter for ISO-8601 timestamps as [Instant].
 *
 * The backend emits microsecond fractions (`2026-10-20T16:17:09.983286+00:00`), which iOS
 * has to truncate by regex before decoding. `java.time` (native at minSdk 26) parses any
 * fraction width and any numeric offset directly, so no truncation is needed. Strings
 * without an offset or with a space separator are not ISO-8601 and throw.
 */
class InstantAdapter {
    @FromJson
    fun fromJson(raw: String): Instant = OffsetDateTime.parse(raw).toInstant()

    @ToJson
    fun toJson(value: Instant): String = value.toString()
}
