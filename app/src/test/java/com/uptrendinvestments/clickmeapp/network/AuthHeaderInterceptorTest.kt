package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.managers.TokenPair
import com.uptrendinvestments.clickmeapp.testing.FakeSecureTokenStore
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthHeaderInterceptorTest {
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

    private fun clientFor(store: FakeSecureTokenStore) = OkHttpClient.Builder().addInterceptor(AuthHeaderInterceptor(store)).build()

    private fun call(
        client: OkHttpClient,
        path: String,
        preset: String? = null,
    ): String? {
        server.enqueue(MockResponse.Builder().code(200).body("{}").build())
        val builder = Request.Builder().url(server.url(path))
        if (preset != null) builder.header("Authorization", preset)
        client.newCall(builder.build()).execute().close()
        return server.takeRequest().headers["Authorization"]
    }

    private val pair = TokenPair("abc", "ref", 1_900_000_000L)

    @Test
    fun `adds bearer to an authed path`() {
        val header = call(clientFor(FakeSecureTokenStore(pair)), "/api/v1/me")
        assertEquals("Bearer abc", header)
    }

    @Test
    fun `no bearer on login even when a token exists`() {
        assertNull(call(clientFor(FakeSecureTokenStore(pair)), "/api/v1/auth/login"))
    }

    @Test
    fun `no bearer on refresh even when a token exists`() {
        assertNull(call(clientFor(FakeSecureTokenStore(pair)), "/api/v1/auth/refresh"))
    }

    @Test
    fun `no bearer on forgot password even when a token exists`() {
        assertNull(call(clientFor(FakeSecureTokenStore(pair)), "/api/v1/auth/password/forgot"))
    }

    @Test
    fun `public path strips a preset Authorization header`() {
        assertNull(call(clientFor(FakeSecureTokenStore(pair)), "/api/v1/auth/login", preset = "Bearer stale"))
    }

    @Test
    fun `no header when the store has no access token`() {
        assertNull(call(clientFor(FakeSecureTokenStore()), "/api/v1/me"))
    }

    @Test
    fun `an already present Authorization header is left unchanged`() {
        assertEquals("Bearer custom", call(clientFor(FakeSecureTokenStore(pair)), "/api/v1/me", preset = "Bearer custom"))
    }

    @Test
    fun `query string does not flip a public path to authed`() {
        assertNull(call(clientFor(FakeSecureTokenStore(pair)), "/api/v1/auth/login?next=1"))
    }

    @Test
    fun `fake store hasSession is true with only a refresh token and false when empty`() {
        val store = FakeSecureTokenStore()
        assertFalse(store.hasSession())
        store.setRefreshOnly("r")
        assertTrue(store.hasSession())
        store.save(TokenPair("a", "r", 1L))
        assertTrue(store.hasSession())
        store.setAccessOnly("a")
        assertTrue(store.hasSession())
        store.clear()
        assertFalse(store.hasSession())
    }
}
