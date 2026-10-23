package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.model.requests.BookingRequestBody
import com.uptrendinvestments.clickmeapp.model.requests.ConfirmBookingRequest
import com.uptrendinvestments.clickmeapp.model.requests.ForgotPasswordRequest
import com.uptrendinvestments.clickmeapp.model.requests.LoginRequest
import com.uptrendinvestments.clickmeapp.model.requests.PaymentIntentRequest
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    @Test
    fun getClientHomeUnfilteredSendsNoQuery() =
        runTest {
            enqueue(200, """{"status":"success","data":{"trending_categories":[{"id":"ai","name":"AI"}]}}""")

            val data = api.getClientHome(null).data

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/client/home", request.url.encodedPath)
            assertEquals(0, request.url.querySize)
            assertEquals("ai", data.trendingCategories.single().id)
        }

    @Test
    fun getClientHomeFilteredSendsOneCommaJoinedCategoryAndNoPaging() =
        runTest {
            enqueue(200, """{"status":"success","data":{}}""")

            api.getClientHome("ai,design")

            val request = server.takeRequest()
            assertEquals("/api/v1/client/home", request.url.encodedPath)
            assertEquals("ai,design", request.url.queryParameter("category"))
            assertEquals(1, request.url.querySize)
            assertNull(request.url.queryParameter("page"))
            assertNull(request.url.queryParameter("limit"))
        }

    @Test
    fun getClientBookingsSendsTypeAndPaging() =
        runTest {
            enqueue(
                200,
                """
                {"status":"success","data":{"bookings":[{"booking_id":"b1","expert":{"id":"e1"},
                 "start_time":"2026-10-21T19:00:00.000000Z","end_time":"2026-10-21T20:00:00.000000Z",
                 "status":"confirmed"}],"pagination":{"current_page":1,"total_pages":1}}}
                """.trimIndent(),
            )

            val data = api.getClientBookings("upcoming", 1, 20).data

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/client/bookings", request.url.encodedPath)
            assertEquals("upcoming", request.url.queryParameter("type"))
            assertEquals("1", request.url.queryParameter("page"))
            assertEquals("20", request.url.queryParameter("limit"))
            assertEquals("b1", data.bookings.single().bookingId)
        }

    @Test
    fun getCategoriesIsAGetAndDecodesList() =
        runTest {
            enqueue(200, """{"status":"success","data":{"categories":[{"id":"ai","name":"AI"}]}}""")

            val data = api.getCategories().data

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/categories", request.url.encodedPath)
            assertEquals("AI", data.categories.single().name)
        }

    @Test
    fun expertDetailsAndReviewsUseIdPathAndPaging() =
        runTest {
            enqueue(200, """{"status":"success","data":{"profile":{"name":"Ada"},"topics_of_discussion":[]}}""")
            enqueue(200, """{"status":"success","data":{"summary":{"total_reviews":0},"reviews":[]}}""")

            val details = api.getExpertDetails("e1").data
            val detailsRequest = server.takeRequest()
            assertEquals("GET", detailsRequest.method)
            assertEquals("/api/v1/experts/e1/details", detailsRequest.url.encodedPath)
            assertEquals("Ada", details.profile.name)

            api.getExpertReviews("e1", 1, 3)
            val reviewsRequest = server.takeRequest()
            assertEquals("GET", reviewsRequest.method)
            assertEquals("/api/v1/experts/e1/reviews", reviewsRequest.url.encodedPath)
            assertEquals("1", reviewsRequest.url.queryParameter("page"))
            assertEquals("3", reviewsRequest.url.queryParameter("limit"))
        }

    @Test
    fun expertDetails404SurfacesAsApiExceptionHttp() =
        runTest {
            enqueue(404, """{"status":"error","code":"NOT_FOUND","message":"Expert not found"}""")

            val error = runCatching { apiCall { api.getExpertDetails("missing") } }.exceptionOrNull()

            assertTrue(error is ApiException.Http)
            assertEquals(404, (error as ApiException.Http).status)
        }

    private fun sampleBookingBody(clientNotes: String?) =
        BookingRequestBody(
            expertId = "e1",
            topicId = "t1",
            scheduledStart = "2026-10-24T14:00:00Z",
            durationMinutes = 30,
            meetingType = "in_app_voice",
            timezone = "America/Toronto",
            clientNotes = clientNotes,
        )

    @Test
    fun getExpertAvailabilitySendsOnlyMonthWhenTopicIdIsNull() =
        runTest {
            enqueue(
                200,
                """{"status":"success","data":{"expert_timezone":"Pacific/Auckland","month":"2026-10","days":[]}}""",
            )

            val data = api.getExpertAvailability("e1", "2026-10", null).data

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/experts/e1/availability", request.url.encodedPath)
            assertEquals(1, request.url.querySize)
            assertEquals("2026-10", request.url.queryParameter("month"))
            assertNull(request.url.queryParameter("topic_id"))
            assertEquals("Pacific/Auckland", data.expertTimezone)
        }

    @Test
    fun getExpertAvailabilitySendsTopicIdWhenGiven() =
        runTest {
            enqueue(200, """{"status":"success","data":{"days":[]}}""")

            api.getExpertAvailability("e1", "2026-10", "t1")

            val request = server.takeRequest()
            assertEquals("2026-10", request.url.queryParameter("month"))
            assertEquals("t1", request.url.queryParameter("topic_id"))
        }

    @Test
    fun requestBookingPostsExactStrictBodyAndDecodesBookingId() =
        runTest {
            enqueue(
                201,
                """{"status":"success","data":{"booking_id":"b1","status":"pending_approval","message":"Sent"}}""",
            )

            val data = api.requestBooking(sampleBookingBody("Hi")).data

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/v1/bookings/request", request.url.encodedPath)
            assertEquals(
                """{"expert_id":"e1","topic_id":"t1","scheduled_start":"2026-10-24T14:00:00Z",""" +
                    """"duration_minutes":30,"meeting_type":"in_app_voice","timezone":"America/Toronto",""" +
                    """"client_notes":"Hi"}""",
                request.body?.utf8(),
            )
            assertEquals("b1", data.bookingId)
        }

    @Test
    fun requestBookingOmitsClientNotesWhenNull() =
        runTest {
            enqueue(201, """{"status":"success","data":{"booking_id":"b1"}}""")

            api.requestBooking(sampleBookingBody(null))

            val request = server.takeRequest()
            val body = request.body?.utf8().orEmpty()
            assertFalse(body, body.contains("client_notes"))
        }

    @Test
    fun requestBooking409SlotUnavailableSurfacesAsHttpWithErrorCode() =
        runTest {
            enqueue(409, """{"status":"error","code":"SLOT_UNAVAILABLE","message":"Slot is no longer available"}""")

            val error = runCatching { apiCall { api.requestBooking(sampleBookingBody(null)) } }.exceptionOrNull()

            assertTrue(error is ApiException.Http)
            error as ApiException.Http
            assertEquals(409, error.status)
            assertEquals(ErrorCode.SlotUnavailable, error.code)
        }

    @Test
    fun requestBooking422MeetingLinkUnavailableSurfacesAsHttpWithErrorCode() =
        runTest {
            enqueue(422, """{"status":"error","code":"MEETING_LINK_UNAVAILABLE","message":"No meeting link"}""")

            val error = runCatching { apiCall { api.requestBooking(sampleBookingBody(null)) } }.exceptionOrNull()

            assertTrue(error is ApiException.Http)
            error as ApiException.Http
            assertEquals(422, error.status)
            assertEquals(ErrorCode.MeetingLinkUnavailable, error.code)
        }

    @Test
    fun getClientBookingDetailUsesIdPathAndDecodes() =
        runTest {
            enqueue(
                200,
                """
                {"status":"success","data":{"booking_id":"b1","status":"pending_approval",
                "start_time":"2026-10-24T18:00:00+00:00","topic":{"id":"t1","title":"Intro","is_free":true},
                "expert":{"id":"e1","full_name":"Ada Lovelace"}}}
                """.trimIndent(),
            )

            val detail = api.getClientBookingDetail("b1").data

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/client/bookings/b1", request.url.encodedPath)
            assertEquals("b1", detail.bookingId)
        }

    @Test
    fun getPaymentMethodsIssuesGetAndDecodesList() =
        runTest {
            enqueue(
                200,
                """{"status":"success","data":{"payment_methods":[""" +
                    """{"id":"pm_1","brand":"visa","last4":"4242","exp_month":9,"exp_year":2028,"is_default":true}]}}""",
            )

            val items = api.getPaymentMethods().data.paymentMethods

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/me/payment-methods", request.url.encodedPath)
            assertEquals("pm_1", items.single().id)
        }

    @Test
    fun createPaymentMethodSetupIntentPostsEmptyJsonObject() =
        runTest {
            enqueue(
                200,
                """{"status":"success","data":{"client_secret":"seti_1_secret_x","customer_id":"cus_1",""" +
                    """"ephemeral_key":"ek_1"}}""",
            )

            val data = api.createPaymentMethodSetupIntent().data

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/v1/me/payment-methods/setup-intent", request.url.encodedPath)
            assertEquals("{}", request.body?.utf8())
            assertEquals("cus_1", data.customerId)
        }

    @Test
    fun setDefaultPaymentMethodPatchesWithEmptyJsonObject() =
        runTest {
            enqueue(200, """{"status":"success","message":"Default updated"}""")

            api.setDefaultPaymentMethod("pm_1")

            val request = server.takeRequest()
            assertEquals("PATCH", request.method)
            assertEquals("/api/v1/me/payment-methods/pm_1/default", request.url.encodedPath)
            assertEquals("{}", request.body?.utf8())
        }

    @Test
    fun deletePaymentMethodIssuesDeleteWithEmptyBody() =
        runTest {
            enqueue(200, """{"status":"success","message":"Removed"}""")

            api.deletePaymentMethod("pm_1")

            val request = server.takeRequest()
            assertEquals("DELETE", request.method)
            assertEquals("/api/v1/me/payment-methods/pm_1", request.url.encodedPath)
            assertEquals(0L, request.body?.size ?: 0L)
        }

    @Test
    fun createPaymentIntentPostsStrictBodyAndDecodesBareResponse() =
        runTest {
            enqueue(
                200,
                """{"payment_intent_id":"pi_1","client_secret":"pi_1_secret_x","customer_id":"cus_1",""" +
                    """"ephemeral_key":"ek_1","total_amount":42.5,"summary":{"total":42.5},"currency":"cad"}""",
            )

            val response =
                api.createPaymentIntent(
                    PaymentIntentRequest("e1", "t2", "2026-10-23", "01:00", 60, "attempt-1"),
                )

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/v1/bookings/payment-intent", request.url.encodedPath)
            assertEquals(
                """{"expert_id":"e1","topic_id":"t2","selected_date":"2026-10-23","time_slot":"01:00",""" +
                    """"duration_minutes":60,"booking_attempt_id":"attempt-1"}""",
                request.body?.utf8(),
            )
            assertEquals("pi_1", response.paymentIntentId)
            assertEquals("cad", response.currency)
        }

    private fun sampleConfirmBody() =
        ConfirmBookingRequest(
            paymentIntentId = "pi_1",
            expertId = "e1",
            topicId = "t2",
            startTime = "2026-10-23T01:00:00Z",
            meetingType = "in_app_voice",
            stripePaymentMethodId = "pm_1",
        )

    @Test
    fun confirmBookingPostsAndDecodes201Envelope() =
        runTest {
            enqueue(
                201,
                """{"status":"success","data":{"booking_id":"b9","status":"pending_approval",""" +
                    """"message":"Sent","estimated_confirmation_time":"24 hours"}}""",
            )

            val data = api.confirmBooking(sampleConfirmBody()).data

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/v1/bookings/confirm", request.url.encodedPath)
            val sent = request.body?.utf8().orEmpty()
            assertFalse(sent, sent.contains("booking_attempt_id"))
            assertEquals("b9", data.bookingId)
        }

    @Test
    fun confirmBooking402SurfacesDetailThroughApiCall() =
        runTest {
            enqueue(
                402,
                """{"status":"error","code":"PAYMENT_REQUIRES_ACTION","message":"Authentication required",""" +
                    """"detail":{"client_secret":"pi_1_secret_x","stripe_account":"acct_1"}}""",
            )

            val error = runCatching { apiCall { api.confirmBooking(sampleConfirmBody()) } }.exceptionOrNull()

            assertTrue(error is ApiException.Http)
            error as ApiException.Http
            assertEquals(402, error.status)
            assertEquals(ErrorCode.PaymentRequiresAction, error.code)
            assertEquals("pi_1_secret_x", error.detail?.get("client_secret"))
            assertEquals("acct_1", error.detail?.get("stripe_account"))
        }
}
