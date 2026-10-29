package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/*
 * `GET` and `POST /expert/tax-profile` payloads, ported from iOS `TaxProfileData` and `SaveTaxProfileData`. The
 * TIN is never returned (only [TaxProfileData.tinOnFile]). Legal name, date of birth, street address and the
 * financial account identifier are personal data: every toString() here redacts them (threat T-14-01).
 */

/** `GET /expert/tax-profile` data. The default body for an expert with no profile has every field null/false. */
@JsonClass(generateAdapter = true)
data class TaxProfileData(
    @Json(name = "tin_on_file") val tinOnFile: Boolean = false,
    @Json(name = "legal_name") val legalName: String? = null,
    val address: TaxProfileAddress? = null,
    val dob: String? = null,
    @Json(name = "financial_account_identifier") val financialAccountIdentifier: String? = null,
) {
    override fun toString(): String = "TaxProfileData(tinOnFile=$tinOnFile)"
}

/**
 * Stored address. The write side sends `region` (iOS parity) while the read side may answer `region` or `province`,
 * so [regionOrProvince] returns whichever is present (RESEARCH A9).
 */
@JsonClass(generateAdapter = true)
data class TaxProfileAddress(
    val line1: String? = null,
    val line2: String? = null,
    val city: String? = null,
    val region: String? = null,
    val province: String? = null,
    @Json(name = "postal_code") val postalCode: String? = null,
    val country: String? = null,
) {
    val regionOrProvince: String? get() = region ?: province

    override fun toString(): String = "TaxProfileAddress(<redacted>)"
}

/**
 * `POST /expert/tax-profile` data. `saved = false` with [tinRequired] true means no row was written because the
 * first save needs a TIN.
 */
@JsonClass(generateAdapter = true)
data class SaveTaxProfileData(
    val saved: Boolean,
    @Json(name = "tin_required") val tinRequired: Boolean = false,
    val message: String? = null,
)
