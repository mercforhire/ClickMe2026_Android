package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.model.requests.ForgotPasswordRequest
import com.uptrendinvestments.clickmeapp.model.requests.LoginRequest
import com.uptrendinvestments.clickmeapp.model.requests.ResetPasswordRequest
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class ClickMeApiTest {
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
    fun loginPostsJsonBodyAndDecodesLoginData() =
        runTest {
            enqueue(
                200,
                """
                {"status":"success","data":{"token":"jwt","refresh_token":"rt","expires_at":1792513029,
                 "expires_in":900,"user":{"id":"u1","first_name":"Ada","last_name":"L","roles":["client"]}}}
                """.trimIndent(),
            )

            val response = api.login(LoginRequest("a@b.co", "pw12345678"))

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/v1/auth/login", request.url.encodedPath)
            assertTrue(request.headers["Content-Type"].orEmpty().startsWith("application/json"))
            assertEquals("""{"email":"a@b.co","password":"pw12345678"}""", request.body?.utf8())
            assertEquals("success", response.status)
            assertEquals("jwt", response.data.token)
            assertEquals("rt", response.data.refreshToken)
            assertEquals(1_792_513_029L, response.data.expiresAt)
            assertEquals("Ada", response.data.user.firstName)
        }

    @Test
    fun forgotPasswordPostsIdentityAndDecodesMessage() =
        runTest {
            enqueue(200, """{"status":"success","message":"If an account exists, a reset code has been sent."}""")

            val response = api.forgotPassword(ForgotPasswordRequest("a@b.co"))

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/v1/auth/password/forgot", request.url.encodedPath)
            assertEquals("""{"identity":"a@b.co"}""", request.body?.utf8())
            assertEquals("If an account exists, a reset code has been sent.", response.message)
        }

    @Test
    fun resetPasswordPostsAllFourFieldsAndIgnoresRedirect() =
        runTest {
            enqueue(
                200,
                """{"status":"success","message":"Password reset.","data":{"redirect_to":"/login"}}""",
            )

            val response = api.resetPassword(ResetPasswordRequest("a@b.co", "123456", "Passw0rd!", "Passw0rd!"))

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/v1/auth/password/reset", request.url.encodedPath)
            assertEquals(
                """{"email":"a@b.co","code":"123456","password":"Passw0rd!","password_confirmation":"Passw0rd!"}""",
                request.body?.utf8(),
            )
            assertEquals("success", response.status)
            assertEquals("Password reset.", response.message)
        }

    @Test
    fun resetPasswordErrorsMapToHttpWithFieldErrors() =
        runTest {
            val request = ResetPasswordRequest("a@b.co", "123456", "weak", "weak")
            enqueue(400, """{"status":"error","code":"EXPIRED_TOKEN","message":"Invalid or expired code"}""")
            enqueue(422, """{"status":"error","errors":[{"field":"password","message":"Too weak"}]}""")

            val expired = runCatching { apiCall { api.resetPassword(request) } }.exceptionOrNull()
            val weak = runCatching { apiCall { api.resetPassword(request) } }.exceptionOrNull()

            assertTrue(expired is ApiException.Http)
            assertEquals(400, (expired as ApiException.Http).status)
            assertTrue(weak is ApiException.Http)
            val http = weak as ApiException.Http
            assertEquals(422, http.status)
            assertEquals(listOf("password"), http.fieldErrors.map { it.field })
        }

    @Test
    fun getMeIssuesGetAndDecodesIdentityAndPricingLimits() =
        runTest {
            enqueue(
                200,
                """
                {"status":"success","data":{"id":"u1","email":"a@b.co","roles":["client","expert"],
                 "pricing_limits":{"min_session_amount":{"by_currency":{"USD":50},
                 "default_minor_units":50,"default_currency":"CAD"}}}}
                """.trimIndent(),
            )

            val me = api.getMe().data

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/me", request.url.encodedPath)
            assertEquals("u1", me.id)
            assertEquals("a@b.co", me.email)
            assertEquals(listOf("client", "expert"), me.roles)
            assertEquals(50, me.pricingLimits!!.minSessionAmount.minMinorUnits("usd"))
        }

    @Test
    fun getUserProfileDecodesNestedProfile() =
        runTest {
            enqueue(
                200,
                """
                {"status":"success","data":{
                 "personal_details":{"first_name":"Ada","last_name":"L","email":"a@b.co","bio":"hi"},
                 "professional_details":{"job_title":"Eng","company":"X","education":"MIT"},
                 "location":{"city":"Toronto","state_province":"ON","country":"CAN","timezone":"America/Toronto"},
                 "languages":[{"id":"en","label":"English"}]}}
                """.trimIndent(),
            )

            val profile = api.getUserProfile().data

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/user/profile", request.url.encodedPath)
            assertEquals("Ada", profile.personalDetails.firstName)
            assertEquals("Eng", profile.professionalDetails.jobTitle)
            assertEquals("CAN", profile.location.country)
            assertEquals("English", profile.languages.single().label)
        }

    @Test
    fun getExpertProfileDecodesSetupCompleted() =
        runTest {
            enqueue(200, """{"status":"success","data":{"setup_completed":true}}""")

            val data = api.getExpertProfile().data

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/expert/profile", request.url.encodedPath)
            assertTrue(data.setupCompleted)
        }

    @Test
    fun getMyTopicsDecodesTwoItemList() =
        runTest {
            enqueue(
                200,
                """{"status":"success","data":{"topics":[{"id":"t1","title":"A"},{"id":"t2","title":"B"}]}}""",
            )

            val topics = api.getMyTopics().data.topics

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/expert/topics", request.url.encodedPath)
            assertEquals(2, topics.size)
            assertEquals("t2", topics[1]["id"])
        }

    @Test
    fun unprocessableResponseSurfacesAsHttpExceptionParsedToHttp422() =
        runTest {
            enqueue(
                422,
                """{"status":"error","errors":[{"field":"email","message":"Email is invalid"}]}""",
            )

            try {
                api.login(LoginRequest("bad", "pw12345678"))
                fail("Expected HttpException")
            } catch (e: HttpException) {
                assertEquals(422, e.code())
                val parsed = ApiErrorParser.from(e)
                assertNotNull(parsed)
                val http = parsed as ApiException.Http
                assertEquals(422, http.status)
                assertEquals("email", http.fieldErrors.single().field)
                assertEquals("Email is invalid", http.userMessage)
            }
        }
}
