package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.JsonClass

/** `GET /auth/email/status` payload. */
@JsonClass(generateAdapter = true)
data class EmailStatusData(
    val verified: Boolean,
)
