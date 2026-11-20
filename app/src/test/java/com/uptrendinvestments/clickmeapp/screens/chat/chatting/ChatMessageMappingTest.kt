package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.realtime.RealtimeEvent
import com.uptrendinvestments.clickmeapp.screens.chat.ScannedBlock
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.subviews.titleRes
import com.uptrendinvestments.clickmeapp.testing.chatEvent
import com.uptrendinvestments.clickmeapp.testing.chatMessage
import com.uptrendinvestments.clickmeapp.testing.chatThreadItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ChatMessageMappingTest {
    @Test
    fun textRowIsMineWhenSenderIsMeIgnoringCase() {
        val mine = chatMessage(senderId = "P1").toThreadItem("p1") as ThreadItem.Message
        val theirs = chatMessage(senderId = "p2").toThreadItem("p1") as ThreadItem.Message
        assertTrue(mine.mine)
        assertEquals(false, theirs.mine)
    }

    @Test
    fun readStatusMapsToRead() {
        val read = chatMessage(status = "read").toThreadItem("me") as ThreadItem.Message
        val sent = chatMessage(status = "sent").toThreadItem("me") as ThreadItem.Message
        assertEquals(Delivery.Read, read.delivery)
        assertEquals(Delivery.Sent, sent.delivery)
    }

    @Test
    fun textRowWithNullSenderIsAMessageNotAnEvent() {
        val item = chatMessage(senderId = null, content = "bye").toThreadItem("me")
        assertTrue(item is ThreadItem.Message)
        assertEquals(false, (item as ThreadItem.Message).mine)
        assertNull(item.senderId)
    }

    @Test
    fun eventRowMapsKindBookingTopicAndStartTime() {
        val item =
            chatEvent(eventType = "BOOKING_CANCELLED", bookingId = "b1", topic = "Algorithms")
                .toThreadItem("me") as ThreadItem.SystemEvent
        assertEquals(SystemEventKind.BookingCancelled, item.kind)
        assertEquals("b1", item.bookingId)
        assertEquals("Algorithms", item.topic)
        assertEquals(Instant.parse("2026-11-21T15:00:00Z"), item.startTime)
    }

    @Test
    fun eventWithoutEventDataKeepsServerContent() {
        val item =
            chatEvent(
                eventType = "BOOKING_CONFIRMED",
                bookingId = null,
                topic = null,
                startTime = null,
                content = "Legacy",
            ).toThreadItem("me") as ThreadItem.SystemEvent
        assertEquals("Legacy", item.content)
        assertNull(item.bookingId)
        assertNull(item.topic)
        assertNull(item.startTime)
    }

    @Test
    fun unparseableStartTimeIsNull() {
        val item = chatEvent(startTime = "not a date").toThreadItem("me") as ThreadItem.SystemEvent
        assertNull(item.startTime)
    }

    private fun event(
        eventType: String,
        reasonText: String? = null,
        reasonBy: String? = null,
        declineReasonText: String? = null,
        declineReasonCode: String? = null,
        rescheduleDecline: Boolean? = null,
    ) = chatEvent(
        eventType = eventType,
        reasonText = reasonText,
        reasonBy = reasonBy,
        declineReasonText = declineReasonText,
        declineReasonCode = declineReasonCode,
        rescheduleDecline = rescheduleDecline,
    ).toThreadItem("me") as ThreadItem.SystemEvent

    @Test
    fun cancelReasonFromTheClientIsTrimmedAndLabelled() {
        val item = event("BOOKING_CANCELLED", reasonText = "  Running late  ", reasonBy = "client")
        assertEquals("Reason from the client: Running late", item.reasonCaption)
        assertNull(item.reasonCodeLabel)
    }

    @Test
    fun cancelReasonFromTheExpertIsLabelledAsTheExpert() {
        val item = event("BOOKING_CANCELLED", reasonText = "Running late", reasonBy = "expert")
        assertEquals("Reason from the expert: Running late", item.reasonCaption)
    }

    @Test
    fun cancelReasonWithoutReasonByUsesThePlainLabel() {
        val item = event("BOOKING_CANCELLED", reasonText = "Running late")
        assertEquals("Reason: Running late", item.reasonCaption)
    }

    @Test
    fun unknownReasonByUsesThePlainLabelNeverTheRawValue() {
        val item = event("BOOKING_CANCELLED", reasonText = "Running late", reasonBy = "admin")
        assertEquals("Reason: Running late", item.reasonCaption)
    }

    @Test
    fun rescheduleProposalNoteIsLabelledByWhoSentIt() {
        assertEquals(
            "Note from the expert: Can we do 3pm?",
            event("BOOKING_RESCHEDULED", reasonText = "Can we do 3pm?", reasonBy = "expert").reasonCaption,
        )
        assertEquals(
            "Note from the client: Can we do 3pm?",
            event("BOOKING_RESCHEDULED", reasonText = "Can we do 3pm?", reasonBy = "client").reasonCaption,
        )
        assertEquals(
            "Note: Can we do 3pm?",
            event("BOOKING_RESCHEDULED", reasonText = "Can we do 3pm?").reasonCaption,
        )
    }

    @Test
    fun requestDeclineFallsBackToDeclineReasonTextAsTheExpert() {
        val item = event("BOOKING_DECLINED", declineReasonText = "Fully booked", declineReasonCode = "UNAVAILABLE")
        assertEquals("Reason from the expert: Fully booked", item.reasonCaption)
        assertEquals("Not available at that time", item.reasonCodeLabel)
    }

    @Test
    fun requestDeclinePrefersReasonTextOverDeclineReasonText() {
        val item = event("BOOKING_DECLINED", reasonText = "New wording", declineReasonText = "Old wording")
        assertEquals("Reason from the expert: New wording", item.reasonCaption)
    }

    @Test
    fun declineCodeLabelsMatchTheIosTable() {
        val expected =
            mapOf(
                "UNAVAILABLE" to "Not available at that time",
                "OUTSIDE_EXPERTISE" to "Outside their expertise",
                "OUT_OF_SCOPE" to "Outside their expertise",
                "SCHEDULE_CONFLICT" to "Schedule conflict",
                "CONFLICT" to "Schedule conflict",
                "PRICING" to "Pricing mismatch",
                "OTHER" to "Other reason",
            )
        expected.forEach { (code, label) ->
            assertEquals(code, label, event("BOOKING_DECLINED", declineReasonCode = code).reasonCodeLabel)
        }
    }

    @Test
    fun unknownDeclineCodeShowsNothingAndNeverTheRawCode() {
        val item = event("BOOKING_DECLINED", declineReasonText = "Sorry", declineReasonCode = "SOMETHING_NEW")
        assertNull(item.reasonCodeLabel)
        assertEquals("Reason from the expert: Sorry", item.reasonCaption)
    }

    @Test
    fun rescheduleDeclineReadsRescheduleDeclinedAndIsNotTerminal() {
        val item = event("BOOKING_DECLINED", reasonText = "Too late", reasonBy = "client", rescheduleDecline = true)
        assertTrue(item.isRescheduleDecline)
        assertEquals(R.string.chat_event_reschedule_declined, item.titleRes())
        assertFalse(item.isTerminal)
        assertEquals("Reason from the client: Too late", item.reasonCaption)
    }

    @Test
    fun rescheduleDeclineWithoutReasonByUsesThePlainLabel() {
        val item = event("BOOKING_DECLINED", reasonText = "No", rescheduleDecline = true)
        assertEquals("Reason: No", item.reasonCaption)
    }

    @Test
    fun declineWithoutTheMarkerStaysABookingDeclinedAndTerminal() {
        val item = event("BOOKING_DECLINED", rescheduleDecline = false)
        assertFalse(item.isRescheduleDecline)
        assertEquals(R.string.chat_event_booking_declined, item.titleRes())
        assertTrue(item.isTerminal)
    }

    @Test
    fun blankReasonShowsNoCaption() {
        assertNull(event("BOOKING_CANCELLED", reasonText = "   ", reasonBy = "client").reasonCaption)
        assertNull(event("BOOKING_DECLINED", reasonText = "  ", declineReasonText = "\n").reasonCaption)
    }

    @Test
    fun eventWithNoReasonKeysMapsAsBefore() {
        val item = event("BOOKING_CANCELLED")
        assertNull(item.reasonCaption)
        assertNull(item.reasonCodeLabel)
        assertFalse(item.isRescheduleDecline)
        assertEquals(SystemEventKind.BookingCancelled, item.kind)
        assertEquals(R.string.chat_event_booking_cancelled, item.titleRes())
        assertTrue(item.isTerminal)
    }

    @Test
    fun reasonTextNeverReachesToString() {
        val item = event("BOOKING_CANCELLED", reasonText = "Private reason", reasonBy = "client")
        assertFalse(item.toString().contains("Private reason"))
    }

    @Test
    fun textIsKeptVerbatim() {
        val item = chatMessage(content = "  a\nb  ").toThreadItem("me") as ThreadItem.Message
        assertEquals("  a\nb  ", item.text)
    }

    @Test
    fun nullContentMapsToEmptyText() {
        val item = chatMessage(content = null).toThreadItem("me") as ThreadItem.Message
        assertEquals("", item.text)
    }

    @Test
    fun messageNewEventMapsToMessage() {
        val event =
            RealtimeEvent.MessageNew(
                messageId = "m9",
                threadId = "t1",
                senderId = "P1",
                type = "text",
                content = "Hi",
                timestamp = Instant.parse("2026-11-20T10:00:00Z"),
                status = "sent",
            )
        val item = event.toThreadItem("p1")
        assertEquals("m9", item.serverId)
        assertTrue(item.mine)
        assertEquals("Hi", item.text)
    }

    @Test
    fun firstPeerSenderIdSkipsMyMessages() {
        val items =
            listOf(
                chatMessage(id = "m1", senderId = "me").toThreadItem("me"),
                chatMessage(id = "m2", senderId = "p1").toThreadItem("me"),
            )
        assertEquals("p1", firstPeerSenderId(items))
        assertNull(firstPeerSenderId(items.take(1)))
    }

    @Test
    fun scannedBlockCarriesBothFlags() {
        assertEquals(
            ScannedBlock(isBlocked = true, blockedByMe = true),
            chatThreadItem(isBlocked = true, blockedByMe = true).toScannedBlock(),
        )
    }
}
