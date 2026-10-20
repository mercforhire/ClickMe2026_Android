package com.uptrendinvestments.clickmeapp.model.requests

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** `POST /auth/login` body. */
@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String,
)

/** `POST /auth/password/forgot` body. The key is `identity` (iOS and backend), not `email`. */
@JsonClass(generateAdapter = true)
data class ForgotPasswordRequest(
    val identity: String,
)

/** `POST /auth/password/reset` body. */
@JsonClass(generateAdapter = true)
data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val password: String,
    @Json(name = "password_confirmation") val passwordConfirmation: String,
)

/** `POST /auth/refresh` body. */
@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(
    @Json(name = "refresh_token") val refreshToken: String,
)
