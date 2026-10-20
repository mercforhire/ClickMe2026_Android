package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.managers.SecureTokenStore
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds the bearer to authed paths only. The five public paths never carry it (T-02-19), and an
 * Authorization header already on the request (the Authenticator's retry) is left unchanged.
 */
class AuthHeaderInterceptor(
    private val tokens: SecureTokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val outgoing =
            when {
                !ApiRequestUrls.needAuthToken(request.url.encodedPath) ->
                    request.newBuilder().removeHeader(AUTHORIZATION).build()
                request.header(AUTHORIZATION) != null -> request
                else ->
                    tokens.accessToken
                        ?.let { request.newBuilder().header(AUTHORIZATION, "Bearer $it").build() }
                        ?: request
            }
        return chain.proceed(outgoing)
    }

    private companion object {
        const val AUTHORIZATION = "Authorization"
    }
}
