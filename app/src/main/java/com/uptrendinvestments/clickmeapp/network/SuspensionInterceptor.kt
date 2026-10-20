package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.managers.SecureTokenStore
import com.uptrendinvestments.clickmeapp.managers.SessionEvent
import com.uptrendinvestments.clickmeapp.managers.SessionEvents
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Global ACCOUNT_SUSPENDED trap (FND-06, D-08). A 403 with that code, on any path including /auth/login,
 * clears the tokens synchronously and raises [SessionEvent.Suspended]. The response is always returned
 * untouched so screens can still react to the error, as iOS does. 403 never reaches the Authenticator
 * (401 only), so the trap runs before any refresh by construction. Idempotency of the user-visible handling
 * (iOS `guard !wasSuspended`) lives in UserManager.
 */
class SuspensionInterceptor(
    private val tokens: SecureTokenStore,
    private val sessionEvents: SessionEvents,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 403) {
            val body = runCatching { response.peekBody(PEEK_LIMIT_BYTES).string() }.getOrNull()
            if (ApiErrorParser.codeOf(body) == ErrorCode.AccountSuspended) {
                tokens.clear()
                sessionEvents.emit(SessionEvent.Suspended)
            }
        }
        return response
    }

    private companion object {
        const val PEEK_LIMIT_BYTES = 64L * 1024
    }
}
