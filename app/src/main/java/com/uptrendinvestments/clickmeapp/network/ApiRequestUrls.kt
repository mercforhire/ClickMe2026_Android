package com.uptrendinvestments.clickmeapp.network

/**
 * Path constants (relative to the base URL, which ends in /api/v1) and the bearer allowlist.
 * Port of iOS APIRequestURLs.needAuthToken, fixed to test the encoded path rather than the full URL
 * string so a query string cannot change the answer. Phase 4 adds the signup, email-verification, expert
 * setup, meta and avatar groups. Phase 5 adds the discovery group (home, client bookings, categories, expert
 * details and reviews).
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

    // Phase 4: signup, email verification, expert setup, meta lookups, avatar.
    const val SIGNUP = "auth/signup"
    const val EMAIL_RESEND = "auth/email/resend"
    const val EMAIL_VERIFY = "auth/email/verify"
    const val EMAIL_STATUS = "auth/email/status"
    const val EXPERT_PROFILE_SETUP = "expert/profile/setup"
    const val META_LANGUAGES = "meta/languages"
    const val META_EXPERTISE_TAGS = "meta/expertise-tags"
    const val USER_AVATAR = "user/profile/avatar"

    // Phase 5: discovery (home, client bookings, categories, expert details/reviews).
    const val CLIENT_HOME = "client/home"
    const val CLIENT_BOOKINGS = "client/bookings"
    const val CATEGORIES = "categories"
    const val EXPERT_DETAILS = "experts/{id}/details"
    const val EXPERT_REVIEWS = "experts/{id}/reviews"

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
