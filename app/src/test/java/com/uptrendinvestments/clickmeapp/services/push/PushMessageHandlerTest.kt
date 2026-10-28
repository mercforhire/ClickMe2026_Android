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
    fun toStringNeverPrintsContent() {
        val secret = push("category" to "message", "booking_id" to BOOKING).copy(title = "Secret", body = "Hidden")

        assertEquals("ForegroundPush(channelId=null, keys=[category, booking_id])", secret.toString())
    }

    private companion object {
        const val BOOKING = "11111111-1111-4111-8111-111111111111"
    }
}
