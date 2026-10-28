package com.uptrendinvestments.clickmeapp.model.requests

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** The only platform this app registers (`registerTokenSchema.platform` is `ios` or `android`). */
const val PLATFORM_ANDROID = "android"

/**
 * `POST /notifications/tokens` body. The backend `registerTokenSchema` is `.strict()` and `app_version` is
 * `optional()`, not nullable, so a null [appVersion] must be omitted rather than sent as JSON null. The app Moshi
 * omits nulls; never turn on null serialization for this type (RESEARCH Pattern 5, Pitfall 8).
 *
 * The backend replaces every row of the caller with this one (one device per account, last registration wins).
 * [token] is a push credential, so [toString] prints the device id and platform only.
 */
@JsonClass(generateAdapter = true)
data class RegisterDeviceTokenRequest(
    @Json(name = "device_id") val deviceId: String,
    val token: String,
    val platform: String = PLATFORM_ANDROID,
    @Json(name = "app_version") val appVersion: String? = null,
) {
    override fun toString(): String = "RegisterDeviceTokenRequest(deviceId=$deviceId, platform=$platform)"
}
