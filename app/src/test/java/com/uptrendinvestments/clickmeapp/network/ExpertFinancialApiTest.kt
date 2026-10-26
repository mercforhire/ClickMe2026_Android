package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.ExpertFinancialRepository
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/** Wire-level tests (MockWebServer + the real Moshi) for the Phase 11 earnings, payout and Connect endpoints. */
class ExpertFinancialApiTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: ExpertFinancialRepository

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
        repository = ExpertFinancialRepository(api)
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
    fun weeklyDecodesMinorUnitLongs() =
        runTest {
            enqueue(
                200,
                """{"status":"success","data":{"week_start":"2026-10-19","week_end":"2026-10-25",""" +
                    """"gross_amount":15000,"net_amount":12750,"currency":"CAD","session_count":3}}""",
            )
            enqueue(
                200,
                """{"status":"success","data":{"week_start":"2026-10-26","week_end":"2026-11-01",""" +
                    """"gross_amount":0,"net_amount":0,"currency":"CAD","session_count":0}}""",
            )

            val week = repository.weeklyEarnings()
            val request = server.takeRequest()
            val empty = repository.weeklyEarnings()

            assertEquals("GET", request.method)
            assertEquals("/api/v1/expert/earnings/weekly", request.url.encodedPath)
            assertEquals(12_750L, week.netAmount)
            assertEquals(15_000L, week.grossAmount)
            assertEquals(3, week.sessionCount)
            assertEquals("CAD", week.currency)
            assertEquals(0L, empty.netAmount)
            assertEquals("CAD", empty.currency)
            assertEquals(0, empty.sessionCount)
        }

    @Test
    fun weeklyMissingNetFailsDecoding() =
        runTest {
            enqueue(
                200,
                """{"status":"success","data":{"week_start":"2026-10-19","week_end":"2026-10-25",""" +
                    """"gross_amount":15000,"currency":"CAD","session_count":3}}""",
            )

            try {
                repository.weeklyEarnings()
                fail("Expected ApiException.Decoding")
            } catch (e: ApiException.Decoding) {
                assertEquals("Decoding error", e.message)
            }
        }

    @Test
    fun connectStatusDecodes() =
        runTest {
            enqueue(
                200,
                """{"status":"success","data":{"charges_enabled":false,"currently_due":["external_account"]}}""",
            )
            enqueue(404, """{"status":"error","code":"NOT_FOUND","message":"No connected account."}""")

            val status = repository.connectStatus()
            val request = server.takeRequest()

            assertEquals("GET", request.method)
            assertEquals("/api/v1/expert/connect/status", request.url.encodedPath)
            assertFalse(status.chargesEnabled)
            assertEquals(listOf("external_account"), status.currentlyDue)
            try {
                repository.connectStatus()
                fail("Expected ApiException.Http")
            } catch (e: ApiException.Http) {
                assertEquals(404, e.status)
            }
        }

    @Test
    fun onboardAndRefreshAreBodylessPosts() =
        runTest {
            enqueue(200, """{"status":"success","data":{"url":"https://connect.stripe.com/setup/e/abc"}}""")
            enqueue(200, """{"status":"success","data":{"url":"https://connect.stripe.com/setup/e/def"}}""")

            val onboard = repository.onboardingLink(refresh = false)
            val onboardRequest = server.takeRequest()
            val refresh = repository.onboardingLink(refresh = true)
            val refreshRequest = server.takeRequest()

            assertEquals("POST", onboardRequest.method)
            assertEquals("/api/v1/expert/connect/onboard", onboardRequest.url.encodedPath)
            assertEquals(0L, onboardRequest.bodySize)
            assertEquals("https://connect.stripe.com/setup/e/abc", onboard)
            assertEquals("POST", refreshRequest.method)
            assertEquals("/api/v1/expert/connect/refresh-link", refreshRequest.url.encodedPath)
            assertEquals(0L, refreshRequest.bodySize)
            assertEquals("https://connect.stripe.com/setup/e/def", refresh)
        }

    @Test
    fun payoutSummaryDecodesAndMaps400() =
        runTest {
            enqueue(
                200,
                """{"status":"success","data":{"available_amount":5000,"pending_amount":0,"currency":"CAD",""" +
                    """"next_payout_date":null,"payout_method":"bank_transfer"}}""",
            )
            enqueue(400, """{"status":"error","code":"NO_PAYOUT_METHOD","message":"Add a payout method."}""")

            val summary = repository.payoutSummary()
            val request = server.takeRequest()

            assertEquals("GET", request.method)
            assertEquals("/api/v1/expert/payouts/summary", request.url.encodedPath)
            assertEquals(5_000L, summary.availableAmount)
            assertEquals(0L, summary.pendingAmount)
            assertNull(summary.nextPayoutDate)
            try {
                repository.payoutSummary()
                fail("Expected ApiException.Http")
            } catch (e: ApiException.Http) {
                assertEquals(400, e.status)
                assertEquals(ErrorCode.NoPayoutMethod, e.code)
            }
        }
}
