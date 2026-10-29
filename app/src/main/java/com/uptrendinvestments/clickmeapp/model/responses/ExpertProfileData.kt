package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * `GET /expert/profile`, mirroring iOS `ExpertProfileData`. Phase 2 read only [setupCompleted]; Phase 4 uses
 * the rest to resume signup and prefill screens (D-10). Every field except [setupCompleted] is nullable with
 * a null default so older `{setup_completed}`-only bodies and explicit nulls still decode.
 *
 * Note the location key differs by direction: GET returns `state_province` ([ExpertLocationDetails]), the
 * PATCH body sends `province_state` (`SetupLocation`). `professional_details` is modelled since Phase 14 (the
 * expert meeting link); tag `category_id` is still not modelled; unknown keys are ignored.
 */
@JsonClass(generateAdapter = true)
data class ExpertProfileData(
    @Json(name = "setup_completed") val setupCompleted: Boolean,
    @Json(name = "profile_id") val profileId: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "personal_info") val personalInfo: ExpertPersonalInfo? = null,
    @Json(name = "location_details") val locationDetails: ExpertLocationDetails? = null,
    val languages: List<ExpertLanguage>? = null,
    @Json(name = "expertise_tags") val expertiseTags: List<ExpertTag>? = null,
    @Json(name = "professional_details") val professionalDetails: ExpertProfessionalDetails? = null,
)

/** `professional_details` of the expert profile. [meetingUrl] is the Skype/Zoom link (Phase 14, PROF-05). */
@JsonClass(generateAdapter = true)
data class ExpertProfessionalDetails(
    @Json(name = "job_title") val jobTitle: String? = null,
    val company: String? = null,
    val education: String? = null,
    @Json(name = "meeting_url") val meetingUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class ExpertPersonalInfo(
    @Json(name = "first_name") val firstName: String? = null,
    @Json(name = "last_name") val lastName: String? = null,
    val email: String? = null,
    val bio: String? = null,
)

@JsonClass(generateAdapter = true)
data class ExpertLocationDetails(
    val city: String? = null,
    @Json(name = "state_province") val stateProvince: String? = null,
    @Json(name = "country_code") val countryCode: String? = null,
    val timezone: String? = null,
)

/** [id] is null for a custom (free-text) language that has no catalogue entry. */
@JsonClass(generateAdapter = true)
data class ExpertLanguage(
    val id: String? = null,
    val label: String? = null,
)

@JsonClass(generateAdapter = true)
data class ExpertTag(
    val id: String,
    val label: String,
    @Json(name = "is_primary") val isPrimary: Boolean = false,
)
