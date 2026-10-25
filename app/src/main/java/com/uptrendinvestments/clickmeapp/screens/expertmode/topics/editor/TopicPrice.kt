package com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor

import com.uptrendinvestments.clickmeapp.model.responses.PricingLimits
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale

/** Result of parsing the topic price field. */
sealed interface PriceParse {
    /** A valid amount in integer minor units. `0` means a free session (D-11). */
    data class Ok(
        val minor: Long,
    ) : PriceParse

    /** Not a price this currency can represent (negative, too many decimals, too long, not a number). */
    data object Invalid : PriceParse
}

/**
 * Pure price rules for the topic editor.
 *
 * - D-09: the field is parsed into minor units with the currency's fraction digits (0 for JPY, otherwise 2), accepts
 *   '.' or ',' as the separator and shows an edited 1250 CAD topic as "12.50". This deviates from iOS, which
 *   truncates to whole units. Parsing uses [BigDecimal] only, never floating point.
 * - D-10: an existing topic keeps its own currency; a new topic uses the `pricing_limits` default currency, else CAD.
 * - D-11: blank or 0 is a free session. A free topic still sends a positive wire amount (the currency floor, or
 *   [FREE_WIRE_FALLBACK] when limits are unknown or the floor is 0) because older servers may require amount > 0.
 *   A paid amount below the floor is flagged; the check is UX only and the server's PRICE_BELOW_MINIMUM wins.
 */
object TopicPrice {
    /** Keeps minor units well inside the server's INTEGER column. */
    const val MAX_INTEGER_DIGITS = 7
    const val FREE_WIRE_FALLBACK = 100L
    const val FALLBACK_CURRENCY = "CAD"

    private const val DEFAULT_DIGITS = 2
    private const val CURRENCY_CODE_LENGTH = 3
    private val NUMBER_SHAPE = Regex("[0-9]*[.]?[0-9]*")

    /** Fraction digits for [currency]; unknown or pseudo currencies use 2. */
    fun fractionDigits(currency: String): Int =
        try {
            Currency.getInstance(currency.trim().uppercase(Locale.ROOT)).defaultFractionDigits
        } catch (_: IllegalArgumentException) {
            DEFAULT_DIGITS
        }.takeIf { it >= 0 } ?: DEFAULT_DIGITS

    /**
     * What the field keeps as the user types: ASCII digits plus at most one separator (the user's own '.' or ',')
     * when the currency has fraction digits. Fraction and integer digits are capped.
     */
    fun filterInput(
        raw: String,
        currency: String,
    ): String {
        val digits = fractionDigits(currency)
        val out = StringBuilder()
        var integerCount = 0
        var fractionCount = 0
        var separatorSeen = false
        for (ch in raw) {
            when {
                ch in '0'..'9' -> {
                    if (!separatorSeen) {
                        if (integerCount < MAX_INTEGER_DIGITS) {
                            out.append(ch)
                            integerCount++
                        }
                    } else if (fractionCount < digits) {
                        out.append(ch)
                        fractionCount++
                    }
                }
                (ch == '.' || ch == ',') && !separatorSeen -> {
                    // A zero-digit currency has no fraction part: drop the separator and everything after it.
                    if (digits == 0) return out.toString()
                    out.append(ch)
                    separatorSeen = true
                }
            }
        }
        return out.toString()
    }

    /** Parses [text] into minor units for [currency]. Blank is free (`Ok(0)`). */
    fun parse(
        text: String,
        currency: String,
    ): PriceParse {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return PriceParse.Ok(0)
        val normalized = trimmed.replace(',', '.')
        if (!NUMBER_SHAPE.matches(normalized) || normalized.none { it in '0'..'9' }) return PriceParse.Invalid
        val value = normalized.toBigDecimalOrNull() ?: return PriceParse.Invalid
        val digits = fractionDigits(currency)
        val tooLong = value.precision() - value.scale() > MAX_INTEGER_DIGITS
        if (value.signum() < 0 || value.scale() > digits || tooLong) return PriceParse.Invalid
        return try {
            PriceParse.Ok(value.movePointRight(digits).longValueExact())
        } catch (_: ArithmeticException) {
            PriceParse.Invalid
        }
    }

    /** Seed text for the field. Blank for a free topic or 0, otherwise full decimals with '.' ("12.50"). */
    fun fieldText(
        minor: Long,
        isFree: Boolean,
        currency: String,
    ): String {
        if (isFree || minor <= 0) return ""
        val digits = fractionDigits(currency)
        val major = BigDecimal.valueOf(minor).movePointLeft(digits)
        return major.setScale(digits).toPlainString()
    }

    /** Floor in minor units for [currency], or null when limits are unknown. */
    fun floorFor(
        limits: PricingLimits?,
        currency: String,
    ): Int? = limits?.minSessionAmount?.minMinorUnits(currency)

    /** True when [minor] is a paid amount under [floor]. Free (0) and unknown floors are never below. */
    fun isBelowFloor(
        minor: Long,
        floor: Int?,
    ): Boolean = floor != null && minor > 0 && minor < floor

    /** The amount sent on the wire. A free topic sends the floor, or [FREE_WIRE_FALLBACK] when there is none. */
    fun wireAmount(
        minor: Long,
        floor: Int?,
    ): Long =
        when {
            minor > 0 -> minor
            floor != null && floor > 0 -> floor.toLong()
            else -> FREE_WIRE_FALLBACK
        }

    /** Stored code if valid, else the default code if valid, else [FALLBACK_CURRENCY] (blank on old free topics). */
    fun resolveCurrency(
        stored: String?,
        defaultCurrency: String?,
    ): String = validCode(stored) ?: validCode(defaultCurrency) ?: FALLBACK_CURRENCY

    private fun validCode(code: String?): String? =
        code
            ?.trim()
            ?.takeIf { it.length == CURRENCY_CODE_LENGTH && it.all { c -> c in 'a'..'z' || c in 'A'..'Z' } }
            ?.uppercase(Locale.ROOT)
}
