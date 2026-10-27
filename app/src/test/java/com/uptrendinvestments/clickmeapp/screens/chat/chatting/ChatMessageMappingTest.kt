package com.uptrendinvestments.clickmeapp.screens.chat.chatting

import com.uptrendinvestments.clickmeapp.realtime.RealtimeEvent
import com.uptrendinvestments.clickmeapp.screens.chat.ScannedBlock
import com.uptrendinvestments.clickmeapp.testing.chatEvent
import com.uptrendinvestments.clickmeapp.testing.chatMessage
import com.uptrendinvestments.clickmeapp.testing.chatThreadItem
import org.junit.Assert.assertEquals
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
