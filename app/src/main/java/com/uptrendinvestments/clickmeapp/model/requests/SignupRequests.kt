package com.uptrendinvestments.clickmeapp.model.requests

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * `POST /auth/signup` body. Signup is expert-only, matching iOS (D-01), so [role] defaults to
 * [ROLE_EXPERT]. [toString] redacts the credentials so they can never reach a log (T-04-02).
 */
@JsonClass(generateAdapter = true)
data class SignupRequest(
    val email: String,
    val password: String,
    val role: String = ROLE_EXPERT,
) {
    override fun toString(): String = "SignupRequest(email=<redacted>, password=<redacted>, role=$role)"

    companion object {
        const val ROLE_EXPERT = "expert"
    }
}

/** `POST /auth/email/verify` body: the 6-digit code from the verification email. */
@JsonClass(generateAdapter = true)
data class VerifyEmailCodeRequest(
    val code: String,
)

/**
 * `PATCH /expert/profile/setup` body, mirroring iOS `SetupExpertProfileRequest`. Every field is optional
 * and a null field is omitted from the JSON (the Moshi converter has no serializeNulls), which is what the
 * backend's strict partial-update schema needs (D-04, D-25). An empty list serializes as `[]` (clears the
 * list); a null list is omitted (leaves it untouched). Do not add member properties here: Moshi would try
 * to serialize them.
 */
@JsonClass(generateAdapter = true)
data class SetupExpertProfileRequest(
    @Json(name = "first_name") val firstName: String? = null,
    val location: SetupLocation? = null,
    val timezone: String? = null,
    val languages: List<String>? = null,
    @Json(name = "expertise_tags") val expertiseTags: List<String>? = null,
    @Json(name = "setup_completed") val setupCompleted: Boolean? = null,
)

/** The `location` object of [SetupExpertProfileRequest]. Note `province_state` here, `state_province` on GET. */
@JsonClass(generateAdapter = true)
data class SetupLocation(
    val city: String? = null,
    @Json(name = "province_state") val provinceState: String? = null,
    @Json(name = "country_code") val countryCode: String? = null,
)
