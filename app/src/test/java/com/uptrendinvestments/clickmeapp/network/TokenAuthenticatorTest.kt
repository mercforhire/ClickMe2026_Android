@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.managers.SessionEvent
import com.uptrendinvestments.clickmeapp.managers.SessionEvents
import com.uptrendinvestments.clickmeapp.managers.TokenPair
import com.uptrendinvestments.clickmeapp.testing.FakeSecureTokenStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class TokenAuthenticatorTest {
    private lateinit var server: MockWebServer
    private val refreshCount = AtomicInteger()
    private val meCount = AtomicInteger()
    private val refreshBodies = CopyOnWriteArrayList<String>()
    private val refreshAuthHeaders = CopyOnWriteArrayList<String?>()
    private val staleStoreOnRetry = AtomicBoolean(false)

    @Volatile
    private var refreshResponse: () -> MockResponse = { successRefresh() }

    @Volatile
    private var retryAlways401: Boolean = false

    private lateinit var store: FakeSecureTokenStore
    private val events = SessionEvents()

    private fun json(
        status: Int,
        body: String,
    ) = MockResponse
        .Builder()
        .code(status)
        .addHeader("Content-Type", "application/json")
        .body(body)
        .build()

    private fun successRefresh() =
        MockResponse
            .Builder()
            .code(200)
            .addHeader("Content-Type", "application/json")
            .body(
                """{"status":"success","data":{"token":"new","refresh_token":"ref2",""" +
                    """"expires_at":1900000000,"expires_in":3600}}""",
            ).headersDelay(300, TimeUnit.MILLISECONDS)
            .build()

    private val invalidToken = """{"status":"error","code":"INVALID_TOKEN","message":"Invalid token."}"""

    @Before
    fun setUp() {
        server = MockWebServer()
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val path = request.url.encodedPath
                    val bearer = request.headers["Authorization"]
                    return when {
                        path.endsWith("/auth/refresh") -> {
                            refreshCount.incrementAndGet()
                            refreshBodies += request.body?.utf8().orEmpty()
                            refreshAuthHeaders += bearer
                            refreshResponse()
                        }
                        path.endsWith("/auth/login") ->
                            json(401, """{"status":"error","code":"INVALID_CREDENTIALS","message":"Bad credentials."}""")
                        path.endsWith("/user/account") ->
                            if (bearer == "Bearer new") {
                                json(401, """{"status":"error","message":"Invalid password."}""")
                            } else {
                                json(401, invalidToken)
                            }
                        path.endsWith("/me") -> {
                            meCount.incrementAndGet()
                            when {
                                retryAlways401 -> json(401, invalidToken)
                                bearer == "Bearer new" -> {
                                    // The rotated pair must already be persisted when the retry arrives.
                                    if (store.accessToken != "new") staleStoreOnRetry.set(true)
                                    json(200, """{"status":"success"}""")
                                }
                                else -> json(401, invalidToken)
                            }
                        }
                        else -> json(404, "{}")
                    }
                }
            }
        server.start()
        store = FakeSecureTokenStore(TokenPair("old", "ref1", 100L))
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun baseUrl() = server.url("/api/v1").toString()

    private fun client(refresherBaseUrl: String = baseUrl()): OkHttpClient {
        val refresher = TokenRefresher(OkHttpClient(), refresherBaseUrl, MoshiProvider.moshi)
        return OkHttpClient
            .Builder()
            .addInterceptor(AuthHeaderInterceptor(store))
            .authenticator(TokenAuthenticator(store, refresher, events))
            .build()
    }

    private fun get(
        client: OkHttpClient,
        path: String = "/api/v1/me",
        header: String? = null,
    ): Int {
        val builder = Request.Builder().url(server.url(path))
        if (header != null) builder.header("Authorization", header)
        return client.newCall(builder.build()).execute().use { it.code }
    }

    private fun TestScope.collectEvents(): MutableList<SessionEvent> {
        val seen = CopyOnWriteArrayList<SessionEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { events.events.collect { seen += it } }
        return seen
    }

    @Test
    fun `twelve concurrent 401s cause exactly one refresh and all succeed`() =
        runTest {
            val seen = collectEvents()
            val client = client()
            val pool = Executors.newFixedThreadPool(12)
            val gate = CountDownLatch(1)
            val futures =
                (1..12).map {
                    pool.submit<Int> {
                        gate.await()
                        get(client)
                    }
                }
            gate.countDown()
            val codes = futures.map { it.get(30, TimeUnit.SECONDS) }
            pool.shutdown()

            assertEquals(List(12) { 200 }, codes)
            assertEquals(1, refreshCount.get())
            assertEquals(1, store.saveCount)
            assertEquals(0, store.clearCount)
            assertEquals(listOf<SessionEvent>(SessionEvent.Refreshed), seen)
            assertFalse("rotated pair must be saved before the retry", staleStoreOnRetry.get())
            assertEquals("new", store.accessToken)
            assertEquals("ref2", store.refreshToken)
        }

    @Test
    fun `late 401 with a superseded token retries with the current token and does not refresh again`() {
        val client = client()
        assertEquals(200, get(client))
        assertEquals(1, refreshCount.get())

        assertEquals(200, get(client, header = "Bearer old"))

        assertEquals(1, refreshCount.get())
        assertEquals(1, store.saveCount)
    }

    @Test
    fun `refresh request carries the stored refresh token and no Authorization header`() {
        assertEquals(200, get(client()))

        assertEquals(1, refreshBodies.size)
        assertEquals("""{"refresh_token":"ref1"}""", refreshBodies.single())
        assertNull(refreshAuthHeaders.single())
    }

    @Test
    fun `rotated pair is in the store before the retried request reaches the server`() {
        assertEquals(200, get(client()))

        assertFalse(staleStoreOnRetry.get())
        assertEquals("new", store.accessToken)
        assertEquals(1_900_000_000L, store.expiresAt)
    }

    @Test
    fun `401 on login returns null with no refresh and the 401 reaches the caller`() {
        val code = get(client(), path = "/api/v1/auth/login")

        assertEquals(401, code)
        assertEquals(0, refreshCount.get())
        assertEquals(0, store.clearCount)
    }

    @Test
    fun `refresh answered 401 clears the store emits Rejected and surfaces the original 401`() =
        runTest {
            val seen = collectEvents()
            refreshResponse = { json(401, """{"status":"error","code":"INVALID_REFRESH_TOKEN","message":"no"}""") }

            assertEquals(401, get(client()))

            assertEquals(1, refreshCount.get())
            assertEquals(1, store.clearCount)
            assertFalse(store.hasSession())
            assertEquals(listOf<SessionEvent>(SessionEvent.Rejected), seen)
        }

    @Test
    fun `refresh answered 500 keeps tokens emits nothing and a later request tries again`() =
        runTest {
            val seen = collectEvents()
            refreshResponse = { json(500, """{"status":"error","code":"INTERNAL_ERROR","message":"boom"}""") }
            val client = client()

            assertEquals(401, get(client))
            assertEquals(0, store.clearCount)
            assertEquals(0, store.saveCount)
            assertTrue(seen.isEmpty())
            assertEquals("old", store.accessToken)

            assertEquals(401, get(client))
            assertEquals(2, refreshCount.get())
        }

    @Test
    fun `refresh answered 429 keeps tokens and emits nothing`() =
        runTest {
            val seen = collectEvents()
            refreshResponse = { json(429, """{"status":"error","code":"RATE_LIMITED","message":"slow down"}""") }

            assertEquals(401, get(client()))

            assertEquals(0, store.clearCount)
            assertEquals(0, store.saveCount)
            assertTrue(seen.isEmpty())
            assertTrue(store.hasSession())
        }

    @Test
    fun `refresh failing with an IO error keeps tokens and emits nothing`() =
        runTest {
            val seen = collectEvents()
            // Port 1 on loopback refuses connections, so the refresh call throws IOException.
            val client = client(refresherBaseUrl = "http://127.0.0.1:1/api/v1")

            assertEquals(401, get(client))

            assertEquals(0, refreshCount.get())
            assertEquals(0, store.clearCount)
            assertEquals(0, store.saveCount)
            assertTrue(seen.isEmpty())
            assertEquals("ref1", store.refreshToken)
        }

    @Test
    fun `access token without a refresh token clears the session and never calls refresh`() =
        runTest {
            val seen = collectEvents()
            store.setAccessOnly("old")

            assertEquals(401, get(client()))

            assertEquals(0, refreshCount.get())
            assertEquals(1, store.clearCount)
            assertEquals(listOf<SessionEvent>(SessionEvent.Rejected), seen)
        }

    @Test
    fun `retried request that 401s again with INVALID_TOKEN clears the session`() =
        runTest {
            val seen = collectEvents()
            retryAlways401 = true

            assertEquals(401, get(client()))

            assertEquals(1, refreshCount.get())
            assertEquals(2, meCount.get())
            assertEquals(1, store.saveCount)
            assertEquals(1, store.clearCount)
            assertFalse(store.hasSession())
            assertEquals(listOf<SessionEvent>(SessionEvent.Refreshed, SessionEvent.Rejected), seen)
        }

    @Test
    fun `code-less business 401 after a successful refresh keeps the new pair`() =
        runTest {
            val seen = collectEvents()
            val request =
                Request
                    .Builder()
                    .url(server.url("/api/v1/user/account"))
                    .delete()
                    .build()

            val code = client().newCall(request).execute().use { it.code }

            assertEquals(401, code)
            assertEquals(1, refreshCount.get())
            assertEquals(0, store.clearCount)
            assertEquals("new", store.accessToken)
            assertEquals(listOf<SessionEvent>(SessionEvent.Refreshed), seen)
        }

    @Test
    fun `401 with an already empty store returns null and emits nothing`() =
        runTest {
            val seen = collectEvents()
            store.clear()

            assertEquals(401, get(client()))

            assertEquals(0, refreshCount.get())
            assertTrue(seen.isEmpty())
        }
}
