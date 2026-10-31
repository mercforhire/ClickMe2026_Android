package com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule

import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingTimeSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class RescheduleRulesTest {
    private val now = Instant.parse("2026-11-02T15:00:00Z")
    private val zone = ZoneId.of("America/Toronto")

    private fun slot(
        start: Instant,
        available: Boolean = true,
    ) = BookingTimeSlot(start, start.plus(Duration.ofMinutes(30)), available)

    // canClientStartReschedule (D-04)

    @Test
    fun canStart_confirmedExactly24HoursAway_isTrue() {
        assertTrue(canClientStartReschedule("confirmed", now.plus(Duration.ofHours(24)), now))
    }

    @Test
    fun canStart_confirmedOneSecondUnder24Hours_isFalse() {
        assertFalse(canClientStartReschedule("confirmed", now.plus(Duration.ofHours(24)).minusSeconds(1), now))
    }

    @Test
    fun canStart_confirmedThreeDaysAway_isTrue() {
        assertTrue(canClientStartReschedule("confirmed", now.plus(Duration.ofDays(3)), now))
    }

    @Test
    fun canStart_pendingRescheduleIsFalse() {
        assertFalse(canClientStartReschedule("pending_reschedule", now.plus(Duration.ofDays(3)), now))
    }

    @Test
    fun canStart_pendingApprovalIsFalse() {
        assertFalse(canClientStartReschedule("pending_approval", now.plus(Duration.ofDays(3)), now))
    }

    @Test
    fun canStart_startInThePast_isFalse() {
        assertFalse(canClientStartReschedule("confirmed", now.minus(Duration.ofMinutes(10)), now))
    }

    // clientBookingIsPaid

    @Test
    fun paid_noneAndFreeTopic_isFalse() {
        assertFalse(clientBookingIsPaid("none", topicIsFree = true))
    }

    @Test
    fun paid_heldAndCapturedAreAlwaysPaid() {
        assertTrue(clientBookingIsPaid("held", topicIsFree = true))
        assertTrue(clientBookingIsPaid("captured", topicIsFree = true))
    }

    @Test
    fun paid_nullStatusFallsBackToTopic() {
        assertTrue(clientBookingIsPaid(null, topicIsFree = false))
        assertFalse(clientBookingIsPaid(null, topicIsFree = true))
    }

    @Test
    fun paid_noneStatusButPaidTopic_isTrue() {
        assertTrue(clientBookingIsPaid("none", topicIsFree = false))
    }

    // latestPaidStart (D-01, D-02)

    @Test
    fun latestPaidStart_paidIsNowPlusSixDays_freeIsNull() {
        assertEquals(now.plus(Duration.ofDays(6)), latestPaidStart(true, now))
        assertNull(latestPaidStart(false, now))
    }

    // isCurrent / isPickable (D-12)

    @Test
    fun isPickable_paidBoundaryIsInclusive() {
        val latest = latestPaidStart(true, now)
        assertTrue(slot(now.plus(Duration.ofDays(6))).isPickable(now, latest, null))
        assertFalse(slot(now.plus(Duration.ofDays(6)).plusSeconds(1)).isPickable(now, latest, null))
    }

    @Test
    fun isPickable_freeHasNoLimit() {
        assertTrue(slot(now.plus(Duration.ofDays(30))).isPickable(now, null, null))
    }

    @Test
    fun isPickable_pastAndUnavailableAreNotPickable() {
        assertFalse(slot(now).isPickable(now, null, null))
        assertFalse(slot(now.minusSeconds(60)).isPickable(now, null, null))
        assertFalse(slot(now.plusSeconds(3600), available = false).isPickable(now, null, null))
    }

    @Test
    fun isPickable_currentSlotIsNeverPickable() {
        val current = now.plus(Duration.ofDays(2))
        assertFalse(slot(current).isPickable(now, null, current))
    }

    @Test
    fun isCurrent_withinOneSecondOnly() {
        val current = now.plus(Duration.ofDays(2))
        assertTrue(slot(current.plusMillis(400)).isCurrent(current))
        assertTrue(slot(current.minusMillis(400)).isCurrent(current))
        assertFalse(slot(current.plusSeconds(2)).isCurrent(current))
        assertFalse(slot(current).isCurrent(null))
    }

    @Test
    fun pickableDays_excludesTheDayWhoseOnlySlotIsCurrent() {
        val current = Instant.parse("2026-11-04T15:00:00Z")
        val other = Instant.parse("2026-11-05T15:00:00Z")
        val days =
            mapOf(
                LocalDate.of(2026, 11, 4) to listOf(slot(current)),
                LocalDate.of(2026, 11, 5) to listOf(slot(other)),
            )
        assertEquals(setOf(LocalDate.of(2026, 11, 5)), days.pickableDays(now, null, current))
    }

    @Test
    fun pickableDays_respectsPaidLimit() {
        val within = Instant.parse("2026-11-05T15:00:00Z")
        val beyond = Instant.parse("2026-11-20T15:00:00Z")
        val days =
            mapOf(
                LocalDate.of(2026, 11, 5) to listOf(slot(within)),
                LocalDate.of(2026, 11, 20) to listOf(slot(beyond)),
            )
        assertEquals(setOf(LocalDate.of(2026, 11, 5)), days.pickableDays(now, latestPaidStart(true, now), null))
    }

    // disclosureSessionEnd (D-11 as amended, duration first)

    private val start = Instant.parse("2026-11-05T15:00:00Z")

    @Test
    fun sessionEnd_durationWinsOverShorterSlotEnd() {
        assertEquals(
            start.plus(Duration.ofMinutes(60)),
            disclosureSessionEnd(start, start.plus(Duration.ofMinutes(30)), Duration.ofMinutes(60)),
        )
    }

    @Test
    fun sessionEnd_durationWinsOverLongerSlotEnd() {
        assertEquals(
            start.plus(Duration.ofMinutes(30)),
            disclosureSessionEnd(start, start.plus(Duration.ofMinutes(60)), Duration.ofMinutes(30)),
        )
    }

    @Test
    fun sessionEnd_noDurationFallsBackToSlotEnd() {
        val slotEnd = start.plus(Duration.ofMinutes(30))
        assertEquals(slotEnd, disclosureSessionEnd(start, slotEnd, null))
    }

    @Test
    fun sessionEnd_neitherFallsBackToStart() {
        assertEquals(start, disclosureSessionEnd(start, null, null))
    }

    // proposeDisclosure

    @Test
    fun durationCrossesExpiryOnlyViaDuration() {
        val proposed = now.plus(Duration.ofHours(72))
        val result =
            proposeDisclosure(
                proposedStart = proposed,
                slotEnd = proposed.plus(Duration.ofMinutes(30)),
                originalDuration = Duration.ofMinutes(60),
                authorizationExpiresAt = proposed.plus(Duration.ofMinutes(45)),
                now = now,
            )
        assertTrue(result.showsPaymentDisclosure)
        assertFalse(result.chargesSoon)
    }

    @Test
    fun propose_noAuthorizationExpiry_hidesDisclosure() {
        val proposed = now.plus(Duration.ofHours(72))
        assertFalse(
            proposeDisclosure(proposed, proposed.plusSeconds(1800), Duration.ofMinutes(30), null, now)
                .showsPaymentDisclosure,
        )
    }

    @Test
    fun propose_endAfterExpiryShows_equalHides() {
        val proposed = now.plus(Duration.ofHours(72))
        val duration = Duration.ofMinutes(60)
        val end = proposed.plus(duration)
        assertTrue(proposeDisclosure(proposed, null, duration, end.minusSeconds(1), now).showsPaymentDisclosure)
        assertFalse(proposeDisclosure(proposed, null, duration, end, now).showsPaymentDisclosure)
    }

    @Test
    fun propose_chargesSoonUnder48Hours() {
        val expiry = now.plus(Duration.ofHours(1))
        val at47 = proposeDisclosure(now.plus(Duration.ofHours(47)), null, Duration.ofMinutes(30), expiry, now)
        val at49 = proposeDisclosure(now.plus(Duration.ofHours(49)), null, Duration.ofMinutes(30), expiry, now)
        assertTrue(at47.chargesSoon)
        assertFalse(at49.chargesSoon)
    }

    // normalizeRescheduleNote (D-15)

    @Test
    fun note_isTrimmed() {
        assertEquals("hi", normalizeRescheduleNote("  hi  "))
    }

    @Test
    fun note_blankIsNull() {
        assertNull(normalizeRescheduleNote("   "))
        assertNull(normalizeRescheduleNote(""))
    }

    @Test
    fun note_isCappedAt500() {
        assertEquals(500, normalizeRescheduleNote("a".repeat(600))?.length)
    }

    // pickerSeedDate (Pitfall 7)

    @Test
    fun seed_futureBookingSeedsItsOwnLocalDate() {
        val today = LocalDate.of(2026, 11, 2)
        val booking = Instant.parse("2026-11-03T15:00:00Z")
        assertEquals(LocalDate.of(2026, 11, 3), pickerSeedDate(booking, zone, today))
    }

    @Test
    fun seed_inProgressBookingSeedsToday() {
        val today = LocalDate.of(2026, 11, 2)
        val booking = Instant.parse("2026-11-01T15:00:00Z")
        assertEquals(today, pickerSeedDate(booking, zone, today))
    }

    // toProposedStartWire (D-15)

    @Test
    fun wire_isSecondsPrecisionUtc() {
        assertEquals("2026-11-02T15:00:00Z", Instant.parse("2026-11-02T15:00:00.123Z").toProposedStartWire())
    }
}
