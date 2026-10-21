package com.uptrendinvestments.clickmeapp.constants

import java.util.Locale

/**
 * A country surfaced by the profile country picker. [code] is the ISO-3166-1 alpha-3 identifier, which is the wire
 * value the backend expects.
 */
data class Country(
    val code: String,
    val name: String,
)

/** Port of iOS `Countries` (Constants/Countries.swift). */
object Countries {
    /** ISO alpha-2 to alpha-3, ported verbatim from iOS. Cross-checked against JDK locale data in the tests. */
    internal val alpha2ToAlpha3: Map<String, String> =
        mapOf(
            "AR" to "ARG",
            "AU" to "AUS",
            "AT" to "AUT",
            "BE" to "BEL",
            "BR" to "BRA",
            "CA" to "CAN",
            "CH" to "CHE",
            "CL" to "CHL",
            "CN" to "CHN",
            "CO" to "COL",
            "CZ" to "CZE",
            "DE" to "DEU",
            "DK" to "DNK",
            "EG" to "EGY",
            "ES" to "ESP",
            "FI" to "FIN",
            "FR" to "FRA",
            "GB" to "GBR",
            "GR" to "GRC",
            "HK" to "HKG",
            "HU" to "HUN",
            "ID" to "IDN",
            "IE" to "IRL",
            "IL" to "ISR",
            "IN" to "IND",
            "IT" to "ITA",
            "JP" to "JPN",
            "KR" to "KOR",
            "MX" to "MEX",
            "MY" to "MYS",
            "NG" to "NGA",
            "NL" to "NLD",
            "NO" to "NOR",
            "NZ" to "NZL",
            "PE" to "PER",
            "PH" to "PHL",
            "PL" to "POL",
            "PT" to "PRT",
            "RO" to "ROU",
            "RU" to "RUS",
            "SA" to "SAU",
            "SE" to "SWE",
            "SG" to "SGP",
            "TH" to "THA",
            "TR" to "TUR",
            "TW" to "TWN",
            "UA" to "UKR",
            "US" to "USA",
            "VN" to "VNM",
            "ZA" to "ZAF",
        )

    private val wellFormedAlpha3 = Regex("^[A-Z]{3}$")

    /** The curated picker list with English (en_US) names, sorted by name. */
    val all: List<Country> =
        alpha2ToAlpha3
            .map { (alpha2, alpha3) ->
                val name = Locale("", alpha2).getDisplayCountry(Locale.US).ifBlank { alpha2 }
                Country(code = alpha3, name = name)
            }.sortedBy { it.name }

    /** Looks up a country by its wire (alpha-3) code; null when it is not in the curated list. */
    fun find(code: String): Country? = all.firstOrNull { it.code == code }

    /**
     * Normalizes a raw country code (UI, pre-fill, legacy stored value) to the uppercase alpha-3 form the backend
     * requires. Returns null when the input is empty or unrecognizable.
     *
     * Deviation from iOS: a well-formed alpha-3 code that is not in the curated list passes through, so a country
     * saved from another client is normalized rather than dropped from the publish payload.
     */
    fun normalize(raw: String): String? {
        val cleaned = raw.trim().uppercase(Locale.ROOT)
        if (cleaned.isEmpty()) return null
        if (cleaned.length == 3 && wellFormedAlpha3.matches(cleaned)) return cleaned
        if (cleaned.length == 2) return alpha2ToAlpha3[cleaned]
        return null
    }

    /** The listed English name for [code], or the code itself when unlisted. */
    fun displayName(code: String): String = find(code)?.name ?: code
}
