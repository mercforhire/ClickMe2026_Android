package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.model.responses.NotificationItem
import com.uptrendinvestments.clickmeapp.screens.notifications.NotificationsRepository
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.time.Instant

/** Wire-level tests (MockWebServer + the real Moshi) for `GET /notifications` (Phase 13). */
class NotificationsApiTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: NotificationsRepository

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
        repository = NotificationsRepository(api)
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

    @Test
    fun listSendsPageOneAndLimitFifty() =
        runTest {
            enqueue(200, """{"status":"success","data":{"notifications":[]}}""")

            repository.recent()

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/notifications", request.url.encodedPath)
            assertEquals("1", request.url.queryParameter("page"))
            assertEquals("50", request.url.queryParameter("limit"))
        }

    @Test
    fun rowDecodesWithFreeFormMetaData() =
        runTest {
            val first =
                """{"id":"n1","user_id":"u1","category":"booking","sub_category":"confirmations",""" +
                    """"title":"Booking confirmed","body":"Your session is confirmed.",""" +
                    """"meta_data":{"booking_id":"b1","refund_amount":1250,"nested":{"a":1}},""" +
                    """"expires_at":null,"is_expert_only":false,"created_at":"2026-10-27T15:00:00Z"}"""
            val second =
                """{"id":"n2","user_id":"u1","category":"promo","sub_category":null,"title":"Hello",""" +
                    """"body":"World","meta_data":null,"is_expert_only":true,"created_at":"2026-10-26T15:00:00Z"}"""
            enqueue(
                200,
                """{"status":"success","data":{"notifications":[$first,$second],""" +
                    """"pagination":{"current_page":1,"total_pages":1}}}""",
            )

            val rows = repository.recent()

            assertEquals(2, rows.size)
            val a = rows[0]
            assertEquals("n1", a.id)
            assertEquals("booking", a.category)
            assertEquals("confirmations", a.subCategory)
            assertEquals("Booking confirmed", a.title)
            assertEquals("b1", a.metaData?.get("booking_id"))
            assertEquals(1250.0, a.metaData?.get("refund_amount") as Double, 0.0)
            assertTrue(a.metaData?.get("nested") is Map<*, *>)
            assertFalse(a.isExpertOnly)
            assertEquals(Instant.parse("2026-10-27T15:00:00Z"), a.createdAt)
            val b = rows[1]
            assertEquals("promo", b.category)
            assertNull(b.subCategory)
            assertNull(b.metaData)
            assertTrue(b.isExpertOnly)
        }

    @Test
    fun emptyListAndMissingPaginationDecode() =
        runTest {
            enqueue(200, """{"status":"success","data":{"notifications":[]}}""")

            val rows = repository.recent()

            assertTrue(rows.isEmpty())
        }

    @Test
    fun toStringHidesTitleAndBody() {
        val item =
            NotificationItem(
                id = "n1",
                category = "booking",
                title = "secret-title",
                body = "secret-body",
                createdAt = Instant.parse("2026-10-27T15:00:00Z"),
            )

        val printed = item.toString()

        assertFalse(printed, printed.contains("secret-title"))
        assertFalse(printed, printed.contains("secret-body"))
        assertTrue(printed, printed.contains("n1"))
    }
}
