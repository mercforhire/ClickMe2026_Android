package com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.uptrendinvestments.clickmeapp.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RescheduleStringsTest {
    private val context: Application = ApplicationProvider.getApplicationContext()

    private fun s(id: Int) = context.getString(id)

    private val ids: Map<String, Int> =
        R.string::class.java.fields
            .filter { it.name.startsWith("reschedule_start_") }
            .associate { it.name to it.getInt(null) }

    @Test
    fun allTwentyNineRescheduleStartStringsResolveToNonBlankText() {
        assertEquals(29, ids.size)
        ids.forEach { (name, id) ->
            val raw = context.resources.getString(id)
            assertTrue("$name is blank", raw.isNotBlank())
        }
    }

    @Test
    fun keyCopyMatchesIosVerbatim() {
        assertEquals("Reschedule", s(R.string.reschedule_start_title))
        assertEquals("Submit Reschedule Request", s(R.string.reschedule_start_submit))
        assertEquals("Message Text (optional)", s(R.string.reschedule_start_message_header))
        assertEquals("Couldn't load booking", s(R.string.reschedule_start_load_error_title))
        assertEquals("Couldn't reschedule booking", s(R.string.reschedule_start_error_title))
        assertEquals("Current", s(R.string.reschedule_start_current_chip))
        assertEquals("Current: Mon", context.getString(R.string.reschedule_start_current_banner, "Mon"))
        assertEquals("Your card hold will be released", s(R.string.reschedule_start_disclosure_title))
        assertEquals(
            "Because you're moving this session more than a few days out, we'll release the current payment hold " +
                "on your card and instead charge it automatically about 24 hours before your new session time. " +
                "You won't be charged today.",
            s(R.string.reschedule_start_disclosure_message),
        )
        assertEquals(
            "Please choose a new time before rescheduling.",
            s(R.string.reschedule_start_error_no_slot),
        )
        assertEquals(
            "Please choose a different time than the current session.",
            s(R.string.reschedule_start_error_current_slot),
        )
        assertEquals(
            "This time is past the payment hold on your original booking. Please cancel and book again to " +
                "authorize a new payment.",
            s(R.string.reschedule_start_error_outside_window_client),
        )
        assertEquals(
            "This time is past the payment hold on the original booking. Please cancel and ask the client to " +
                "book a new session.",
            s(R.string.reschedule_start_error_outside_window_expert),
        )
    }

    @Test
    fun androidAuthoredCopyMatchesThePlan() {
        assertEquals("Reschedule request sent", s(R.string.reschedule_start_sent))
        assertEquals("Choose a new time first", s(R.string.reschedule_start_submit_disabled_a11y))
        assertEquals(
            "Paid sessions can be rescheduled up to 6 days ahead.",
            s(R.string.reschedule_start_paid_window_note),
        )
        assertEquals(
            "That time has already passed. Please choose a new time.",
            s(R.string.reschedule_start_error_time_passed),
        )
        assertEquals("This booking can't be rescheduled right now.", s(R.string.reschedule_start_error_invalid_status))
        assertEquals(
            "Please add a payment method before rescheduling this session.",
            s(R.string.reschedule_start_payment_needed_message),
        )
        assertEquals("Reschedule session with Sam", context.getString(R.string.reschedule_start_action_a11y, "Sam"))
    }
}
