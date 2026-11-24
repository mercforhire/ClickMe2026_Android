package com.uptrendinvestments.clickmeapp.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * The one currency formatter. Shared by Phases 5, 6, 7, 11 and 14.
 *
 * Backend amounts are integer minor units plus an ISO-4217 code (D-18). The number of decimals comes from
 * [Currency.getDefaultFractionDigits] so JPY has none and KWD has three, and decimals are always shown in full
 * (D-20). Symbol placement and separators follow the device locale (D-19); the currency itself always comes from
 * the caller. The one exception is the symbol of USD (`US$`) and CAD (`CA$`), which is fixed (see [pinnedSymbol]).
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
        return currencyFormat(currency, digits, locale).format(major)
    }

    /**
     * The formatter behind [format]. Internal so a test can tamper with its symbols the way a platform might.
     *
     * A pinned symbol is written into the pattern as quoted literal text, taken from the locale's own currency pattern,
     * so placement, grouping and separators still follow the locale (D-19, D-32). It is deliberately not set through
     * `DecimalFormatSymbols.currencySymbol`: on Android `java.text.DecimalFormat` delegates to ICU's `DecimalFormat`,
     * which resolves the currency symbol itself, and the JVM and Robolectric unit tests cannot see how a given ICU
     * treats that override. A literal in the pattern involves no symbol resolution at all.
     */
    internal fun currencyFormat(
        currency: Currency,
        digits: Int,
        locale: Locale,
    ): NumberFormat {
        val localized = NumberFormat.getCurrencyInstance(locale)
        val symbol = pinnedSymbol(currency.currencyCode)
        val formatter =
            if (symbol != null && localized is DecimalFormat) {
                val pattern = localized.toPattern().replace(CURRENCY_SIGN, quoted(symbol))
                DecimalFormat(pattern, localized.decimalFormatSymbols)
            } else {
                localized.apply { this.currency = currency }
            }
        return formatter.apply {
            minimumFractionDigits = digits
            maximumFractionDigits = digits
        }
    }

    /** The pattern character that stands for the currency symbol (U+00A4). */
    private const val CURRENCY_SIGN = "¤"

    /** [text] as a quoted literal in a `DecimalFormat` pattern; a quote inside is doubled. */
    private fun quoted(text: String): String = "'${text.replace("'", "''")}'"

    /**
     * USD is always `US$` and CAD always `CA$`, whatever the device locale (D-24, D-32), so a CAD and a USD price read
     * apart in any region. Only the symbol is replaced: placement, grouping and separators still follow the locale.
     */
    private fun pinnedSymbol(code: String): String? =
        when (code) {
            "USD" -> "US$"
            "CAD" -> "CA$"
            else -> null
        }
}
