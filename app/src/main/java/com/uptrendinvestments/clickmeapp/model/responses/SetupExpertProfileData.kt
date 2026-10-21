package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** `PATCH /expert/profile/setup` success payload, mirroring iOS `SetupExpertProfileData`. */
@JsonClass(generateAdapter = true)
data class SetupExpertProfileData(
    @Json(name = "profile_id") val profileId: String? = null,
    @Json(name = "completion_percentage") val completionPercentage: Int? = null,
    @Json(name = "missing_fields") val missingFields: List<String>? = null,
    @Json(name = "next_recommended_step") val nextRecommendedStep: String? = null,
    @Json(name = "setup_completed") val setupCompleted: Boolean? = null,
)
