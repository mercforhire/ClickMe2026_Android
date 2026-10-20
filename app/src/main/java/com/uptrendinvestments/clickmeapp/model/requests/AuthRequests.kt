package com.uptrendinvestments.clickmeapp.model.requests

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** `POST /auth/login` body. */
@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String,
)

/** `POST /auth/refresh` body. */
@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(
    @Json(name = "refresh_token") val refreshToken: String,
)
