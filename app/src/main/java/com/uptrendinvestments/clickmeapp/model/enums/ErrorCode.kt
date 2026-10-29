package com.uptrendinvestments.clickmeapp.model.enums

/**
 * Canonical enumeration of SCREAMING_SNAKE_CASE error codes the ClickMe API can return.
 * Ported verbatim (the 65 iOS cases in iOS order, then Android-only additions) from the iOS
 * `ErrorCode.swift`; keep in sync when the backend adds codes. Parsing is lenient: an unknown wire value maps to null via [fromWire] so
 * server drift never blanks out the user-visible message.
 *
 * Notes:
 * - `INVALID_TOKEN` serves two HTTP contexts (401 JWT and 400 deletion-token); disambiguate by endpoint.
 * - `TOO_MANY_REQUESTS` (global rate limits) and `RATE_LIMITED` (`/auth/email/resend` only) are distinct
 *   codes with the same 429 semantics.
 */
enum class ErrorCode(
    val wire: String,
) {
    // Auth
    MissingToken("MISSING_TOKEN"),
    InvalidToken("INVALID_TOKEN"),
    InvalidCode("INVALID_CODE"), // /auth/email/verify + /auth/password/reset: wrong or expired 6-digit code
    AuthServiceUnavailable("AUTH_SERVICE_UNAVAILABLE"),
    ExpiredToken("EXPIRED_TOKEN"),
    EmailTaken("EMAIL_TAKEN"),

    // Generic authz / lookup
    Forbidden("FORBIDDEN"),
    NotFound("NOT_FOUND"),
    NoRelationship("NO_RELATIONSHIP"),

    // Validation
    ValidationError("VALIDATION_ERROR"), // 422 (or 400 on DELETE /user/account), field envelope
    ValidationFailed("VALIDATION_FAILED"), // 400, field envelope (password/forgot)
    InvalidInput("INVALID_INPUT"),
    InvalidMonth("INVALID_MONTH"),
    InvalidWeekStart("INVALID_WEEK_START"),

    // Bookings / sessions
    SlotUnavailable("SLOT_UNAVAILABLE"),
    DuplicateBooking("DUPLICATE_BOOKING"),
    InvalidStatus("INVALID_STATUS"),
    AlreadyCancelled("ALREADY_CANCELLED"),
    AlreadyProcessed("ALREADY_PROCESSED"),
    SessionNotActive("SESSION_NOT_ACTIVE"),
    SessionInProgress("SESSION_IN_PROGRESS"),
    ExpertHasUpcomingBookings("EXPERT_HAS_UPCOMING_BOOKINGS"),
    CallUnavailable("CALL_UNAVAILABLE"), // 503, Agora not configured
    CannotBookSelf("CANNOT_BOOK_SELF"), // 422, client tried to book themselves
    ExpertNotBookable("EXPERT_NOT_BOOKABLE"), // 422, expert not charges_enabled yet
    MeetingLinkUnavailable("MEETING_LINK_UNAVAILABLE"), // 422, Skype/Zoom link missing on confirm

    // 422, rare terminal reschedule backstop (iOS Phase 27.1): the booking is genuinely unconvertible
    // (no connected account, direct charging disabled, start < 24h away, or a legacy card-less booking).
    // UX must prompt the user to re-book, not to pick another slot.
    // The 6-day paid reschedule cap, RESCHEDULE_TOO_FAR_AHEAD, is in the Android-only block below.
    RescheduleOutsideAuthWindow("RESCHEDULE_OUTSIDE_AUTH_WINDOW"),

    // 422, client reschedule / accept-reschedule only: no saved card on the booking, or the card cannot be
    // prepared for future billing. DB-only on the server; do not infer "user has no saved cards" from it.
    PaymentMethodRequired("PAYMENT_METHOD_REQUIRED"),

    // Chat
    ThreadBlocked("THREAD_BLOCKED"),

    /** 403, `unblock` by the participant who did not block. Reconcile to is_blocked = true, blocked_by_me = false. */
    NotBlocker("NOT_BLOCKER"),

    /** 422, `block` or `unblock` against a self-thread. UI never shows block controls on a self-thread. */
    CannotBlockSelf("CANNOT_BLOCK_SELF"),

    // Discovery
    ExpertNotFound("EXPERT_NOT_FOUND"),

    // Payments / booking-financial
    PaymentFailed("PAYMENT_FAILED"),
    PaymentRequiresAction("PAYMENT_REQUIRES_ACTION"), // 402, SCA/3DS; detail.client_secret + detail.stripe_account
    IntentExpired("INTENT_EXPIRED"),
    PiContextMismatch("PI_CONTEXT_MISMATCH"),
    PiAmountMismatch("PI_AMOUNT_MISMATCH"),
    AmountBelowMinimum("AMOUNT_BELOW_MINIMUM"), // 422, Stripe's own per-currency floor (booking flow)

    // 422, ClickMe's own per-currency floor on topic price (topic create/patch and both booking paths).
    // Distinct from AmountBelowMinimum. The server message is display-ready and safe to surface.
    PriceBelowMinimum("PRICE_BELOW_MINIMUM"),
    PaidTopicRequiresPayment("PAID_TOPIC_REQUIRES_PAYMENT"),
    InvalidPromo("INVALID_PROMO"),
    InvalidPaymentMethod("INVALID_PAYMENT_METHOD"), // 422, pm_ belongs to a different customer
    BookingAttemptConflict("BOOKING_ATTEMPT_CONFLICT"), // 409, same booking_attempt_id, different details

    // 422, POST /bookings/payment-intent only: session start is > 6 days out. Android gates paid slots to 6 days
    // (quick 261028-to1) and shows this code as an error; it never takes the confirm-without-payment-intent branch.
    // The confirm-side twin, LONG_LEAD_NOT_AVAILABLE, is in the Android-only block below.
    LongLeadRequiresSavedCard("LONG_LEAD_REQUIRES_SAVED_CARD"),
    CommissionNotConfigured("COMMISSION_NOT_CONFIGURED"), // 500, server config issue
    DirectChargeDisabled("DIRECT_CHARGE_DISABLED"), // 503, server kill switch
    RefundAmountMissing("REFUND_AMOUNT_MISSING"),

    // Payouts / Stripe Connect
    NoPayoutMethod("NO_PAYOUT_METHOD"),
    KycIncomplete("KYC_INCOMPLETE"),
    ConfigMissing("CONFIG_MISSING"),
    InvalidCountry("INVALID_COUNTRY"), // 422, non-CA account on onboard
    ConnectNotRatified("CONNECT_NOT_RATIFIED"), // 403, live-mode legal gate

    // Endpoint lifecycle
    EndpointRetired("ENDPOINT_RETIRED"), // 410, retired endpoint (e.g. expert withdraw)

    // Account
    EmailRequired("EMAIL_REQUIRED"),
    ConfirmationRequired("CONFIRMATION_REQUIRED"),
    TokenUserMismatch("TOKEN_USER_MISMATCH"),

    // Account suspension and counterparty unavailability (see backend docs/ios/USER-REPORTING.md)

    /** 403, the caller's own account was suspended. Returned on any authed request and on /auth/login. */
    AccountSuspended("ACCOUNT_SUSPENDED"),

    /** 422, tried to report self. Hide the Report action on the user's own profile. */
    CannotReportSelf("CANNOT_REPORT_SELF"),

    /** 409, booking endpoints when the target expert was suspended or terminated between discovery and action. */
    ExpertUnavailable("EXPERT_UNAVAILABLE"),

    /** 409, expert accept-reschedule when the client was suspended or terminated. */
    ClientUnavailable("CLIENT_UNAVAILABLE"),

    /** 403, POST /chats/:id/send when the recipient is unavailable. Disable the composer. */
    RecipientUnavailable("RECIPIENT_UNAVAILABLE"),

    // Media
    UploadError("UPLOAD_ERROR"), // 400 (multer) or 413 (LIMIT_FILE_SIZE)

    // Rate limiting
    TooManyRequests("TOO_MANY_REQUESTS"), // global rate limits (login, password reset, delete-request, withdraw, ...)
    RateLimited("RATE_LIMITED"), // /auth/email/resend only

    // Catch-all
    InternalError("INTERNAL_ERROR"),

    // Android-only additions (not in iOS ErrorCode.swift). Keep after the 65 iOS cases.
    CannotChatWithSelf("CANNOT_CHAT_WITH_SELF"), // 422 on POST /chats/initiate (USER-REPORTING section 3)

    // 422, POST /bookings/confirm without a payment intent for a paid topic more than 6 days out (backend 261028-uph).
    // Android never sends that call (quick 261028-to1), so the mapping is a backstop.
    LongLeadNotAvailable("LONG_LEAD_NOT_AVAILABLE"),

    // 422, a paid reschedule (propose or accept, client or expert) to a start more than 6 days from now (261028-uph).
    // Accept re-checks against the server clock, so only a proposal made before that change can hit it on Android.
    RescheduleTooFarAhead("RESCHEDULE_TOO_FAR_AHEAD"),

    // 409, DELETE /user/account for a client with upcoming bookings (the client side of the booking guard; the
    // expert side is ExpertHasUpcomingBookings). The Delete Account form shows the server message (phase 14-05).
    ClientHasUpcomingBookings("CLIENT_HAS_UPCOMING_BOOKINGS"),
    ;

    /**
     * True for the codes the backend's authenticate middleware attaches to token failures. Controller-level
     * 401s (bad login, wrong password on DELETE /user/account) carry no code, so this is how the transport
     * layer (plan 02-06) tells an auth rejection from a business 401.
     */
    val isTokenError: Boolean
        get() = this == MissingToken || this == InvalidToken || this == ExpiredToken

    companion object {
        // Plain map lookup instead of Moshi's EnumJsonAdapter, which throws on unknown values.
        private val byWire: Map<String, ErrorCode> = entries.associateBy { it.wire }

        /** Returns the matching case, or null for an unknown or missing wire value. */
        fun fromWire(raw: String?): ErrorCode? = raw?.let(byWire::get)
    }
}
