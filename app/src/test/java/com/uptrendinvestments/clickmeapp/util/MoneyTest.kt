package com.uptrendinvestments.clickmeapp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

class MoneyTest {
    /** Locale-specific formats use NBSP / narrow NBSP between amount and symbol (RESEARCH Pitfall 6). */
    private fun String.normalized() = replace(' ', ' ').replace(' ', ' ')

    @Test
    fun usd_showsTwoDecimals() {
        assertEquals("US$50.00", Money.format(5000, "USD", Locale.US))
    }

    @Test
    fun zero_showsFullDecimals() {
        assertEquals("US$0.00", Money.format(0, "USD", Locale.US))
    }

    @Test
    fun jpy_hasNoDecimals() {
        assertEquals("¥500", Money.format(500, "JPY", Locale.US))
    }

    @Test
    fun cad_showsCadMarkerAndTwoDecimals() {
        assertEquals("CA$50.00", Money.format(5000, "CAD", Locale.US))
    }

    @Test
    fun cad_inCanadianLocale_stillShowsCadMarker() {
        assertEquals("CA$50.00", Money.format(5000, "CAD", Locale.CANADA))
    }

    @Test
    fun usd_inCanadianLocale_showsUsMarker() {
        assertEquals("US$50.00", Money.format(5000, "USD", Locale.CANADA))
    }

    /**
     * The symbol must not depend on `DecimalFormatSymbols.currencySymbol`. On Android, `java.text.DecimalFormat` hands
     * formatting to ICU, which resolves the currency symbol itself, and these JVM tests cannot see how a given ICU
     * treats that override (CAD in en_CA is the case where the locale's own symbol is a bare `$`). Replacing the
     * formatter's symbols with the locale defaults stands in for a platform that ignores or resets the override: the
     * old `currencySymbol` override came back as `$50.00` here, a literal in the pattern does not.
     */
    @Test
    fun pinnedSymbol_survivesTheFormatterSymbolsBeingReset() {
        listOf(
            Triple("CAD", Locale.CANADA, "CA$50.00"),
            Triple("USD", Locale.US, "US$50.00"),
            Triple("CAD", Locale.US, "CA$50.00"),
            Triple("USD", Locale.CANADA, "US$50.00"),
        ).forEach { (code, locale, expected) ->
            val formatter = Money.currencyFormat(Currency.getInstance(code), 2, locale) as DecimalFormat
            formatter.decimalFormatSymbols = DecimalFormatSymbols(locale)
            assertEquals("$code in $locale", expected, formatter.format(BigDecimal("50.00")))
        }
    }

    @Test
    fun cad_inFrenchCanada_keepsLocalePlacementAndSeparators() {
        assertEquals("1 234,56 CA$", Money.format(123456, "CAD", Locale.CANADA_FRENCH).normalized())
    }

    @Test
    fun usd_inGermanLocale_keepsLocalePlacementAndSeparators() {
        assertEquals("1.234,56 US$", Money.format(123456, "USD", Locale.GERMANY).normalized())
    }

    @Test
    fun negativeAmount_keepsTheSignBeforeThePinnedSymbol() {
        assertEquals("-CA$5.00", Money.format(-500, "CAD", Locale.CANADA))
        assertEquals("-US$5.00", Money.format(-500, "USD", Locale.US))
    }

    @Test
    fun eur_inGermanLocale_usesDeviceSeparatorsAndPlacement() {
        assertEquals("1.234,56 €", Money.format(123456, "EUR", Locale.GERMANY).normalized())
    }

    @Test
    fun lowercaseCode_isUpperCased() {
        assertEquals("US$50.00", Money.format(5000, "usd", Locale.US))
    }

    @Test
    fun kwd_hasThreeDecimals() {
        assertTrue(Money.format(5, "KWD", Locale.US).contains("0.005"))
    }

    @Test
    fun unknownCode_fallsBackToCodeAndAmount() {
        assertEquals("ZZZ 50.00", Money.format(5000, "ZZZ", Locale.US))
    }

    @Test
    fun noCurrencyCode_xxx_treatsNegativeDigitsAsTwo() {
        assertTrue(Money.format(5000, "XXX", Locale.US).contains("50.00"))
    }

    @Test
    fun largeAmount_usesGroupingWithoutOverflow() {
        assertEquals("US$1,234,567,890.12", Money.format(123456789012, "USD", Locale.US))
    }

    @Test
    fun formatMajor_decimalCurrencies() {
        assertEquals("CA$0.00", Money.formatMajor(0.0, "CAD", Locale.US))
        assertEquals("CA$12.50", Money.formatMajor(12.5, "CAD", Locale.US))
        assertEquals("US$75.00", Money.formatMajor(75.0, "USD", Locale.US))
    }

    @Test
    fun formatMajor_zeroDecimalCurrency_isNotRescaled() {
        assertEquals("¥1,000", Money.formatMajor(1000.0, "JPY", Locale.US))
    }

    @Test
    fun formatMajor_unknownCode_fallsBackToTwoDigits() {
        assertEquals("ZZZ 12.50", Money.formatMajor(12.5, "zzz", Locale.US))
    }

    @Test
    fun formatMajor_roundsHalfUp() {
        assertEquals("US$0.13", Money.formatMajor(0.125, "USD", Locale.US))
    }

    @Test
    fun backendZeroDecimalSet_hasZeroFractionDigitsOnThisJvm() {
        listOf("JPY", "KRW", "VND", "CLP", "ISK", "XOF", "XAF").forEach { code ->
            assertEquals(code, 0, Currency.getInstance(code).defaultFractionDigits)
        }
    }
}
