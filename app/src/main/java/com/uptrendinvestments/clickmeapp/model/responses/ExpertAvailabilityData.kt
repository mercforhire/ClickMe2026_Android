package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

/**
 * `GET /experts/{id}/availability?month=yyyy-MM` payload. Ports iOS `BookingCalendarPayload`.
 *
 * Every field is optional, so a missing or null value cannot turn the booking screen into a decoding error.
 * A present but malformed timestamp still can: `start_utc`/`end_utc` go through the strict
 * [com.uptrendinvestments.clickmeapp.network.InstantAdapter], so one bad value fails the whole month as
 * `ApiException.Decoding` (the same as iOS, whose `Date` decoding throws too).
 *
 * The backend also sends the local-time label keys, which are deliberately not modelled: they are formatted
 * in the profile zone, not the zone the user picked (the UI formats `startUtc` itself).
 *
 * @property clientTimezone decoded for parity with the iOS DTO but deliberately not read: slots are grouped and
 * labelled in the device zone (`MakeABookingUiState.zone`), not the zone saved on the profile.
 */
@JsonClass(generateAdapter = true)
data class ExpertAvailabilityData(
    @Json(name = "expert_timezone") val expertTimezone: String? = null,
    @Json(name = "client_timezone") val clientTimezone: String? = null,
    val month: String? = null,
    val days: List<AvailabilityDayDto> = emptyList(),
)

/**
 * One calendar day of the availability grid.
 *
 * @property date the EXPERT-local day. It must never be used for grouping slots into the viewer's days;
 * group by `startUtc` converted to the viewer's zone instead (D-07).
 * @property slots null when the expert has nothing on that day.
 */
@JsonClass(generateAdapter = true)
data class AvailabilityDayDto(
    val date: String? = null,
    val slots: List<AvailabilitySlotDto>? = null,
)

/**
 * One bookable time slot.
 *
 * @property held not sent by the current backend. Redis holds arrive as `available: false`. Kept for iOS
 * parity so an older or future backend that does send it still decodes.
 */
@JsonClass(generateAdapter = true)
data class AvailabilitySlotDto(
    @Json(name = "start_utc") val startUtc: Instant? = null,
    @Json(name = "end_utc") val endUtc: Instant? = null,
    val available: Boolean? = null,
    val held: Boolean? = null,
)
