package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * `POST /user/account/delete-request` data. [deletionToken] is single use: hold it in memory only, never log or
 * save it, and send it back on `DELETE /user/account`. [toString] redacts it.
 */
@JsonClass(generateAdapter = true)
data class DeletionRequestData(
    @Json(name = "deletion_token") val deletionToken: String,
) {
    override fun toString(): String = "DeletionRequestData(deletionToken=<redacted>)"
}
