# Requirements: ClickMe Android

**Defined:** 2026-10-19
**Core Value:** The ClickMe MVP is live on Google Play (organization account) by ~2026-12-13. Android users can sign up, book and pay, accept requests, hold the voice session, chat and get paid, and they interoperate fully with the finished iOS app on the same backend.

**Sources:**
- `docs/PORT_ROADMAP.md` (PRD). The requirements below break the PRD's 37 week-level items into atomic, testable requirements. Each category names the PRD items it comes from.
- `CLAUDE.md` (locked ADR), which constrains every requirement. See PROJECT.md Key Decisions.

Behavior follows `ios-reference/`. Contract details follow `backend-reference/docs/ios/`.

## v1 Requirements

Requirements for the v1.0 MVP. Each maps to exactly one roadmap phase.

### Foundation

From the PRD's Week 1 items: project scaffolding, networking core, session/UserManager, design system, app shell, SDK spikes.

- [ ] **FND-01**: The project builds with Hilt, Retrofit + Moshi, OkHttp, coroutines, Coil and Navigation Compose. The dev and prod flavors both point at the production backend (`https://clickme-backend-production.up.railway.app/api/v1`). The Stripe publishable key and Agora App ID come from build config.
- [ ] **FND-02**: Lint and a baseline CI pipeline run on every push.
- [ ] **FND-03**: The networking layer decodes snake_case JSON, ISO-8601 dates with microseconds, `{data: …}` response wrappers and page-based `PaginationMeta`.
- [ ] **FND-04**: API errors in the `{status, code?, message}` envelope, the `errors[]` shape or the status-only shape are parsed into an `ErrorCode` enum of about 65 codes. Unknown codes map to null.
- [ ] **FND-05**: Only authed paths carry the access token (port of `needAuthToken`). On expiry, a single-flight refresh runs (an OkHttp `Authenticator` guarded by a Mutex, so a refresh token is never reused), the request is retried once, and a failed refresh logs the user out.
- [ ] **FND-06**: Any `403 ACCOUNT_SUSPENDED` response logs the user out and shows an explanatory message.
- [ ] **FND-07**: Tokens persist in EncryptedSharedPreferences. The cached profile, `currentMode` and `hasSeenOnboarding` persist in DataStore. A returning user is auto-logged in on launch.
- [ ] **FND-08**: User can log out. Logout clears the tokens, the cached profile and the onboarding flag.
- [ ] **FND-09**: The app uses a dark Material 3 color scheme (surface `#131313`, primary `#44f697`, primary container `#00d97e`, etc.) with a bundled rounded font (e.g. Nunito). The ~40 per-screen iOS `*Brand.swift` color sets are merged into this theme, not copied.
- [ ] **FND-10**: Screens can use these shared components: press-scale button, async save button, validated text field, avatar, glow background, Custom Tab launcher.
- [ ] **FND-11**: The app starts with the Android 12+ SplashScreen API (no fixed 3-second delay) and routes through `AppRoute`: splash → login | signupSetup | clientHome | expertHome.
- [ ] **FND-12**: Client mode has a 5-tab scaffold (Explore, Search placeholder, Bookings, Chats, Profile). Expert mode has a 5-tab scaffold (Dashboard, Bookings, Topics, Chats, Profile). Each tab keeps its own saved back stack.
- [ ] **FND-13**: A throwaway spike app adds a card with Stripe PaymentSheet in SetupIntent mode and completes a `PaymentLauncher` 3DS challenge on a connected account.
- [ ] **FND-14**: A throwaway spike app joins an Agora audio channel from inside a `microphone`-type foreground service, and audio keeps running while the app is in the background.

### Authentication

From the PRD's login flow (Week 2).

- [ ] **AUTH-01**: User can log in with email and password and lands in the right destination for their account state and mode.
- [ ] **AUTH-02**: A user who forgot their password can request an OTP, verify it, set a new password, see the password-changed confirmation, and log in with the new password.

### Signup

From the PRD's signup flow (Week 2; iOS `SignupAccumulator.swift`).

- [ ] **SIGN-01**: A new user can create an account on the signup Initial step. There are no username or phone fields (SIGNUP-USERNAME-OPTIONAL.md, PHONE-NUMBER-REMOVED.md).
- [ ] **SIGN-02**: User can complete the Overview hub steps in any order: Basic Info + language picker, Timezone, Profile Photo, Verify Email, Tags. Each step saves through `PATCH /expert/profile/setup`.
- [ ] **SIGN-03**: The email verification code is sent only when the user taps the button, never automatically, with a 30-second resend cooldown (EMAIL-VERIFICATION-ON-REQUEST.md).
- [ ] **SIGN-04**: User can review their profile and publish it (Review → Publish).
- [ ] **SIGN-05**: A user with an unfinished signup is resumed into setup at login or splash, and completed steps are kept.

### Onboarding & Modes

From the PRD's onboarding and mode switch (Week 2).

- [ ] **ONB-01**: The onboarding carousel shows on the first launch into client home and not again until logout clears the flag.
- [ ] **ONB-02**: User can switch between client and expert mode on the mode switch screen (local only; port of `ModeSwitchView`), and the mode persists across launches.

### Realtime

From the PRD's realtime service (Week 2; iOS `Realtime/RealtimeService.swift`). Infrastructure only.

- [ ] **RT-01**: While logged in, the app holds a socket.io connection (socket.io-client-java v2+, websocket transport) authenticated with the JWT in `auth: {token}`, and it reconnects every 2 seconds when the connection drops.
- [ ] **RT-02**: After each token refresh, the socket reconnects with the new token.
- [ ] **RT-03**: The `message.*`, `typing.*`, `user.status`, `booking.update` and `call.ended` events are exposed to the app as a `SharedFlow`.

### Discovery

From the PRD's Explore and Expert Profile (Week 3).

- [ ] **DISC-01**: Client sees the Explore home from `getClientHome`: categories, featured topics, today's sessions and expert cards. The hero and visuals may be simplified.
- [ ] **DISC-02**: Categories are cached in a `CategoryStore` with a 7-day TTL.
- [ ] **DISC-03**: Client can open an Expert Profile with a hero, stats, about, topics, a reviews preview, and a Book button that starts the booking flow.

### Client Booking

From the PRD's Make a Booking (booking flow and free path) and Request Sent (Week 3), and My Bookings, Upcoming Booking Details, Cancellation and Booking Summary (Week 4).

- [ ] **BOOK-01**: Client can make a booking by picking a topic, then a day on a custom month calendar and a slot, then a meeting type and optional notes.
- [ ] **BOOK-02**: Slots load for the previous, current and next month. They are regrouped by the client's local date using `startUtc`, with duplicates dropped. The expert's timezone is shown, and `held` slots can't be selected.
- [ ] **BOOK-03**: Client can book a free topic (`POST /bookings/request`).
- [ ] **BOOK-04**: After sending a booking request, the client sees the Request Sent confirmation screen. The staggered animation is optional.
- [ ] **BOOK-05**: Client can view My Bookings with Upcoming and Past tabs. A `completed` booking whose end time hasn't passed stays in Upcoming. Past items are labeled completed, cancelled (cancelled or declined) or missed (missed or expired).
- [ ] **BOOK-06**: Client can open Upcoming Booking Details, which shows the "Waiting for expert" banner, the join card and a Cancel action. There is no Reschedule action.
- [ ] **BOOK-07**: Client can cancel a booking with a reason picker and a refund preview through `cancelClientBooking`. Whether to collect and send the cancellation comment is an open item for Phase 8 planning; iOS collects it but never sends it.
- [ ] **BOOK-08**: Client can open a minimal Booking Summary with the session details.
- [ ] **BOOK-09**: Client can accept or decline a reschedule the expert proposed, so bookings in `pending_reschedule` can move forward.

### Payments

From the PRD's Payment Methods and Make a Booking paid paths (Week 3).

- [ ] **PAY-01**: Client can view saved cards in Payment Methods, set a default card and delete a card.
- [ ] **PAY-02**: Client can add a card through a SetupIntent and Stripe PaymentSheet in setup mode.
- [ ] **PAY-03**: Client can book a short-lead paid slot. The sequence is `payment-intent` (with a `booking_attempt_id` UUID that is reused on retry), then setup-intent, then PaymentSheet `FlowController`, then `confirm`.
- [ ] **PAY-04**: Client can book a long-lead paid slot. On `422 LONG_LEAD_REQUIRES_SAVED_CARD`, the booking confirms with the saved card, or the client is sent to Payment Methods if they have no card.
- [ ] **PAY-05**: Client can complete a 3DS payment. On `402 PAYMENT_REQUIRES_ACTION`, the app runs `PaymentLauncher` with `stripeAccountId`, then retries confirm.
- [ ] **PAY-06**: Each of the ~15 error codes handled by iOS `handlePaidFlowError` shows the matching message or next action.
- [ ] **PAY-07**: Prices and charges display correctly. Amounts are in minor units everywhere except `payment-intent` amounts, and JPY has no decimal places.

### Voice Call

From the PRD's Call section (Week 4; iOS `Managers/CallCenter.swift`, `AgoraManager.swift`, `Call/Calling/`) and the Join wiring from Upcoming Booking Details.

- [ ] **CALL-01**: Client can tap Join on Upcoming Booking Details, including when it was opened from My Bookings (unlike iOS). This calls `POST /bookings/:id/join`. An `agoraConfig` response starts an in-app audio call; an `externalConfig.joinUrl` response opens in a Custom Tab or intent.
- [ ] **CALL-02**: User is asked for microphone permission with a rationale. A user who denies it sees a dialog explaining how to recover.
- [ ] **CALL-03**: The call runs in a `microphone`-type foreground service with audio focus and a persistent notification, and it keeps going when the app is in the background.
- [ ] **CALL-04**: A root-level call overlay shows the full-screen call UI. It minimizes to a pill that stays visible across tabs and modes, and restricted actions show a "you're in a call" guard.
- [ ] **CALL-05**: When both sides have joined, the app sends `PATCH confirm-connection` exactly once.
- [ ] **CALL-06**: A `call.ended` event from the peer switches the UI to "waiting for peer". It doesn't end the call.
- [ ] **CALL-07**: The call timer counts from the scheduled start and turns amber in the last 5 minutes. The overtime cutoff is 3600 seconds, not the iOS 5-minute test value.
- [ ] **CALL-08**: Hanging up leaves the channel and then calls `POST /bookings/:id/end`. The booking stays open, and the user can rejoin for the whole window (CALL-END-SEMANTICS.md).

### Expert

From the PRD's Topics + Topic Editor, Availability, Expert Dashboard, Incoming Requests, and Expert Upcoming Bookings + Booking Summary (Week 5).

- [ ] **EXP-01**: Expert can see their topics in the Topics tab and create or edit a topic in the Topic Editor. An expert needs at least one topic to receive bookings.
- [ ] **EXP-02**: The topic price floor comes from `pricing_limits` in `/me`, fetched fresh rather than cached across launches (PRICING-LIMITS.md). A `422 PRICE_BELOW_MINIMUM` error shows on the price field.
- [ ] **EXP-03**: Expert can edit weekly availability (slots stored as minutes from midnight, sent as `HH:mm`) using a time-range picker sheet, with overlap validation per day.
- [ ] **EXP-04**: Expert can pick the timezone for their availability. If the server timezone is UTC, the device timezone is filled in quietly.
- [ ] **EXP-05**: Expert sees a simplified Dashboard: pending request count, upcoming sessions, weekly earnings text and a payout onboarding card.
- [ ] **EXP-06**: Expert can open Incoming Requests and either accept a request with an optional message or decline it with a reason code. After accepting, they see the Confirmed screen.
- [ ] **EXP-07**: Expert can add a confirmed session to their calendar through an `Intent.ACTION_INSERT` calendar intent.
- [ ] **EXP-08**: Expert can view Upcoming and Past bookings. A Join button appears inside the join window and opens the same call experience the client uses.
- [ ] **EXP-09**: From the expert Booking Summary, the expert can accept or decline a client's reschedule request (handling `409 SLOT_UNAVAILABLE`) and can cancel the booking.

### Chat

From the PRD's Chat section (Week 6).

- [ ] **CHAT-01**: User can view their conversations list.
- [ ] **CHAT-02**: User can read a conversation in pages of 30 messages, newest first, with `loadEarlier` prepending older pages. Booking system-event rows appear inline. Messages are text only.
- [ ] **CHAT-03**: A sent message appears immediately (optimistic send) and is reconciled with the server copy by `client_msg_id`.
- [ ] **CHAT-04**: User sees read status and typing indicators driven by realtime events.
- [ ] **CHAT-05**: User can start a chat (`initiateChat`) from Expert Profile, from a booking and from a request.

### Safety

From the PRD's Report / block / unblock (Week 6; USER-REPORTING.md, CHAT-UNBLOCK.md). Store-required and never cut.

- [ ] **SAFE-01**: User can report another user from chat and from Expert Profile. The app sends one of the six report-reason enum values, not a display label.
- [ ] **SAFE-02**: User can block another user from chat and from Expert Profile.
- [ ] **SAFE-03**: In a blocked thread the composer is disabled and a banner is shown. Unblock appears only when `blocked_by_me` is true. When the other side blocked the user, the thread shows "You can't reply to this conversation" with no Unblock button.
- [ ] **SAFE-04**: `THREAD_BLOCKED`, `RECIPIENT_UNAVAILABLE` and `403 NOT_BLOCKER` responses show clear messages instead of generic failures.

### Push & Notifications

From the PRD's Push (FCM) and Notifications list (Week 6).

- [ ] **PUSH-01**: On every app start, the device's FCM token is registered through `POST /notifications/tokens` with `platform: "android"`. It is unregistered on logout.
- [ ] **PUSH-02**: Notifications post to defined notification channels. On Android 13+, users are asked for the `POST_NOTIFICATIONS` permission.
- [ ] **PUSH-03**: The ported `PushRouter` maps category/sub_category, with the legacy `event_type` fallback, to a `PushDestination`.
- [ ] **PUSH-04**: Tapping a push opens the right screen inside the right tab's back stack, both from a cold start and while the app is running.
- [ ] **PUSH-05**: User can view a notifications list grouped by date. Tapping an item routes through the same `PushDestination` map.

### Reviews

From the PRD's Write Review (Week 6).

- [ ] **REV-01**: User can write a review of a completed session, reached from the `session/ended` and `review/nudge` pushes and from Booking Summary.

### Payouts

From the PRD's Payouts, minimal (Week 7; EXPERT-EARNINGS-BREAKDOWN.md, EXPERT-EARNINGS-TRANSACTIONS.md).

- [ ] **PAYOUT-01**: Expert can see their payout balance and Stripe Connect status.
- [ ] **PAYOUT-02**: Expert can complete Stripe Connect onboarding in a Chrome Custom Tab opened on the hosted Account Link. The `clickme://connect/return` deep link re-fetches the status, and `clickme://connect/refresh` calls `refreshConnectLink`.
- [ ] **PAYOUT-03**: Expert sees a short recent-earnings list that headlines `net_amount`. Because `net_amount` can be null, `fee_source` is checked first. Transactions are read as cents and `earnings_breakdown` as dollars.

### Profile & Settings

From the PRD's Profile hubs and settings (Week 7).

- [ ] **PROF-01**: User can open the client and expert profile hubs.
- [ ] **PROF-02**: User can edit their profile and change their avatar with the system photo picker.
- [ ] **PROF-03**: User can change their password from Account Security.
- [ ] **PROF-04**: User can delete their account from inside the app. This is a Play Store requirement and is never cut.
- [ ] **PROF-05**: User can open Help/FAQ, which includes a `mailto:` support contact link.

### Polish

From the PRD's Polish (Week 7).

- [ ] **POL-01**: Every MVP screen has loading, empty and error states.
- [ ] **POL-02**: Core flows survive configuration changes and process death without losing user input or position.
- [ ] **POL-03**: The app draws edge-to-edge with correct insets for system bars, display cutouts and the IME.
- [ ] **POL-04**: Core flows pass a TalkBack check.

### Cross-Platform QA

From the PRD's Week 8 QA and cross-platform matrix.

- [ ] **QA-01**: Bugs reported from internal testing are triaged, and the release-blocking ones are fixed.
- [ ] **QA-02**: The cross-platform matrix passes with an Android client and an iOS expert: Stripe test cards including 3DS, push from a killed app, a call kept alive in the background and across rejoin, and payout onboarding.
- [ ] **QA-03**: The same matrix passes with an iOS client and an Android expert.

### Play Store & Release

From the PRD's Play Store account (Week 1), Start internal testing (Week 6), and Play Console submission and production release (Week 8).

- [ ] **REL-01**: The corporation's D-U-N-S number is confirmed, or a new one is requested.
- [ ] **REL-02**: A Play Console organization developer account is created under the corporation and submitted for Google's organization verification.
- [ ] **REL-03**: The package `com.uptrendinvestments.clickmeapp` is registered in Play Console by uploading a first signed internal build as soon as org verification allows.
- [ ] **REL-04**: The store listing, data safety form and foreground-service (microphone) declaration are drafted in Play Console ahead of release, so policy review isn't on the critical path.
- [ ] **REL-05**: A build is on the internal testing track with a few real users on a mix of devices.
- [ ] **REL-06**: The Play Console release requirements are complete: data safety form, foreground-service (microphone) declaration and video, account-deletion URL, and content rating.
- [ ] **REL-07**: The production release is live on Google Play.

## MVP Cut Order

If the schedule slips, cut in this order and still stay within the MVP:

1. Payouts shrink to the onboarding link only. PAYOUT-02 stays; PAYOUT-01 and PAYOUT-03 can drop.
2. Drop the notifications list (PUSH-05). Push deep links alone are enough.
3. Write Review (REV-01) moves to a quick update.

Never cut: account deletion (PROF-04), report/block (SAFE-01 to SAFE-04), the Stripe paths (PAY-01 to PAY-07) and the call paths (CALL-01 to CALL-08).

## v2 Requirements

Deferred to after the MVP, in the PRD's suggested order. Tracked but not in the current roadmap.

### Deferred

- DEF-1: Starting a reschedule, for clients and experts (the Reschedule screens with the slot picker). It's the riskiest flow, and until then users can cancel and book again. Responding to a reschedule the other side proposed is not deferred (BOOK-09, EXP-09). Backend spec for later: RESCHEDULE-PAYMENT-CONVERSION.md.
- DEF-2: Search tab with filters and sort. The MVP has a placeholder tab (FND-12).
- DEF-3: Favorites, the full Reviews list, Booking Notes (both sides), Client Profile (expert side).
- DEF-4: Client payment history (`/client/transactions`; a page past the end returns a 500, so treat it as end of list) and the expert transactions list.
- DEF-5: Tax Profile. Stripe's hosted onboarding already collects identity details.
- DEF-6: Notification preferences, Feedback, and the Expertise-tag editor in expert settings.
- DEF-7: Cancellation Policies static page, Categories modal, cosmetic animations.
- DEF-8: Analytics endpoints and the Firebase Ads conversion SDK.

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| Android expert proposing a reschedule | No PRD section covers it. It falls under starting a reschedule (DEF-1). |
| Porting `Debug/PreviewSecrets.swift` | It contains a hard-coded JWT. Rotating that JWT is a job for the backend/iOS owners, not Android code. |
| Porting `PreviewNavHarness` | iOS preview tooling only |
| Porting the iOS debug logging | Use OkHttp's logging interceptor in debug builds only |
| Porting the `Call/Ready` screen | Nothing in iOS uses it |
| Porting `NotificationQuietHoursCard` | Nothing in iOS uses it |
| Pixel parity with iOS | The iOS design is adapted to Material 3 |
| Porting all ~114 endpoints up front | API groups are ported one feature at a time |

## Traceability

Which phase covers each requirement. Filled in during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| FND-01 | Phase 1 | Pending |
| FND-02 | Phase 1 | Pending |
| FND-03 | Phase 2 | Pending |
| FND-04 | Phase 2 | Pending |
| FND-05 | Phase 2 | Pending |
| FND-06 | Phase 2 | Pending |
| FND-07 | Phase 2 | Pending |
| FND-08 | Phase 2 | Pending |
| FND-09 | Phase 1 | Pending |
| FND-10 | Phase 1 | Pending |
| FND-11 | Phase 1 | Pending |
| FND-12 | Phase 1 | Pending |
| FND-13 | Phase 2 | Pending |
| FND-14 | Phase 2 | Pending |
| AUTH-01 | Phase 3 | Pending |
| AUTH-02 | Phase 3 | Pending |
| SIGN-01 | Phase 4 | Pending |
| SIGN-02 | Phase 4 | Pending |
| SIGN-03 | Phase 4 | Pending |
| SIGN-04 | Phase 4 | Pending |
| SIGN-05 | Phase 4 | Pending |
| ONB-01 | Phase 4 | Pending |
| ONB-02 | Phase 4 | Pending |
| RT-01 | Phase 3 | Pending |
| RT-02 | Phase 3 | Pending |
| RT-03 | Phase 3 | Pending |
| DISC-01 | Phase 5 | Pending |
| DISC-02 | Phase 5 | Pending |
| DISC-03 | Phase 5 | Pending |
| BOOK-01 | Phase 6 | Pending |
| BOOK-02 | Phase 6 | Pending |
| BOOK-03 | Phase 6 | Pending |
| BOOK-04 | Phase 6 | Pending |
| BOOK-05 | Phase 8 | Pending |
| BOOK-06 | Phase 8 | Pending |
| BOOK-07 | Phase 8 | Pending |
| BOOK-08 | Phase 8 | Pending |
| BOOK-09 | Phase 8 | Pending |
| PAY-01 | Phase 7 | Pending |
| PAY-02 | Phase 7 | Pending |
| PAY-03 | Phase 7 | Pending |
| PAY-04 | Phase 7 | Pending |
| PAY-05 | Phase 7 | Pending |
| PAY-06 | Phase 7 | Pending |
| PAY-07 | Phase 7 | Pending |
| CALL-01 | Phase 9 | Pending |
| CALL-02 | Phase 9 | Pending |
| CALL-03 | Phase 9 | Pending |
| CALL-04 | Phase 9 | Pending |
| CALL-05 | Phase 9 | Pending |
| CALL-06 | Phase 9 | Pending |
| CALL-07 | Phase 9 | Pending |
| CALL-08 | Phase 9 | Pending |
| EXP-01 | Phase 10 | Pending |
| EXP-02 | Phase 10 | Pending |
| EXP-03 | Phase 10 | Pending |
| EXP-04 | Phase 10 | Pending |
| EXP-05 | Phase 11 | Pending |
| EXP-06 | Phase 11 | Pending |
| EXP-07 | Phase 11 | Pending |
| EXP-08 | Phase 11 | Pending |
| EXP-09 | Phase 11 | Pending |
| CHAT-01 | Phase 12 | Pending |
| CHAT-02 | Phase 12 | Pending |
| CHAT-03 | Phase 12 | Pending |
| CHAT-04 | Phase 12 | Pending |
| CHAT-05 | Phase 12 | Pending |
| SAFE-01 | Phase 12 | Pending |
| SAFE-02 | Phase 12 | Pending |
| SAFE-03 | Phase 12 | Pending |
| SAFE-04 | Phase 12 | Pending |
| PUSH-01 | Phase 13 | Pending |
| PUSH-02 | Phase 13 | Pending |
| PUSH-03 | Phase 13 | Pending |
| PUSH-04 | Phase 13 | Pending |
| PUSH-05 | Phase 13 | Pending |
| REV-01 | Phase 13 | Pending |
| PAYOUT-01 | Phase 14 | Pending |
| PAYOUT-02 | Phase 14 | Pending |
| PAYOUT-03 | Phase 14 | Pending |
| PROF-01 | Phase 14 | Pending |
| PROF-02 | Phase 14 | Pending |
| PROF-03 | Phase 14 | Pending |
| PROF-04 | Phase 14 | Pending |
| PROF-05 | Phase 14 | Pending |
| POL-01 | Phase 15 | Pending |
| POL-02 | Phase 15 | Pending |
| POL-03 | Phase 15 | Pending |
| POL-04 | Phase 15 | Pending |
| QA-01 | Phase 16 | Pending |
| QA-02 | Phase 16 | Pending |
| QA-03 | Phase 16 | Pending |
| REL-01 | Phase 1 | Pending |
| REL-02 | Phase 1 | Pending |
| REL-03 | Phase 2 | Pending |
| REL-04 | Phase 13 | Pending |
| REL-05 | Phase 13 | Pending |
| REL-06 | Phase 17 | Pending |
| REL-07 | Phase 17 | Pending |

**Coverage:**
- v1 requirements: 99 total
- Mapped to phases: 99
- Unmapped: 0

---
*Requirements defined: 2026-10-19*
*Last updated: 2026-10-19 after roadmap creation (17 phases)*
