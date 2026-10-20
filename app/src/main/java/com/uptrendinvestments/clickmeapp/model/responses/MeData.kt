package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.uptrendinvestments.clickmeapp.model.enums.UserRole

/**
 * `GET /me`: the authenticated user's identity from the verified JWT payload.
 *
 * [roles] is a set on the backend; a user with `["client", "expert"]` can hit both client-
 * and expert-scoped endpoints. Raw strings are kept so unknown roles never fail decoding;
 * use [userRoles] for typed access.
 *
 * [pricingLimits] must never be persisted across launches (PRICING-LIMITS.md): values,
 * including a global kill-switch, can change without an app release. Phase 2 only decodes
 * it. `null` when the backend predates the rollout.
 */
@JsonClass(generateAdapter = true)
data class MeData(
    val id: String,
    val email: String,
    val roles: List<String>,
    @Json(name = "pricing_limits") val pricingLimits: PricingLimits? = null,
) {
    val userRoles: Set<UserRole>
        get() = roles.mapNotNull(UserRole::fromWire).toSet()
}

/** Platform-wide limits carried by `GET /me`. Currently only `min_session_amount`. */
@JsonClass(generateAdapter = true)
data class PricingLimits(
    @Json(name = "min_session_amount") val minSessionAmount: MinSessionAmount,
)

/**
 * Per-currency minimum session price in integer minor units (same unit as `price.amount`
 * on a topic). Free topics are exempt; never apply this to an `is_free` topic.
 *
 * Lookup rule: `byCurrency[currency] ?: defaultMinorUnits`.
 */
@JsonClass(generateAdapter = true)
data class MinSessionAmount(
    /** Uppercase ISO-4217 code to floor in minor units. */
    @Json(name = "by_currency") val byCurrency: Map<String, Int>,
    /** Fallback floor when the currency is not a key in [byCurrency]. */
    @Json(name = "default_minor_units") val defaultMinorUnits: Int,
    /** Currency the backend assumes when a topic carries none; read from the wire. */
    @Json(name = "default_currency") val defaultCurrency: String,
) {
    /** Floor for [currency] in minor units, falling back to [defaultMinorUnits]. */
    fun minMinorUnits(currency: String): Int = byCurrency[currency.uppercase()] ?: defaultMinorUnits
}
