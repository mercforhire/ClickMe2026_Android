package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.JsonClass

/** `GET /meta/languages` payload. */
@JsonClass(generateAdapter = true)
data class LanguagesPayload(
    val languages: List<LanguageItem>,
)

@JsonClass(generateAdapter = true)
data class LanguageItem(
    val id: String,
    val label: String,
)

/** `GET /meta/expertise-tags` payload. */
@JsonClass(generateAdapter = true)
data class ExpertiseTagsPayload(
    val tags: List<ExpertiseTagItem>,
)

@JsonClass(generateAdapter = true)
data class ExpertiseTagItem(
    val id: String,
    val label: String,
)
