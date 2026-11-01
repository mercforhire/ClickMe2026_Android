package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ReviewsRepository
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/** Exact-wire and decoding tests for the full Reviews list (`GET /experts/{id}/reviews`, Phase 16.3 D-16). */
class ExpertReviewsApiTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: ReviewsRepository

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
        repository = ReviewsRepository(api)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun enqueue(body: String) {
        server.enqueue(
            MockResponse
                .Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body(body)
                .build(),
        )
    }

    @Test
    fun expertReviewsSendsPageOneLimitTwenty() =
        runTest {
            enqueue("""{"status":"success","data":{"summary":{"total_reviews":0},"reviews":[]}}""")

            repository.expertReviews("e1")

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/experts/e1/reviews", request.url.encodedPath)
            assertEquals("1", request.url.queryParameter("page"))
            assertEquals("20", request.url.queryParameter("limit"))
            assertEquals(2, request.url.querySize)
        }

    @Test
    fun decodesSessionContext() =
        runTest {
            enqueue(
                """
                {"status":"success","data":{
                  "summary":{"expert_id":"e1","overall_rating":4.5,"total_reviews":12},
                  "reviews":[{
                    "id":"r1",
                    "reviewer":{"name":"Sam Lee","avatar_url":null},
                    "rating":5,
                    "comment":"Great session",
                    "session_context":{
                      "topic":"Career advice",
                      "date":"2026-10-12T15:00:00.000Z",
                      "date_label":"Oct 12, 2026"
                    }
                  }]}}
                """.trimIndent(),
            )

            val payload = repository.expertReviews("e1")

            assertEquals(4.5, payload.summary.overallRating!!, 0.0)
            assertEquals(12, payload.summary.totalReviews)
            val context = payload.reviews.single().sessionContext!!
            assertEquals("Career advice", context.topic)
            assertEquals("2026-10-12T15:00:00.000Z", context.date)
            assertEquals("Oct 12, 2026", context.dateLabel)
        }

    @Test
    fun decodesMissingAndNullContext() =
        runTest {
            enqueue(
                """
                {"status":"success","data":{
                  "summary":{"total_reviews":2},
                  "reviews":[
                    {"id":"r1","reviewer":{"name":"A"},"rating":4},
                    {"id":"r2","reviewer":{"name":"B"},"rating":3,
                     "session_context":{"topic":null,"date":null,"date_label":null}}
                  ]}}
                """.trimIndent(),
            )

            val reviews = repository.expertReviews("e1").reviews

            assertNull(reviews[0].sessionContext)
            val context = reviews[1].sessionContext!!
            assertNull(context.topic)
            assertNull(context.date)
            assertNull(context.dateLabel)
        }
}
