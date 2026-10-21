package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * `POST /auth/signup` success payload. Same shape as [LoginData] plus [emailVerified]. [toString] redacts
 * both tokens so they can never reach a log (T-04-02).
 */
@JsonClass(generateAdapter = true)
data class SignupData(
    val token: String,
    val user: AuthUser,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "expires_at") val expiresAt: Long,
    @Json(name = "expires_in") val expiresIn: Long,
    @Json(name = "email_verified") val emailVerified: Boolean = false,
) {
    override fun toString(): String =
        "SignupData(token=<redacted>, user=$user, refreshToken=<redacted>, expiresAt=$expiresAt, " +
            "expiresIn=$expiresIn, emailVerified=$emailVerified)"
}
