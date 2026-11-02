package com.uptrendinvestments.clickmeapp.network

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
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.time.Instant

/**
 * Exact-wire tests for `GET /client/transactions` (Phase 16.5, CLIENT-TRANSACTIONS.md): the paging query, every
 * field of the ledger row, and the three decode rules that are silent when wrong (null amount, raw status, no
 * refund timestamp).
 */
class ClientTransactionsApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: ClickMeApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api =
            Retrofit
                .Builder()
                .baseUrl(server.url("/api/v1/"))
                .client(OkHttpClient())
                .addConverterFactory(MoshiConverterFactory.create(MoshiProvider.moshi))
                .build()
                .create(ClickMeApi::class.java)
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
    fun sendsPageAndLimit() =
        runTest {
            enqueue(200, EMPTY_PAGE)

            api.getClientTransactions(2, 20)

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/client/transactions", request.url.encodedPath)
            assertEquals("2", request.url.queryParameter("page"))
            assertEquals("20", request.url.queryParameter("limit"))
            assertEquals(2, request.url.querySize)
        }

    @Test
    fun decodesEveryField() =
        runTest {
            enqueue(
                200,
                """
                {"status":"success","data":{"transactions":[{
                  "booking_id":"8c8b0db9-2ae5-4cdf-bc2b-2ecd1d4e9eb6",
                  "session_date":"2026-08-17T14:30:00+00:00",
                  "topic_title":"Backend code & API design review",
                  "expert_name":"UAT Preonboard Expert",
                  "expert_avatar_url":"https://cdn.example/a.jpeg",
                  "amount":7500,
                  "currency":"CAD",
                  "payment_status":"captured",
                  "refunded_amount":0,
                  "paid_at":"2026-08-17T15:45:02.740+00:00",
                  "refunded_at":null,
                  "created_at":"2026-08-17T14:23:14.871+00:00"
                }],"pagination":{"total_count":15,"total_pages":1,"current_page":1,"has_more":false}}}
                """.trimIndent(),
            )

            val data = api.getClientTransactions(1, 20).data
            val row = data.transactions.single()

            assertEquals("8c8b0db9-2ae5-4cdf-bc2b-2ecd1d4e9eb6", row.bookingId)
            assertEquals(Instant.parse("2026-08-17T14:30:00Z"), row.sessionDate)
            assertEquals("Backend code & API design review", row.topicTitle)
            assertEquals("UAT Preonboard Expert", row.expertName)
            assertEquals("https://cdn.example/a.jpeg", row.expertAvatarUrl)
            assertEquals(7500L, row.amount)
            assertEquals("CAD", row.currency)
            assertEquals("captured", row.paymentStatus)
            assertEquals(0L, row.refundedAmount)
            assertNotNull(row.paidAt)
            assertEquals(Instant.parse("2026-08-17T15:45:02.740Z"), row.paidAt)
            assertEquals(Instant.parse("2026-08-17T14:23:14.871Z"), row.createdAt)
            assertEquals(1, data.pagination?.currentPage)
            assertEquals(15, data.pagination?.totalCount)
            assertFalse(data.hasMore)
        }

    @Test
    fun hasMoreFollowsTheServerFlag() =
        runTest {
            enqueue(
                200,
                """{"status":"success","data":{"transactions":[],"pagination":{"total_count":40,"total_pages":2,
                    |"current_page":1,"has_more":true}}}
                """.trimMargin(),
            )

            assertTrue(api.getClientTransactions(1, 20).data.hasMore)
        }

    @Test
    fun nullFieldsStayNull() =
        runTest {
            enqueue(
                200,
                """
                {"status":"success","data":{"transactions":[{
                  "booking_id":"b1",
                  "session_date":null,
                  "topic_title":null,
                  "expert_name":null,
                  "expert_avatar_url":null,
                  "amount":null,
                  "currency":"CAD",
                  "payment_status":"scheduled",
                  "refunded_amount":0,
                  "paid_at":null,
                  "refunded_at":null,
                  "created_at":"2026-08-17T14:23:14.871+00:00"
                }],"pagination":{"total_count":1,"total_pages":1,"current_page":1,"has_more":false}}}
                """.trimIndent(),
            )

            val row =
                api
                    .getClientTransactions(1, 20)
                    .data.transactions
                    .single()

            assertEquals("b1", row.bookingId)
            assertNull(row.sessionDate)
            assertNull(row.topicTitle)
            assertNull(row.expertName)
            assertNull(row.expertAvatarUrl)
            assertNull(row.amount)
            assertNull(row.paidAt)
        }

    @Test
    fun zeroAmountIsNotNull() =
        runTest {
            enqueue(200, pageWithRow(amount = "0", status = "captured"))

            val row =
                api
                    .getClientTransactions(1, 20)
                    .data.transactions
                    .single()

            assertEquals(0L, row.amount)
        }

    @Test
    fun unknownPaymentStatusDecodes() =
        runTest {
            enqueue(200, pageWithRow(amount = "100", status = "something_new"))

            val data = api.getClientTransactions(1, 20).data

            assertEquals("something_new", data.transactions.single().paymentStatus)
            assertEquals(1, data.transactions.size)
        }

    @Test
    fun http500SurfacesAsHttp() =
        runTest {
            enqueue(500, """{"status":"error","code":"INTERNAL_ERROR","message":"Internal error"}""")

            try {
                apiCall { api.getClientTransactions(99, 20) }
                fail("expected ApiException.Http")
            } catch (e: ApiException.Http) {
                assertEquals(500, e.status)
            }
        }

    @Test
    fun toStringHoldsNoNames() =
        runTest {
            enqueue(
                200,
                pageWithRow(amount = "100", status = "captured", expert = "Ada Lovelace", topic = "Algorithms"),
            )

            val row =
                api
                    .getClientTransactions(1, 20)
                    .data.transactions
                    .single()
            val text = row.toString()

            assertFalse(text, text.contains("Ada Lovelace"))
            assertFalse(text, text.contains("Algorithms"))
            assertTrue(text, text.contains("b1"))
        }

    private fun pageWithRow(
        amount: String,
        status: String,
        expert: String = "Ada",
        topic: String = "Topic",
    ): String =
        """
        {"status":"success","data":{"transactions":[{
          "booking_id":"b1",
          "session_date":"2026-08-17T14:30:00+00:00",
          "topic_title":"$topic",
          "expert_name":"$expert",
          "expert_avatar_url":null,
          "amount":$amount,
          "currency":"CAD",
          "payment_status":"$status",
          "refunded_amount":0,
          "paid_at":null,
          "refunded_at":null,
          "created_at":"2026-08-17T14:23:14.871+00:00"
        }],"pagination":{"total_count":1,"total_pages":1,"current_page":1,"has_more":false}}}
        """.trimIndent()

    private companion object {
        const val EMPTY_PAGE =
            """{"status":"success","data":{"transactions":[],"pagination":""" +
                """{"total_count":0,"total_pages":0,"current_page":1,"has_more":false}}}"""
    }
}
