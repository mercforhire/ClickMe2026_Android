package com.uptrendinvestments.clickmeapp.screens.expertmode.confirmed

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.CalendarContract
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class CalendarIntentTest {
    private val start = Instant.parse("2026-11-02T15:00:00Z")
    private val end = Instant.parse("2026-11-02T16:00:00Z")

    private fun key(
        message: String? = "  See you  ",
        meetingType: String? = "skype_zoom",
    ) = BookingConfirmedKey(
        bookingId = "b1",
        clientName = "Ada Lovelace",
        clientAvatarUrl = null,
        topic = "Algorithms",
        startMillis = start.toEpochMilli(),
        endMillis = end.toEpochMilli(),
        meetingType = meetingType,
        clientMessage = message,
    )

    @Test
    fun specCarriesTitleTimesAndDescription() {
        val spec = calendarEventSpec(key(), "ClickMe · Algorithms with Ada Lovelace", "Skype/Zoom")

        assertEquals("ClickMe · Algorithms with Ada Lovelace", spec.title)
        assertEquals(start.toEpochMilli(), spec.beginMillis)
        assertEquals(end.toEpochMilli(), spec.endMillis)
        assertEquals("Skype/Zoom\nSee you", spec.description)
    }

    @Test
    fun blankMessageIsOmitted() {
        assertEquals("In-App Voice", calendarEventSpec(key(message = null), "t", "In-App Voice").description)
        assertEquals("In-App Voice", calendarEventSpec(key(message = "  "), "t", "In-App Voice").description)
    }

    @Test
    fun intentHasActionDataAndExtrasOnly() {
        val spec = calendarEventSpec(key(), "ClickMe · Algorithms with Ada Lovelace", "Skype/Zoom")

        val intent = buildCalendarInsertIntent(spec)

        assertEquals(Intent.ACTION_INSERT, intent.action)
        assertEquals(CalendarContract.Events.CONTENT_URI, intent.data)
        assertEquals(start.toEpochMilli(), intent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, -1L))
        assertEquals(end.toEpochMilli(), intent.getLongExtra(CalendarContract.EXTRA_EVENT_END_TIME, -1L))
        assertEquals(spec.title, intent.getStringExtra(CalendarContract.Events.TITLE))
        assertEquals(spec.description, intent.getStringExtra(CalendarContract.Events.DESCRIPTION))
        assertFalse(intent.hasExtra(CalendarContract.Events.EVENT_LOCATION))
        assertFalse(intent.hasExtra(CalendarContract.Events.HAS_ALARM))
        assertNotNull(intent.extras)
        assertEquals(4, intent.extras?.size())
    }

    @Test
    fun noHandlerCallsCallbackWithoutCrashing() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val context =
            object : ContextWrapper(base) {
                override fun startActivity(intent: Intent?) = throw ActivityNotFoundException("no calendar")
            }
        var misses = 0

        context.launchCalendarInsert(buildCalendarInsertIntent(calendarEventSpec(key(), "t", "d"))) { misses++ }

        assertEquals(1, misses)
    }

    @Test
    fun launchStartsTheIntentWhenAHandlerExists() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        var started: Intent? = null
        val context =
            object : ContextWrapper(base) {
                override fun startActivity(intent: Intent?) {
                    started = intent
                }
            }
        var misses = 0

        context.launchCalendarInsert(buildCalendarInsertIntent(calendarEventSpec(key(), "t", "d"))) { misses++ }

        assertEquals(0, misses)
        assertEquals(Intent.ACTION_INSERT, started?.action)
        // A non-Activity context needs a new task to start an activity.
        assertTrue(((started?.flags ?: 0) and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
    }
}
