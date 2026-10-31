package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.screens.clientmode.search.SearchRepository
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

/** Exact-wire tests for `GET /experts/search` (the backend's search schema is strict, so extra params fail). */
class SearchApiTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: SearchRepository

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
        repository = SearchRepository(api)
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
    fun browseSetSendsEmptyQSortPagingAndNoCategory() =
        runTest {
            enqueue(200, """{"status":"success","data":{"experts":[]}}""")

            repository.search(q = "", categorySlug = null, sort = "relevance")

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/experts/search", request.url.encodedPath)
            assertEquals("", request.url.queryParameter("q"))
            assertEquals("relevance", request.url.queryParameter("sort"))
            assertEquals("1", request.url.queryParameter("page"))
            assertEquals("20", request.url.queryParameter("limit"))
            assertNull(request.url.queryParameter("category"))
            assertEquals(4, request.url.querySize)
        }

    @Test
    fun categorySlugIsSentAsCategory() =
        runTest {
            enqueue(200, """{"status":"success","data":{"experts":[]}}""")

            repository.search(q = "ada", categorySlug = "business", sort = "rating")

            val request = server.takeRequest()
            assertEquals("ada", request.url.queryParameter("q"))
            assertEquals("business", request.url.queryParameter("category"))
            assertEquals("rating", request.url.queryParameter("sort"))
            assertEquals(5, request.url.querySize)
        }

    @Test
    fun decodesRowsAndIgnoresUnknownColumnsAndPaging() =
        runTest {
            enqueue(
                200,
                """
                {"status":"success","data":{"experts":[{
                  "expert_id":"11111111-1111-1111-1111-111111111111",
                  "full_name":"Ada Lovelace",
                  "headline":"Engineer",
                  "avatar_url":"https://cdn.example/ada.png",
                  "experience_years":9,
                  "avg_rating":4.5,
                  "total_reviews":12,
                  "relevance_score":0.87,
                  "expertise_tags":["AI","Math"],
                  "is_favorite":false,
                  "matched_topic_ids":["t1"],
                  "total_count":41
                }],"pagination":{"current_page":1,"total_pages":3,"has_more":true}}}
                """.trimIndent(),
            )

            val rows = repository.search("", null, "relevance")

            assertEquals(1, rows.size)
            val row = rows.single()
            assertEquals("11111111-1111-1111-1111-111111111111", row.expertId)
            assertEquals("Ada Lovelace", row.fullName)
            assertEquals("Engineer", row.headline)
            assertEquals("https://cdn.example/ada.png", row.avatarUrl)
            assertEquals(4.5, row.avgRating!!, 0.0)
            assertEquals(12, row.totalReviews)
            assertEquals(listOf("AI", "Math"), row.expertiseTags)
        }

    @Test
    fun emptyExpertsDecodesToEmptyList() =
        runTest {
            enqueue(200, """{"status":"success","data":{"experts":[]}}""")

            assertTrue(repository.search("", null, "relevance").isEmpty())
        }

    @Test
    fun httpErrorCarriesServerMessage() =
        runTest {
            enqueue(422, """{"status":"error","code":"VALIDATION_ERROR","message":"Invalid sort"}""")

            try {
                repository.search("", null, "bogus")
                fail("expected ApiException.Http")
            } catch (e: ApiException.Http) {
                assertEquals(422, e.status)
                assertEquals("Invalid sort", e.serverMessage)
            }
        }
}
