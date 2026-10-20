package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.JsonClass

/** Generic `{status, data}` success envelope, mirroring iOS `SuccessDataResponse`. */
@JsonClass(generateAdapter = true)
data class SuccessDataResponse<T>(
    val status: String,
    val data: T,
)
