package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.uptrendinvestments.clickmeapp.model.enums.UserRole

/**
 * User snapshot returned by `POST /auth/login`, mirroring iOS `AuthUser`.
 *
 * [id] is a plain string (Moshi has no UUID adapter and nothing needs UUID math).
 * [roles] stays raw so unknown roles never fail decoding; use [userRoles] for typed access.
 */
@JsonClass(generateAdapter = true)
data class AuthUser(
    val id: String,
    @Json(name = "first_name") val firstName: String,
    @Json(name = "last_name") val lastName: String,
    val roles: List<String>,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
) {
    val userRoles: Set<UserRole>
        get() = roles.mapNotNull(UserRole::fromWire).toSet()
}
