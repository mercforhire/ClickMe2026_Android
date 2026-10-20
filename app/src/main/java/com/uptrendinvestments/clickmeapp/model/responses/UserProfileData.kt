package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * `GET /user/profile`: full mirror of iOS `UserProfileData`, so it can be cached as the
 * offline profile (D-01, resolved Q1). Every nested field is optional.
 */
@JsonClass(generateAdapter = true)
data class UserProfileData(
    @Json(name = "personal_details") val personalDetails: PersonalDetails,
    @Json(name = "professional_details") val professionalDetails: ProfessionalDetails,
    val location: Location,
    val languages: List<LanguageEntry> = emptyList(),
) {
    @JsonClass(generateAdapter = true)
    data class PersonalDetails(
        @Json(name = "first_name") val firstName: String? = null,
        @Json(name = "last_name") val lastName: String? = null,
        val email: String? = null,
        @Json(name = "avatar_url") val avatarUrl: String? = null,
        val bio: String? = null,
    )

    @JsonClass(generateAdapter = true)
    data class ProfessionalDetails(
        @Json(name = "job_title") val jobTitle: String? = null,
        val company: String? = null,
        val education: String? = null,
    )

    @JsonClass(generateAdapter = true)
    data class Location(
        val city: String? = null,
        @Json(name = "state_province") val stateProvince: String? = null,
        /** Despite the key name, this holds the alpha-3 code (e.g. "USA"); iOS x-discrepancy #17. */
        val country: String? = null,
        val timezone: String? = null,
    )

    @JsonClass(generateAdapter = true)
    data class LanguageEntry(
        val id: String? = null,
        val label: String? = null,
    )
}
