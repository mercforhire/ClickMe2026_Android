package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.model.requests.ExpertRescheduleProposalRequest
import com.uptrendinvestments.clickmeapp.model.requests.RescheduleProposalRequest
import com.uptrendinvestments.clickmeapp.model.responses.RescheduleResponseData
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.ClientBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Wire-level tests (MockWebServer + the real Moshi) for the Phase 16.2 reschedule-propose PATCH calls. The backend
 * `rescheduleSchema` is `.strict()`, so the exact JSON body is pinned here.
 */
class RescheduleProposeApiTest {
    private lateinit var server: MockWebServer
    private lateinit var clientRepository: ClientBookingsRepository
    private lateinit var expertRepository: ExpertBookingsRepository

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
        clientRepository = ClientBookingsRepository(api)
        expertRepository = ExpertBookingsRepository(api)
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

    private val okBody =
        """{"status":"success","message":"Reschedule requested",""" +
            """"data":{"booking_id":"b1","status":"pending_reschedule"}}"""

    @Test
    fun clientProposeSendsPatchWithStartTimezoneAndNote() =
        runTest {
            enqueue(200, okBody)

            clientRepository.reschedule(
                "b1",
                RescheduleProposalRequest("2026-11-02T15:00:00Z", "America/Toronto", "Running late"),
            )

            val request = server.takeRequest()
            assertEquals("PATCH", request.method)
            assertEquals("/api/v1/client/bookings/b1/reschedule", request.url.encodedPath)
            assertEquals(
                """{"proposed_start":"2026-11-02T15:00:00Z","timezone":"America/Toronto","note":"Running late"}""",
                request.body?.utf8(),
            )
        }

    @Test
    fun clientProposeOmitsNullNoteAndCard() =
        runTest {
            enqueue(200, okBody)

            clientRepository.reschedule(
                "b1",
                RescheduleProposalRequest("2026-11-02T15:00:00Z", "America/Toronto", null),
            )

            assertEquals(
                """{"proposed_start":"2026-11-02T15:00:00Z","timezone":"America/Toronto"}""",
                server.takeRequest().body?.utf8(),
            )
        }

    @Test
    fun clientRetrySendsCardId() =
        runTest {
            enqueue(200, okBody)

            clientRepository.reschedule(
                "b1",
                RescheduleProposalRequest("2026-11-02T15:00:00Z", "America/Toronto", null, "pm_123"),
            )

            val body =
                server
                    .takeRequest()
                    .body
                    ?.utf8()
                    .orEmpty()
            assertTrue(body, body.contains("\"stripe_payment_method_id\":\"pm_123\""))
        }

    @Test
    fun expertProposeSendsPatchWithoutCardKey() =
        runTest {
            enqueue(200, okBody)

            expertRepository.reschedule(
                "b2",
                ExpertRescheduleProposalRequest("2026-11-02T15:00:00Z", "UTC", null),
            )

            val request = server.takeRequest()
            assertEquals("PATCH", request.method)
            assertEquals("/api/v1/expert/bookings/b2/reschedule", request.url.encodedPath)
            assertEquals(
                """{"proposed_start":"2026-11-02T15:00:00Z","timezone":"UTC"}""",
                request.body?.utf8(),
            )
        }

    @Test
    fun responseDecodes() =
        runTest {
            enqueue(200, okBody)

            val data =
                clientRepository.reschedule(
                    "b1",
                    RescheduleProposalRequest("2026-11-02T15:00:00Z", "UTC"),
                )

            assertEquals(RescheduleResponseData(bookingId = "b1", status = "pending_reschedule"), data)
        }

    @Test
    fun httpErrorCarriesCode() =
        runTest {
            val message = "Paid sessions can be rescheduled up to 6 days ahead."
            enqueue(
                422,
                """{"status":"error","code":"RESCHEDULE_TOO_FAR_AHEAD","message":"$message"}""",
            )

            try {
                clientRepository.reschedule(
                    "b1",
                    RescheduleProposalRequest("2026-11-02T15:00:00Z", "UTC"),
                )
                fail("expected ApiException.Http")
            } catch (e: ApiException.Http) {
                assertEquals(422, e.status)
                assertEquals(ErrorCode.RescheduleTooFarAhead, e.code)
                assertEquals(message, e.serverMessage)
            }
        }
}
