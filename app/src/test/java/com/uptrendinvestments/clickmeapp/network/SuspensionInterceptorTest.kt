@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.managers.SessionEvent
import com.uptrendinvestments.clickmeapp.managers.SessionEvents
import com.uptrendinvestments.clickmeapp.managers.TokenPair
import com.uptrendinvestments.clickmeapp.testing.FakeSecureTokenStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SuspensionInterceptorTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    private val pair = TokenPair("abc", "ref", 1_900_000_000L)
    private val suspendedBody =
        """{"status":"error","code":"ACCOUNT_SUSPENDED","message":"This account has been suspended."}"""

    private data class Result(
        val code: Int,
        val body: String,
    )

    private fun fire(
        client: OkHttpClient,
        path: String,
        method: String = "GET",
        status: Int,
        body: String,
    ): Result {
        server.enqueue(MockResponse.Builder().code(status).body(body).build())
        val builder = Request.Builder().url(server.url(path))
        if (method == "POST") builder.post("{}".toRequestBody()) else builder.get()
        client.newCall(builder.build()).execute().use { return Result(it.code, it.body.string()) }
    }

    private fun clientFor(
        store: FakeSecureTokenStore,
        events: SessionEvents,
    ) = OkHttpClient.Builder().addInterceptor(SuspensionInterceptor(store, events)).build()

    @Test
    fun `403 ACCOUNT_SUSPENDED clears tokens emits Suspended once and caller still sees the 403 body`() =
        runTest {
            val store = FakeSecureTokenStore(pair)
            val events = SessionEvents()
            val seen = mutableListOf<SessionEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { events.events.collect { seen += it } }

            val result = fire(clientFor(store, events), "/api/v1/me", status = 403, body = suspendedBody)

            assertEquals(403, result.code)
            assertEquals(suspendedBody, result.body)
            assertEquals(1, store.clearCount)
            assertEquals(false, store.hasSession())
            assertEquals(listOf<SessionEvent>(SessionEvent.Suspended), seen)
        }

    @Test
    fun `403 ACCOUNT_SUSPENDED on login also emits Suspended`() =
        runTest {
            val store = FakeSecureTokenStore()
            val events = SessionEvents()
            val seen = mutableListOf<SessionEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { events.events.collect { seen += it } }

            val result =
                fire(clientFor(store, events), "/api/v1/auth/login", method = "POST", status = 403, body = suspendedBody)

            assertEquals(403, result.code)
            assertEquals(listOf<SessionEvent>(SessionEvent.Suspended), seen)
        }

    @Test
    fun `403 with another code emits nothing and keeps tokens`() =
        runTest {
            val store = FakeSecureTokenStore(pair)
            val events = SessionEvents()
            val seen = mutableListOf<SessionEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { events.events.collect { seen += it } }

            fire(clientFor(store, events), "/api/v1/me", status = 403, body = """{"status":"error","code":"FORBIDDEN","message":"no"}""")

            assertTrue(seen.isEmpty())
            assertEquals(0, store.clearCount)
            assertTrue(store.hasSession())
        }

    @Test
    fun `403 with an HTML body emits nothing and keeps tokens`() =
        runTest {
            val store = FakeSecureTokenStore(pair)
            val events = SessionEvents()
            val seen = mutableListOf<SessionEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { events.events.collect { seen += it } }

            val result = fire(clientFor(store, events), "/api/v1/me", status = 403, body = "<html>Forbidden</html>")

            assertEquals("<html>Forbidden</html>", result.body)
            assertTrue(seen.isEmpty())
            assertEquals(0, store.clearCount)
        }

    @Test
    fun `401 emits nothing and keeps tokens`() =
        runTest {
            val store = FakeSecureTokenStore(pair)
            val events = SessionEvents()
            val seen = mutableListOf<SessionEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { events.events.collect { seen += it } }

            fire(clientFor(store, events), "/api/v1/me", status = 401, body = suspendedBody)

            assertTrue(seen.isEmpty())
            assertEquals(0, store.clearCount)
        }
}
