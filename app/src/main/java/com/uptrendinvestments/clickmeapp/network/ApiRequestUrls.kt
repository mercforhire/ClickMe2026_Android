package com.uptrendinvestments.clickmeapp.network

/**
 * Path constants (relative to the base URL, which ends in /api/v1) and the bearer allowlist.
 * Port of iOS APIRequestURLs.needAuthToken, fixed to test the encoded path rather than the full URL
 * string so a query string cannot change the answer. Phase 4 adds the signup, email-verification, expert
 * setup, meta and avatar groups. Phase 5 adds the discovery group (home, client bookings, categories, expert
 * details and reviews). Phase 6 adds the booking group (availability, booking request, booking detail). Phase 7 adds the payment
 * group (saved cards, setup and payment intents, booking confirm). Phase 8 adds the client booking actions
 * (cancel, accept and decline a reschedule). Phase 9 adds the call group (join, confirm connection, end).
 * Phase 10 adds expert topics (create, update, delete) and the expert's own weekly availability. Phase 11 adds the
 * expert request, booking, earnings and payout groups (booking requests, expert bookings, weekly earnings, payout
 * summary and Stripe Connect). Phase 14 adds change password, account deletion, the help FAQ list, the earnings
 * transactions ledger and the expert tax profile. Phase 16.1 adds the expert search (Search tab). Phase 16.3 adds the
 * client favorites group. Phase 16.4 adds the booking note paths and the expert's client profile. Phase 16.5 adds the
 * client payment history, the notification preferences and the feedback paths.
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

    /** Ports iOS `ClickMeAPI+Discovery.searchExperts` (Phase 16.1, Search tab). Authed: not in [publicPaths]. */
    const val EXPERTS_SEARCH = "experts/search"

    /**
     * Phase 16.3: Saved Experts and the profile heart (PARITY-AUDIT #2, #2b). Authed: not in [publicPaths]. Add is
     * PUT on the backend (routes/favorites.js), not POST.
     */
    const val CLIENT_FAVORITES = "client/favorites"

    /** Add (PUT) and remove (DELETE) one favorite. See [CLIENT_FAVORITES]. */
    const val CLIENT_FAVORITE = "client/favorites/{expertId}"

    // Phase 16.4: private booking notes and the expert's view of a client (PARITY-AUDIT #7, #8, #9). Authed.
    const val CLIENT_BOOKING_NOTE = "client/bookings/{id}/note"
    const val BOOKING_NOTE = "bookings/{id}/note"
    const val EXPERT_CLIENT = "expert/clients/{id}"

    // Phase 16.5: client payment history, notification preferences and feedback (PARITY-AUDIT #4, #5, #6). Authed.
    const val CLIENT_TRANSACTIONS = "client/transactions"
    const val NOTIFICATION_PREFERENCES = "notifications/preferences"
    const val FEEDBACK_TYPES = "feedback/types"
    const val FEEDBACK_SUBMIT = "feedback/submit"

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

    // Phase 16.2: start a reschedule (PARITY-AUDIT #10 / #11).
    const val CLIENT_BOOKING_RESCHEDULE = "client/bookings/{id}/reschedule"

    // Phase 9: calls (join, confirm connection, end).
    const val BOOKING_JOIN = "bookings/{id}/join"
    const val BOOKING_CONFIRM_CONNECTION = "bookings/{id}/confirm-connection"
    const val BOOKING_END = "bookings/{id}/end"

    // Phase 10: expert topics and weekly availability.
    const val EXPERT_TOPIC = "expert/topics/{id}"

    /**
     * The expert's OWN weekly schedule. Not [EXPERT_AVAILABILITY], which is the public client-facing
     * `experts/{id}/availability` route (note the singular/plural difference).
     */
    const val EXPERT_MY_AVAILABILITY = "expert/availability"

    // Phase 11: expert requests, bookings, earnings and payouts.
    const val EXPERT_BOOKING_REQUESTS = "expert/booking-requests"
    const val EXPERT_BOOKING_REQUEST = "expert/booking-requests/{id}"
    const val EXPERT_BOOKING_REQUEST_ACCEPT = "expert/booking-requests/{id}/accept"
    const val EXPERT_BOOKING_REQUEST_DECLINE = "expert/booking-requests/{id}/decline"
    const val EXPERT_BOOKINGS = "expert/bookings"

    /**
     * The expert booking detail route ends in `/details`. The backend has no GET on the bare booking id, so a
     * request there is a 404 (RESEARCH C1).
     */
    const val EXPERT_BOOKING_DETAILS = "expert/bookings/{id}/details"
    const val EXPERT_BOOKING_CANCEL = "expert/bookings/{id}/cancel"
    const val EXPERT_BOOKING_ACCEPT_RESCHEDULE = "expert/bookings/{id}/accept-reschedule"
    const val EXPERT_BOOKING_DECLINE_RESCHEDULE = "expert/bookings/{id}/decline-reschedule"

    // Phase 16.2: start a reschedule (PARITY-AUDIT #10 / #11).
    const val EXPERT_BOOKING_RESCHEDULE = "expert/bookings/{id}/reschedule"
    const val EXPERT_EARNINGS_WEEKLY = "expert/earnings/weekly"
    const val EXPERT_PAYOUTS_SUMMARY = "expert/payouts/summary"
    const val EXPERT_CONNECT_STATUS = "expert/connect/status"
    const val EXPERT_CONNECT_ONBOARD = "expert/connect/onboard"
    const val EXPERT_CONNECT_REFRESH_LINK = "expert/connect/refresh-link"

    // Phase 12: chat and reporting.
    const val CHATS_INITIATE = "chats/initiate"
    const val CHATS = "chats"
    const val CHAT_MESSAGES = "chats/{id}/messages"
    const val CHAT_SEND = "chats/{id}/send"
    const val CHAT_ACTIONS = "chats/{id}/actions"
    const val USER_REPORT = "users/{id}/report"

    // Phase 13: push device tokens, the notifications list and post-session reviews.
    const val NOTIFICATION_TOKENS = "notifications/tokens"
    const val NOTIFICATION_TOKEN = "notifications/tokens/{device_id}"
    const val NOTIFICATIONS = "notifications"
    const val BOOKING_REVIEW_CONTEXT = "bookings/{id}/reviews/context"
    const val BOOKING_REVIEWS = "bookings/{id}/reviews"

    // Phase 14: account security, deletion, help, earnings ledger and tax profile.
    const val USER_PASSWORD = "user/password"
    const val USER_ACCOUNT_DELETE_REQUEST = "user/account/delete-request"
    const val USER_ACCOUNT = "user/account"
    const val HELP_FAQS = "help/faqs"
    const val EXPERT_EARNINGS_TRANSACTIONS = "expert/earnings/transactions"
    const val EXPERT_TAX_PROFILE = "expert/tax-profile"

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
