package com.uptrendinvestments.clickmeapp.services.push

import com.uptrendinvestments.clickmeapp.screens.chat.ChatSessionState
import com.uptrendinvestments.clickmeapp.screens.push.PushLaunchRequest
import org.junit.Assert.assertEquals
import org.junit.Test

class PushMessageHandlerTest {
    private val launchRequest = PushLaunchRequest()
    private val chatSession = ChatSessionState()
    private val poster = RecordingPoster()
    private val handler = PushMessageHandler(launchRequest, chatSession, poster)

    private class RecordingPoster : PushNotificationPoster {
        val posted = mutableListOf<ForegroundPush>()

        override fun post(push: ForegroundPush) {
            posted += push
        }
    }

    private fun push(vararg data: Pair<String, String>) = ForegroundPush(mapOf(*data), "Title", "Body", null)

    @Test
    fun foregroundPushTicksAndPosts() {
        val push =
            ForegroundPush(
                mapOf("category" to "booking", "sub_category" to "confirmations", "booking_id" to BOOKING),
                "Booking confirmed",
                "Your booking was confirmed",
                null,
            )

        handler.onMessage(push)

        assertEquals(1L, launchRequest.arrivals.value)
        assertEquals(listOf(push), poster.posted)
    }

    @Test
    fun visibleThreadChatPushIsHidden() {
        chatSession.onThreadShown(THREAD.uppercase())
        val visible = push("category" to "message", "sub_category" to "new_message", "thread_id" to THREAD)
        val other = push("category" to "message", "sub_category" to "new_message", "thread_id" to OTHER_THREAD)
        val booking = push("category" to "booking", "booking_id" to BOOKING)

        handler.onMessage(visible)
        assertEquals(1L, launchRequest.arrivals.value)
        assertEquals(emptyList<ForegroundPush>(), poster.posted)

        handler.onMessage(other)
        handler.onMessage(booking)
        assertEquals(listOf(other, booking), poster.posted)
        assertEquals(3L, launchRequest.arrivals.value)
    }

    @Test
    fun legacyChatPushIsAlsoHidden() {
        chatSession.onThreadShown(THREAD)

        handler.onMessage(push("event_type" to "NEW_MESSAGE", "thread_id" to THREAD))

        assertEquals(emptyList<ForegroundPush>(), poster.posted)
        assertEquals(1L, launchRequest.arrivals.value)
    }

    @Test
    fun chatPushWithoutOpenThreadOrThreadIdStillPosts() {
        val noThreadId = push("category" to "message")
        handler.onMessage(noThreadId)
        handler.onMessage(push("category" to "message", "thread_id" to THREAD))

        assertEquals(2, poster.posted.size)
    }

    @Test
    fun toStringNeverPrintsContent() {
        val secret = push("category" to "message", "booking_id" to BOOKING).copy(title = "Secret", body = "Hidden")

        assertEquals("ForegroundPush(channelId=null, keys=[category, booking_id])", secret.toString())
    }

    private companion object {
        const val BOOKING = "11111111-1111-4111-8111-111111111111"
        const val THREAD = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
        const val OTHER_THREAD = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"
    }
}
