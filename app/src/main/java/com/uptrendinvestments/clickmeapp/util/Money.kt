package com.uptrendinvestments.clickmeapp.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * The one currency formatter. Shared by Phases 5, 6, 7, 11 and 14.
 *
 * Backend amounts are integer minor units plus an ISO-4217 code (D-18). The number of decimals comes from
 * [Currency.getDefaultFractionDigits] so JPY has none and KWD has three, and decimals are always shown in full
 * (D-20). Symbol placement and separators follow the device locale (D-19); the currency itself always comes from
 * the caller.
 *
 * `payment-intent` amounts are the one non-minor-unit exception (ROADMAP Phase 7 note) and must not be passed here.
 * The cancel response `refund_amount` (major units) is the second exception and must go through [formatMajor] only.
 */
object Money {
    private const val DEFAULT_DIGITS = 2

    private fun currencyOrNull(code: String): Currency? =
        try {
            Currency.getInstance(code)
        } catch (_: IllegalArgumentException) {
            null
        }

    private fun fractionDigits(currency: Currency?): Int =
        (currency?.defaultFractionDigits ?: DEFAULT_DIGITS).takeIf { it >= 0 } ?: DEFAULT_DIGITS

    /** Formats a major-unit [major] amount (e.g. the cancel response `refund_amount`) through [format]. */
    fun formatMajor(
        major: Double,
        currencyCode: String,
        locale: Locale = Locale.getDefault(),
    ): String {
        val digits = fractionDigits(currencyOrNull(currencyCode.uppercase(Locale.ROOT)))
        val scaled = BigDecimal.valueOf(major).movePointRight(digits)
        val minor = scaled.setScale(0, RoundingMode.HALF_UP).toLong()
        return format(minor, currencyCode, locale)
    }

    fun format(
        minorUnits: Long,
        currencyCode: String,
        locale: Locale = Locale.getDefault(),
    ): String {
        val code = currencyCode.uppercase(Locale.ROOT)
        val currency = currencyOrNull(code)
        val digits = fractionDigits(currency)
        val major = BigDecimal.valueOf(minorUnits).movePointLeft(digits)
        if (currency == null) return "$code ${major.setScale(digits).toPlainString()}"
        val formatter =
            NumberFormat.getCurrencyInstance(locale).apply {
                this.currency = currency
                minimumFractionDigits = digits
                maximumFractionDigits = digits
            }
        return formatter.format(major)
    }
}
