package com.uptrendinvestments.clickmeapp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
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
