package com.uptrendinvestments.clickmeapp.screens.chat

import com.uptrendinvestments.clickmeapp.model.enums.UserReportReason
import com.uptrendinvestments.clickmeapp.model.requests.SendMessageRequest
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakeRealtimeConnection
import com.uptrendinvestments.clickmeapp.testing.chatThreadItem
import com.uptrendinvestments.clickmeapp.testing.chatsPayload
import com.uptrendinvestments.clickmeapp.testing.reportData
import com.uptrendinvestments.clickmeapp.testing.sendAck
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatsRepositoryTest {
    private val api = FakeClickMeApi()
    private val repository = ChatsRepository(api)

    @Test
    fun threadsUsesPageOneAndListLimit() =
        runTest {
            api.chatsHandler = { _, _ -> chatsPayload(chatThreadItem()) }

            repository.threads()

            assertEquals(listOf(1 to 50), api.chatsQueries)
        }

    @Test
    fun findThreadUsesScanLimitAndReturnsNullWhenAbsent() =
        runTest {
            api.chatsHandler = { _, _ -> chatsPayload(chatThreadItem(threadId = "t1")) }

            val found = repository.findThread("T1")
            val missing = repository.findThread("other")

            assertEquals("t1", found?.threadId)
            assertNull(missing)
            assertEquals(1 to 100, api.chatsQueries.last())
        }

    @Test
    fun sendRecordsThreadAndBody() =
        runTest {
            api.sendChatMessageHandler = { _, _ -> sendAck() }

            repository.send("t1", "hi", "c1")

            assertEquals("t1" to SendMessageRequest("text", "hi", "c1"), api.sentChatMessages.single())
        }

    @Test
    fun actionsAreRecordedInOrder() =
        runTest {
            api.reportUserHandler = { _, _ -> reportData() }

            repository.block("t1")
            repository.unblock("t1")
            repository.reportInChat("t1", UserReportReason.Spam, null)
            repository.reportUser("u1", UserReportReason.Other, "x")

            assertEquals(listOf("block", "unblock", "report"), api.chatActions.map { it.second.action })
            val (_, lastBody) = api.chatActions.last()
            assertEquals("spam", lastBody.reason)
            val (userId, body) = api.reportUserRequests.single()
            assertEquals("u1", userId)
            assertEquals("other", body.reason)
        }

    @Test
    fun realtimeFakeRecordsEmits() {
        val realtime = FakeRealtimeConnection()

        realtime.emitTypingStart("t1", "p1")
        realtime.emitTypingStop("t1", "p1")
        realtime.emitMessageRead("t1", listOf("m1"))

        assertEquals(listOf("t1" to "p1"), realtime.typingStarts)
        assertEquals(listOf("t1" to "p1"), realtime.typingStops)
        assertEquals(listOf("t1" to listOf("m1")), realtime.messageReads)
    }
}
