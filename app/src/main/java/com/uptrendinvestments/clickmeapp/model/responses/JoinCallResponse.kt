package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * `POST /bookings/{id}/join` data. Port of iOS JoinCallResponse (PITFALL 2 dual shape): exactly one config is
 * non-null. Callers branch on [connectionType] (`in_app_voice` or `skype_zoom`, kept as the raw wire string) and
 * then require the matching config; a missing one is a malformed response (RESEARCH Pitfall 15).
 */
@JsonClass(generateAdapter = true)
data class JoinCallResponse(
    @Json(name = "booking_id") val bookingId: String,
    @Json(name = "connection_type") val connectionType: String,
    @Json(name = "agora_config") val agoraConfig: AgoraConfig? = null,
    @Json(name = "external_config") val externalConfig: ExternalConfig? = null,
)

/**
 * Agora join credentials for an `in_app_voice` booking. [toString] redacts every value so the RTC token cannot
 * reach a log line or crash report by accident (RESEARCH Security V8, T-09-02).
 */
@JsonClass(generateAdapter = true)
data class AgoraConfig(
    @Json(name = "rtc_token") val rtcToken: String,
    val uid: Int,
    @Json(name = "channel_name") val channelName: String,
) {
    override fun toString(): String = "AgoraConfig(uid=<redacted>, channelName=<redacted>, rtcToken=<redacted>)"
}

/** External meeting details for a `skype_zoom` booking; [joinUrl] is null while the expert has set no link. */
@JsonClass(generateAdapter = true)
data class ExternalConfig(
    @Json(name = "join_url") val joinUrl: String? = null,
    val message: String? = null,
)
