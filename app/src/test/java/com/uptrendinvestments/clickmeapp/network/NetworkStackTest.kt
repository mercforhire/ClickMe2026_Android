@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.managers.SessionEvent
import com.uptrendinvestments.clickmeapp.managers.SessionEvents
import com.uptrendinvestments.clickmeapp.managers.TokenPair
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.model.requests.LoginRequest
import com.uptrendinvestments.clickmeapp.testing.FakeSecureTokenStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import java.util.concurrent.CopyOnWriteArrayList

/** The assembled stack (NetworkFactory + transport classes + Retrofit) against MockWebServer, no Hilt. */
class NetworkStackTest {
    private lateinit var server: MockWebServer
    private lateinit var store: FakeSecureTokenStore
    private lateinit var api: ClickMeApi
    private val events = SessionEvents()
    private val recorded = CopyOnWriteArrayList<RecordedRequest>()

    @Volatile
    private var meResponse: (RecordedRequest) -> MockResponse = { me200() }

    @Volatile
    private var refreshResponse: () -> MockResponse = { refresh200() }

    private fun json(
        code: Int,
        body: String,
    ) = MockResponse
        .Builder()
        .code(code)
        .addHeader("Content-Type", "application/json")
        .body(body)
        .build()

    private fun me200() = json(200, """{"status":"success","data":{"id":"u1","email":"a@b.co","roles":["client"]}}""")

    private fun refresh200() =
        json(
            200,
            """{"status":"success","data":{"token":"new","refresh_token":"r2",""" +
                """"expires_at":1900000000,"expires_in":3600}}""",
        )

    private fun invalidToken() = json(401, """{"status":"error","code":"INVALID_TOKEN","message":"Invalid token."}""")

    private fun loginOk() =
        json(
            200,
            """{"status":"success","data":{"token":"t","refresh_token":"r","expires_at":1792513029,"expires_in":900,""" +
                """"user":{"id":"u1","first_name":"Ada","last_name":"L","roles":["client"]}}}""",
        )

    private fun startStack(initial: TokenPair?) {
        server = MockWebServer()
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    recorded += request
                    return when (request.url.encodedPath) {
                        "/api/v1/me" -> meResponse(request)
                        "/api/v1/auth/refresh" -> refreshResponse()
                        "/api/v1/auth/login" -> loginOk()
                        else -> json(404, "{}")
                    }
                }
            }
        server.start()
        store = FakeSecureTokenStore(initial)
        val baseUrl = server.url("/api/v1").toString()
        val bare = NetworkFactory.bareClient(emptyList())
        val refresher = TokenRefresher(bare, baseUrl, MoshiProvider.moshi)
        val client = NetworkFactory.apiClient(store, events, refresher, emptyList())
        api = NetworkFactory.retrofit(baseUrl, client).create(ClickMeApi::class.java)
    }

    @Before
    fun setUp() {
        recorded.clear()
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun refreshCalls() = recorded.count { it.url.encodedPath == "/api/v1/auth/refresh" }

    private val old = TokenPair("old", "r1", 1L)

    @Test
    fun expiredTokenRefreshesOnceAndReturnsDecodedMe() =
        runTest {
            startStack(old)
            meResponse = { request ->
                if (request.headers["Authorization"] == "Bearer new") me200() else invalidToken()
            }

            val me = api.getMe().data

            assertEquals("u1", me.id)
            assertEquals("a@b.co", me.email)
            assertEquals(1, refreshCalls())
            assertEquals("new", store.accessToken)
            assertEquals("r2", store.refreshToken)
        }

    @Test
    fun loginNeverCarriesBearerEvenWithStoredToken() =
        runTest {
            startStack(old)

            val response = api.login(LoginRequest("a@b.co", "pw12345678"))

            assertEquals("t", response.data.token)
            val request = recorded.single { it.url.encodedPath == "/api/v1/auth/login" }
            assertNull(request.headers["Authorization"])
            assertEquals("old", store.accessToken)
        }

    @Test
    fun suspendedAccountOnMeClearsTokensAndEmitsSuspended() =
        runTest {
            startStack(old)
            val seen = CopyOnWriteArrayList<SessionEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { events.events.collect { seen += it } }
            meResponse = {
                json(403, """{"status":"error","code":"ACCOUNT_SUSPENDED","message":"Account suspended."}""")
            }

            try {
                api.getMe()
                fail("expected HttpException")
            } catch (e: HttpException) {
                assertEquals(403, e.code())
                val error = ApiErrorParser.from(e) as ApiException.Http
                assertEquals(ErrorCode.AccountSuspended, error.code)
            }

            assertNull(store.accessToken)
            assertNull(store.refreshToken)
            assertEquals(listOf<SessionEvent>(SessionEvent.Suspended), seen.toList())
            assertEquals(0, refreshCalls())
        }

    @Test
    fun transientRefreshFailureSurfaces401AndKeepsTokens() =
        runTest {
            startStack(old)
            val seen = CopyOnWriteArrayList<SessionEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { events.events.collect { seen += it } }
            meResponse = { invalidToken() }
            refreshResponse = { json(503, """{"status":"error","message":"Unavailable"}""") }

            try {
                api.getMe()
                fail("expected HttpException")
            } catch (e: HttpException) {
                assertEquals(401, e.code())
            }

            assertEquals(1, refreshCalls())
            assertEquals("old", store.accessToken)
            assertEquals("r1", store.refreshToken)
            assertTrue(seen.isEmpty())
        }

    @Test
    fun retryRejectedWithTokenCodeAfterRefreshClearsSessionAndEmitsRejected() =
        runTest {
            startStack(old)
            val seen = CopyOnWriteArrayList<SessionEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { events.events.collect { seen += it } }
            meResponse = { invalidToken() }

            try {
                api.getMe()
                fail("expected HttpException")
            } catch (e: HttpException) {
                assertEquals(401, e.code())
            }

            assertEquals(1, refreshCalls())
            assertNull(store.accessToken)
            assertNull(store.refreshToken)
            assertEquals(listOf(SessionEvent.Refreshed, SessionEvent.Rejected), seen.toList())
        }
}
