package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.model.requests.AvailabilityTimeRangeBody
import com.uptrendinvestments.clickmeapp.model.requests.CreateExpertTopicRequest
import com.uptrendinvestments.clickmeapp.model.requests.ExpertTagSelection
import com.uptrendinvestments.clickmeapp.model.requests.TopicHourlyRateBody
import com.uptrendinvestments.clickmeapp.model.requests.UpdateExpertTagsRequest
import com.uptrendinvestments.clickmeapp.model.requests.UpdateExpertTopicRequest
import com.uptrendinvestments.clickmeapp.model.requests.UpdateMyAvailabilityRequest
import com.uptrendinvestments.clickmeapp.model.requests.WeeklyScheduleBody
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/** Wire-level tests (MockWebServer + the real Moshi) for the Phase 10 expert topic and availability endpoints. */
class ExpertApiTest {
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

    private val topicJson =
        """{"id":"t1","title":"Tax","description":null,"duration_mins":60,"icon_slug":null,""" +
            """"price":{"amount":2500,"currency":"CAD","is_free":false,"label":"x"},"expertise_tags":[]}"""

    @Test
    fun getMyTopicsIsGetAndReturnsTypedItems() =
        runTest {
            enqueue(200, """{"status":"success","data":{"topics":[$topicJson]}}""")

            val topics = api.getMyTopics().data.topics

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/expert/topics", request.url.encodedPath)
            assertEquals(2500L, topics.single().amountMinor)
        }

    @Test
    fun createTopicPostsCodegenBodyAndDecodesTopic() =
        runTest {
            enqueue(201, """{"status":"success","data":{"topic":$topicJson}}""")

            val created =
                api
                    .createTopic(
                        CreateExpertTopicRequest(
                            title = "Tax",
                            durationMins = 60,
                            isFree = false,
                            hourlyRate = TopicHourlyRateBody(2500, "CAD"),
                            expertiseTagIds = listOf("a1"),
                        ),
                    ).data.topic

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/v1/expert/topics", request.url.encodedPath)
            assertEquals(
                """{"title":"Tax","duration_mins":60,"is_free":false,""" +
                    """"hourly_rate":{"amount":2500,"currency":"CAD"},"expertise_tag_ids":["a1"]}""",
                request.body?.utf8(),
            )
            assertEquals("t1", created.id)
        }

    @Test
    fun updateTopicPatchesWithExplicitNullIcon() =
        runTest {
            enqueue(200, """{"status":"success","data":{"topic":$topicJson}}""")

            api.updateTopic(
                "t1",
                UpdateExpertTopicRequest(
                    title = "Tax",
                    description = null,
                    durationMins = 60,
                    isFree = false,
                    hourlyRate = TopicHourlyRateBody(2500, "CAD"),
                    iconSlug = null,
                    expertiseTagIds = emptyList(),
                ),
            )

            val request = server.takeRequest()
            assertEquals("PATCH", request.method)
            assertEquals("/api/v1/expert/topics/t1", request.url.encodedPath)
            val body = request.body?.utf8().orEmpty()
            assertTrue(body, body.contains(""""icon_slug":null"""))
            assertTrue(body, body.contains(""""description":null"""))
        }

    @Test
    fun deleteTopicIsDeleteAndDecodesMessage() =
        runTest {
            enqueue(200, """{"status":"success","message":"Topic archived"}""")

            val response = api.deleteTopic("t1")

            val request = server.takeRequest()
            assertEquals("DELETE", request.method)
            assertEquals("/api/v1/expert/topics/t1", request.url.encodedPath)
            assertEquals("Topic archived", response.message)
        }

    @Test
    fun getMyAvailabilityHitsExpertOwnRoute() =
        runTest {
            enqueue(
                200,
                """{"status":"success","data":{"timezone":{"id":"UTC","display_name":"UTC","utc_offset":0},""" +
                    """"recurring_schedule":{"monday":[{"start_time":"09:00","end_time":"17:00"}]}}}""",
            )

            val data = api.getMyAvailability().data

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/expert/availability", request.url.encodedPath)
            assertEquals("UTC", data.timezone?.id)
            val monday = data.recurringSchedule?.monday
            assertEquals("09:00", monday?.single()?.startTime)
        }

    @Test
    fun updateMyAvailabilityPutsTimezoneAndAllSevenDays() =
        runTest {
            enqueue(
                200,
                """{"status":"success","message":"Availability updated","data":{"updated_days":["monday"]}}""",
            )
            val empty = emptyList<AvailabilityTimeRangeBody>()

            val response =
                api.updateMyAvailability(
                    UpdateMyAvailabilityRequest(
                        timezoneId = "America/Toronto",
                        schedule =
                            WeeklyScheduleBody(
                                monday = listOf(AvailabilityTimeRangeBody("09:00", "17:00")),
                                tuesday = empty,
                                wednesday = empty,
                                thursday = empty,
                                friday = empty,
                                saturday = empty,
                                sunday = empty,
                            ),
                    ),
                )

            val request = server.takeRequest()
            assertEquals("PUT", request.method)
            assertEquals("/api/v1/expert/availability", request.url.encodedPath)
            val body = request.body?.utf8().orEmpty()
            assertTrue(body, body.contains(""""timezone_id":"America/Toronto""""))
            listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday").forEach { day ->
                assertTrue("$day in $body", body.contains(""""$day":["""))
            }
            assertEquals("Availability updated", response.message)
        }

    @Test
    fun updateExpertProfileTagsPatchesExpertiseTags() =
        runTest {
            enqueue(
                200,
                """{"status":"success","message":"ok","data":{"profile_id":"p1","updated_fields":["tags"]}}""",
            )

            api.updateExpertProfileTags(
                UpdateExpertTagsRequest(listOf(ExpertTagSelection("a1", true))),
            )

            val request = server.takeRequest()
            assertEquals("PATCH", request.method)
            assertEquals("/api/v1/expert/profile", request.url.encodedPath)
            assertEquals(
                """{"expertise_tags":[{"tag_id":"a1","is_primary":true}]}""",
                request.body?.utf8(),
            )
        }

    @Test
    fun priceBelowMinimumSurfacesAsHttpExceptionWithCodeAndMessage() =
        runTest {
            val message = "A paid session must be at least CA${'$'}5."
            enqueue(422, """{"status":"error","code":"PRICE_BELOW_MINIMUM","message":"$message"}""")

            try {
                apiCall {
                    api.createTopic(
                        CreateExpertTopicRequest(
                            title = "Tax",
                            durationMins = 60,
                            isFree = false,
                            hourlyRate = TopicHourlyRateBody(100, "CAD"),
                            expertiseTagIds = emptyList(),
                        ),
                    )
                }
                fail("Expected ApiException.Http")
            } catch (e: ApiException.Http) {
                assertEquals(422, e.status)
                assertEquals(ErrorCode.PriceBelowMinimum, e.code)
                assertEquals(message, e.serverMessage)
            }
        }
}
