package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.model.requests.AccountDeletionRequest
import com.uptrendinvestments.clickmeapp.model.requests.DeleteAccountRequest
import com.uptrendinvestments.clickmeapp.model.responses.DeletionRequestData
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/** Wire-level tests (MockWebServer + the real Moshi) for the Phase 14 account deletion endpoints. */
class AccountApiTest {
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
    fun deleteRequestPostsPasswordAndDecodesToken() =
        runTest {
            enqueue(200, """{"status":"success","data":{"deletion_token":"tok-1"}}""")

            val data = apiCall { api.requestAccountDeletion(AccountDeletionRequest("pw1")) }.data
            val request = server.takeRequest()

            assertEquals(DeletionRequestData("tok-1"), data)
            assertEquals("POST", request.method)
            assertEquals("/api/v1/user/account/delete-request", request.url.encodedPath)
            assertEquals("""{"password":"pw1"}""", request.body?.utf8())
        }

    @Test
    fun deleteAccountIsDeleteWithJsonBody() =
        runTest {
            val ok = """{"status":"success","message":"Account deleted","data":{"status":"account_deleted"}}"""
            enqueue(200, ok)
            enqueue(200, ok)

            val response = apiCall { api.deleteAccount(DeleteAccountRequest("tok-1", "privacy_concerns")) }
            val plain = server.takeRequest()
            apiCall { api.deleteAccount(DeleteAccountRequest("tok-1", "privacy_concerns", "too slow")) }
            val withFeedback = server.takeRequest()

            assertEquals("DELETE", plain.method)
            assertEquals("/api/v1/user/account", plain.url.encodedPath)
            assertEquals(
                """{"deletion_token":"tok-1","reason_category":"privacy_concerns","confirm_permanent_action":true}""",
                plain.body?.utf8(),
            )
            assertEquals("DELETE", withFeedback.method)
            assertEquals(
                """{"deletion_token":"tok-1","reason_category":"privacy_concerns",""" +
                    """"feedback_details":"too slow","confirm_permanent_action":true}""",
                withFeedback.body?.utf8(),
            )
            assertEquals("Account deleted", response.message)
        }

    @Test
    fun deleteErrorsReachApiException() =
        runTest {
            enqueue(401, """{"status":"error","message":"Invalid password."}""")
            enqueue(
                409,
                """{"status":"error","code":"EXPERT_HAS_UPCOMING_BOOKINGS","message":"Finish your sessions first."}""",
            )
            enqueue(400, """{"status":"error","code":"INVALID_TOKEN","message":"Token expired."}""")
            enqueue(429, """{"status":"error","code":"TOO_MANY_REQUESTS","message":"Slow down."}""")

            val wrongPassword = failure { api.requestAccountDeletion(AccountDeletionRequest("bad")) }
            val upcoming = failure { api.requestAccountDeletion(AccountDeletionRequest("pw1")) }
            val badToken = failure { api.deleteAccount(DeleteAccountRequest("old", "other")) }
            val limited = failure { api.deleteAccount(DeleteAccountRequest("tok-1", "other")) }

            assertEquals(401, wrongPassword.status)
            assertNull(wrongPassword.rawCode)
            assertEquals("Invalid password.", wrongPassword.serverMessage)
            assertEquals(409, upcoming.status)
            assertEquals(ErrorCode.ExpertHasUpcomingBookings, upcoming.code)
            assertEquals("Finish your sessions first.", upcoming.serverMessage)
            assertEquals(400, badToken.status)
            assertEquals(ErrorCode.InvalidToken, badToken.code)
            assertEquals(429, limited.status)
        }

    @Test
    fun secretsNeverPrintFromToString() {
        val texts =
            listOf(
                AccountDeletionRequest("pw1").toString(),
                DeleteAccountRequest("tok-1", "other", "x").toString(),
                DeletionRequestData("tok-1").toString(),
            )
        for (text in texts) {
            assertFalse(text, text.contains("pw1"))
            assertFalse(text, text.contains("tok-1"))
        }
        assertTrue(texts[1].contains("other"))
    }

    private suspend fun failure(block: suspend () -> Any): ApiException.Http {
        try {
            apiCall { block() }
        } catch (e: ApiException.Http) {
            return e
        }
        fail("expected ApiException.Http")
        throw AssertionError()
    }
}
