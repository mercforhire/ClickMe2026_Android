package com.uptrendinvestments.clickmeapp.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.uptrendinvestments.clickmeapp.managers.TokenPair
import com.uptrendinvestments.clickmeapp.model.requests.RefreshTokenRequest
import com.uptrendinvestments.clickmeapp.model.responses.RefreshTokenData
import com.uptrendinvestments.clickmeapp.model.responses.SuccessDataResponse
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/** Outcome of one refresh attempt. */
sealed interface RefreshResult {
    data class Success(
        val tokens: TokenPair,
    ) : RefreshResult

    /** The server refused the refresh token with HTTP 401: the session is over. */
    data object Rejected : RefreshResult

    /** Network failure, 429, 5xx or an undecodable body: keep the tokens and let the caller fail soft (D-03). */
    data object Transient : RefreshResult
}

/**
 * Calls POST auth/refresh on a bare [client]: no authenticator and no auth interceptor, so the refresh can
 * never recurse into [TokenAuthenticator]. Blocking by design (runs inside the Authenticator's critical
 * section on an OkHttp thread).
 *
 * An undecodable 2xx after the server already rotated is unrecoverable client-side (same residual risk as
 * iOS); the old pair is kept.
 */
class TokenRefresher(
    private val client: OkHttpClient,
    private val baseUrl: String,
    moshi: Moshi,
) {
    private val requestAdapter = moshi.adapter(RefreshTokenRequest::class.java)
    private val responseAdapter =
        moshi.adapter<SuccessDataResponse<RefreshTokenData>>(
            Types.newParameterizedType(SuccessDataResponse::class.java, RefreshTokenData::class.java),
        )

    fun refresh(refreshToken: String): RefreshResult {
        val url =
            baseUrl
                .toHttpUrl()
                .newBuilder()
                .addPathSegments(ApiRequestUrls.REFRESH)
                .build()
        val body = requestAdapter.toJson(RefreshTokenRequest(refreshToken)).toRequestBody(JSON)
        val request =
            Request
                .Builder()
                .url(url)
                .post(body)
                .build()
        return try {
            client.newCall(request).execute().use { response ->
                when {
                    response.isSuccessful -> decode(response.body.string())
                    response.code == 401 -> RefreshResult.Rejected
                    else -> RefreshResult.Transient
                }
            }
        } catch (_: IOException) {
            RefreshResult.Transient
        }
    }

    private fun decode(raw: String): RefreshResult {
        val data = runCatching { responseAdapter.fromJson(raw)?.data }.getOrNull() ?: return RefreshResult.Transient
        return RefreshResult.Success(TokenPair(data.token, data.refreshToken, data.expiresAt))
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
