package com.uptrendinvestments.clickmeapp.network

import com.squareup.moshi.Types
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.model.enums.UserReportReason
import com.uptrendinvestments.clickmeapp.model.requests.ChatActionRequest
import com.uptrendinvestments.clickmeapp.model.requests.ReportUserRequest
import com.uptrendinvestments.clickmeapp.model.requests.SendMessageRequest
import com.uptrendinvestments.clickmeapp.model.responses.ChatMessageItem
import com.uptrendinvestments.clickmeapp.model.responses.ChatPartnerDto
import com.uptrendinvestments.clickmeapp.model.responses.ChatThreadItem
import com.uptrendinvestments.clickmeapp.screens.chat.ChatsRepository
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.time.Instant

/** Wire-level tests (MockWebServer + the real Moshi) for the Phase 12 chat and reporting endpoints. */
class ChatsApiTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: ChatsRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val api =
            Retrofit
                .Builder()
                .baseUrl(server.url("/api/v1/"))
                .client(OkHttpClient())
                .addConverterFactory(MoshiConverterFactory.create(MoshiProvider.moshi))
                .build()
                .create(ClickMeApi::class.java)
        repository = ChatsRepository(api)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun enqueue(
        code: Int,
        body: String,
    ) {
        server.enqueue(
            MockResponse
                .Builder()
                .code(code)
                .addHeader("Content-Type", "application/json")
                .body(body)
                .build(),
        )
    }

    /** The JSON keys of a request body, parsed so the assertion does not depend on key order. */
    private fun keysOf(json: String?): Set<String> {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        val map = MoshiProvider.moshi.adapter<Map<String, Any?>>(type).fromJson(requireNotNull(json))
        return requireNotNull(map).keys
    }

    private val threadJson =
        """{"thread_id":"t1","partner":{"id":"p1","name":"Ada Lovelace","avatar_url":null,"roles":["expert"]},""" +
            """"last_message":null,"last_msg_at":null,"unread_count":3,"is_online":true,"is_blocked":true}"""

    @Test
    fun initiatePostsPeerIdOnly() =
        runTest {
            enqueue(201, """{"status":"success","data":{"thread_id":"t1","created_at":"2026-11-20T10:00:00Z"}}""")

            val data = repository.initiate("p1")

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/v1/chats/initiate", request.url.encodedPath)
            val body = request.body?.utf8()
            assertEquals("""{"peer_id":"p1"}""", body)
            assertFalse(body.orEmpty().contains("expert_id"))
            assertEquals("t1", data.threadId)
            assertEquals(Instant.parse("2026-11-20T10:00:00Z"), data.createdAt)
        }

    @Test
    fun listSendsPageAndLimitAndDecodesThread() =
        runTest {
            enqueue(
                200,
                """{"status":"success","data":{"threads":[$threadJson],""" +
                    """"pagination":{"current_page":1,"total_pages":1}}}""",
            )

            val payload = repository.threads()

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/chats", request.url.encodedPath)
            assertEquals("1", request.url.queryParameter("page"))
            assertEquals("50", request.url.queryParameter("limit"))
            val thread = payload.threads.single()
            assertEquals("t1", thread.threadId)
            assertNull(thread.lastMessage)
            assertNull(thread.lastMsgAt)
            assertFalse(thread.blockedByMe)
            assertTrue(thread.isBlocked)
            assertTrue(thread.isOnline)
            assertEquals(3, thread.unreadCount)
            assertEquals(listOf("expert"), thread.partner.roles)
            assertEquals("Ada Lovelace", thread.partner.name)
            assertEquals(1, payload.pagination?.currentPage)
        }

    @Test
    fun findThreadScansOneHundredAndMatchesIgnoringCase() =
        runTest {
            enqueue(200, """{"status":"success","data":{"threads":[$threadJson]}}""")
            enqueue(200, """{"status":"success","data":{"threads":[$threadJson]}}""")

            val found = repository.findThread("T1")
            val first = server.takeRequest()
            val missing = repository.findThread("nope")

            assertEquals("100", first.url.queryParameter("limit"))
            assertEquals("1", first.url.queryParameter("page"))
            assertEquals("t1", found?.threadId)
            assertNull(missing)
        }

    @Test
    fun messagesSendPageAndLimitAndDecodeEventRows() =
        runTest {
            val eventRow =
                """{"id":"e1","thread_id":"t1","sender_id":null,"type":"event","content":null,"status":"sent",""" +
                    """"client_msg_id":null,"event_type":"BOOKING_CONFIRMED","event_data":{"booking_id":"b1",""" +
                    """"duration_mins":45,"topic_title":"Algorithms"},"created_at":"2026-11-20T10:00:00Z",""" +
                    """"sender":null}"""
            val textRow =
                """{"id":"m1","thread_id":"t1","sender_id":null,"type":"text","content":"Hello","status":"read",""" +
                    """"client_msg_id":"c1","created_at":"2026-11-20T10:01:00Z"}"""
            enqueue(
                200,
                """{"status":"success","data":{"messages":[$eventRow,$textRow],""" +
                    """"pagination":{"current_page":2,"total_pages":3}}}""",
            )

            val payload = repository.messages("t1", page = 2)

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/chats/t1/messages", request.url.encodedPath)
            assertEquals("2", request.url.queryParameter("page"))
            assertEquals("30", request.url.queryParameter("limit"))
            val event = payload.messages[0]
            assertEquals("event", event.type)
            assertNull(event.senderId)
            assertNull(event.sender)
            assertEquals("BOOKING_CONFIRMED", event.eventType)
            assertEquals("b1", event.eventData?.get("booking_id"))
            assertEquals(45.0, event.eventData?.get("duration_mins") as Double, 0.0)
            assertEquals("Algorithms", event.eventData?.get("topic_title"))
            val text = payload.messages[1]
            assertNull(text.senderId)
            assertEquals("Hello", text.content)
            assertEquals("read", text.status)
            assertEquals("c1", text.clientMsgId)
            assertEquals(Instant.parse("2026-11-20T10:01:00Z"), text.createdAt)
            assertEquals(2, payload.pagination?.currentPage)
            assertEquals(3, payload.pagination?.totalPages)
        }

    @Test
    fun sendPostsOnlySchemaKeys() =
        runTest {
            enqueue(
                201,
                """{"status":"success","data":{"message_id":"m-server","status":"sent",""" +
                    """"timestamp":"2026-11-20T10:02:00Z"}}""",
            )

            val ack = repository.send("t1", "hi there", "c1")

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/v1/chats/t1/send", request.url.encodedPath)
            val body = request.body?.utf8()
            assertEquals(setOf("type", "content", "client_msg_id"), keysOf(body))
            assertFalse(body.orEmpty().contains("null"))
            assertTrue(body.orEmpty().contains(""""type":"text""""))
            assertEquals("m-server", ack.messageId)
            assertEquals("sent", ack.status)
            assertEquals(Instant.parse("2026-11-20T10:02:00Z"), ack.timestamp)
            assertNotNull(ack.timestamp)
        }

    @Test
    fun blockAndUnblockSendOnlyAction() =
        runTest {
            enqueue(200, """{"status":"success"}""")
            enqueue(200, """{"status":"success"}""")

            repository.block("t1")
            repository.unblock("t1")

            val block = server.takeRequest()
            val unblock = server.takeRequest()
            assertEquals("POST", block.method)
            assertEquals("/api/v1/chats/t1/actions", block.url.encodedPath)
            assertEquals("""{"action":"block"}""", block.body?.utf8())
            assertEquals("""{"action":"unblock"}""", unblock.body?.utf8())
        }

    @Test
    fun reportInChatSendsWireValueAndTrimmedDetails() =
        runTest {
            enqueue(200, """{"status":"success"}""")
            enqueue(200, """{"status":"success"}""")

            repository.reportInChat("t1", UserReportReason.Harassment, "  rude  ")
            repository.reportInChat("t1", UserReportReason.Harassment, "   ")

            val withDetails = server.takeRequest()
            val blank = server.takeRequest()
            assertEquals("/api/v1/chats/t1/actions", withDetails.url.encodedPath)
            assertEquals(
                """{"action":"report","reason":"harassment","details":"rude"}""",
                withDetails.body?.utf8(),
            )
            assertEquals("""{"action":"report","reason":"harassment"}""", blank.body?.utf8())
        }

    @Test
    fun reportUserPostsReasonAndDecodes201() =
        runTest {
            enqueue(
                201,
                """{"status":"success","data":{"report_id":"r1","status":"open",""" +
                    """"created_at":"2026-11-20T10:03:00Z"}}""",
            )

            val data = repository.reportUser("u1", UserReportReason.ScamOrFraud, null)

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/v1/users/u1/report", request.url.encodedPath)
            assertEquals("""{"reason":"scam_or_fraud"}""", request.body?.utf8())
            assertEquals("r1", data.reportId)
            assertEquals("open", data.status)
        }

    private suspend fun codeOf(block: suspend () -> Unit): ApiException.Http {
        try {
            block()
        } catch (e: ApiException.Http) {
            return e
        }
        throw AssertionError("Expected ApiException.Http")
    }

    @Test
    fun errorCodesMapOnHttpErrors() =
        runTest {
            fun error(code: String) = """{"status":"error","code":"$code","message":"m"}"""
            enqueue(403, error("THREAD_BLOCKED"))
            enqueue(403, error("RECIPIENT_UNAVAILABLE"))
            enqueue(403, error("NOT_BLOCKER"))
            enqueue(429, error("TOO_MANY_REQUESTS"))
            enqueue(422, error("CANNOT_CHAT_WITH_SELF"))
            enqueue(404, error("NOT_FOUND"))

            val blocked = codeOf { repository.send("t1", "hi", "c1") }
            val unavailable = codeOf { repository.send("t1", "hi", "c2") }
            val notBlocker = codeOf { repository.unblock("t1") }
            val limited = codeOf { repository.reportUser("u1", UserReportReason.Spam, null) }
            val self = codeOf { repository.initiate("me") }
            val missing = codeOf { repository.initiate("ghost") }

            assertEquals(ErrorCode.ThreadBlocked, blocked.code)
            assertEquals(ErrorCode.RecipientUnavailable, unavailable.code)
            assertEquals(ErrorCode.NotBlocker, notBlocker.code)
            assertEquals(ErrorCode.TooManyRequests, limited.code)
            assertEquals(ErrorCode.CannotChatWithSelf, self.code)
            assertEquals(422, self.status)
            assertEquals(404, missing.status)
        }

    @Test
    fun toStringNeverPrintsUserText() {
        val message =
            ChatMessageItem(
                id = "m1",
                threadId = "t1",
                type = "text",
                content = "secret-body",
                createdAt = Instant.parse("2026-11-20T10:00:00Z"),
            )
        val thread = ChatThreadItem(threadId = "t1", partner = ChatPartnerDto(id = "p1"), lastMessage = "secret-body")
        val printed =
            listOf(
                SendMessageRequest("text", "secret-body", "id").toString(),
                ChatActionRequest("report", "spam", "secret-details").toString(),
                ReportUserRequest("spam", "secret-details").toString(),
                message.toString(),
                thread.toString(),
            )
        printed.forEach {
            assertFalse(it, it.contains("secret-body"))
            assertFalse(it, it.contains("secret-details"))
        }
    }
}
