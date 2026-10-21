package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.model.requests.ForgotPasswordRequest
import com.uptrendinvestments.clickmeapp.model.requests.LoginRequest
import com.uptrendinvestments.clickmeapp.model.requests.ResetPasswordRequest
import com.uptrendinvestments.clickmeapp.model.requests.SetupExpertProfileRequest
import com.uptrendinvestments.clickmeapp.model.requests.SetupLocation
import com.uptrendinvestments.clickmeapp.model.requests.SignupRequest
import com.uptrendinvestments.clickmeapp.model.requests.VerifyEmailCodeRequest
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
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

    @Test
    fun signupPostsExactBodyAndDecodesTokens() =
        runTest {
            enqueue(
                200,
                """
                {"status":"success","data":{"token":"jwt","refresh_token":"rt","expires_at":1792513029,
                 "expires_in":900,"email_verified":false,
                 "user":{"id":"u1","first_name":"","last_name":"","roles":["expert"]}}}
                """.trimIndent(),
            )

            val data = api.signup(SignupRequest("a@b.co", "abcdefg1")).data

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/v1/auth/signup", request.url.encodedPath)
            assertEquals("""{"email":"a@b.co","password":"abcdefg1","role":"expert"}""", request.body?.utf8())
            assertEquals("jwt", data.token)
            assertEquals("rt", data.refreshToken)
            assertEquals(false, data.emailVerified)
        }

    @Test
    fun setupExpertProfileSendsOnlyTheKeysThatWereSet() =
        runTest {
            val body = """{"status":"success","data":{"profile_id":"p1","setup_completed":false}}"""
            repeat(3) { enqueue(200, body) }

            api.setupExpertProfile(SetupExpertProfileRequest(firstName = "Ada"))
            val first = server.takeRequest()
            assertEquals("PATCH", first.method)
            assertEquals("/api/v1/expert/profile/setup", first.url.encodedPath)
            assertEquals("""{"first_name":"Ada"}""", first.body?.utf8())

            api.setupExpertProfile(SetupExpertProfileRequest(location = SetupLocation(city = "Toronto")))
            assertEquals("""{"location":{"city":"Toronto"}}""", server.takeRequest().body?.utf8())

            api.setupExpertProfile(SetupExpertProfileRequest(expertiseTags = emptyList()))
            assertEquals("""{"expertise_tags":[]}""", server.takeRequest().body?.utf8())
        }

    @Test
    fun setupExpertProfileFullRequestKeepsListOrder() =
        runTest {
            enqueue(200, """{"status":"success","data":{"setup_completed":true}}""")

            api.setupExpertProfile(
                SetupExpertProfileRequest(
                    firstName = "Ada",
                    location = SetupLocation(city = "Toronto", provinceState = "ON", countryCode = "CAN"),
                    timezone = "America/Toronto",
                    languages = listOf("fr", "en"),
                    expertiseTags = listOf("t2", "t1"),
                    setupCompleted = true,
                ),
            )

            assertEquals(
                """{"first_name":"Ada","location":{"city":"Toronto","province_state":"ON","country_code":"CAN"},""" +
                    """"timezone":"America/Toronto","languages":["fr","en"],"expertise_tags":["t2","t1"],""" +
                    """"setup_completed":true}""",
                server.takeRequest().body?.utf8(),
            )
        }

    @Test
    fun uploadAvatarSendsMultipartImagePartAndDecodesUrl() =
        runTest {
            enqueue(200, """{"status":"success","data":{"avatar_url":"https://x/a.jpg"}}""")
            val part =
                MultipartBody.Part.createFormData(
                    "image",
                    "avatar.jpg",
                    byteArrayOf(1, 2, 3).toRequestBody("image/jpeg".toMediaType()),
                )

            val data = api.uploadAvatar(part).data

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/v1/user/profile/avatar", request.url.encodedPath)
            assertTrue(request.headers["Content-Type"].orEmpty().startsWith("multipart/form-data"))
            val text = request.body?.utf8().orEmpty()
            assertTrue(text, text.contains("""name="image""""))
            assertTrue(text, text.contains("""filename="avatar.jpg""""))
            assertTrue(text, text.contains("Content-Type: image/jpeg"))
            assertEquals("https://x/a.jpg", data.avatarUrl)
        }

    @Test
    fun deleteAvatarIssuesDelete() =
        runTest {
            enqueue(200, """{"status":"success","message":"Avatar removed"}""")

            api.deleteAvatar()

            val request = server.takeRequest()
            assertEquals("DELETE", request.method)
            assertEquals("/api/v1/user/profile/avatar", request.url.encodedPath)
        }

    @Test
    fun emailEndpointsUseTheirVerbsAndBodies() =
        runTest {
            enqueue(200, """{"status":"success","message":"sent"}""")
            enqueue(200, """{"status":"success","message":"verified"}""")
            enqueue(200, """{"status":"success","data":{"verified":true}}""")

            api.resendVerificationEmail()
            val resend = server.takeRequest()
            assertEquals("POST", resend.method)
            assertEquals("/api/v1/auth/email/resend", resend.url.encodedPath)

            api.verifyEmailCode(VerifyEmailCodeRequest("123456"))
            val verify = server.takeRequest()
            assertEquals("POST", verify.method)
            assertEquals("/api/v1/auth/email/verify", verify.url.encodedPath)
            assertEquals("""{"code":"123456"}""", verify.body?.utf8())

            val status = api.getEmailStatus().data
            val statusRequest = server.takeRequest()
            assertEquals("GET", statusRequest.method)
            assertEquals("/api/v1/auth/email/status", statusRequest.url.encodedPath)
            assertTrue(status.verified)
        }

    @Test
    fun metaEndpointsAreGets() =
        runTest {
            enqueue(200, """{"status":"success","data":{"languages":[{"id":"en","label":"English"}]}}""")
            enqueue(200, """{"status":"success","data":{"tags":[{"id":"u1","label":"Tax"}]}}""")

            assertEquals(
                "en",
                api
                    .getLanguages()
                    .data.languages
                    .single()
                    .id,
            )
            val languages = server.takeRequest()
            assertEquals("GET", languages.method)
            assertEquals("/api/v1/meta/languages", languages.url.encodedPath)

            assertEquals(
                "u1",
                api
                    .getExpertiseTags()
                    .data.tags
                    .single()
                    .id,
            )
            val tags = server.takeRequest()
            assertEquals("GET", tags.method)
            assertEquals("/api/v1/meta/expertise-tags", tags.url.encodedPath)
        }
}
