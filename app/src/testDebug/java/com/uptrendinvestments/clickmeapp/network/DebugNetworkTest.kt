package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.managers.SessionEvents
import com.uptrendinvestments.clickmeapp.testing.FakeSecureTokenStore
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/** Debug source set: the real [DebugNetwork] logging interceptor plus the [NetworkFactory] client shapes. */
class DebugNetworkTest {
    private val store = FakeSecureTokenStore()
    private val events = SessionEvents()

    private fun refresher(client: OkHttpClient) = TokenRefresher(client, "https://x/api/v1", MoshiProvider.moshi)

    @Test
    fun debugInterceptorsIsExactlyOneBasicLogger() {
        val interceptors = DebugNetwork.interceptors()

        assertEquals(1, interceptors.size)
        val logging = interceptors.single() as HttpLoggingInterceptor
        assertEquals(HttpLoggingInterceptor.Level.BASIC, logging.level)
    }

    @Test
    fun bareClientHasNoAuthenticatorAndOnlyDebugInterceptors() {
        val debug = DebugNetwork.interceptors()

        val client = NetworkFactory.bareClient(debug)

        assertSame(Authenticator.NONE, client.authenticator)
        assertTrue(client.interceptors.isEmpty())
        assertEquals(debug, client.networkInterceptors)
        assertEquals(TimeUnit.SECONDS.toMillis(15), client.connectTimeoutMillis.toLong())
        assertEquals(TimeUnit.SECONDS.toMillis(60), client.readTimeoutMillis.toLong())
        assertEquals(TimeUnit.SECONDS.toMillis(60), client.writeTimeoutMillis.toLong())
    }

    @Test
    fun apiClientOrdersBearerBeforeSuspensionAndLogsAsNetworkInterceptors() {
        val debug = DebugNetwork.interceptors()
        val bare = NetworkFactory.bareClient(debug)

        val client = NetworkFactory.apiClient(store, events, refresher(bare), debug)

        assertEquals(2, client.interceptors.size)
        assertTrue(client.interceptors[0] is AuthHeaderInterceptor)
        assertTrue(client.interceptors[1] is SuspensionInterceptor)
        assertEquals(debug, client.networkInterceptors)
        assertTrue(client.authenticator is TokenAuthenticator)
        assertEquals(TimeUnit.SECONDS.toMillis(15), client.connectTimeoutMillis.toLong())
        assertEquals(TimeUnit.SECONDS.toMillis(60), client.readTimeoutMillis.toLong())
        assertEquals(TimeUnit.SECONDS.toMillis(60), client.writeTimeoutMillis.toLong())
    }

    @Test
    fun retrofitAppendsTrailingSlashOnlyWhenMissing() {
        val client = OkHttpClient()

        val without = NetworkFactory.retrofit("https://x/api/v1", client)
        val with = NetworkFactory.retrofit("https://x/api/v1/", client)

        assertEquals("https://x/api/v1/", without.baseUrl().toString())
        assertEquals("https://x/api/v1/", with.baseUrl().toString())
    }
}
