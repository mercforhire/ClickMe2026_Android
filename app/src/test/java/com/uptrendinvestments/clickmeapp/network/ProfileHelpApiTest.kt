package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.model.requests.MeetingUrlBody
import com.uptrendinvestments.clickmeapp.model.requests.ProfileBasicBody
import com.uptrendinvestments.clickmeapp.model.requests.ProfileLanguagesBody
import com.uptrendinvestments.clickmeapp.model.requests.ProfileLocationBody
import com.uptrendinvestments.clickmeapp.model.requests.UpdateExpertMeetingUrlRequest
import com.uptrendinvestments.clickmeapp.model.requests.UpdatePasswordRequest
import com.uptrendinvestments.clickmeapp.model.requests.UpdateUserProfileRequest
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/** Wire-level tests (MockWebServer + the real Moshi) for the Phase 14 password, profile, meeting link and FAQ calls. */
class ProfileHelpApiTest {
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

    private val profileBody =
        """{"status":"success","data":{"personal_details":{"first_name":"Ana","last_name":"Lee"},""" +
            """"professional_details":{},"location":{},"languages":[]}}"""

    @Test
    fun updatePasswordPatchesBody() =
        runTest {
            enqueue(200, """{"status":"success","message":"Password updated"}""")

            val response = apiCall { api.updatePassword(UpdatePasswordRequest("old1pass", "new2pass")) }
            val request = server.takeRequest()

            assertEquals("PATCH", request.method)
            assertEquals("/api/v1/user/password", request.url.encodedPath)
            assertEquals("""{"current_password":"old1pass","new_password":"new2pass"}""", request.body?.utf8())
            assertEquals("Password updated", response.message)
        }

    @Test
    fun passwordErrorsMap() =
        runTest {
            enqueue(403, """{"status":"error","message":"Current password is incorrect"}""")
            enqueue(
                422,
                """{"status":"error","errors":[{"field":"new_password","message":"Password must contain a number"}]}""",
            )

            val wrong = failure { api.updatePassword(UpdatePasswordRequest("bad", "new2pass")) }
            val weak = failure { api.updatePassword(UpdatePasswordRequest("old1pass", "weak")) }

            assertEquals(403, wrong.status)
            assertEquals("Current password is incorrect", wrong.serverMessage)
            assertEquals(422, weak.status)
            assertEquals("new_password", weak.fieldErrors.single().field)
        }

    @Test
    fun profilePatchSendsOnlyPresentGroups() =
        runTest {
            enqueue(200, profileBody)

            val data =
                apiCall {
                    api.updateUserProfile(
                        UpdateUserProfileRequest(basic = ProfileBasicBody("Ana", "Lee", "")),
                    )
                }.data
            val request = server.takeRequest()

            assertEquals("PATCH", request.method)
            assertEquals("/api/v1/user/profile", request.url.encodedPath)
            assertEquals("""{"basic":{"first_name":"Ana","last_name":"Lee","bio":""}}""", request.body?.utf8())
            assertEquals("Ana", data.personalDetails.firstName)
        }

    @Test
    fun languagesGroupIsFullReplace() =
        runTest {
            enqueue(200, profileBody)
            enqueue(200, profileBody)

            apiCall {
                api.updateUserProfile(
                    UpdateUserProfileRequest(languages = ProfileLanguagesBody(emptyList(), emptyList())),
                )
            }
            val languages = server.takeRequest().body?.utf8()
            apiCall {
                api.updateUserProfile(
                    UpdateUserProfileRequest(
                        location = ProfileLocationBody(city = "Toronto", stateProvince = "ON", countryCode = "CAN"),
                    ),
                )
            }
            val location = server.takeRequest().body?.utf8()

            assertEquals("""{"languages":{"language_ids":[],"custom_languages":[]}}""", languages)
            assertEquals(
                """{"location":{"city":"Toronto","state_province":"ON","country_code":"CAN"}}""",
                location,
            )
        }

    @Test
    fun meetingUrlPatchSendsOnlyProfessionalDetails() =
        runTest {
            enqueue(200, """{"status":"success","message":"Profile updated"}""")

            apiCall {
                api.updateExpertMeetingUrl(UpdateExpertMeetingUrlRequest(MeetingUrlBody("https://zoom.us/j/1")))
            }
            val request = server.takeRequest()

            assertEquals("PATCH", request.method)
            assertEquals("/api/v1/expert/profile", request.url.encodedPath)
            assertEquals("""{"professional_details":{"meeting_url":"https://zoom.us/j/1"}}""", request.body?.utf8())
        }

    @Test
    fun expertProfileDecodesMeetingUrl() =
        runTest {
            enqueue(
                200,
                """{"status":"success","data":{"setup_completed":true,""" +
                    """"professional_details":{"job_title":"Coach","meeting_url":"https://zoom.us/j/1"}}}""",
            )
            enqueue(200, """{"status":"success","data":{"setup_completed":true}}""")

            val withLink = apiCall { api.getExpertProfile() }.data
            val without = apiCall { api.getExpertProfile() }.data

            assertEquals("https://zoom.us/j/1", withLink.professionalDetails?.meetingUrl)
            assertEquals("Coach", withLink.professionalDetails?.jobTitle)
            assertNull(without.professionalDetails)
        }

    @Test
    fun faqsDecode() =
        runTest {
            enqueue(
                200,
                """{"status":"success","data":{"categories":[""" +
                    """{"id":"c2","title":"Payments","icon_url":null,"display_order":2,"featured_articles":[""" +
                    """{"id":"a1","category_id":"c2","question":"How do I get paid?",""" +
                    """"snippet":null,"view_count":3}]},""" +
                    """{"id":"c1","title":"Account","display_order":1}]}}""",
            )

            val payload = apiCall { api.getFaqs() }.data
            val request = server.takeRequest()

            assertEquals("/api/v1/help/faqs", request.url.encodedPath)
            assertEquals(2, payload.categories.size)
            val payments = payload.categories.first { it.id == "c2" }
            assertEquals(2, payments.displayOrder)
            assertNull(payments.iconUrl)
            val article = payments.featuredArticles.single()
            val account = payload.categories.first { it.id == "c1" }
            assertEquals("How do I get paid?", article.question)
            assertNull(article.snippet)
            assertTrue(account.featuredArticles.isEmpty())
            assertTrue(payload.trendingArticles.isEmpty())
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
