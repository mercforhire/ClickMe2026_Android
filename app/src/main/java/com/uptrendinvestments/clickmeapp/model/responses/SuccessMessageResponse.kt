package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.JsonClass

/**
 * The `{status, message}` shape returned by the password-recovery endpoints, which do not use the
 * `{status, data}` envelope. Extra keys such as reset's `data.redirect_to` are ignored by Moshi.
 */
@JsonClass(generateAdapter = true)
data class SuccessMessageResponse(
    val status: String? = null,
    val message: String? = null,
)
