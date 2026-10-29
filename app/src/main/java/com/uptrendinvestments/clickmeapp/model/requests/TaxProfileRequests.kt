package com.uptrendinvestments.clickmeapp.model.requests

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * `POST /expert/tax-profile` body, ported from iOS `ClickMeAPI+TaxProfile`. The backend schema is `.strict()`, so no
 * key beyond these is ever sent. A null [tin] or [financialAccountIdentifier] is omitted (the converter has no
 * `serializeNulls`). The TIN is sent once and never stored on the device (CONTEXT D-08). [toString] prints nothing
 * personal (threat T-14-01).
 */
@JsonClass(generateAdapter = true)
data class SaveTaxProfileRequest(
    @Json(name = "legal_name") val legalName: String,
    val address: TaxAddressBody,
    val dob: String,
    val tin: String? = null,
    @Json(name = "financial_account_identifier") val financialAccountIdentifier: String? = null,
) {
    override fun toString(): String = "SaveTaxProfileRequest(<redacted>)"
}

/** `address` object of [SaveTaxProfileRequest]. [line2] is sent only when set. */
@JsonClass(generateAdapter = true)
data class TaxAddressBody(
    val line1: String,
    val line2: String? = null,
    val city: String,
    val region: String,
    @Json(name = "postal_code") val postalCode: String,
    val country: String,
) {
    override fun toString(): String = "TaxAddressBody(<redacted>)"
}
