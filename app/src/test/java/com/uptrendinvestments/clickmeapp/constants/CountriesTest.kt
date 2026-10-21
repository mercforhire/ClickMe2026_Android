package com.uptrendinvestments.clickmeapp.constants

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class CountriesTest {
    @Test
    fun all_hasFiftyWellFormedEntries() {
        assertEquals(50, Countries.all.size)
        val pattern = Regex("^[A-Z]{3}$")
        Countries.all.forEach {
            assertTrue("bad code ${it.code}", pattern.matches(it.code))
            assertTrue("blank name for ${it.code}", it.name.isNotBlank())
        }
        assertEquals(50, Countries.all.map { it.code }.toSet().size)
    }

    @Test
    fun all_isSortedByName() {
        val names = Countries.all.map { it.name }
        assertEquals(names.sorted(), names)
    }

    @Test
    fun find_matchesAlpha3Only() {
        assertEquals("Canada", Countries.find("CAN")?.name)
        assertNull(Countries.find("XXX"))
        assertNull(Countries.find("CA"))
    }

    @Test
    fun alpha2ToAlpha3_agreesWithJdkLocaleData() {
        assertEquals(50, Countries.alpha2ToAlpha3.size)
        Countries.alpha2ToAlpha3.forEach { (alpha2, alpha3) ->
            val jdk = Locale.Builder().setRegion(alpha2).build().isO3Country
            assertEquals("transcription error for $alpha2", jdk, alpha3)
        }
    }

    @Test
    fun normalize_acceptsPaddedLowercaseAndAlpha2() {
        assertEquals("CAN", Countries.normalize(" can "))
        assertEquals("CAN", Countries.normalize("ca"))
        assertEquals("TWN", Countries.normalize("TW"))
        assertEquals("USA", Countries.normalize("USA"))
    }

    @Test
    fun normalize_passesWellFormedUnlistedAlpha3() {
        assertEquals("BGD", Countries.normalize("BGD"))
    }

    @Test
    fun normalize_rejectsEmptyAndMalformed() {
        assertNull(Countries.normalize(""))
        assertNull(Countries.normalize("   "))
        assertNull(Countries.normalize("X1"))
        assertNull(Countries.normalize("CANADA"))
        assertNull(Countries.normalize("ZZ"))
    }

    @Test
    fun displayName_listedOrCode() {
        assertEquals("Canada", Countries.displayName("CAN"))
        assertEquals("BGD", Countries.displayName("BGD"))
        assertNotNull(Countries.find("TWN"))
    }
}
