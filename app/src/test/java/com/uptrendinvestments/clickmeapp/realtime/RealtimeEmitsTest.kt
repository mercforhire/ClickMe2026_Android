package com.uptrendinvestments.clickmeapp.realtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RealtimeEmitsTest {
    @Test
    fun `typing payload lowercases both ids`() {
        assertEquals(
            """{"thread_id":"3f2a-ab","recipient_id":"c0ffee"}""",
            RealtimeEmits.typingPayload("3F2A-AB", "C0FFEE"),
        )
    }

    @Test
    fun `message read payload lowercases every id`() {
        assertEquals(
            """{"thread_id":"t1","message_ids":["m1","m2"]}""",
            RealtimeEmits.messageReadPayload("T1", listOf("M1", "m2")),
        )
    }

    @Test
    fun `message read with no ids produces nothing`() {
        assertNull(RealtimeEmits.messageReadPayload("t1", emptyList()))
    }

    @Test
    fun `emit event names match the contract`() {
        assertEquals("typing.start", RealtimeEventNames.TYPING_START)
        assertEquals("typing.stop", RealtimeEventNames.TYPING_STOP)
        assertEquals("message.read", RealtimeEventNames.MESSAGE_READ)
    }

    @Test
    fun `ids are escaped by the serializer rather than concatenated`() {
        assertEquals(
            """{"thread_id":"a\"b","recipient_id":"c"}""",
            RealtimeEmits.typingPayload("a\"b", "c"),
        )
    }
}
