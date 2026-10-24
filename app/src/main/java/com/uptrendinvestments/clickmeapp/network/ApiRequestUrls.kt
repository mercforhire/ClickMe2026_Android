package com.uptrendinvestments.clickmeapp.network

/**
 * Path constants (relative to the base URL, which ends in /api/v1) and the bearer allowlist.
 * Port of iOS APIRequestURLs.needAuthToken, fixed to test the encoded path rather than the full URL
 * string so a query string cannot change the answer. Phase 4 adds the signup, email-verification, expert
 * setup, meta and avatar groups. Phase 5 adds the discovery group (home, client bookings, categories, expert
 * details and reviews). Phase 6 adds the booking group (availability, booking request, booking detail). Phase 7 adds the payment
 * group (saved cards, setup and payment intents, booking confirm). Phase 8 adds the client booking actions
 * (cancel, accept and decline a reschedule). Phase 9 adds the call group (join, confirm connection, end).
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

    // Phase 6: booking (availability, free booking request, client booking detail).
    const val EXPERT_AVAILABILITY = "experts/{id}/availability"
    const val BOOKING_REQUEST = "bookings/request"
    const val CLIENT_BOOKING_DETAIL = "client/bookings/{id}"

    // Phase 7: payment methods and paid booking.
    const val PAYMENT_METHODS = "me/payment-methods"
    const val PAYMENT_METHODS_SETUP_INTENT = "me/payment-methods/setup-intent"
    const val PAYMENT_METHOD = "me/payment-methods/{id}"
    const val PAYMENT_METHOD_DEFAULT = "me/payment-methods/{id}/default"
    const val PAYMENT_INTENT = "bookings/payment-intent"
    const val BOOKING_CONFIRM = "bookings/confirm"

    // Phase 8: client booking management (cancel, reschedule response).
    const val CLIENT_BOOKING_CANCEL = "client/bookings/{id}/cancel"
    const val CLIENT_BOOKING_ACCEPT_RESCHEDULE = "client/bookings/{id}/accept-reschedule"
    const val CLIENT_BOOKING_DECLINE_RESCHEDULE = "client/bookings/{id}/decline-reschedule"

    // Phase 9: calls (join, confirm connection, end).
    const val BOOKING_JOIN = "bookings/{id}/join"
    const val BOOKING_CONFIRM_CONNECTION = "bookings/{id}/confirm-connection"
    const val BOOKING_END = "bookings/{id}/end"

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
