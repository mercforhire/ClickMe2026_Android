package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.model.requests.SubmitFeedbackRequest
import com.uptrendinvestments.clickmeapp.screens.login.fieldMessage
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.time.Instant

/**
 * Exact-wire tests for `GET /feedback/types` and `POST /feedback/submit` (Phase 16.5). The submit schema is strict:
 * a null email is omitted (never an empty string) and `metadata` is never sent.
 */
class FeedbackApiTest {
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
    fun typesCallsThePathAndDecodes() =
        runTest {
            enqueue(
                200,
                """
                {"status":"success","data":{"types":[
                  {"id":"general","label":"General feedback","description":"Tell us anything"},
                  {"id":"bug","label":"Report a bug","description":null}
                ]}}
                """.trimIndent(),
            )

            val types = api.getFeedbackTypes().data.types

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/feedback/types", request.url.encodedPath)
            assertEquals("general", types[0].id)
            assertEquals("General feedback", types[0].label)
            assertEquals("Tell us anything", types[0].description)
            assertEquals("bug", types[1].id)
            assertNull(types[1].description)
        }

    @Test
    fun submitSendsTrimmedShapeWithoutEmailWhenNull() =
        runTest {
            enqueue(201, CREATED)

            api.submitFeedback(SubmitFeedbackRequest("general", "0123456789"))

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/v1/feedback/submit", request.url.encodedPath)
            assertEquals("""{"type_id":"general","details":"0123456789"}""", request.body?.utf8())
        }

    @Test
    fun submitSendsEmailWhenPresent() =
        runTest {
            enqueue(201, CREATED)

            api.submitFeedback(SubmitFeedbackRequest("general", "0123456789", "a@b.co"))

            val request = server.takeRequest()
            assertEquals(
                """{"type_id":"general","details":"0123456789","email":"a@b.co"}""",
                request.body?.utf8(),
            )
        }

    @Test
    fun created201Decodes() =
        runTest {
            enqueue(201, CREATED)

            val data = api.submitFeedback(SubmitFeedbackRequest("general", "0123456789")).data

            assertEquals("s1", data.submissionId)
            assertNotNull(data.submittedAt)
            assertEquals(Instant.parse("2026-11-02T10:00:00.123Z"), data.submittedAt)
        }

    @Test
    fun validationErrorMapsToItsField() =
        runTest {
            enqueue(422, """{"status":"error","errors":[{"field":"details","message":"Too short"}]}""")

            try {
                apiCall { api.submitFeedback(SubmitFeedbackRequest("general", "short")) }
                fail("expected ApiException.Http")
            } catch (e: ApiException.Http) {
                assertEquals(422, e.status)
                assertEquals("Too short", e.fieldMessage("details"))
            }
        }

    @Test
    fun toStringHoldsNoUserText() {
        val text = SubmitFeedbackRequest("general", "my private text", "a@b.co").toString()

        assertFalse(text, text.contains("my private text"))
        assertFalse(text, text.contains("a@b.co"))
    }

    private companion object {
        const val CREATED =
            """{"status":"success","data":{"submission_id":"s1","submitted_at":"2026-11-02T10:00:00.123+00:00"}}"""
    }
}
