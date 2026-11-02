package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

/**
 * The expert's view of one of their clients (`GET /expert/clients/{id}`). Ports iOS `ExpertClientProfileData`.
 *
 * The server sends no email or phone. [ClientLocationDto.country] is the raw code the server sends and
 * [ClientLocationDto.timezone] is a human-readable label. Names and bio are untrusted plain text, so [toString]
 * prints the id only.
 */
@JsonClass(generateAdapter = true)
data class ExpertClientProfileData(
    val id: String,
    @Json(name = "personal_details") val personalDetails: ClientPersonalDetailsDto = ClientPersonalDetailsDto(),
    val location: ClientLocationDto = ClientLocationDto(),
    val languages: List<String> = emptyList(),
    @Json(name = "member_since") val memberSince: Instant? = null,
    val relationship: ClientRelationshipDto = ClientRelationshipDto(),
) {
    override fun toString(): String = "ExpertClientProfileData(id=$id)"
}

/** Who the client is. Every field can be null. */
@JsonClass(generateAdapter = true)
data class ClientPersonalDetailsDto(
    @Json(name = "first_name") val firstName: String? = null,
    @Json(name = "last_name") val lastName: String? = null,
    @Json(name = "full_name") val fullName: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
)

/** Where the client is. Every field can be null. */
@JsonClass(generateAdapter = true)
data class ClientLocationDto(
    val city: String? = null,
    @Json(name = "state_province") val stateProvince: String? = null,
    val country: String? = null,
    val timezone: String? = null,
)

/** This expert's history with the client. */
@JsonClass(generateAdapter = true)
data class ClientRelationshipDto(
    @Json(name = "total_sessions") val totalSessions: Int = 0,
    @Json(name = "first_session_date") val firstSessionDate: Instant? = null,
    @Json(name = "last_session_date") val lastSessionDate: Instant? = null,
)
