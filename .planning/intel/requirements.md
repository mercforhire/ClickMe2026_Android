# Requirements (from PRDs)

Source PRDs: 1
- source: /Users/LeonChen/Documents/ClickMe_Android/docs/PORT_ROADMAP.md
  title: ClickMe Android — MVP Port Roadmap
  precedence: 1 (below the LOCKED ADR /Users/LeonChen/Documents/ClickMe_Android/CLAUDE.md)

## Program frame
- source: docs/PORT_ROADMAP.md (header, "Scope at a glance")
- Window: 2026-10-19 → ~2026-12-13. 7 build weeks plus 1 week of QA and release.
- Source of truth: `ios-reference/` (behavior) and `backend-reference/docs/ios/` (contract).
- Design: iOS design adapted to Material 3. Don't chase pixel parity.
- MVP keeps every path where money or a session is involved: sign up, discover, book and pay, accept, call, get paid. It also keeps what the stores require: account deletion, report/block, support contact.
- Both modes (client and expert) are in scope. Every signup creates an expert-role account, and experts must be able to accept requests from their phone.
- Testing approach: use the finished iOS app as the "other side" of every flow (for example, book from Android and accept on iOS).

Structure: the PRD's week sections are kept below as the phase skeleton. Each week's "End of Week N" checkpoint is quoted verbatim as that week's acceptance criteria. All requirements in a week share that acceptance, plus the specifics listed under each requirement.

---

## Week 1 (Oct 19–25): Foundation and SDK spikes

Week 1 acceptance (verbatim, source: docs/PORT_ROADMAP.md "End of Week 1"):
> the app launches into an empty tab layout. Login and token refresh work against the real backend, tested with a temporary login screen.

### REQ-play-console-org-account: Play Store organization account (start in Week 1)
- source: docs/PORT_ROADMAP.md, "Play Store account (start this week)"
- description: Publish from an organization Play developer account under the corporation. Because it is an org account, the 14-day closed test for new personal accounts does not apply.
- specifics:
  1. Get or confirm the corporation's D-U-N-S number. A new one can take a few days to several weeks.
  2. Create the Play Console org account. Google's organization verification can take days.
  3. Register the package name and upload a first internal build early.
  - Set up the listing, data safety form and foreground-service declaration ahead of time. Policy review can take several days.
- acceptance: The PRD gives no checkpoint for this item. It must be "done in Week 1, so the account is ready long before release."
- open item: see INGEST-CONFLICTS.md WARNING "applicationId uses com.example". The final package name must be chosen before step 3.

### REQ-project-scaffolding: Project scaffolding
- source: docs/PORT_ROADMAP.md, Week 1 §1
- specifics:
  - Hilt, Retrofit + Moshi, OkHttp, coroutines, Coil, Navigation Compose
  - Build flavors (dev/prod). All iOS environments point at the production backend: `https://clickme-backend-production.up.railway.app/api/v1`
  - Config: Stripe publishable key, Agora App ID
  - Lint and baseline CI

### REQ-networking-core: Networking core
- source: docs/PORT_ROADMAP.md, Week 1 §2 (iOS: `Network/NetworkService.swift`, `APIRequestURLs.swift`)
- specifics:
  - Moshi with snake_case. Custom date adapter, because the backend sends ISO-8601 with microseconds.
  - Error envelope `{status, code?, message}` plus an `ErrorCode` enum (~65 codes, unknown codes map to null). Also handle the `errors[]` and status-only error shapes.
  - Wrapper for `{data: …}` responses and `PaginationMeta` (page-based).
  - Auth header only on authed paths (port `needAuthToken`).
  - Single-flight token refresh as an OkHttp `Authenticator` guarded by a Mutex. The backend revokes the session if a refresh token is reused. Retry once; log out if the refresh itself fails.
  - Global `403 ACCOUNT_SUSPENDED` handling: log out and show a message.
  - Port API groups gradually, one per feature, not all ~114 endpoints up front.

### REQ-session-user-manager: Session / UserManager
- source: docs/PORT_ROADMAP.md, Week 1 §3 (iOS: `Managers/UserManager.swift`)
- specifics:
  - Tokens in EncryptedSharedPreferences (matches the LOCKED DEC-jwt-encrypted-sharedprefs). Cached profile, `currentMode` and `hasSeenOnboarding` in DataStore.
  - Auto-login and logout. Logout clears the onboarding flag and unregisters the push token.

### REQ-design-system: Design system
- source: docs/PORT_ROADMAP.md, Week 1 §4 (iOS: `Theme/Brand.swift`)
- specifics:
  - Dark `darkColorScheme`: surface `#131313`, primary `#44f697`, primary container `#00d97e`, etc.
  - Bundle a rounded font (e.g. Nunito). iOS uses SF Rounded.
  - Merge the ~40 per-screen `*Brand.swift` color sets into the theme. Don't copy them over.
  - Shared components: press-scale button, async save button, validated text field, avatar, glow background, Custom Tab launcher.

### REQ-app-shell: App shell
- source: docs/PORT_ROADMAP.md, Week 1 §5
- specifics:
  - `AppRoute`: splash → login | signupSetup | clientHome | expertHome.
  - Android 12 SplashScreen API instead of the iOS 3-second delay.
  - Two 5-tab scaffolds, each tab with its own saved back stack:
    - Client: Explore, Search*, Bookings, Chats, Profile
    - Expert: Dashboard, Bookings, Topics, Chats, Profile
  - *Search can be a placeholder tab at first. Full Search is deferred (DEF-2).

### REQ-sdk-spikes: SDK spikes (throwaway test apps)
- source: docs/PORT_ROADMAP.md, Week 1 §6
- specifics:
  - Stripe PaymentSheet in SetupIntent mode, plus `PaymentLauncher` 3DS on a connected account.
  - Agora audio join, running inside a foreground service.
  - Goal: find SDK and Gradle problems now, not in Week 3 or 4.

---

## Week 2 (Oct 26–Nov 1): Auth, signup, realtime plumbing

Week 2 acceptance (verbatim, source: docs/PORT_ROADMAP.md "End of Week 2"):
> a user can sign up, finish setup, log in again and switch modes. The socket connects and receives events.

### REQ-login-flow: Login flow
- source: docs/PORT_ROADMAP.md, Week 2 §1
- specifics: Login, forgot password (OTP), set new password, password changed.

### REQ-signup-flow: Signup flow
- source: docs/PORT_ROADMAP.md, Week 2 §2 (iOS: `SignupAccumulator.swift`)
- specifics:
  - The Initial step creates the account.
  - Then the Overview hub, which the user can complete in any order: Basic Info + language picker, Timezone, Profile Photo, Verify Email, Tags.
  - Then Review → Publish.
  - Each step saves through `PATCH /expert/profile/setup`.
  - Resume an unfinished signup at login/splash.
  - Send the email verification code only when the user taps the button (30s cooldown). Don't send it automatically. (Backed by backend-reference/docs/ios/EMAIL-VERIFICATION-ON-REQUEST.md.)
  - No username or phone fields. (Backed by SIGNUP-USERNAME-OPTIONAL.md and PHONE-NUMBER-REMOVED.md.)

### REQ-onboarding-mode-switch: Onboarding and mode switch
- source: docs/PORT_ROADMAP.md, Week 2 §3
- specifics:
  - Onboarding carousel, shown on first launch into client home.
  - Mode switch screen (local only, `ModeSwitchView`).

### REQ-realtime-service: Realtime service (infrastructure only)
- source: docs/PORT_ROADMAP.md, Week 2 §4 (iOS: `Realtime/RealtimeService.swift`)
- specifics:
  - socket.io-client-java v2+, websocket transport, JWT in `auth: {token}`, reconnect every 2s.
  - Reconnect with the new token after each token refresh.
  - Expose events as a `SharedFlow`: `message.*`, `typing.*`, `user.status`, `booking.update`, `call.ended`.
  - No UI yet. Verify with logging.

---

## Week 3 (Nov 2–8): Client discovery and booking (highest risk)

Week 3 acceptance (verbatim, source: docs/PORT_ROADMAP.md "End of Week 3"):
> an Android client can book and pay (free, short-lead, long-lead and 3DS test cards), and an iOS expert receives the request.

### REQ-explore: Explore
- source: docs/PORT_ROADMAP.md, Week 3 §1
- specifics:
  - `getClientHome`: categories, featured topics, today's sessions, expert cards.
  - Simplify the hero and visuals if time runs short.
  - `CategoryStore` cache with a 7-day TTL.

### REQ-expert-profile: Expert Profile
- source: docs/PORT_ROADMAP.md, Week 3 §2
- specifics:
  - Hero, stats, about, topics, reviews preview, Book button.
  - The Chat button and report/block are added in Week 6 (REQ-chat, REQ-report-block-unblock).

### REQ-payment-methods: Payment Methods (Settings)
- source: docs/PORT_ROADMAP.md, Week 3 §3
- specifics:
  - List, set default, delete.
  - Add a card with a SetupIntent and Stripe PaymentSheet (setup mode).
  - Required because long-lead bookings charge a saved card.

### REQ-make-a-booking: Make a Booking (the biggest client screen)
- source: docs/PORT_ROADMAP.md, Week 3 §4
- specifics:
  - Topic picker, then a custom month calendar and slot picker, then meeting type and notes.
  - Timezones:
    - Load the previous, current and next month.
    - Regroup slots by the client's local date using `startUtc`, and drop duplicates.
    - Show the expert's timezone.
    - Treat `held` slots as unavailable.
  - Payment paths:
    - Free: `POST /bookings/request`.
    - Short-lead paid: `payment-intent` (with a `booking_attempt_id` UUID that is reused on retry), then setup-intent, then PaymentSheet `FlowController`, then `confirm`.
    - Long-lead paid: the server returns `422 LONG_LEAD_REQUIRES_SAVED_CARD`; confirm with the saved card, or send the user to Payment Methods if they have none.
    - 3DS: `402 PAYMENT_REQUIRES_ACTION`; run `PaymentLauncher` with `stripeAccountId`, then retry confirm.
  - Port the ~15 error codes in `handlePaidFlowError`.
  - Money units: minor units everywhere except `payment-intent` amounts. JPY has no decimal places.

### REQ-request-sent: Request Sent
- source: docs/PORT_ROADMAP.md, Week 3 §5
- specifics: Confirmation screen. The staggered animation is optional.

---

## Week 4 (Nov 9–15): Client bookings and the call

Week 4 acceptance (verbatim, source: docs/PORT_ROADMAP.md "End of Week 4"):
> the client side is complete. An Android client and an iOS expert can finish a full paid session, including the app going to the background mid-call.

### REQ-my-bookings: My Bookings
- source: docs/PORT_ROADMAP.md, Week 4 §1
- specifics:
  - Upcoming/Past tabs. A `completed` booking whose end time hasn't passed stays in Upcoming.
  - Past labels: completed, cancelled (cancelled or declined), missed (missed or expired).

### REQ-upcoming-booking-details: Upcoming Booking Details
- source: docs/PORT_ROADMAP.md, Week 4 §2
- specifics:
  - "Waiting for expert" banner, join card, Cancel.
  - Wire up Join here. On iOS, the push from `MyBookingsView.swift:108` doesn't pass `onJoinCall`, so the button does nothing. Report that to the iOS side. (Confirmed at ingest: the iOS call site passes only `onReschedule` and `onCancel`.)
  - No Reschedule action on Android. Client-initiated reschedule is deferred (DEF-1).

### REQ-client-cancellation: Cancellation
- source: docs/PORT_ROADMAP.md, Week 4 §3
- specifics: Reason picker, refund preview, `cancelClientBooking`.
- open item: iOS collects a cancellation comment but never sends it (see constraints.md CON-ios-cancel-comment-dropped). The PRD does not say whether Android should collect and send it.

### REQ-client-booking-summary: Booking Summary (minimal)
- source: docs/PORT_ROADMAP.md, Week 4 §4
- specifics:
  - Session details.
  - Accept/decline an expert-proposed reschedule. Required: without it, bookings stuck in `pending_reschedule` can't move.
- resolved (user, 2026-10-19): responding to a reschedule is in MVP scope; only initiating one is deferred (DEF-1).

### REQ-call: Call
- source: docs/PORT_ROADMAP.md, Week 4 §5 (iOS: `Managers/CallCenter.swift`, `AgoraManager.swift`, `Call/Calling/`)
- specifics:
  - Setup:
    - `POST /bookings/:id/join` returns either `agoraConfig` (audio only) or `externalConfig.joinUrl` (opened in a Custom Tab or an intent).
    - Ask for mic permission (rationale plus a denied dialog).
    - Run the call in a foreground service of type `microphone`, with audio focus and a persistent notification.
  - During the call:
    - Root-level overlay host: full-screen call UI that minimizes to a pill and stays visible across tabs and modes. Show a "you're in a call" guard on restricted actions.
    - When both sides have joined, send `PATCH confirm-connection` once.
    - A peer's `call.ended` event switches to "waiting for peer". It doesn't end the call.
    - Timer counts from the scheduled start, with an amber warning in the last 5 minutes.
  - Ending:
    - Hang-up leaves the channel, then calls `POST /bookings/:id/end`. The booking stays open, and the user can rejoin for the whole window (backend-reference/docs/ios/CALL-END-SEMANTICS.md).
    - Overtime cutoff: use 3600s. iOS still has the 5-minute test value (`MeetingCallViewModel.swift:532`). The iOS source marks it "TEMP: cutoff is 5 min for testing; revert to 3600 (1 hour) for production."
  - Skip the `Call/Ready` screen. Nothing in iOS uses it.

---

## Week 5 (Nov 16–22): Expert core

Week 5 acceptance (verbatim, source: docs/PORT_ROADMAP.md "End of Week 5"):
> the expert side works. An iOS client books, the Android expert accepts, and both join the call.

### REQ-topics-editor: Topics + Topic Editor
- source: docs/PORT_ROADMAP.md, Week 5 §1
- specifics:
  - Experts can't receive bookings without at least one topic.
  - The price floor comes from `/me` `pricing_limits`. Don't cache it across launches. (See backend-reference/docs/ios/PRICING-LIMITS.md.)
  - Show `422 PRICE_BELOW_MINIMUM` on the price field.

### REQ-availability: Availability
- source: docs/PORT_ROADMAP.md, Week 5 §2
- specifics:
  - Weekly editor with slots as minutes-from-midnight, sent as `HH:mm`.
  - Overlap validation per day and a time-range picker sheet.
  - Timezone picker. If the server timezone is UTC, quietly fill in the device timezone.

### REQ-expert-dashboard: Expert Dashboard (simplified)
- source: docs/PORT_ROADMAP.md, Week 5 §3
- specifics: Pending request count, upcoming sessions, weekly earnings text, payout onboarding card.

### REQ-incoming-requests: Incoming Requests → Request decision → Confirmed
- source: docs/PORT_ROADMAP.md, Week 5 §4
- specifics:
  - Accept with an optional message; decline with a reason code.
  - Add-to-calendar uses an `Intent.ACTION_INSERT` calendar intent.

### REQ-expert-bookings: Expert Upcoming Bookings + Booking Summary
- source: docs/PORT_ROADMAP.md, Week 5 §5
- specifics:
  - Upcoming/Past tabs. Join button inside the join window (reuses the Week 4 call).
  - Accept/decline client reschedule requests (handle `409 SLOT_UNAVAILABLE`), and Cancel.
- resolved (user, 2026-10-19): Reading A. Only *initiating* a reschedule is deferred (DEF-1). Accepting or declining a client's reschedule request is in MVP scope here.

---

## Week 6 (Nov 23–29): Chat, push, notifications

Week 6 acceptance (verbatim, source: docs/PORT_ROADMAP.md "End of Week 6"):
> every MVP flow except payouts and settings works, and a build is with testers.

### REQ-chat: Chat
- source: docs/PORT_ROADMAP.md, Week 6 §1
- specifics:
  - Conversations list.
  - Chatting screen:
    - Paging: 30 messages per page, newest first, `loadEarlier` prepends.
    - Optimistic send reconciled by `client_msg_id`.
    - Read and typing events.
    - Booking system-event rows.
    - Text only.
  - `initiateChat` entry points from Expert Profile, bookings and requests.

### REQ-report-block-unblock: Report / block / unblock
- source: docs/PORT_ROADMAP.md, Week 6 §2
- specifics:
  - Required by the Play Store. Also add report/block to Expert Profile.
  - Follow backend-reference/docs/ios/CHAT-UNBLOCK.md and USER-REPORTING.md:
    - Send the six report reason enum values, not display labels (an iOS bug).
    - Disable the composer and show a banner when blocked.
    - Handle `THREAD_BLOCKED` and `RECIPIENT_UNAVAILABLE`.
  - CHAT-UNBLOCK.md details, noted for planning: show Unblock only when `blocked_by_me` is true; when `is_blocked` is true and `blocked_by_me` is false, show "You can't reply to this conversation" with no Unblock button; a non-blocker calling unblock gets `403 NOT_BLOCKER`.
- never-cut: the PRD says not to cut report/block.

### REQ-push-fcm: Push (FCM)
- source: docs/PORT_ROADMAP.md, Week 6 §3
- specifics:
  - Register with `POST /notifications/tokens` and `platform: "android"` (the backend schema accepts it). Re-register on every app start; unregister on logout.
  - Port `PushRouter`: category/sub_category plus the legacy `event_type` fallback, mapped to destinations.
  - Deep links into the right tab's back stack, from both a cold start and a running app.
  - Notification channels.
  - `POST_NOTIFICATIONS` permission (Android 13+).

### REQ-notifications-list: Notifications list
- source: docs/PORT_ROADMAP.md, Week 6 §4
- specifics: Grouped by date. Tapping routes through the same `PushDestination` map.
- cut priority: #2 in the cut order (push deep links alone are enough).

### REQ-write-review: Write Review
- source: docs/PORT_ROADMAP.md, Week 6 §5
- specifics: Reached from the `session/ended` and `review/nudge` pushes and from Booking Summary.
- cut priority: #3 in the cut order (it can come in a quick update).

### REQ-internal-testing: Start internal testing
- source: docs/PORT_ROADMAP.md, Week 6 §6
- specifics: Put a build on the internal testing track for a few real users on a mix of devices. Not a Play requirement for org accounts; it's for catching bugs early.

---

## Week 7 (Nov 30–Dec 6): Payouts, settings, polish

Week 7 acceptance: the source has no "End of Week 7" checkpoint. Not invented here. Implied by the Week 6 checkpoint ("every MVP flow except payouts and settings works"): after Week 7, payouts and settings also work. The roadmapper should confirm success criteria with the user rather than assume them.

### REQ-payouts: Payouts (minimal)
- source: docs/PORT_ROADMAP.md, Week 7 §1
- specifics:
  - Balance and Connect status.
  - Stripe Connect onboarding:
    - Open the hosted Account Link in a Chrome Custom Tab.
    - Catch the `clickme://connect/return|refresh` deep link. On refresh, call `refreshConnectLink`; on return, re-fetch the status.
  - Show a short recent-earnings list. Headline `net_amount`, which can be null, so check `fee_source` first.
  - Units: transactions are in cents, `earnings_breakdown` is in dollars. (See EXPERT-EARNINGS-BREAKDOWN.md, EXPERT-EARNINGS-TRANSACTIONS.md.)
- cut priority: #1 in the cut order (payouts: show the onboarding link only).

### REQ-profile-settings: Profile hubs and settings
- source: docs/PORT_ROADMAP.md, Week 7 §2
- specifics:
  - Client and expert profile hubs, edit profile and avatar.
  - Account Security (change password).
  - Delete account (Play Store requirement). Never cut.
  - Help/FAQ with a `mailto:` support link.

### REQ-polish: Polish
- source: docs/PORT_ROADMAP.md, Week 7 §3
- specifics:
  - Empty, loading and error states on every MVP screen.
  - Process death and configuration changes.
  - Edge-to-edge and insets.
  - TalkBack check on core flows.

---

## Week 8 (Dec 7–13): QA and release

Week 8 acceptance: the source has no "End of Week 8" checkpoint. Its release target (verbatim, source: docs/PORT_ROADMAP.md):
> Target public release: ~Dec 11–13. No mandatory testing wait applies, so if Weeks 1–7 stay on schedule, release can move up to as early as ~Dec 7–8. Treat Week 8 as buffer.

### REQ-qa-cross-platform: QA and cross-platform matrix
- source: docs/PORT_ROADMAP.md, Week 8
- specifics:
  - Fix bugs from internal testing.
  - Cross-platform matrix, both directions (Android client with iOS expert, and the reverse):
    - Stripe test cards, including 3DS
    - Push from a killed app
    - Call kept alive in the background and across rejoin
    - Payout onboarding

### REQ-play-release: Play Console submission and production release
- source: docs/PORT_ROADMAP.md, Week 8
- specifics:
  - Data safety form
  - Foreground service (microphone) declaration and video
  - Account-deletion URL
  - Content rating
  - Production release

---

## MVP cut order (if schedule slips)
- source: docs/PORT_ROADMAP.md, "If you need to cut further"
- Cut in this order while staying in the MVP:
  1. Payouts: show the onboarding link only. (REQ-payouts)
  2. Notifications list: push deep links alone are enough. (REQ-notifications-list)
  3. Write Review: it can come in a quick update. (REQ-write-review)
- Never cut: Delete account (REQ-profile-settings), report/block (REQ-report-block-unblock), the Stripe paths (REQ-payment-methods, REQ-make-a-booking) and the call paths (REQ-call).

---

## Out of scope: Deferred to v2 (post-MVP)
- source: docs/PORT_ROADMAP.md, "Deferred (post-MVP), in suggested order"
- status: OUT OF SCOPE for MVP (v2). Listed in the PRD's suggested order.

- DEF-1 (v2): Starting a reschedule, for clients and experts (the Reschedule screens with the slot picker). It's the riskiest flow. Until then, users can cancel and book again. Accepting or declining a reschedule the other side proposed is NOT deferred (REQ-client-booking-summary, REQ-expert-bookings). Relevant backend spec for later: RESCHEDULE-PAYMENT-CONVERSION.md.
- DEF-2 (v2): Search tab with filters and sort. (A placeholder tab is allowed in the MVP, per REQ-app-shell.)
- DEF-3 (v2): Favorites, the full Reviews list, Booking Notes (both sides), Client Profile (expert side).
- DEF-4 (v2): Client payment history (`/client/transactions`; a page past the end returns a 500, so treat it as end of list) and the expert transactions list.
- DEF-5 (v2): Tax Profile. Stripe's hosted onboarding already collects identity details.
- DEF-6 (v2): Notification preferences, Feedback, the Expertise-tag editor in expert settings.
- DEF-7 (v2): Cancellation Policies static page, Categories modal, cosmetic animations.
- DEF-8 (v2): Analytics endpoints and the Firebase Ads conversion SDK.

Not in MVP by omission (no PRD section covers it; noted so it isn't assumed): an Android expert proposing a reschedule. Android clients must respond to expert proposals (REQ-client-booking-summary), but nothing in the PRD has the Android expert initiate one.
