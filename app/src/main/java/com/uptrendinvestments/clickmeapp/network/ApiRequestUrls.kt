package com.uptrendinvestments.clickmeapp.network

/**
 * Path constants (relative to the base URL, which ends in /api/v1) and the bearer allowlist.
 * Port of iOS APIRequestURLs.needAuthToken, fixed to test the encoded path rather than the full URL
 * string so a query string cannot change the answer.
 */
object ApiRequestUrls {
    const val LOGIN = "auth/login"
    const val REFRESH = "auth/refresh"
    const val FORGOT_PASSWORD = "auth/password/forgot"
    const val RESET_PASSWORD = "auth/password/reset"
    const val ME = "me"
    const val USER_PROFILE = "user/profile"
    const val EXPERT_PROFILE = "expert/profile"
    const val EXPERT_TOPICS = "expert/topics"

    /**
     * The five credential-establishing paths that must never carry a bearer. /auth/refresh being public is
     * what stops the refresh from recursing into the authenticator. Everything else, including the email
     * verification endpoints (they use the bearer issued at signup), stays authed.
     */
    val publicPaths: Set<String> =
        setOf(
            "/auth/login",
            "/auth/signup",
            "/auth/refresh",
            "/auth/password/forgot",
            "/auth/password/reset",
        )

    /** [encodedPath] is `HttpUrl.encodedPath`, so it never includes a query string. */
    fun needAuthToken(encodedPath: String): Boolean = publicPaths.none { encodedPath.endsWith(it) }
}
