package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.managers.SecureTokenStore
import com.uptrendinvestments.clickmeapp.managers.SessionEvent
import com.uptrendinvestments.clickmeapp.managers.SessionEvents
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * The one 401 handler in the app: single-flight, retry-once token refresh (FND-05).
 *
 * Reactive only (D-04): it runs because a request got a 401 and never reads the token expiry to refresh
 * early. A [Mutex] serializes callers; inside it the token the failed request carried (`sent`) is compared
 * with the stored one, so a request that 401s after another caller already refreshed is simply retried with
 * the current token instead of refreshing again. (iOS's actor releases its in-flight task after completion,
 * which leaves exactly that late-401 gap.) The rotated pair is persisted before the lock is released and
 * before the retry is built, because the backend revokes the session if a refresh token is reused (T-02-18).
 *
 * Session clearing (D-03): only a 401 from /auth/refresh, a missing refresh token, or a token-coded 401 on
 * the retry that follows a successful refresh. Network errors, 429 and 5xx keep the tokens and let the
 * original 401 reach the caller. The code check on the retry matters because controller 401s such as
 * DELETE /user/account "Invalid password." carry no code and must not log the user out.
 */
class TokenAuthenticator(
    private val tokens: SecureTokenStore,
    private val refresher: TokenRefresher,
    private val sessionEvents: SessionEvents,
) : Authenticator {
    private val mutex = Mutex()

    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? {
        // A 401 on a public path (login) means bad credentials, not an expired token.
        if (!ApiRequestUrls.needAuthToken(response.request.url.encodedPath)) return null
        val sent = response.request.header("Authorization")?.removePrefix("Bearer ")
        // OkHttp calls authenticate on a blocking thread.
        return runBlocking {
            mutex.withLock { resolve(response, sent) }
        }
    }

    private fun resolve(
        response: Response,
        sent: String?,
    ): Request? {
        if (response.priorResponse != null) {
            // This 401 answers our own retry: never retry twice.
            val code = ApiErrorParser.codeOf(runCatching { response.peekBody(PEEK_LIMIT_BYTES).string() }.getOrNull())
            if (sent != null && sent == tokens.accessToken && code?.isTokenError == true) rejectSession()
            return null
        }
        val current = tokens.accessToken
        if (current != null && current != sent) return response.retryWith(current)
        val refresh = tokens.refreshToken
        if (refresh == null) {
            rejectSession()
            return null
        }
        return when (val result = refresher.refresh(refresh)) {
            is RefreshResult.Success -> {
                tokens.save(result.tokens)
                sessionEvents.emit(SessionEvent.Refreshed)
                response.retryWith(result.tokens.accessToken)
            }
            RefreshResult.Rejected -> {
                rejectSession()
                null
            }
            RefreshResult.Transient -> null
        }
    }

    private fun rejectSession() {
        val had = tokens.hasSession()
        tokens.clear()
        if (had) sessionEvents.emit(SessionEvent.Rejected)
    }

    private fun Response.retryWith(token: String): Request =
        request
            .newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

    private companion object {
        const val PEEK_LIMIT_BYTES = 64L * 1024
    }
}
