package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/*
 * Expert financial payloads (weekly earnings, payout summary, Stripe Connect).
 *
 * Weekly and payout amounts are MINOR units (integers): format them with `Money.format`. Net is never null on the
 * weekly endpoint (the server writes `net_amount || 0`) and a zero-session week reports "CAD", so a missing key is
 * a malformed body and must fail decoding rather than show a fake zero. The weekly net may equal gross because of
 * a known backend gap (EXPERT-EARNINGS-BREAKDOWN 6.1); it is displayed as received and never corrected on the
 * client. The retired withdraw fields are not declared.
 */

/** `GET /expert/earnings/weekly` data, in MINOR units. */
@JsonClass(generateAdapter = true)
data class WeeklyEarningsData(
    @Json(name = "week_start") val weekStart: String? = null,
    @Json(name = "week_end") val weekEnd: String? = null,
    @Json(name = "gross_amount") val grossAmount: Long,
    @Json(name = "net_amount") val netAmount: Long,
    val currency: String,
    @Json(name = "session_count") val sessionCount: Int,
)

/** `GET /expert/connect/status` data. 404 means no connected account exists yet. */
@JsonClass(generateAdapter = true)
data class ConnectStatusData(
    @Json(name = "charges_enabled") val chargesEnabled: Boolean,
    @Json(name = "currently_due") val currentlyDue: List<String> = emptyList(),
)

/**
 * `POST /expert/connect/onboard` and `.../refresh-link` data. [url] is a single-use Stripe Account Link that
 * expires in about 5 minutes: open it once and never log, cache or persist it.
 */
@JsonClass(generateAdapter = true)
data class ConnectLinkData(
    val url: String,
)

/** `GET /expert/payouts/summary` data, in MINOR units. [nextPayoutDate] is `YYYY-MM-DD` or null. */
@JsonClass(generateAdapter = true)
data class PayoutSummaryData(
    @Json(name = "available_amount") val availableAmount: Long,
    @Json(name = "pending_amount") val pendingAmount: Long = 0,
    val currency: String,
    @Json(name = "next_payout_date") val nextPayoutDate: String? = null,
)
