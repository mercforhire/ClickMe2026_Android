# ClickMe Android

## What This Is

ClickMe Android is a native Android port of the finished ClickMe iOS app. ClickMe is a marketplace for paid and free one-to-one voice sessions. Clients discover experts, book and pay, join the call and chat. Experts publish topics and weekly availability, accept requests, hold the session and get paid through Stripe Connect. The Android app uses the same production backend and REST contract as iOS and must interoperate fully with it. One developer builds it with Claude Code.

**Milestone:** v1.0 MVP (2026-10-19 → ~2026-12-13)
**Package:** `com.uptrendinvestments.clickmeapp` (same as the iOS bundle ID)

## Core Value

The ClickMe MVP is live on Google Play (organization account) by ~2026-12-13. Android users can sign up, book and pay, accept requests, hold the voice session, chat and get paid, and they interoperate fully with the finished iOS app on the same backend.

## Requirements

### Validated

- FND-01, FND-02, FND-09, FND-10, FND-11, FND-12: build flavors and CI, Material 3 dark theme with Nunito, shared components, splash and launcher icon, two 5-tab shells with per-tab back stacks. Validated in Phase 1: Foundation, App Shell & Play Account (2026-10-20; CI green, device UAT approved).
- REL-01, REL-02: D-U-N-S confirmed; Play Console organization account created and verified (2026-10-20). Validated in Phase 1.
- AUTH-01, AUTH-02, RT-01, RT-02, RT-03: real Login, OTP password recovery (Forgot → Set New Password → Password Changed), and an authenticated socket.io connection with 2 s reconnect, token-refresh reconnect and an event `SharedFlow`. Validated in Phase 3: Login, Password Recovery & Realtime (2026-10-21; automated gate green, 436 unit tests; code-review warnings fixed; 6 device checks deferred in 03-HUMAN-UAT.md).
- SIGN-01..SIGN-05, ONB-01, ONB-02: signup without username or phone, a five-step setup hub saving through `PATCH /expert/profile/setup` (send-on-tap email code with a 30 s resend cooldown, photo crop and upload), Review → Publish, resume into setup, the once-per-login onboarding carousel, and a persisted client/expert mode switch. A "Not now" setup exit is remembered per login (D-27). Validated in Phase 4: Signup, Onboarding & Mode Switch (2026-10-21; automated gate green, 876 unit tests; all 11 code-review blockers and warnings fixed; 6 device checks deferred in 04-HUMAN-UAT.md).
- DISC-01, DISC-02, DISC-03: the Explore home (hero, Today's Sessions, Trending Categories with the All Categories multi-select sheet, Featured Topics, Recommended Experts, with separate states per section) from `GET /client/home`, a `CategoryStore` with a 7-day TTL in its own DataStore, and Expert Profile (hero, stats, about, expertise, topics, the first 3 reviews, unavailable on 404). Book Session lands on the real `BookingKey` route with a stub body that Phase 6 replaces. Validated in Phase 5: Explore & Expert Profile (2026-10-22; automated gate green, 1056 unit tests; 5 code-review warnings open in 05-REVIEW.md; 4 device checks deferred in 05-HUMAN-UAT.md).
- BOOK-01..BOOK-04: Make a Booking (topic sheet, custom month calendar, slots for three months regrouped by the client's local date with dedup and held slots disabled, expert timezone note, meeting type with the meeting-link check, notes), the free path via `POST /bookings/request`, and Request Sent from `GET /client/bookings/:id`. A booking that succeeds after the app is backgrounded survives process death (attempt id in saved state plus a short-lived DataStore record). Validated in Phase 6: Booking Flow & Free Bookings (2026-10-22; 1340 unit tests; three code-review rounds fixed; device UAT 15/16 with the far-zone check skipped; security 47/47 closed).
- BOOK-05..BOOK-09: My Bookings as the Bookings tab root (Upcoming/Past with the completed-but-not-ended rejoin rule, pull-to-refresh, quiet reloads on resume, tab re-show and `booking.update`), Upcoming Booking Details (status, pending banner, inert Phase 9 join card, Cancel), two-step cancellation with the strict-24h refund preview and a single-flight `POST /client/bookings/:id/cancel`, Booking Summary with Book Again, and the shared reschedule response card (accept with one-shot saved-card recovery, decline with reason). Validated in Phase 8: Client Bookings & Cancellation (2026-10-24; 1888 unit tests per flavor, R8 release green; 6 code-review warnings open in 08-REVIEW.md, WR-01/WR-06 recommended before Phase 9; 5 device checks deferred in 08-HUMAN-UAT.md).
- CALL-01..CALL-08: in-app Agora voice calls from all three Join sites (Upcoming Details, My Bookings card, Today's Sessions) through one app-scoped `CallCenter`; microphone permission with rationale and settings recovery; a microphone foreground service with an ongoing notification (tap to expand, End call); a root call overlay that minimizes to a pill across tabs and mode switches; the "you're in a call" guard on mode switch, logout, add payment method and cancel; waiting-for-peer on peer hang-up, rejoin, a schedule-anchored timer with the 5-minute amber stretch and the end + 3600 s cutoff; confirm-connection exactly once; leave-then-`/end` hang-up and local teardown on forced logout. External (Skype/Zoom) links open app-first with a Custom Tab fallback. Android keeps Join enabled until max(start + 60 min, scheduled end), unlike iOS. `:spikes:agora` removed. Validated in Phase 9: Voice Call (2026-10-24; 2237 unit tests per flavor, lint and R8 release green; all 10 code-review warnings fixed; 9 device checks deferred in 09-HUMAN-UAT.md, paid end-to-end blocked on the test-mode Stripe backend).
- EXP-05..EXP-09: the Expert Dashboard (pending count, reschedule banner, Today's Sessions with Join, weekly earnings, Stripe payout onboarding card opened in a Custom Tab), Incoming Requests → Request Decision (net earnings only, accept/decline with reason, expiry and ALREADY_PROCESSED handling) → Booking Confirmed with Add to Calendar, expert Upcoming/Past bookings with Booking Summary, cancellation and the expert reschedule response, and expert Join into the shared `CallCenter`. Cross-platform calls work: Android ↔ iOS meet in one Agora channel after three gap fixes (engine context NPE, COMMUNICATION profile, and a backend fix that derives the channel from `booking.id` in lowercase, deployed `4c694e7`). Validated in Phase 11: Expert Requests & Bookings (2026-10-27; 2961 unit tests; UAT 7/7 incl. a two-sided Android expert ↔ iOS client call; verification 4/4; security 70/70 closed; review WR-01/IN-02 open in 11-REVIEW-DISPOSITION.md; 09-HUMAN-UAT items 1, 2, 5 unblocked but not yet run).
- DEF-1 (pulled into scope by the 2026-10-30 parity decision): starting a reschedule on both sides, as on iOS. A shared slot picker with the current-slot chip and the paid 6-day cap, client and expert `PATCH .../reschedule`, the payment-conversion card-hold disclosure, the PAYMENT_METHOD_REQUIRED retry with a saved or one-off card, and the expert side gated on `can_reschedule`. The client Reschedule button follows the iOS/server allow-list: hidden only on pending_reschedule and on cancelled or ended bookings. Validated in Phase 16.2 (2026-10-31): 4672 unit tests, verification 13/13 passed, UAT 5/8 passed (two device-reported gaps fixed and re-tested on the SM-A075F). Test 5 (200% font backstops) is deferred to the Phase 16 matrix. Review WR-01 is open (no My Bookings call-site test for the gate), and picker warnings WR-01..03 from the first review are still unaddressed.
- Part of DEF-3 (pulled into scope by the 2026-10-30 parity decision): favorites and the full Reviews list, as on iOS. An optimistic favorite heart on Expert Profile with rollback, Saved Experts from the client Profile hub (empty state, remove with confirmation, refresh on return), and a full Reviews list from "See all" on Expert Profile. Validated in Phase 16.3 (2026-11-01): 4801 unit tests, verification 12/12 passed, UAT 4/5 passed on device. Test 5 (200% font backstops) is deferred to the Phase 16 matrix. Review warnings WR-01..04 are open, including WR-02 (the heart is shown in Expert mode, where the backend answers 403).

### Active

Full checkable list with IDs: `.planning/REQUIREMENTS.md` (99 v1 requirements).

- [ ] Foundation: scaffolding, networking core with single-flight token refresh, session storage, Material 3 design system, app shell with two 5-tab scaffolds, Stripe and Agora SDK spikes (FND-01 to FND-14)
- [x] Authentication, signup, onboarding and mode switch (AUTH-01 to AUTH-02, SIGN-01 to SIGN-05, ONB-01 to ONB-02), Phases 3–4
- [x] Realtime socket plumbing (RT-01 to RT-03), Phase 3
- [x] Client booking: calendar and slot picker, free booking, My Bookings, details, cancellation, responding to reschedules (BOOK-01 to BOOK-09), Phases 6 and 8
- [ ] Payments: saved cards, short-lead, long-lead and 3DS paid paths (PAY-01 to PAY-07). Code complete in Phase 7 (2026-10-23; 1556 unit tests; WR-02..07 fixed). Not validated: every card device check is blocked because the only backend runs Stripe in live mode and the dev `pk_test_` key is a sandbox. CR-01 (3DS reachability under off_session + error_on_requires_action) is an open cross-platform design risk. See 07-HUMAN-UAT.md.
- [ ] Voice call: Agora in a microphone foreground service, overlay/pill, rejoin semantics (CALL-01 to CALL-08)
- [x] Expert core: topics, availability, dashboard, requests, expert bookings (EXP-01 to EXP-09), Phases 10–11
- [ ] Chat, report/block/unblock (CHAT-01 to CHAT-05, SAFE-01 to SAFE-04)
- [ ] Push, notifications list, write review (PUSH-01 to PUSH-05, REV-01)
- [ ] Payouts, profile and settings, account deletion (PAYOUT-01 to PAYOUT-03, PROF-01 to PROF-05)
- [ ] Polish (POL-01 to POL-04)
- [ ] Cross-platform QA and Play Store release, including the organization account (QA-01 to QA-03, REL-01 to REL-07)

### Out of Scope

Deferred to v2, in the PRD's suggested order (details in REQUIREMENTS.md):

- ~~DEF-1: Starting a reschedule, for clients and experts.~~ Moved in scope by the 2026-10-30 full-parity decision and shipped in Phase 16.2 (see Validated).
- DEF-2: Search tab with filters and sort. The MVP has a placeholder tab.
- DEF-3: Booking Notes (both sides), Client Profile (expert side). These aren't on a money or session path. ~~Favorites, the full Reviews list.~~ Moved in scope by the 2026-10-30 full-parity decision and shipped in Phase 16.3 (see Validated).
- DEF-4: Client payment history and the expert transactions list. Not needed to book, pay or get paid.
- DEF-5: Tax Profile. Stripe's hosted Connect onboarding already collects identity details.
- DEF-6: Notification preferences, Feedback, the Expertise-tag editor in expert settings. Nice-to-have settings.
- DEF-7: Cancellation Policies static page, Categories modal, cosmetic animations. Cosmetic or informational.
- DEF-8: Analytics endpoints and the Firebase Ads conversion SDK. Not user-facing.
- ~~An Android expert proposing a reschedule.~~ Shipped with DEF-1 in Phase 16.2.
- Pixel parity with iOS. The iOS design is adapted to Material 3.

## Context

- **Source of truth.** `ios-reference/` (symlink to `../ClickMe2026`) defines feature behavior and business logic. `backend-reference/docs/ios/` (symlink to `../ClickMe2026 Backend`) holds the authoritative contract specs: CALL-END-SEMANTICS, CHAT-UNBLOCK, CLIENT-TRANSACTIONS, EMAIL-VERIFICATION-ON-REQUEST, EXPERT-EARNINGS-BREAKDOWN, EXPERT-EARNINGS-TRANSACTIONS, PHONE-NUMBER-REMOVED, PRICING-LIMITS, RESCHEDULE-PAYMENT-CONVERSION, SESSION-BUFFER, SIGNUP-USERNAME-OPTIONAL, STRIPE-INTEGRATION, USER-REPORTING (plus SocketSmokeTest.swift).
- **Size of the port.** The iOS app has about 50k lines of screen code in about 70 screen folders, plus about 9k lines of networking, models and managers. The backend has about 114 endpoints. Port API groups one feature at a time, not all up front.
- **Testing approach.** The iOS app is finished. Use it as the "other side" of every flow (for example, book from Android and accept on iOS). Android doesn't need both modes finished before end-to-end testing.
- **Both modes are in scope.** Every signup creates an expert-role account, and experts must be able to accept requests from their phone.
- **Starting state (2026-10-19).** Android Studio Compose template. `applicationId` and `namespace` are already `com.uptrendinvestments.clickmeapp` and the project compiles. Nothing else is built yet.
- **Why 8 weeks.** The first 4-week draft only worked if nothing slipped. Each high-risk piece now gets its own week (Make a Booking + Stripe, the call, push + chat), and there's room for Play account setup and review.
- **Biggest schedule risks.** (1) Make a Booking + Stripe (Week 3): timezone regrouping, three payment paths, 3DS. (2) Call service and overlay (Week 4): background audio and keeping the call across navigation. (3) Push deep-link routing into per-tab back stacks (Week 6). (4) Play Console org verification (D-U-N-S + Google review), an outside wait. The Week 1 spikes reduce risks 1 and 2.
- **MVP cut order if the schedule slips.** (1) Payouts shrink to the onboarding link only. (2) Drop the notifications list; push deep links are enough. (3) Write Review moves to a quick update.
- **Never cut.** Delete account, report/block, the Stripe paths and the call paths.
- **Open items.**
  - Should Android collect and send the cancellation comment? iOS collects it but never sends it. Resolve in Phase 8 planning against the `cancelClientBooking` contract.
  - EncryptedSharedPreferences is deprecated upstream (androidx.security-crypto). It still works and the decision stays locked. Revisit on purpose rather than by accident.
- **Report back to the iOS side (not Android work).**
  - The Join button in Upcoming Booking Details is a no-op when opened from My Bookings.
  - The overtime cutoff is still the 5-minute test value.
  - Report reasons are sent as display labels.
  - Chat has no blocked state.
  - The cancellation comment is dropped.
  - The hard-coded JWT in `Debug/PreviewSecrets.swift` needs rotating (owned by backend/iOS).

## Constraints

- **Tech stack (locked)**: Jetpack Compose + Material 3, MVVM with Compose State, coroutines, Hilt, Retrofit + Moshi, EncryptedSharedPreferences for JWTs, Firebase Messaging, Agora Android SDK, Stripe Android SDK with PaymentSheet. Locked by CLAUDE.md; see Key Decisions.
- **Allowed additions**: OkHttp, Coil, Navigation Compose, DataStore (non-secret state only), socket.io-client-java v2+, AndroidX SplashScreen, Chrome Custom Tabs. The PRD adds these. None of them contradicts the locked stack.
- **SDK levels**: minSdk 26, targetSdk 37, compileSdk 37.
- **Package name**: `com.uptrendinvestments.clickmeapp` for `applicationId`, `namespace` and source folders. It can't change after the first Play upload.
- **Timeline**: 2026-10-19 → ~2026-12-13. 7 build weeks plus 1 week for QA and release. Target public release is ~Dec 11–13, or as early as ~Dec 7–8 if Weeks 1–7 hold. Week 8 is buffer.
- **Team**: Solo developer with Claude Code. No team process.
- **Backend**: Every flavor points at production, `https://clickme-backend-production.up.railway.app/api/v1`. Same REST contract as iOS.
- **API contract: refresh tokens**: The backend revokes the session if a refresh token is reused. Refresh is single-flight (OkHttp `Authenticator` + Mutex) and retries once. If the refresh fails, log out. The socket reconnects with the new token after every refresh.
- **API contract: formats**: snake_case JSON. ISO-8601 dates with microseconds. Errors come as the `{status, code?, message}` envelope, the `errors[]` shape or status-only. Unknown error codes map to null.
- **API contract: money**: Amounts are in minor units everywhere except `payment-intent` amounts. JPY has no decimal places. Payout transactions are in cents and `earnings_breakdown` is in dollars. `net_amount` can be null, so check `fee_source` first.
- **API contract: push**: Register with `POST /notifications/tokens` using `platform: "android"`.
- **Release: Play org account**: Publish from an organization account. It needs a D-U-N-S number (days to weeks to get) and Google's org verification (days). Start in Week 1. The 14-day closed test for personal accounts doesn't apply.
- **Release: store-required features**: Account deletion (plus an account-deletion URL), report/block and a support contact (`mailto:`) must ship and are never cut.
- **Release: foreground service**: The call runs in a `microphone`-type foreground service. Play Console needs a foreground-service declaration and video before release.
- **Do not port**:
  - `Debug/PreviewSecrets.swift`: it holds a hard-coded JWT, which should be rotated.
  - `PreviewNavHarness`.
  - iOS debug logging. Use OkHttp's logging interceptor in debug builds only.
  - The unused `Call/Ready` screen.
  - The unused `NotificationQuietHoursCard`.
- **iOS issues not to copy**:
  - Wire Join in Upcoming Booking Details, including when opened from My Bookings (iOS `MyBookingsView.swift:108` doesn't pass `onJoinCall`).
  - Use a 3600s overtime cutoff (iOS still has the 5-minute test value in `MeetingCallViewModel.swift`).
  - Send the six report-reason enum values, not display labels (USER-REPORTING.md).
  - Show a blocked chat state: disabled composer plus a banner (CHAT-UNBLOCK.md).
  - Cancellation comment: open item, decided in Phase 8 planning.
- **Design**: Adapt the iOS design to Material 3. Don't chase pixel parity. Merge the ~40 per-screen `*Brand.swift` color sets into one theme.
- **File organization**: Mirror the iOS folder tree. Example: `Views/Screens/Signup Flow/Initial/SignupInitialView.swift` becomes `screens/signup/initial/SignupInitialScreen.kt` + `SignupInitialViewModel.kt`.

## Key Decisions

Locked decisions from the ADR (`CLAUDE.md`) and the user's package decision at ingest. No phase plan may contradict these.

<decisions>
### Locked (ADR: CLAUDE.md, precedence 0)
- **DEC-ios-source-of-truth** [locked]: `ios-reference/` is the source of truth for feature behavior and business logic. Deviate only for Android platform conventions. Documented iOS defects backed by a backend spec or by the iOS code's own stated intent count as consistent with this rule (see the Constraints list above).
- **DEC-ui-compose-m3** [locked]: UI uses Jetpack Compose + Material 3.
- **DEC-mvvm-compose-state** [locked]: Presentation is MVVM with Compose State.
- **DEC-coroutines** [locked]: Concurrency uses Kotlin coroutines, not RxJava.
- **DEC-hilt** [locked]: Dependency injection uses Hilt.
- **DEC-retrofit-moshi** [locked]: Networking uses Retrofit + Moshi.
- **DEC-jwt-encrypted-sharedprefs** [locked]: JWTs are stored in EncryptedSharedPreferences. Upstream deprecation is noted; the decision stands.
- **DEC-fcm** [locked]: Push uses Firebase Messaging.
- **DEC-agora** [locked]: Voice uses the Agora Android SDK.
- **DEC-stripe-paymentsheet** [locked]: Payments use the Stripe Android SDK with PaymentSheet.
- **DEC-sdk-levels** [locked]: minSdk 26, targetSdk 37, compileSdk 37.
- **DEC-platform-swaps** [locked]: `.sheet` → `ModalBottomSheet`; iOS back chevron → `TopAppBar` with back arrow; `.alert` → Material `AlertDialog`; iOS switch → Material `Switch`; `PhotosPicker` → `rememberLauncherForActivityResult(PickVisualMedia)`; `.toolbar` nav title → `TopAppBar` title slot; `.task` on appear → `LaunchedEffect(Unit)`.
- **DEC-file-organization** [locked]: Mirror the iOS folder tree under `app/src/main/java/com/uptrendinvestments/clickmeapp/` (screen → `*Screen.kt` + `*ViewModel.kt`).
- **DEC-backend-contract** [locked]: Same REST contract as iOS. `backend-reference/docs/ios/` holds the authoritative specs.

### Locked (user decision at ingest, 2026-10-19)
- **DEC-application-id** [locked]: `applicationId` and `namespace` are `com.uptrendinvestments.clickmeapp`, the same as the iOS bundle ID. It can't change after the first Play upload.
</decisions>

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Reschedule scope: only *initiating* a reschedule is deferred. Responding to one is in the MVP (client BOOK-09, expert EXP-09). | Without a response path, bookings from iOS users stay stuck in `pending_reschedule`. | Good — client (Phase 8) and expert (Phase 11) response shipped |
| Package `com.uptrendinvestments.clickmeapp` replaces `com.example.clickme_android` | Play rejects `com.example`. It matches the iOS bundle ID and can't change after the first upload. | Pending |
| Merge the ~40 per-screen iOS color sets into one Material 3 theme | Platform convention. Avoids copying per-screen styling. | Pending |
| Fix documented iOS defects instead of copying them | Each fix is backed by a backend spec or by the iOS code's own stated intent. | Pending |
| Week 7 and Week 8 success criteria (the PRD has none) were confirmed by the user | Week 7: Connect onboarding, profile/avatar/password/delete, and loading/empty/error states. Week 8: the cross-platform matrix passes both ways and the production release is live. | Pending |
| Roadmap granularity: 17 phases, about 2 per PRD week (3 in Week 3) | Each phase is plannable and executable in 2–4 days. Week 3 is the highest-risk week. | Pending |
| Server-bound identifiers (Agora channel, echoed booking ids) come from the loaded DB row, never from request path text (G-11-6, backend `4c694e7`) | iOS sends UUIDs uppercase and Android lowercase, so the call peers were split across two channels. Fixing it on the server also covers iOS builds already in the field. | Good — two-sided call verified 2026-10-27 |
| Keep EncryptedSharedPreferences despite the upstream deprecation | Locked by the ADR. It still works. | Revisit |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? Move to Out of Scope with the reason.
2. Requirements validated? Move to Validated with a phase reference.
3. New requirements emerged? Add to Active.
4. Decisions to log? Add to Key Decisions.
5. "What This Is" still accurate? Update it if it has drifted.

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections.
2. Core Value check: still the right priority?
3. Audit Out of Scope: are the reasons still valid?
4. Update Context with the current state.

---
*Last updated: 2026-11-01 after Phase 16.3 completion*
