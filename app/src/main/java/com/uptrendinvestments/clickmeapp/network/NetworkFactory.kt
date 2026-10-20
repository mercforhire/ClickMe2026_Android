package com.uptrendinvestments.clickmeapp.network

import com.squareup.moshi.Moshi
import com.uptrendinvestments.clickmeapp.managers.SecureTokenStore
import com.uptrendinvestments.clickmeapp.managers.SessionEvents
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Plain (non-Hilt) builders for the network stack, shared by `NetworkModule` and the tests.
 *
 * Timeouts match iOS: 15 s to connect, 60 s read/write (`timeoutIntervalForRequest = 60`).
 *
 * Debug interceptors are passed in rather than referenced here because the logging interceptor only exists
 * on the debug classpath.
 */
object NetworkFactory {
    const val CONNECT_TIMEOUT_SECONDS = 15L
    const val IO_TIMEOUT_SECONDS = 60L

    /**
     * Client used only by the token refresh: no authenticator and no bearer interceptor, so refreshing can
     * never recurse into [TokenAuthenticator]. [debugInterceptors] make the refresh call visible in debug logcat.
     */
    fun bareClient(debugInterceptors: List<Interceptor>): OkHttpClient =
        baseBuilder()
            .apply { debugInterceptors.forEach { addNetworkInterceptor(it) } }
            .build()

    /**
     * The authenticated client behind Retrofit.
     *
     * Order matters. [AuthHeaderInterceptor] then [SuspensionInterceptor] are application interceptors, so the
     * suspension trap sees the final response after any [TokenAuthenticator] retry. A 403 never reaches the
     * Authenticator (only 401s do), which keeps the suspension check ahead of the refresh path (D-08).
     * Debug logging is a network interceptor so each attempt, including the Authenticator's retry, is logged once.
     */
    fun apiClient(
        tokens: SecureTokenStore,
        sessionEvents: SessionEvents,
        refresher: TokenRefresher,
        debugInterceptors: List<Interceptor>,
    ): OkHttpClient =
        baseBuilder()
            .addInterceptor(AuthHeaderInterceptor(tokens))
            .addInterceptor(SuspensionInterceptor(tokens, sessionEvents))
            .apply { debugInterceptors.forEach { addNetworkInterceptor(it) } }
            .authenticator(TokenAuthenticator(tokens, refresher, sessionEvents))
            .build()

    /** Retrofit over [client]; a trailing slash is appended to [baseUrl] when missing (Retrofit requires one). */
    fun retrofit(
        baseUrl: String,
        client: OkHttpClient,
        moshi: Moshi = MoshiProvider.moshi,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    private fun baseBuilder(): OkHttpClient.Builder =
        OkHttpClient
            .Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(IO_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(IO_TIMEOUT_SECONDS, TimeUnit.SECONDS)
}
