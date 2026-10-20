package com.uptrendinvestments.clickmeapp.network

import com.squareup.moshi.Moshi
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.model.responses.ApiErrorEnvelope
import retrofit2.HttpException
import java.io.IOException

/**
 * Lenient error-body parsing and Throwable to [ApiException] mapping. Bodies can be HTML from a proxy,
 * empty, or carry codes this build does not know; none of those may throw (T-02-06). Uses its own Moshi
 * so it does not depend on the shared provider.
 */
object ApiErrorParser {
    private val adapter = Moshi.Builder().build().adapter(ApiErrorEnvelope::class.java)

    /** Returns the decoded envelope, or null for a null, blank, non-JSON or wrongly shaped body. */
    fun parseEnvelope(body: String?): ApiErrorEnvelope? {
        if (body.isNullOrBlank()) return null
        return runCatching { adapter.fromJson(body) }.getOrNull()
    }

    /** The known [ErrorCode] in the body, or null when absent or unknown. */
    fun codeOf(body: String?): ErrorCode? = parseEnvelope(body)?.code?.let(ErrorCode::fromWire)

    fun httpError(
        status: Int,
        body: String?,
    ): ApiException.Http {
        val envelope = parseEnvelope(body)
        val rawCode = envelope?.code
        return ApiException.Http(
            status = status,
            code = ErrorCode.fromWire(rawCode),
            rawCode = rawCode,
            serverMessage = envelope?.message,
            fieldErrors = envelope?.errors.orEmpty(),
            detail = envelope?.detail,
        )
    }

    fun from(t: Throwable): ApiException =
        when (t) {
            is ApiException -> t
            is HttpException -> {
                val body = runCatching { t.response()?.errorBody()?.string() }.getOrNull()
                httpError(t.code(), body)
            }
            is IOException -> ApiException.Network(t)
            else -> ApiException.Decoding(t)
        }
}
