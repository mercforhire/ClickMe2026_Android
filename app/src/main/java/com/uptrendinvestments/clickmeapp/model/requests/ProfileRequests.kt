package com.uptrendinvestments.clickmeapp.model.requests

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * `PATCH /user/profile` body, ported from iOS `UpdateUserProfileRequest`. Partial-merge semantics: only the groups
 * that are non-null are sent (the converter has no `serializeNulls`, so a null property is omitted), and inside a
 * group only the non-null fields. There is deliberately no email property anywhere: the server answers 403 for
 * `basic.email`. Server maximums: names 100, bio 500, job title and company 200.
 */
@JsonClass(generateAdapter = true)
data class UpdateUserProfileRequest(
    val basic: ProfileBasicBody? = null,
    val location: ProfileLocationBody? = null,
    val professional: ProfileProfessionalBody? = null,
    val languages: ProfileLanguagesBody? = null,
)

/** `basic` group. An empty [bio] is sent as `""` to clear it. */
@JsonClass(generateAdapter = true)
data class ProfileBasicBody(
    @Json(name = "first_name") val firstName: String? = null,
    @Json(name = "last_name") val lastName: String? = null,
    val bio: String? = null,
)

/** `location` group. [countryCode] is the alpha-3 code (iOS x-discrepancy #17: the GET calls the same value `country`). */
@JsonClass(generateAdapter = true)
data class ProfileLocationBody(
    val city: String? = null,
    @Json(name = "state_province") val stateProvince: String? = null,
    @Json(name = "country_code") val countryCode: String? = null,
)

/** `professional` group. */
@JsonClass(generateAdapter = true)
data class ProfileProfessionalBody(
    @Json(name = "job_title") val jobTitle: String? = null,
    val company: String? = null,
)

/**
 * `languages` group. Both lists are non-null: the group present means a full replace of the caller's languages, so an
 * empty list is sent as `[]` and clears that side (RESEARCH Finding 5).
 */
@JsonClass(generateAdapter = true)
data class ProfileLanguagesBody(
    @Json(name = "language_ids") val languageIds: List<String>,
    @Json(name = "custom_languages") val customLanguages: List<String>,
)

/**
 * `PATCH /expert/profile` body that sets only the expert's Skype/Zoom meeting link. A separate type from
 * [UpdateExpertTagsRequest] so the two calls never send each other's keys. Ports iOS `UpdateExpertProfileRequest`.
 */
@JsonClass(generateAdapter = true)
data class UpdateExpertMeetingUrlRequest(
    @Json(name = "professional_details") val professionalDetails: MeetingUrlBody,
)

@JsonClass(generateAdapter = true)
data class MeetingUrlBody(
    @Json(name = "meeting_url") val meetingUrl: String,
)
