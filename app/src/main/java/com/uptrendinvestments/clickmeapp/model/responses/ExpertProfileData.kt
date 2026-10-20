package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * `GET /expert/profile` (partial). Phase 4 (SIGN-0x) extends this to the full iOS
 * `ExpertProfileData` for signup resume; Phase 2 reads only the flag for D-05.
 */
@JsonClass(generateAdapter = true)
data class ExpertProfileData(
    @Json(name = "setup_completed") val setupCompleted: Boolean,
)
