package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.model.responses.FEE_SOURCE_EXPERT_DEFAULT
import com.uptrendinvestments.clickmeapp.model.responses.FEE_SOURCE_UNAVAILABLE
import com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests.BookingRequestsRepository
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.time.Instant

/** Wire-level tests (MockWebServer + the real Moshi) for the Phase 11 booking-request endpoints. */
class BookingRequestsApiTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: BookingRequestsRepository

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
        repository = BookingRequestsRepository(api)
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

    private val paidItem =
        """{"request_id":"r1","client":{"id":"c1","full_name":"Ada Lovelace","avatar_url":null},""" +
            """"topic":"Taxes","earnings":75.0,"currency":"USD",""" +
            """"earnings_breakdown":{"gross_amount":75,"platform_fee":11.25,"net_amount":63.75,""" +
            """"commission_pct":15,"currency":"USD","fee_source":"booking"},""" +
            """"time_slot":{"start_time":"2026-11-02T15:00:00Z","end_time":"2026-11-02T16:00:00Z"},""" +
            """"expires_at":"2026-11-01T10:00:00Z","status":"pending_approval"}"""

    private val freeItem =
        """{"request_id":"r2","client":{"id":"c2","full_name":"Grace Hopper","avatar_url":"https://x/a.png"},""" +
            """"topic":"Intro","earnings":null,"currency":"USD",""" +
            """"earnings_breakdown":{"gross_amount":null,"platform_fee":null,"net_amount":null,""" +
            """"commission_pct":null,"currency":"USD","fee_source":"unavailable"},""" +
            """"time_slot":{"start_time":"2026-11-03T15:00:00Z","end_time":"2026-11-03T15:30:00Z"},""" +
            """"expires_at":"2026-11-02T10:00:00Z","status":"pending_approval"}"""

    @Test
    fun listIsBodylessGetWithoutPagingAndDecodesItems() =
        runTest {
            enqueue(200, """{"status":"success","data":{"requests":[$paidItem,$freeItem]}}""")

            val items = repository.list()

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/expert/booking-requests", request.url.encodedPath)
            assertNull(request.url.encodedQuery)
            assertEquals(2, items.size)
            val paid = items[0]
            assertEquals("r1", paid.requestId)
            assertEquals("Ada Lovelace", paid.client.fullName)
            assertEquals(63.75, paid.earningsBreakdown?.netAmount ?: 0.0, 0.0)
            assertEquals(Instant.parse("2026-11-01T10:00:00Z"), paid.expiresAt)
            assertEquals(Instant.parse("2026-11-02T15:00:00Z"), paid.timeSlot.startTime)
            val free = items[1]
            assertNull(free.earnings)
            assertNull(free.earningsBreakdown?.netAmount)
            assertEquals(FEE_SOURCE_UNAVAILABLE, free.earningsBreakdown?.feeSource)
        }

    @Test
    fun detailDecodesSessionAndEconomics() =
        runTest {
            enqueue(
                200,
                """{"status":"success","data":{"request_id":"r1",""" +
                    """"client":{"id":"c1","name":"Ada Lovelace","avatar_url":null},""" +
                    """"session":{"start_time":"2026-11-02T15:00:00Z","end_time":"2026-11-02T16:00:00Z",""" +
                    """"topic":"Taxes","meeting_type":"skype_zoom","client_message":"Hello"},""" +
                    """"economics":{"potential_earnings":0.75,"currency":"USD","earnings_breakdown":""" +
                    """{"gross_amount":75,"platform_fee":11.25,"net_amount":63.75,"commission_pct":15,""" +
                    """"currency":"USD","fee_source":"expert_default"}}}}""",
            )

            val detail = repository.detail("r1")

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/expert/booking-requests/r1", request.url.encodedPath)
            assertEquals("Ada Lovelace", detail.client.name)
            assertEquals("Hello", detail.session.clientMessage)
            assertEquals("skype_zoom", detail.session.meetingType)
            assertEquals(FEE_SOURCE_EXPERT_DEFAULT, detail.economics.earningsBreakdown?.feeSource)
            assertEquals("USD", detail.economics.currency)
        }

    @Test
    fun acceptWithoutMessageSendsEmptyObject() =
        runTest {
            enqueue(
                200,
                """{"status":"success","message":"ok","data":{"booking_id":"b9","status":"confirmed",""" +
                    """"confirmed_at":"2026-11-01T09:00:00Z","meeting_link":null}}""",
            )
            enqueue(
                200,
                """{"status":"success","data":{"booking_id":"b9","status":"confirmed",""" +
                    """"confirmed_at":"2026-11-01T09:00:00Z","meeting_link":null}}""",
            )

            val result = repository.accept("r1", null)
            val withoutMessage = server.takeRequest()
            repository.accept("r1", "See you")
            val withMessage = server.takeRequest()

            assertEquals("POST", withoutMessage.method)
            assertEquals("/api/v1/expert/booking-requests/r1/accept", withoutMessage.url.encodedPath)
            assertEquals("{}", withoutMessage.body?.utf8())
            assertEquals("""{"message":"See you"}""", withMessage.body?.utf8())
            assertEquals("b9", result.bookingId)
            assertEquals("confirmed", result.status)
            assertNotNull(result.confirmedAt)
        }

    @Test
    fun declineAlwaysSendsBothKeys() =
        runTest {
            enqueue(
                200,
                """{"status":"success","data":{"request_id":"r1","status":"declined",""" +
                    """"declined_at":"2026-11-01T09:00:00Z"}}""",
            )

            val result = repository.decline("r1", "OUT_OF_SCOPE", "Outside my expertise")

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/v1/expert/booking-requests/r1/decline", request.url.encodedPath)
            assertEquals(
                """{"reason_code":"OUT_OF_SCOPE","reason_text":"Outside my expertise"}""",
                request.body?.utf8(),
            )
            assertEquals("declined", result.status)
            assertEquals("r1", result.requestId)
        }

    @Test
    fun alreadyProcessedMapsToTypedError() =
        runTest {
            val message = "This request has already been handled."
            enqueue(409, """{"status":"error","code":"ALREADY_PROCESSED","message":"$message"}""")

            try {
                repository.accept("r1", null)
                fail("Expected ApiException.Http")
            } catch (e: ApiException.Http) {
                assertEquals(409, e.status)
                assertEquals(ErrorCode.AlreadyProcessed, e.code)
                assertEquals(message, e.serverMessage)
                assertTrue(e.message.orEmpty().isNotEmpty())
            }
        }
}
