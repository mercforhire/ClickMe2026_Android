package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * `GET /client/home` payload. Ports iOS `ClientHomeData`. Every list defaults to empty and every scalar is
 * nullable so a single missing key cannot fail the whole feed decode.
 */
@JsonClass(generateAdapter = true)
data class ClientHomeData(
    @Json(name = "trending_categories") val trendingCategories: List<TrendingCategoryDto> = emptyList(),
    @Json(name = "recommended_experts") val recommendedExperts: List<RecommendedExpertDto> = emptyList(),
    @Json(name = "featured_topics") val featuredTopics: List<FeaturedTopicDto>? = null,
)

/** A trending category chip. [id] is the category slug sent back as the `category` filter. */
@JsonClass(generateAdapter = true)
data class TrendingCategoryDto(
    val id: String,
    val name: String? = null,
    @Json(name = "icon_name") val iconName: String? = null,
    @Json(name = "color_accent") val colorAccent: String? = null,
)

@JsonClass(generateAdapter = true)
data class RecommendedExpertDto(
    @Json(name = "expert_id") val expertId: String,
    @Json(name = "full_name") val fullName: String? = null,
    val title: String? = null,
    @Json(name = "profile_image_url") val profileImageUrl: String? = null,
    val rating: Double? = null,
    @Json(name = "review_count") val reviewCount: Int? = null,
    @Json(name = "years_experience") val yearsExperience: Int? = null,
    @Json(name = "expertise_tags") val expertiseTags: List<String>? = null,
)

@JsonClass(generateAdapter = true)
data class FeaturedTopicDto(
    @Json(name = "topic_id") val topicId: String,
    val title: String,
    @Json(name = "duration_mins") val durationMins: Int? = null,
    /**
     * Despite the key name this is the flat per-session price in minor units, not an hourly rate
     * (CONTEXT D-21). A free topic arrives with a null [MoneyDto.amount].
     */
    @Json(name = "hourly_rate") val hourlyRate: MoneyDto? = null,
    /** Top-level free flag, a sibling of `hourly_rate`; null is treated as not free. */
    @Json(name = "is_free") val isFree: Boolean? = null,
    val expert: FeaturedTopicExpertDto,
    val rating: Double? = null,
)

@JsonClass(generateAdapter = true)
data class FeaturedTopicExpertDto(
    @Json(name = "expert_id") val expertId: String,
    @Json(name = "full_name") val fullName: String? = null,
    @Json(name = "profile_image_url") val profileImageUrl: String? = null,
)

/** Money in minor units (cents). Both fields are null for a free topic. */
@JsonClass(generateAdapter = true)
data class MoneyDto(
    val amount: Long? = null,
    val currency: String? = null,
)
