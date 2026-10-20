package com.uptrendinvestments.clickmeapp.managers

/**
 * The JWT pair the backend issues. [expiresAt] is epoch seconds; it is stored for diagnostics only and is
 * never used to refresh early (D-04: refresh is reactive, triggered by a 401).
 */
data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
)

/**
 * Durable storage for the session tokens.
 *
 * Synchronous by design: the OkHttp Authenticator persists a rotated pair inside its critical section on a
 * blocking OkHttp thread, and that write must not be cancellable (a cancelled caller would otherwise lose
 * the new refresh token, RESEARCH Pitfall 3). Callers on the main thread must hop to an IO dispatcher.
 */
interface SecureTokenStore {
    val accessToken: String?
    val refreshToken: String?
    val expiresAt: Long?

    /** Persists the pair durably before returning. */
    fun save(tokens: TokenPair)

    /** Removes every token before returning. */
    fun clear()

    fun hasSession(): Boolean = accessToken != null || refreshToken != null
}
