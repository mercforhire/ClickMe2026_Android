package com.uptrendinvestments.clickmeapp.model.requests

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * `POST /expert/topics` body. Constructor order fixes JSON key order. Null [description] and
 * [iconSlug] are omitted (the strict server schema rejects explicit nulls on create).
 */
@JsonClass(generateAdapter = true)
data class CreateExpertTopicRequest(
    val title: String,
    val description: String? = null,
    @Json(name = "duration_mins") val durationMins: Int,
    @Json(name = "is_free") val isFree: Boolean,
    @Json(name = "hourly_rate") val hourlyRate: TopicHourlyRateBody,
    @Json(name = "icon_slug") val iconSlug: String? = null,
    @Json(name = "expertise_tag_ids") val expertiseTagIds: List<String>,
)

/** `hourly_rate` object: [amount] in minor units, ISO [currency]. */
@JsonClass(generateAdapter = true)
data class TopicHourlyRateBody(
    val amount: Long,
    val currency: String,
)

/**
 * `PATCH /expert/topics/{id}` body. A full body so a cleared icon or description is sent as an
 * explicit `null`, except [durationMins]: null means the duration is unchanged and the key is omitted,
 * so the server keeps the stored value. A stored value the write schema would reject (an off-grid legacy
 * row) or that exceeds the 480 button cap is therefore never rewritten by an unrelated edit (WR-02).
 * Deliberately NOT `@JsonClass`: it is serialized only by `UpdateExpertTopicRequestAdapter`, which writes
 * those nulls (codegen would omit them).
 */
data class UpdateExpertTopicRequest(
    val title: String,
    val description: String?,
    val durationMins: Int?,
    val isFree: Boolean,
    val hourlyRate: TopicHourlyRateBody,
    val iconSlug: String?,
    val expertiseTagIds: List<String>,
)

/** `PUT /expert/availability` body. All seven day keys are always written. */
@JsonClass(generateAdapter = true)
data class UpdateMyAvailabilityRequest(
    @Json(name = "timezone_id") val timezoneId: String,
    val schedule: WeeklyScheduleBody,
)

@JsonClass(generateAdapter = true)
data class WeeklyScheduleBody(
    val monday: List<AvailabilityTimeRangeBody>,
    val tuesday: List<AvailabilityTimeRangeBody>,
    val wednesday: List<AvailabilityTimeRangeBody>,
    val thursday: List<AvailabilityTimeRangeBody>,
    val friday: List<AvailabilityTimeRangeBody>,
    val saturday: List<AvailabilityTimeRangeBody>,
    val sunday: List<AvailabilityTimeRangeBody>,
)

@JsonClass(generateAdapter = true)
data class AvailabilityTimeRangeBody(
    @Json(name = "start_time") val startTime: String,
    @Json(name = "end_time") val endTime: String,
)

/** `PATCH /expert/profile` body replacing the profile's expertise tags (full replace). */
@JsonClass(generateAdapter = true)
data class UpdateExpertTagsRequest(
    @Json(name = "expertise_tags") val expertiseTags: List<ExpertTagSelection>,
)

@JsonClass(generateAdapter = true)
data class ExpertTagSelection(
    @Json(name = "tag_id") val tagId: String,
    @Json(name = "is_primary") val isPrimary: Boolean,
)
