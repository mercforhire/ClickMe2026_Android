package com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor

import com.uptrendinvestments.clickmeapp.model.responses.MinSessionAmount
import com.uptrendinvestments.clickmeapp.model.responses.PricingLimits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicPriceTest {
    private val limits =
        PricingLimits(
            MinSessionAmount(
                byCurrency = mapOf("CAD" to 500, "JPY" to 800),
                defaultMinorUnits = 500,
                defaultCurrency = "CAD",
            ),
        )

    @Test
    fun fractionDigits_perCurrency() {
        assertEquals(2, TopicPrice.fractionDigits("CAD"))
        assertEquals(2, TopicPrice.fractionDigits("USD"))
        assertEquals(0, TopicPrice.fractionDigits("JPY"))
        assertEquals(2, TopicPrice.fractionDigits("cad"))
        assertEquals(2, TopicPrice.fractionDigits("XYZ"))
        assertEquals(2, TopicPrice.fractionDigits(""))
    }

    @Test
    fun parse_cad() {
        fun ok(text: String) = TopicPrice.parse(text, "CAD")
        assertEquals(PriceParse.Ok(0), ok(""))
        assertEquals(PriceParse.Ok(0), ok("  "))
        assertEquals(PriceParse.Ok(0), ok("0"))
        assertEquals(PriceParse.Ok(1200), ok("12"))
        assertEquals(PriceParse.Ok(1250), ok("12.5"))
        assertEquals(PriceParse.Ok(1250), ok("12.50"))
        assertEquals(PriceParse.Ok(1250), ok("12,50"))
        assertEquals(PriceParse.Ok(99), ok("0.99"))
        assertEquals(PriceParse.Invalid, ok("12.345"))
        assertEquals(PriceParse.Invalid, ok("-5"))
        assertEquals(PriceParse.Invalid, ok("1.2.3"))
        assertEquals(PriceParse.Invalid, ok("."))
        assertEquals(PriceParse.Invalid, ok("abc"))
        assertEquals(PriceParse.Invalid, ok("1e5"))
        assertEquals(PriceParse.Invalid, ok("12345678"))
        assertEquals(PriceParse.Ok(123456789), ok("1234567.89"))
    }

    @Test
    fun parse_jpy() {
        assertEquals(PriceParse.Ok(800), TopicPrice.parse("800", "JPY"))
        assertEquals(PriceParse.Invalid, TopicPrice.parse("800.5", "JPY"))
        assertEquals(PriceParse.Ok(0), TopicPrice.parse("0", "JPY"))
    }

    @Test
    fun filterInput_cad() {
        assertEquals("12.34", TopicPrice.filterInput("1a2.3x4", "CAD"))
        assertEquals("12,50", TopicPrice.filterInput("12,505", "CAD"))
        assertEquals("12.5", TopicPrice.filterInput("12..5", "CAD"))
        assertEquals("12.53", TopicPrice.filterInput("12.5,3", "CAD"))
        assertEquals("1234567", TopicPrice.filterInput("123456789", "CAD"))
        assertEquals("", TopicPrice.filterInput("", "CAD"))
    }

    @Test
    fun filterInput_jpy() {
        assertEquals("12", TopicPrice.filterInput("12.5", "JPY"))
        assertEquals("1", TopicPrice.filterInput("1,000", "JPY"))
    }

    @Test
    fun fieldText_seedsFromStoredAmount() {
        assertEquals("12.50", TopicPrice.fieldText(1250, false, "CAD"))
        assertEquals("12.00", TopicPrice.fieldText(1200, false, "CAD"))
        assertEquals("800", TopicPrice.fieldText(800, false, "JPY"))
        assertEquals("", TopicPrice.fieldText(500, true, "CAD"))
        assertEquals("", TopicPrice.fieldText(0, false, "CAD"))
    }

    @Test
    fun floorFor_lookupWithFallback() {
        assertEquals(500, TopicPrice.floorFor(limits, "CAD"))
        assertEquals(800, TopicPrice.floorFor(limits, "jpy"))
        assertEquals(500, TopicPrice.floorFor(limits, "EUR"))
        assertNull(TopicPrice.floorFor(null, "CAD"))
    }

    @Test
    fun isBelowFloor_onlyForPaidAmounts() {
        assertTrue(TopicPrice.isBelowFloor(499, 500))
        assertFalse(TopicPrice.isBelowFloor(500, 500))
        assertFalse(TopicPrice.isBelowFloor(0, 500))
        assertFalse(TopicPrice.isBelowFloor(100, null))
    }

    @Test
    fun wireAmount_freeTopicSendsFloorOrFallback() {
        assertEquals(500L, TopicPrice.wireAmount(0, 500))
        assertEquals(100L, TopicPrice.wireAmount(0, null))
        assertEquals(100L, TopicPrice.wireAmount(0, 0))
        assertEquals(1250L, TopicPrice.wireAmount(1250, 500))
    }

    @Test
    fun resolveCurrency_prefersStoredThenDefaultThenCad() {
        assertEquals("CAD", TopicPrice.resolveCurrency("cad", "USD"))
        assertEquals("USD", TopicPrice.resolveCurrency("", "USD"))
        assertEquals("CAD", TopicPrice.resolveCurrency(null, null))
        assertEquals("USD", TopicPrice.resolveCurrency(" usd ", null))
        assertEquals("USD", TopicPrice.resolveCurrency("CA", "usd"))
        assertEquals("CAD", TopicPrice.resolveCurrency("C4D", null))
    }
}
