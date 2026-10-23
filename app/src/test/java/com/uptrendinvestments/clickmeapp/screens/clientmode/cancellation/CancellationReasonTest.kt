package com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CancellationReasonTest {
    @Test
    fun entries_areTheIosLabelsInOrder() {
        assertEquals(
            listOf("Change of plans", "Found another expert", "Schedule conflict", "Personal emergency", "Other"),
            CancellationReason.entries.map { it.wireLabel },
        )
    }

    @Test
    fun blankComment_sendsJustTheLabel() {
        assertEquals("Change of plans", cancelReason(CancellationReason.ChangeOfPlans, ""))
        assertEquals("Change of plans", cancelReason(CancellationReason.ChangeOfPlans, "   \n "))
    }

    @Test
    fun comment_isTrimmedAndAppendedAfterTheLabel() {
        assertEquals(
            "Schedule conflict: Moved my flight",
            cancelReason(CancellationReason.ScheduleConflict, "  Moved my flight  "),
        )
    }

    @Test
    fun maxComment_withLongestLabel_staysUnderTheBackendLimit() {
        val reason = cancelReason(CancellationReason.FoundAnotherExpert, "x".repeat(CANCEL_COMMENT_MAX_CHARS))
        assertEquals(472, reason.length)
        assertTrue(reason.length <= CANCEL_REASON_MAX_CHARS)
    }

    @Test
    fun oversizedComment_isCutAtTheBackendLimit() {
        assertEquals(500, cancelReason(CancellationReason.Other, "y".repeat(600)).length)
    }

    @Test
    fun cutThroughASurrogatePair_dropsTheLoneHighSurrogate() {
        // "Other: " is 7 units, so 492 filler units put the emoji's high surrogate at index 499 (the 500th unit).
        val comment = "a".repeat(492) + "😀" + "tail"
        val reason = cancelReason(CancellationReason.Other, comment)
        assertEquals(499, reason.length)
        assertFalse(Character.isHighSurrogate(reason.last()))
    }

    @Test
    fun capCancelComment_limitsTheInputToFourFiftyUnits() {
        assertEquals(CANCEL_COMMENT_MAX_CHARS, capCancelComment("z".repeat(451)).length)
        assertEquals("short", capCancelComment("short"))
    }
}
