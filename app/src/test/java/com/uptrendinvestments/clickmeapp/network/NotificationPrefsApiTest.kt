package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.model.requests.NotificationPreferenceInput
import com.uptrendinvestments.clickmeapp.model.requests.UpdateNotificationPreferencesRequest
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Exact-wire tests for `GET` and `PATCH /notifications/preferences` (Phase 16.5, D-01). The PATCH schema is strict
 * at both levels, so the body must hold exactly the five documented keys per row and no quiet-hours field.
 */
class NotificationPrefsApiTest {
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
    fun getCallsThePath() =
        runTest {
            enqueue(200, """{"status":"success","data":{"preferences":[]}}""")

            val data = api.getNotificationPreferences().data

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/notifications/preferences", request.url.encodedPath)
            assertTrue(data.preferences.isEmpty())
        }

    @Test
    fun decodesSingularKeysAndAllChannels() =
        runTest {
            enqueue(
                200,
                """
                {"status":"success","data":{"preferences":[
                  {"category":"booking","sub_category":"requests",
                   "push_enabled":false,"email_enabled":true,"in_app_enabled":false},
                  {"category":"booking","sub_category":"reminders",
                   "push_enabled":false,"email_enabled":false,"in_app_enabled":false}
                ]}}
                """.trimIndent(),
            )

            val rows = api.getNotificationPreferences().data.preferences

            assertEquals("booking", rows[0].category)
            assertEquals("requests", rows[0].subCategory)
            assertFalse(rows[0].pushEnabled)
            assertTrue(rows[0].emailEnabled)
            assertFalse(rows[0].inAppEnabled)
            assertTrue(rows[0].isAnyChannelEnabled)
            assertFalse(rows[1].isAnyChannelEnabled)
        }

    @Test
    fun extraServerFieldsAndUnknownCategoriesDecode() =
        runTest {
            enqueue(
                200,
                """
                {"status":"success","data":{"preferences":[
                  {"category":"promotions","sub_category":"offers","push_enabled":true,"email_enabled":true,
                   "in_app_enabled":true,"updated_at":"2026-01-01T00:00:00Z","something_new":1}
                ]}}
                """.trimIndent(),
            )

            val row =
                api
                    .getNotificationPreferences()
                    .data.preferences
                    .single()

            assertEquals("promotions", row.category)
            assertEquals("offers", row.subCategory)
        }

    @Test
    fun patchSendsExactlyFiveKeys() =
        runTest {
            enqueue(200, """{"status":"success","message":"Preferences updated."}""")

            api.updateNotificationPreferences(
                UpdateNotificationPreferencesRequest(
                    listOf(
                        NotificationPreferenceInput(
                            category = "booking",
                            subCategory = "requests",
                            pushEnabled = false,
                            emailEnabled = false,
                            inAppEnabled = false,
                        ),
                    ),
                ),
            )

            val request = server.takeRequest()
            assertEquals("PATCH", request.method)
            assertEquals("/api/v1/notifications/preferences", request.url.encodedPath)
            assertEquals(
                """{"preferences":[{"category":"booking","sub_category":"requests",""" +
                    """"push_enabled":false,"email_enabled":false,"in_app_enabled":false}]}""",
                request.body?.utf8(),
            )
        }

    @Test
    fun patchResponseDecodes() =
        runTest {
            enqueue(200, """{"status":"success","message":"Preferences updated."}""")

            val response =
                api.updateNotificationPreferences(
                    UpdateNotificationPreferencesRequest(emptyList()),
                )

            assertEquals("success", response.status)
            assertEquals("Preferences updated.", response.message)
        }
}
