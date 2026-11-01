package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * `GET /experts/{id}/details` payload. Ports iOS `PublicProfileDetailsData`.
 *
 * [isBookable] null means bookable (iOS parity). The Book gating built on it is display-only (D-04): the
 * server enforces `EXPERT_NOT_BOOKABLE` when the booking is confirmed.
 *
 * [isFavorite] seeds the Expert Profile heart (Phase 16.3 D-01). The backend computes it best effort (false on any
 * lookup error and for self-view); the iOS list scan for it is deliberately not ported (its comment that details
 * lack the flag is stale).
 */
@JsonClass(generateAdapter = true)
data class PublicProfileDetailsData(
    @Json(name = "is_bookable") val isBookable: Boolean? = null,
    @Json(name = "is_favorite") val isFavorite: Boolean = false,
    @Json(name = "available_meeting_types") val availableMeetingTypes: List<String>? = null,
    val profile: PublicProfileDto = PublicProfileDto(),
    @Json(name = "topics_of_discussion") val topicsOfDiscussion: List<TopicOfDiscussionDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class PublicProfileDto(
    val name: String = "",
    val title: String? = null,
    val bio: String? = null,
    val stats: PublicProfileStatsDto = PublicProfileStatsDto(),
)

@JsonClass(generateAdapter = true)
data class PublicProfileStatsDto(
    @Json(name = "experience_years") val experienceYears: Int? = null,
    @Json(name = "total_bookings") val totalBookings: Int = 0,
)

@JsonClass(generateAdapter = true)
data class TopicOfDiscussionDto(
    val id: String,
    val title: String,
    @Json(name = "duration_mins") val durationMins: Int? = null,
    val description: String? = null,
    @Json(name = "icon_slug") val iconSlug: String? = null,
    val price: TopicPriceDto = TopicPriceDto(),
)

/** Flat per-session price in minor units. A free topic has a null [amount] and [isFree] true. */
@JsonClass(generateAdapter = true)
data class TopicPriceDto(
    val amount: Long? = null,
    val currency: String? = null,
    @Json(name = "is_free") val isFree: Boolean = false,
)
