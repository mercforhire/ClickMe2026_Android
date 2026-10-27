package com.uptrendinvestments.clickmeapp.model.enums

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserReportReasonTest {
    @Test
    fun wireValuesAreTheSixBackendReasonsInOrder() {
        assertEquals(
            listOf("harassment", "spam", "inappropriate_content", "scam_or_fraud", "impersonation", "other"),
            UserReportReason.entries.map { it.wire },
        )
    }

    @Test
    fun fromWireRoundTripsEveryReason() {
        UserReportReason.entries.forEach { assertEquals(it, UserReportReason.fromWire(it.wire)) }
        assertEquals(UserReportReason.ScamOrFraud, UserReportReason.fromWire("scam_or_fraud"))
    }

    @Test
    fun fromWireRejectsLabelsAndNull() {
        assertNull(UserReportReason.fromWire("Fake profile"))
        assertNull(UserReportReason.fromWire(null))
    }
}
