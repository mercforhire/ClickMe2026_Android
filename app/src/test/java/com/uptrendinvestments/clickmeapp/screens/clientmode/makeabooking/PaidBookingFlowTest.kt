package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.Locale
import java.util.TimeZone

class PaidBookingFlowTest {
    private lateinit var savedZone: TimeZone
    private lateinit var savedLocale: Locale

    @Before
    fun setUp() {
        savedZone = TimeZone.getDefault()
        savedLocale = Locale.getDefault()
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(savedZone)
        Locale.setDefault(savedLocale)
    }

    @Test
    fun utc_strings_ignore_the_device_time_zone() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Toronto"))
        val slot = Instant.parse("2026-10-23T01:00:00Z")
        assertEquals("2026-10-23", slot.toUtcDateString())
        assertEquals("01:00", slot.toUtcTimeSlotString())
    }

    @Test
    fun utc_strings_use_ascii_digits_under_arabic_locale() {
        Locale.setDefault(Locale.forLanguageTag("ar-EG"))
        val slot = Instant.parse("2026-10-23T01:00:00Z")
        assertEquals("2026-10-23", slot.toUtcDateString())
        assertEquals("01:00", slot.toUtcTimeSlotString())
    }

    @Test
    fun utc_strings_use_ascii_digits_under_hindi_locale() {
        Locale.setDefault(Locale.forLanguageTag("hi-IN"))
        val slot = Instant.parse("2026-10-23T01:00:00Z")
        assertEquals("2026-10-23", slot.toUtcDateString())
        assertEquals("01:00", slot.toUtcTimeSlotString())
    }

    @Test
    fun utc_strings_for_an_evening_slot() {
        val slot = Instant.parse("2026-10-22T20:30:00Z")
        assertEquals("2026-10-22", slot.toUtcDateString())
        assertEquals("20:30", slot.toUtcTimeSlotString())
    }

    @Test
    fun stripe_requests_never_print_their_secrets() {
        val sheet = StripeRequest.PresentSheet("seti_1_secret_x", "cus_1", "ek_1").toString()
        assertFalse(sheet.contains("seti_1_secret_x"))
        assertFalse(sheet.contains("ek_1"))
        val challenge = StripeRequest.RunChallenge("pi_1_secret_x", "acct_1").toString()
        assertFalse(challenge.contains("pi_1_secret_x"))
        assertFalse(challenge.contains("acct_1"))
    }

    @Test
    fun present_sheet_keeps_data_class_equality() {
        assertEquals(
            StripeRequest.PresentSheet("seti_1_secret_x", "cus_1", "ek_1"),
            StripeRequest.PresentSheet("seti_1_secret_x", "cus_1", "ek_1"),
        )
    }
}
