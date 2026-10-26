package com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.uptrendinvestments.clickmeapp.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeclineReasonTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun wireCodesAreTheBackendEnumInOrder() {
        assertEquals(
            listOf("CONFLICT", "OUT_OF_SCOPE", "UNAVAILABLE", "OTHER"),
            DeclineReason.entries.map { it.wire },
        )
    }

    @Test
    fun wireLabelsEqualTheStringResources() {
        val expected =
            mapOf(
                DeclineReason.ScheduleConflict to R.string.decline_reason_conflict,
                DeclineReason.OutsideExpertise to R.string.decline_reason_out_of_scope,
                DeclineReason.NotAvailable to R.string.decline_reason_unavailable,
                DeclineReason.Other to R.string.decline_reason_other,
            )
        assertEquals(DeclineReason.entries.toSet(), expected.keys)
        expected.forEach { (reason, res) -> assertEquals(context.getString(res), reason.wireLabel) }
    }

    @Test
    fun blankNoteSendsTheReasonLabel() {
        assertEquals("Outside my expertise", declineReasonText(DeclineReason.OutsideExpertise, "  "))
        assertEquals("Schedule conflict", declineReasonText(DeclineReason.ScheduleConflict, ""))
    }

    @Test
    fun noteIsTrimmed() {
        assertEquals("Booked solid", declineReasonText(DeclineReason.Other, "  Booked solid  "))
    }

    @Test
    fun longNoteIsCutToOneThousand() {
        assertEquals(DECLINE_NOTE_MAX_CHARS, declineReasonText(DeclineReason.Other, "n".repeat(1200)).length)
    }

    @Test
    fun cutThroughASurrogatePairDropsTheLoneHighSurrogate() {
        val text = declineReasonText(DeclineReason.Other, "a".repeat(DECLINE_NOTE_MAX_CHARS - 1) + "😀" + "tail")
        assertEquals(DECLINE_NOTE_MAX_CHARS - 1, text.length)
        assertFalse(Character.isHighSurrogate(text.last()))
    }

    @Test
    fun reasonTextIsNeverEmpty() {
        DeclineReason.entries.forEach { assertFalse(declineReasonText(it, " \n ").isEmpty()) }
    }

    @Test
    fun blankAcceptMessageIsOmitted() {
        assertNull(acceptMessage(""))
        assertNull(acceptMessage("  \n "))
    }

    @Test
    fun acceptMessageIsTrimmedAndCapped() {
        assertEquals("See you", acceptMessage("  See you  "))
        assertEquals(ACCEPT_MESSAGE_MAX_CHARS, acceptMessage("m".repeat(1500))?.length)
    }
}
