@file:Suppress("ktlint:standard:filename")

package com.uptrendinvestments.clickmeapp.network

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.uptrendinvestments.clickmeapp.model.requests.UpdateExpertTopicRequest

/**
 * Writes [UpdateExpertTopicRequest] with explicit `null` for `description` and `icon_slug`.
 *
 * The app-wide Moshi omits nulls, but the PATCH contract clears those two fields only on a literal
 * `null`. `serializeNulls` is switched on around exactly those two names and restored in a `finally`.
 * `duration_mins` is omitted when null (unchanged); the server rejects an explicit null. Request-only:
 * decoding is unsupported.
 */
class UpdateExpertTopicRequestAdapter : JsonAdapter<UpdateExpertTopicRequest>() {
    override fun toJson(
        writer: JsonWriter,
        value: UpdateExpertTopicRequest?,
    ) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        writer.name("title").value(value.title)
        writeNullable(writer, "description", value.description)
        value.durationMins?.let { writer.name("duration_mins").value(it.toLong()) }
        writer.name("is_free").value(value.isFree)
        writer.name("hourly_rate")
        writer.beginObject()
        writer.name("amount").value(value.hourlyRate.amount)
        writer.name("currency").value(value.hourlyRate.currency)
        writer.endObject()
        writeNullable(writer, "icon_slug", value.iconSlug)
        writer.name("expertise_tag_ids")
        writer.beginArray()
        value.expertiseTagIds.forEach { writer.value(it) }
        writer.endArray()
        writer.endObject()
    }

    private fun writeNullable(
        writer: JsonWriter,
        name: String,
        text: String?,
    ) {
        val previous = writer.serializeNulls
        writer.serializeNulls = true
        try {
            writer.name(name)
            if (text == null) writer.nullValue() else writer.value(text)
        } finally {
            writer.serializeNulls = previous
        }
    }

    override fun fromJson(reader: JsonReader): UpdateExpertTopicRequest? =
        throw UnsupportedOperationException("UpdateExpertTopicRequest is request-only")
}
