package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * `POST /auth/login` success payload, mirroring iOS `LoginData`.
 *
 * [expiresAt] is the access-token expiry as unix epoch seconds; [expiresIn] is seconds
 * until it expires.
 */
@JsonClass(generateAdapter = true)
data class LoginData(
    val token: String,
    val user: AuthUser,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "expires_at") val expiresAt: Long,
    @Json(name = "expires_in") val expiresIn: Long,
)
