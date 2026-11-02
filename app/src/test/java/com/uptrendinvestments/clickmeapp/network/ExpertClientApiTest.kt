package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.isNotFound
import com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile.ExpertClientProfileRepository
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.time.Instant

/**
 * Wire tests for `GET /expert/clients/{id}` (Phase 16.4). A 403 `NO_RELATIONSHIP` and a 404 `NOT_FOUND` must stay
 * distinguishable, and every nullable column must decode to null.
 */
class ExpertClientApiTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: ExpertClientProfileRepository

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
        repository = ExpertClientProfileRepository(api)
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
    fun getsTheClientPath() =
        runTest {
            enqueue(200, """{"status":"success","data":{"id":"c1"}}""")

            repository.profile("c1")

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/expert/clients/c1", request.url.encodedPath)
        }

    @Test
    fun decodesEveryField() =
        runTest {
            enqueue(
                200,
                """
                {"status":"success","data":{
                  "id":"c1",
                  "personal_details":{
                    "first_name":"Sophia","last_name":"Carter","full_name":"Sophia Carter",
                    "avatar_url":"https://cdn.example/s.png","bio":"Loves learning."
                  },
                  "location":{
                    "city":"Toronto","state_province":"ON","country":"CAN","timezone":"(GMT-05:00) Eastern Time"
                  },
                  "languages":["English","Spanish"],
                  "member_since":"2025-03-10T12:00:00.123456+00:00",
                  "relationship":{
                    "total_sessions":3,
                    "first_session_date":"2026-02-01T15:00:00Z",
                    "last_session_date":"2026-10-01T15:00:00Z"
                  }
                }}
                """.trimIndent(),
            )

            val profile = repository.profile("c1")

            assertEquals("c1", profile.id)
            assertEquals("Sophia", profile.personalDetails.firstName)
            assertEquals("Carter", profile.personalDetails.lastName)
            assertEquals("Sophia Carter", profile.personalDetails.fullName)
            assertEquals("https://cdn.example/s.png", profile.personalDetails.avatarUrl)
            assertEquals("Loves learning.", profile.personalDetails.bio)
            assertEquals("Toronto", profile.location.city)
            assertEquals("ON", profile.location.stateProvince)
            assertEquals("CAN", profile.location.country)
            assertEquals("(GMT-05:00) Eastern Time", profile.location.timezone)
            assertEquals(listOf("English", "Spanish"), profile.languages)
            assertEquals(Instant.parse("2025-03-10T12:00:00.123456Z"), profile.memberSince)
            assertEquals(3, profile.relationship.totalSessions)
            assertEquals(Instant.parse("2026-02-01T15:00:00Z"), profile.relationship.firstSessionDate)
            assertEquals(Instant.parse("2026-10-01T15:00:00Z"), profile.relationship.lastSessionDate)
        }

    @Test
    fun decodesNullFields() =
        runTest {
            enqueue(
                200,
                """
                {"status":"success","data":{
                  "id":"c1",
                  "personal_details":{
                    "first_name":null,"last_name":null,"full_name":null,"avatar_url":null,"bio":null
                  },
                  "location":{"city":null,"state_province":null,"country":null,"timezone":null},
                  "languages":[],
                  "member_since":null,
                  "relationship":{"total_sessions":0,"first_session_date":null,"last_session_date":null}
                }}
                """.trimIndent(),
            )

            val profile = repository.profile("c1")

            assertNull(profile.personalDetails.firstName)
            assertNull(profile.personalDetails.lastName)
            assertNull(profile.personalDetails.fullName)
            assertNull(profile.personalDetails.avatarUrl)
            assertNull(profile.personalDetails.bio)
            assertNull(profile.location.city)
            assertNull(profile.location.stateProvince)
            assertNull(profile.location.country)
            assertNull(profile.location.timezone)
            assertTrue(profile.languages.isEmpty())
            assertNull(profile.memberSince)
            assertEquals(0, profile.relationship.totalSessions)
            assertNull(profile.relationship.firstSessionDate)
            assertNull(profile.relationship.lastSessionDate)
        }

    @Test
    fun noRelationshipIsDistinguishable() =
        runTest {
            enqueue(
                403,
                """{"status":"error","code":"NO_RELATIONSHIP","message":"You have no bookings with this client."}""",
            )

            try {
                repository.profile("c1")
                fail("expected ApiException.Http")
            } catch (e: ApiException.Http) {
                assertEquals(403, e.status)
                assertEquals(ErrorCode.NoRelationship, e.code)
                assertFalse(e.isNotFound())
            }
        }

    @Test
    fun notFoundIsDistinguishable() =
        runTest {
            enqueue(404, """{"status":"error","code":"NOT_FOUND","message":"Client not found."}""")

            try {
                repository.profile("c1")
                fail("expected ApiException.Http")
            } catch (e: ApiException.Http) {
                assertTrue(e.isNotFound())
                assertNotEquals(ErrorCode.NoRelationship, e.code)
            }
        }

    @Test
    fun toStringPrintsTheIdOnly() =
        runTest {
            enqueue(
                200,
                """
                {"status":"success","data":{"id":"c1",
                  "personal_details":{"full_name":"Sophia Carter","bio":"Loves learning."}}}
                """.trimIndent(),
            )

            val text = repository.profile("c1").toString()

            assertTrue(text.contains("c1"))
            assertFalse(text.contains("Sophia"))
            assertFalse(text.contains("Loves learning"))
        }
}
