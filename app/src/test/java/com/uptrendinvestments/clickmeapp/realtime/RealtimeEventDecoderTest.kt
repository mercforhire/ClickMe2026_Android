package com.uptrendinvestments.clickmeapp.realtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class RealtimeEventDecoderTest {
    private val decoder = RealtimeEventDecoder()

    @Test
    fun `decodes message new with microsecond timestamp`() {
        val event =
            decoder.decode(
                "message.new",
                """{"message_id":"m1","thread_id":"t1","sender_id":"u2","type":"text","content":"hi",""" +
                    """"timestamp":"2026-10-20T16:17:09.983286+00:00","status":"sent"}""",
            )
        assertEquals(
            RealtimeEvent.MessageNew(
                messageId = "m1",
                threadId = "t1",
                senderId = "u2",
                type = "text",
                content = "hi",
                timestamp = Instant.parse("2026-10-20T16:17:09.983286Z"),
                status = "sent",
            ),
            event,
        )
    }

    @Test
    fun `message new without content decodes with null content`() {
        val event =
            decoder.decode(
                "message.new",
                """{"message_id":"m1","thread_id":"t1","sender_id":"u2","type":"image",""" +
                    """"timestamp":"2026-10-20T16:17:09+00:00","status":"sent"}""",
            ) as RealtimeEvent.MessageNew
        assertNull(event.content)
    }

    @Test
    fun `message new without message id returns null`() {
        val event =
            decoder.decode(
                "message.new",
                """{"thread_id":"t1","sender_id":"u2","type":"text","timestamp":"2026-10-20T16:17:09+00:00","status":"sent"}""",
            )
        assertNull(event)
    }

    @Test
    fun `decodes message read`() {
        val event = decoder.decode("message.read", """{"thread_id":"t1","message_ids":["m1","m2"],"read_by":"u2"}""")
        assertEquals(RealtimeEvent.MessageRead("t1", listOf("m1", "m2"), "u2"), event)
    }

    @Test
    fun `decodes user status online and offline`() {
        val online = decoder.decode("user.status", """{"user_id":"u2","status":"online"}""") as RealtimeEvent.UserStatus
        val offline =
            decoder.decode(
                "user.status",
                """{"user_id":"u2","status":"offline"}""",
            ) as RealtimeEvent.UserStatus
        assertEquals("u2", online.userId)
        assertTrue(online.isOnline)
        assertFalse(offline.isOnline)
    }

    @Test
    fun `decodes typing start and stop`() {
        assertEquals(
            RealtimeEvent.TypingStart("t1", "u2"),
            decoder.decode("typing.start", """{"thread_id":"t1","sender_id":"u2"}"""),
        )
        assertEquals(
            RealtimeEvent.TypingStop("t1", "u2"),
            decoder.decode("typing.stop", """{"thread_id":"t1","sender_id":"u2"}"""),
        )
    }

    @Test
    fun `decodes a plain booking update`() {
        assertEquals(
            RealtimeEvent.BookingUpdate("b1", "confirmed", null, null, null),
            decoder.decode("booking.update", """{"booking_id":"b1","status":"confirmed"}"""),
        )
    }

    @Test
    fun `decodes the completed booking update variant`() {
        assertEquals(
            RealtimeEvent.BookingUpdate("b1", "completed", "u9", null, true),
            decoder.decode(
                "booking.update",
                """{"booking_id":"b1","status":"completed","review_eligible":true,"reviewee_id":"u9"}""",
            ),
        )
    }

    @Test
    fun `decodes the expired booking update variant`() {
        assertEquals(
            RealtimeEvent.BookingUpdate("b1", "expired", null, true, null),
            decoder.decode("booking.update", """{"booking_id":"b1","status":"expired","actions_invalidated":true}"""),
        )
    }

    @Test
    fun `keeps the raw rescheduled status`() {
        val event = decoder.decode("booking.update", """{"booking_id":"b1","status":"rescheduled"}""")
        assertEquals("rescheduled", (event as RealtimeEvent.BookingUpdate).status)
    }

    @Test
    fun `decodes call ended with and without a reason`() {
        assertEquals(
            RealtimeEvent.CallEnded("b1", "dropped"),
            decoder.decode("call.ended", """{"booking_id":"b1","end_reason":"dropped"}"""),
        )
        assertEquals(
            RealtimeEvent.CallEnded("b1", null),
            decoder.decode("call.ended", """{"booking_id":"b1"}"""),
        )
    }

    @Test
    fun `unknown keys are ignored`() {
        assertEquals(
            RealtimeEvent.TypingStart("t1", "u2"),
            decoder.decode("typing.start", """{"thread_id":"t1","sender_id":"u2","extra":{"a":1}}"""),
        )
    }

    @Test
    fun `unknown event names return null`() {
        assertNull(decoder.decode("room.joined", """{"thread_id":"t1"}"""))
    }

    @Test
    fun `malformed payloads return null without throwing`() {
        assertNull(decoder.decode("typing.start", "{"))
        assertNull(decoder.decode("typing.start", "[]"))
        assertNull(decoder.decode("typing.start", "null"))
        assertNull(decoder.decode("typing.start", ""))
        // Moshi coerces a JSON number to a String, so a number is not a wrong type; boolean and object are.
        assertNull(decoder.decode("user.status", """{"user_id":"u2","status":true}"""))
        assertNull(decoder.decode("user.status", """{"user_id":"u2","status":{"a":1}}"""))
        assertNull(decoder.decode("message.read", """{"thread_id":"t1","message_ids":"m1","read_by":"u2"}"""))
        assertNull(
            decoder.decode(
                "message.new",
                """{"message_id":"m1","thread_id":"t1","sender_id":"u2","type":"text","timestamp":"yesterday","status":"sent"}""",
            ),
        )
    }

    @Test
    fun `message new toString does not leak the content`() {
        val text =
            RealtimeEvent
                .MessageNew("m1", "t1", "u2", "text", "secret text", Instant.EPOCH, "sent")
                .toString()
        assertFalse(text.contains("secret text"))
        assertTrue(text.contains("m1"))
    }

    @Test
    fun `inbound event names are exactly the seven contract events`() {
        assertEquals(
            listOf(
                "message.new",
                "message.read",
                "user.status",
                "typing.start",
                "typing.stop",
                "booking.update",
                "call.ended",
            ),
            RealtimeEventNames.INBOUND,
        )
    }
}
