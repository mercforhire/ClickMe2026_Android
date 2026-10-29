package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.model.requests.SaveTaxProfileRequest
import com.uptrendinvestments.clickmeapp.model.requests.TaxAddressBody
import com.uptrendinvestments.clickmeapp.model.responses.TaxProfileAddress
import com.uptrendinvestments.clickmeapp.model.responses.TaxProfileData
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

/** Wire-level tests (MockWebServer + the real Moshi) for the Phase 14 ledger, payout method and tax profile. */
class PayoutsTaxApiTest {
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

    private val page =
        """{"total_count":2,"total_pages":1,"current_page":1,"has_more":false}"""

    private val address =
        TaxAddressBody(
            line1 = "1 Main",
            city = "Toronto",
            region = "ON",
            postalCode = "M5V",
            country = "CA",
        )

    @Test
    fun transactionsSendPageAndLimit() =
        runTest {
            enqueue(200, """{"status":"success","data":{"transactions":[],"pagination":$page}}""")

            apiCall { api.getEarningsTransactions(2, 20) }
            val request = server.takeRequest()

            assertEquals("GET", request.method)
            assertEquals("/api/v1/expert/earnings/transactions", request.url.encodedPath)
            assertEquals("2", request.url.queryParameter("page"))
            assertEquals("20", request.url.queryParameter("limit"))
        }

    @Test
    fun transactionsDecodeMinorUnitsAndNulls() =
        runTest {
            enqueue(
                200,
                """{"status":"success","data":{"transactions":[""" +
                    """{"id":"e1","booking_id":"b1","session_date":"2026-07-30T14:00:00.000Z",""" +
                    """"topic_title":"Career Advice","client_name":"Liam Carter","gross_amount":7500,""" +
                    """"commission_amount":1125,"net_amount":6375,"currency":"USD",""" +
                    """"payout_status":"settled_direct",""" +
                    """"processed_at":"2026-08-01T12:00:00.000Z","created_at":"2026-07-30T10:00:00.000Z"},""" +
                    """{"id":"e2","booking_id":null,"session_date":null,"topic_title":null,"client_name":null,""" +
                    """"gross_amount":100,"commission_amount":15,"net_amount":85,"currency":"USD",""" +
                    """"payout_status":"mystery","processed_at":null,"created_at":"2026-07-31T10:00:00.000Z"},""" +
                    """{"id":"e3","gross_amount":1,"commission_amount":0,"net_amount":1,"currency":"USD",""" +
                    """"payout_status":null,"created_at":"2026-07-31T10:00:00.000Z"}],"pagination":$page}}""",
            )

            val data = apiCall { api.getEarningsTransactions(1, 20) }.data
            val first = data.transactions[0]
            val second = data.transactions[1]

            assertEquals(6375L, first.netAmount)
            assertEquals(7500L, first.grossAmount)
            assertEquals(1125L, first.commissionAmount)
            assertEquals("USD", first.currency)
            assertEquals("settled_direct", first.payoutStatus)
            assertEquals("Liam Carter", first.clientName)
            assertNull(second.bookingId)
            assertNull(second.sessionDate)
            assertNull(second.topicTitle)
            assertNull(second.clientName)
            assertNull(second.processedAt)
            assertEquals("mystery", second.payoutStatus)
            assertNull(data.transactions[2].payoutStatus)
            assertEquals(false, data.pagination?.hasMoreOrDerived)
        }

    @Test
    fun summaryDecodesOptionalPayoutMethod() =
        runTest {
            enqueue(
                200,
                """{"status":"success","data":{"available_amount":-500,"pending_amount":0,"currency":"CAD",""" +
                    """"payout_method":"bank_transfer"}}""",
            )
            enqueue(
                200,
                """{"status":"success","data":{"available_amount":900,"pending_amount":0,"currency":"CAD"}}""",
            )

            val withMethod = apiCall { api.getPayoutSummary() }.data
            val without = apiCall { api.getPayoutSummary() }.data

            assertEquals("bank_transfer", withMethod.payoutMethod)
            assertEquals(-500L, withMethod.availableAmount)
            assertNull(without.payoutMethod)
        }

    @Test
    fun taxProfileDecodesDefaultsAndRegionOrProvince() =
        runTest {
            enqueue(
                200,
                """{"status":"success","data":{"tin_on_file":false,"legal_name":null,"address":null,""" +
                    """"dob":null,"financial_account_identifier":null}}""",
            )
            enqueue(
                200,
                """{"status":"success","data":{"tin_on_file":true,"legal_name":"Ana Lee","address":""" +
                    """{"line1":"1 Main","city":"Toronto","province":"ON","postal_code":"M5V","country":"CA"},""" +
                    """"dob":"1990-01-02","financial_account_identifier":"acct"}}""",
            )

            val empty = apiCall { api.getTaxProfile() }.data
            val request = server.takeRequest()
            val filled = apiCall { api.getTaxProfile() }.data

            assertEquals("/api/v1/expert/tax-profile", request.url.encodedPath)
            assertFalse(empty.tinOnFile)
            assertNull(empty.address)
            assertTrue(filled.tinOnFile)
            assertEquals("ON", filled.address?.regionOrProvince)
            assertEquals("M5V", filled.address?.postalCode)
        }

    @Test
    fun saveTaxProfileStrictBody() =
        runTest {
            enqueue(200, """{"status":"success","data":{"saved":false,"tin_required":true,"message":"TIN required"}}""")
            enqueue(200, """{"status":"success","data":{"saved":true,"tin_required":false}}""")

            val noTin = apiCall { api.saveTaxProfile(SaveTaxProfileRequest("Ana Lee", address, "1990-01-02")) }.data
            val noTinRequest = server.takeRequest()
            val withTin =
                apiCall {
                    api.saveTaxProfile(SaveTaxProfileRequest("Ana Lee", address, "1990-01-02", tin = "123456789"))
                }.data
            val withTinRequest = server.takeRequest()

            assertEquals("POST", noTinRequest.method)
            assertEquals("/api/v1/expert/tax-profile", noTinRequest.url.encodedPath)
            assertEquals(
                """{"legal_name":"Ana Lee","address":{"line1":"1 Main","city":"Toronto","region":"ON",""" +
                    """"postal_code":"M5V","country":"CA"},"dob":"1990-01-02"}""",
                noTinRequest.body?.utf8(),
            )
            assertEquals(
                """{"legal_name":"Ana Lee","address":{"line1":"1 Main","city":"Toronto","region":"ON",""" +
                    """"postal_code":"M5V","country":"CA"},"dob":"1990-01-02","tin":"123456789"}""",
                withTinRequest.body?.utf8(),
            )
            assertFalse(noTin.saved)
            assertTrue(noTin.tinRequired)
            assertEquals("TIN required", noTin.message)
            assertTrue(withTin.saved)
        }

    @Test
    fun taxTypesRedactPii() {
        val request =
            SaveTaxProfileRequest(
                legalName = "Ana Lee",
                address = address,
                dob = "1990-01-02",
                tin = "123456789",
                financialAccountIdentifier = "acct99",
            )
        val data = TaxProfileData(true, "Ana Lee", TaxProfileAddress(line1 = "1 Main"), "1990-01-02", "acct99")
        val texts = listOf(request.toString(), address.toString(), data.toString(), data.address.toString())
        for (text in texts) {
            for (secret in listOf("123456789", "1990-01-02", "Ana", "acct99", "1 Main")) {
                assertFalse("$text leaks $secret", text.contains(secret))
            }
        }
    }
}
