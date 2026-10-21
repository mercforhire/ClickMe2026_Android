package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** `POST /user/profile/avatar` payload. */
@JsonClass(generateAdapter = true)
data class AvatarUploadData(
    @Json(name = "avatar_url") val avatarUrl: String,
)
