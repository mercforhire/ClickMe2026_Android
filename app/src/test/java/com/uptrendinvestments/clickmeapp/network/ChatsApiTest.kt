package com.uptrendinvestments.clickmeapp.network

import com.squareup.moshi.Types
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
}
