package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * `POST /auth/refresh` success payload. Same token trio as [LoginData] but without the
 * user snapshot; the client keeps whatever cached user it already had.
 *
 * Reuse contract: [refreshToken] **rotates**. The old value is invalidated the moment this
 * response lands. Store the new one immediately and discard the old one; replaying an old
 * refresh token triggers server-side reuse detection and revokes the entire session (all
 * future requests 401 until re-login).
 *
 * [expiresAt] is the access-token expiry as unix epoch **seconds**; [expiresIn] is seconds
 * until the new access token expires.
 */
@JsonClass(generateAdapter = true)
data class RefreshTokenData(
    val token: String,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "expires_at") val expiresAt: Long,
    @Json(name = "expires_in") val expiresIn: Long,
)
