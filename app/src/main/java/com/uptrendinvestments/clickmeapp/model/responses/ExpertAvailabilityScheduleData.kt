package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * `GET /expert/availability` payload: the expert's OWN weekly schedule. Distinct from the public
 * [ExpertAvailabilityData] (`GET /experts/{id}/availability`). With no saved row the server returns
 * timezone id `UTC` and seven empty days.
 */
@JsonClass(generateAdapter = true)
data class ExpertAvailabilityScheduleData(
    val timezone: AvailabilityTimezoneDto? = null,
    @Json(name = "recurring_schedule") val recurringSchedule: RecurringScheduleDto? = null,
)

@JsonClass(generateAdapter = true)
data class AvailabilityTimezoneDto(
    val id: String? = null,
    @Json(name = "display_name") val displayName: String? = null,
    @Json(name = "utc_offset") val utcOffset: Double? = null,
)

/** Time ranges per weekday; a missing day decodes to null, an empty day to an empty list. */
@JsonClass(generateAdapter = true)
data class RecurringScheduleDto(
    val monday: List<AvailabilityTimeRangeDto>? = null,
    val tuesday: List<AvailabilityTimeRangeDto>? = null,
    val wednesday: List<AvailabilityTimeRangeDto>? = null,
    val thursday: List<AvailabilityTimeRangeDto>? = null,
    val friday: List<AvailabilityTimeRangeDto>? = null,
    val saturday: List<AvailabilityTimeRangeDto>? = null,
    val sunday: List<AvailabilityTimeRangeDto>? = null,
)

@JsonClass(generateAdapter = true)
data class AvailabilityTimeRangeDto(
    @Json(name = "start_time") val startTime: String? = null,
    @Json(name = "end_time") val endTime: String? = null,
)
