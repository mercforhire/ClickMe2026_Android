package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** [EarningsBreakdownDto.feeSource] when the fee came from the booking itself. */
const val FEE_SOURCE_BOOKING = "booking"

/** [EarningsBreakdownDto.feeSource] when the expert's default commission was applied. */
const val FEE_SOURCE_EXPERT_DEFAULT = "expert_default"

/** [EarningsBreakdownDto.feeSource] when the server could not work out the fee (free session or missing data). */
const val FEE_SOURCE_UNAVAILABLE = "unavailable"

/**
 * The per-session earnings breakdown on expert request and booking payloads (`earnings_breakdown`).
 *
 * Every amount is in MAJOR units, already scaled by the server for the currency (so a JPY value has no decimals):
 * format with `Money.formatMajor` and never divide by 100. [netAmount] may be null, and gross must never stand in
 * for it under an earnings label. [commissionPct] is a percent (15.0 is 15%) and 0 is a real value, so callers
 * branch on [feeSource] and on `== null`, never on truthiness.
 */
@JsonClass(generateAdapter = true)
data class EarningsBreakdownDto(
    @Json(name = "gross_amount") val grossAmount: Double? = null,
    @Json(name = "platform_fee") val platformFee: Double? = null,
    @Json(name = "net_amount") val netAmount: Double? = null,
    @Json(name = "commission_pct") val commissionPct: Double? = null,
    @Json(name = "currency") val currency: String? = null,
    @Json(name = "fee_source") val feeSource: String? = null,
)
