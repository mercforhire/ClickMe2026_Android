package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class RescheduleDisclosureTest {
    private val now = Instant.parse("2026-10-22T16:00:00Z")
    private val authEnd = Instant.parse("2026-10-29T16:00:00Z")

    @Test
    fun disclosure_hidden_when_authorization_expiry_is_missing() {
        val result = rescheduleDisclosure(now.plusSeconds(3600 * 100), now.plusSeconds(3600 * 101), null, now)
        assertFalse(result.showsPaymentDisclosure)
    }

    @Test
    fun disclosure_hidden_when_proposed_end_is_missing() {
        val result = rescheduleDisclosure(now.plusSeconds(3600 * 100), null, authEnd, now)
        assertFalse(result.showsPaymentDisclosure)
    }

    @Test
    fun disclosure_only_when_proposed_end_is_strictly_after_authorization_expiry() {
        val start = now.plusSeconds(3600 * 100)
        assertFalse(rescheduleDisclosure(start, authEnd, authEnd, now).showsPaymentDisclosure)
        assertTrue(rescheduleDisclosure(start, authEnd.plusSeconds(1), authEnd, now).showsPaymentDisclosure)
    }

    @Test
    fun charges_soon_only_when_proposed_start_is_strictly_under_48_hours_away() {
        assertTrue(rescheduleDisclosure(now.plusSeconds(47 * 3600), null, null, now).chargesSoon)
        assertFalse(rescheduleDisclosure(now.plusSeconds(48 * 3600), null, null, now).chargesSoon)
        assertFalse(rescheduleDisclosure(now.plusSeconds(72 * 3600), null, null, now).chargesSoon)
    }

    @Test
    fun proposal_is_null_unless_status_is_pending_reschedule() {
        val detail = bookingDetail(status = "confirmed", reschedule = rescheduleDto())
        assertNull(detail.toRescheduleProposalUi(now))
    }

    @Test
    fun proposal_is_null_without_a_reschedule_or_proposed_start() {
        assertNull(bookingDetail(status = STATUS_PENDING_RESCHEDULE, reschedule = null).toRescheduleProposalUi(now))
        val noStart = rescheduleDto(proposedStart = null)
        assertNull(bookingDetail(status = STATUS_PENDING_RESCHEDULE, reschedule = noStart).toRescheduleProposalUi(now))
    }

    @Test
    fun expert_proposal_maps_all_fields_and_trims_the_note() {
        val proposal = Instant.parse("2026-10-30T18:00:00Z")
        val detail =
            bookingDetail(
                status = STATUS_PENDING_RESCHEDULE,
                start = Instant.parse("2026-10-24T18:00:00Z"),
                authorizationExpiresAt = Instant.parse("2026-10-29T00:00:00Z"),
                reschedule = rescheduleDto(proposedStart = proposal, initiatedBy = "expert", note = "  Can we move?  "),
            )
        val ui = detail.toRescheduleProposalUi(now)
        assertNotNull(ui)
        assertEquals(Instant.parse("2026-10-24T18:00:00Z"), ui!!.currentStart)
        assertEquals(proposal, ui.proposedStart)
        assertEquals(proposal.plusSeconds(3600), ui.proposedEnd)
        assertEquals("Can we move?", ui.note)
        assertTrue(ui.initiatedByExpert)
        assertTrue(ui.showsPaymentDisclosure)
        assertFalse(ui.chargesSoon)
    }

    @Test
    fun blank_note_maps_to_null() {
        val detail =
            bookingDetail(status = STATUS_PENDING_RESCHEDULE, reschedule = rescheduleDto(note = "   "))
        assertNull(detail.toRescheduleProposalUi(now)!!.note)
    }

    @Test
    fun client_or_unknown_initiator_is_not_actionable() {
        val client = bookingDetail(status = STATUS_PENDING_RESCHEDULE, reschedule = rescheduleDto(initiatedBy = "client"))
        val unknown = bookingDetail(status = STATUS_PENDING_RESCHEDULE, reschedule = rescheduleDto(initiatedBy = null))
        assertFalse(client.toRescheduleProposalUi(now)!!.initiatedByExpert)
        assertFalse(unknown.toRescheduleProposalUi(now)!!.initiatedByExpert)
    }
}
