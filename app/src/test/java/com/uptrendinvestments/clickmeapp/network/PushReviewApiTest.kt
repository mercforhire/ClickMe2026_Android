package com.uptrendinvestments.clickmeapp.network

import com.squareup.moshi.Types
import com.uptrendinvestments.clickmeapp.model.requests.RegisterDeviceTokenRequest
import com.uptrendinvestments.clickmeapp.model.requests.SubmitReviewRequest
import com.uptrendinvestments.clickmeapp.model.responses.ReviewPrefillDto
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ReviewsRepository
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

/** Wire-level tests (MockWebServer + the real Moshi) for the Phase 13 token and review endpoints. */
class PushReviewApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: ClickMeApi
    private lateinit var repository: ReviewsRepository

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
        repository = ReviewsRepository(api)
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

    private fun bodyMap(json: String?): Map<String, Any?> {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        return requireNotNull(MoshiProvider.moshi.adapter<Map<String, Any?>>(type).fromJson(requireNotNull(json)))
    }

    @Test
    fun registerSendsOnlySchemaKeys() =
        runTest {
            enqueue(200, """{"status":"success","message":"Device token registered."}""")
            enqueue(200, """{"status":"success","message":"Device token registered."}""")

            api.registerDeviceToken(RegisterDeviceTokenRequest("dev-1", "tok-1", appVersion = "0.1.0"))
            api.registerDeviceToken(RegisterDeviceTokenRequest("dev-1", "tok-1"))

            val withVersion = server.takeRequest()
            val withoutVersion = server.takeRequest()
            assertEquals("POST", withVersion.method)
            assertEquals("/api/v1/notifications/tokens", withVersion.url.encodedPath)
            val withBody = bodyMap(withVersion.body?.utf8())
            assertEquals(setOf("device_id", "token", "platform", "app_version"), withBody.keys)
            assertEquals("android", withBody["platform"])
            val withoutBody = withoutVersion.body?.utf8()
            assertEquals(setOf("device_id", "token", "platform"), bodyMap(withoutBody).keys)
            assertFalse(withoutBody.orEmpty().contains("null"))
        }

    @Test
    fun deleteUsesDeviceIdPath() =
        runTest {
            enqueue(200, """{"status":"success","message":"Device token removed."}""")

            val response = api.deleteDeviceToken("dev-1")

            val request = server.takeRequest()
            assertEquals("DELETE", request.method)
            assertEquals("/api/v1/notifications/tokens/dev-1", request.url.encodedPath)
            assertEquals("success", response.status)
            assertEquals("Device token removed.", response.message)
        }

    @Test
    fun contextDecodesWithAndWithoutPrefill() =
        runTest {
            enqueue(
                200,
                """{"status":"success","data":{"has_submitted":false,"review_target":{"id":"e1",""" +
                    """"name":"Ada Lovelace","avatar_url":null,"roles":["expert"]},"session_topic":"Algorithms"}}""",
            )
            enqueue(
                200,
                """{"status":"success","data":{"has_submitted":true,"review_target":{"id":"e1",""" +
                    """"name":"Ada Lovelace","avatar_url":"https://x/a.png","roles":["expert"]},""" +
                    """"session_topic":"Algorithms","prefill":{"rating":4,"comment":"Great","tags":[]}}}""",
            )

            val fresh = repository.context("b1")
            val submitted = repository.context("b1")

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/bookings/b1/reviews/context", request.url.encodedPath)
            assertFalse(fresh.hasSubmitted)
            assertEquals("e1", fresh.reviewTarget?.id)
            assertEquals("Ada Lovelace", fresh.reviewTarget?.name)
            assertNull(fresh.reviewTarget?.avatarUrl)
            assertEquals("Algorithms", fresh.sessionTopic)
            assertNull(fresh.prefill)
            assertTrue(submitted.hasSubmitted)
            assertEquals(4, submitted.prefill?.rating)
            assertEquals("Great", submitted.prefill?.comment)
            assertEquals("https://x/a.png", submitted.reviewTarget?.avatarUrl)
        }

    @Test
    fun submitTrimsAndOmitsBlankComment() =
        runTest {
            repeat(3) {
                enqueue(
                    201,
                    """{"status":"success","data":{"review_id":"r1","status":"published","message":"ok"}}""",
                )
            }

            val data = repository.submit("b1", 4, "  Great  ")
            repository.submit("b1", 4, "   ")
            repository.submit("b1", 4, null)

            val trimmed = server.takeRequest()
            val blank = server.takeRequest()
            val none = server.takeRequest()
            assertEquals("POST", trimmed.method)
            assertEquals("/api/v1/bookings/b1/reviews", trimmed.url.encodedPath)
            assertEquals("""{"rating":4,"comment":"Great"}""", trimmed.body?.utf8())
            assertEquals("""{"rating":4}""", blank.body?.utf8())
            assertEquals("""{"rating":4}""", none.body?.utf8())
            assertEquals("r1", data.reviewId)
        }

    @Test
    fun errorsReachApiException() =
        runTest {
            enqueue(404, """{"status":"error","code":"NOT_FOUND","message":"Booking not found."}""")
            enqueue(422, """{"status":"error","code":"VALIDATION_ERROR","message":"Rating must be 1 to 5."}""")

            val missing = codeOf { repository.context("ghost") }
            val invalid = codeOf { repository.submit("b1", 5, "x") }

            assertEquals(404, missing.status)
            assertEquals("Booking not found.", missing.serverMessage)
            assertEquals(422, invalid.status)
            assertEquals("Rating must be 1 to 5.", invalid.serverMessage)
        }

    @Test
    fun toStringHidesTokenAndComment() {
        val printed =
            listOf(
                RegisterDeviceTokenRequest("dev-1", "secret-token").toString(),
                SubmitReviewRequest(5, "secret-comment").toString(),
                ReviewPrefillDto(5, "secret-comment").toString(),
            )

        printed.forEach {
            assertFalse(it, it.contains("secret-token"))
            assertFalse(it, it.contains("secret-comment"))
        }
    }

    private suspend fun codeOf(block: suspend () -> Unit): ApiException.Http {
        try {
            block()
        } catch (e: ApiException.Http) {
            return e
        }
        throw AssertionError("Expected ApiException.Http")
    }
}
