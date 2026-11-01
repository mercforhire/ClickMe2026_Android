package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.ToggleFavoriteData
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.favorites.FavoritesRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.favorites.toFavoriteFailure
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException

/**
 * Exact-wire tests for the three `/client/favorites` calls (Phase 16.3). The add verb is PUT (the backend route),
 * not the POST written in PARITY-AUDIT, and the list must ask for `limit=100` (the server default is 20).
 */
class FavoritesApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: ClickMeApi
    private lateinit var repository: FavoritesRepository

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
        repository = FavoritesRepository(api)
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
    fun listSendsPageOneLimitHundredOnly() =
        runTest {
            enqueue(200, """{"status":"success","data":{"experts":[]}}""")

            repository.list()

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/client/favorites", request.url.encodedPath)
            assertEquals("1", request.url.queryParameter("page"))
            assertEquals("100", request.url.queryParameter("limit"))
            assertEquals(2, request.url.querySize)
        }

    @Test
    fun listDecodesRowsAndIgnoresUndeclaredColumns() =
        runTest {
            enqueue(
                200,
                """
                {"status":"success","data":{"experts":[{
                  "expert_id":"11111111-1111-1111-1111-111111111111",
                  "full_name":"Ada Lovelace",
                  "headline":"Engineer",
                  "avatar_url":"https://cdn.example/ada.png",
                  "expertise_tags":["AI","Math"],
                  "total_count":3,
                  "favorited_at":"2026-01-02T03:04:05Z",
                  "avg_rating":4.5,
                  "total_reviews":12,
                  "is_favorite":true
                }],"pagination":{"current_page":1,"total_pages":1,"has_more":false}}}
                """.trimIndent(),
            )

            val row = repository.list().experts.single()

            assertEquals("11111111-1111-1111-1111-111111111111", row.expertId)
            assertEquals("Ada Lovelace", row.fullName)
            assertEquals("Engineer", row.headline)
            assertEquals("https://cdn.example/ada.png", row.avatarUrl)
            assertEquals(listOf("AI", "Math"), row.expertiseTags)
        }

    @Test
    fun listDecodesNullFields() =
        runTest {
            enqueue(200, """{"status":"success","data":{"experts":[{"expert_id":"e9"}]}}""")

            val row = repository.list().experts.single()

            assertEquals("e9", row.expertId)
            assertNull(row.fullName)
            assertNull(row.headline)
            assertNull(row.avatarUrl)
            assertNull(row.expertiseTags)

            enqueue(200, """{"status":"success","data":{"experts":[]}}""")

            assertTrue(repository.list().experts.isEmpty())
        }

    @Test
    fun addSendsPutWithEmptyBody() =
        runTest {
            enqueue(200, """{"status":"success","data":{"expert_id":"$EXPERT_ID","is_favorite":true}}""")

            val result = repository.add(EXPERT_ID)

            val request = server.takeRequest()
            assertEquals("PUT", request.method)
            assertEquals("/api/v1/client/favorites/$EXPERT_ID", request.url.encodedPath)
            assertEquals(0, request.body?.size ?: 0)
            assertEquals("0", request.headers["Content-Length"])
            assertEquals(ToggleFavoriteData(expertId = EXPERT_ID, isFavorite = true), result)
        }

    @Test
    fun removeSendsDelete() =
        runTest {
            enqueue(200, """{"status":"success","data":{"expert_id":"$EXPERT_ID","is_favorite":false}}""")

            val result = repository.remove(EXPERT_ID)

            val request = server.takeRequest()
            assertEquals("DELETE", request.method)
            assertEquals("/api/v1/client/favorites/$EXPERT_ID", request.url.encodedPath)
            assertEquals(ToggleFavoriteData(expertId = EXPERT_ID, isFavorite = false), result)
        }

    @Test
    fun errorCarriesServerMessage() =
        runTest {
            enqueue(403, """{"status":"error","code":"FORBIDDEN","message":"Client role required"}""")

            try {
                repository.add(EXPERT_ID)
                fail("expected ApiException.Http")
            } catch (e: ApiException.Http) {
                assertEquals(403, e.status)
                assertEquals("Client role required", e.userMessage)
            }
        }

    @Test
    fun detailsDecodesIsFavorite() =
        runTest {
            val profile = ExpertProfileRepository(api)

            enqueue(200, """{"status":"success","data":{"is_favorite":true,"profile":{"name":"Ada"}}}""")
            assertTrue(profile.details("e1").isFavorite)
            assertEquals("/api/v1/experts/e1/details", server.takeRequest().url.encodedPath)

            enqueue(200, """{"status":"success","data":{"is_favorite":false,"profile":{"name":"Ada"}}}""")
            assertFalse(profile.details("e1").isFavorite)

            enqueue(200, """{"status":"success","data":{"profile":{"name":"Ada"}}}""")
            assertFalse(profile.details("e1").isFavorite)
        }

    @Test
    fun failureMappingUsesServerMessageThenFallback() {
        val withMessage = ApiException.Http(403, null, "FORBIDDEN", "Client role required", emptyList(), null)
        val withoutMessage = ApiException.Http(500, null, null, null, emptyList(), null)
        val fallback = FieldError.Res(R.string.favorites_update_failed)

        assertEquals(FieldError.Text("Client role required"), withMessage.toFavoriteFailure())
        assertEquals(fallback, withoutMessage.toFavoriteFailure())
        assertEquals(fallback, ApiException.Network(IOException("offline")).toFavoriteFailure())
        assertEquals(fallback, ApiException.Decoding(IllegalStateException("bad json")).toFavoriteFailure())
    }

    private companion object {
        const val EXPERT_ID = "0f1e2d3c-0000-4000-8000-000000000001"
    }
}
