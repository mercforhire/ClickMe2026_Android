# ClickMe Android — MVP Port Roadmap

**Window:** 2026-10-19 → ~2026-12-13 (7 build weeks + 1 week of QA and release)
**Source of truth:** `ios-reference/` (behavior) and `backend-reference/docs/ios/` (contract)
**Design:** iOS design adapted to Material 3; don't chase pixel parity.

## Why 8 weeks, not 4

The first draft fit everything into 4 weeks. That only works if nothing slips:
- Week 3 had expert core, the Agora call **and** realtime.
- Week 4 had chat, push, payouts, settings and store compliance.

Each of those is roughly a week of work on its own. This version gives each high-risk piece its own week (Make a Booking and Stripe, the call, push and chat). It also leaves time for setting up the Play Store account and getting through review (see below).

## Scope at a glance

The iOS app is about 50k lines of screen code across roughly 70 screen folders, plus about 9k lines of networking, models and managers. The MVP keeps every path where **money or a session** is involved: sign up, discover, book and pay, accept, call, and get paid. It also keeps what the stores require (account deletion, report/block, support contact). Everything else is listed under **Deferred** below.

Both modes are in scope. Every signup creates an expert-role account, and experts have to be able to accept requests from their phone.

**Testing tip:** the iOS app is finished. Use it as the "other side" for every flow; for example, book from Android and accept on iOS. You don't need to finish both modes on Android before you can test end to end.

## Play Store account (start this week)

We'll publish from an **organization** Play developer account under the corporation. That means the 14-day closed test required of new personal accounts doesn't apply.

Org accounts have their own lead time, though:
- They need a **D-U-N-S number** for the corporation. Look up whether you already have one; getting a new one can take a few days to several weeks.
- Google then verifies the organization, which can take days.

**Do this in Week 1**, so the account is ready long before release:
1. Get or confirm the D-U-N-S number.
2. Create the Play Console org account.
3. Register the package name and upload a first internal build early.

Set up the listing, data safety form and foreground-service declaration ahead of time too. Policy review can take several days.

---

## Week 1 (Oct 19–25): Foundation and SDK spikes

### 1. Project scaffolding
- Hilt, Retrofit + Moshi, OkHttp, coroutines, Coil, Navigation Compose
- Build flavors (dev/prod). All iOS environments point at the production backend: `https://clickme-backend-production.up.railway.app/api/v1`
- Config: Stripe publishable key, Agora App ID
- Lint and baseline CI

### 2. Networking core (`Network/NetworkService.swift`, `APIRequestURLs.swift`)
- Moshi with snake_case. Add a custom date adapter, because the backend sends ISO-8601 with **microseconds**.
- Error envelope `{status, code?, message}` plus an `ErrorCode` enum (~65 codes, unknown codes map to null). Also handle the `errors[]` and status-only error shapes.
- Wrapper for `{data: …}` responses and `PaginationMeta` (page-based).
- Auth header only on authed paths (port `needAuthToken`).
- **Single-flight token refresh** as an OkHttp `Authenticator` guarded by a Mutex. The backend revokes the session if a refresh token is reused. Retry once; log out if the refresh itself fails.
- Global `403 ACCOUNT_SUSPENDED` handling: log out and show a message.
- Port the API groups gradually, one per feature, instead of all ~114 endpoints up front.

### 3. Session / UserManager (`Managers/UserManager.swift`)
- Tokens in EncryptedSharedPreferences. Cached profile, `currentMode` and `hasSeenOnboarding` in DataStore.
- Auto-login and logout (logout clears the onboarding flag and unregisters the push token).

### 4. Design system (`Theme/Brand.swift`)
- Dark `darkColorScheme`: surface `#131313`, primary `#44f697`, primary container `#00d97e`, etc.
- Bundle a rounded font (e.g. Nunito); iOS uses SF Rounded.
- **Merge** the ~40 per-screen `*Brand.swift` color sets into the theme. Don't copy them over.
- Shared components: press-scale button, async save button, validated text field, avatar, glow background, Custom Tab launcher.

### 5. App shell
- `AppRoute`: splash → login | signupSetup | clientHome | expertHome.
- Android 12 SplashScreen API instead of the iOS 3-second delay.
- Two 5-tab scaffolds, each tab with its own saved back stack:
  - Client: Explore, Search\*, Bookings, Chats, Profile
  - Expert: Dashboard, Bookings, Topics, Chats, Profile
- \*Search can be a placeholder tab at first.

### 6. SDK spikes (throwaway test apps)
- Stripe PaymentSheet in SetupIntent mode, plus `PaymentLauncher` 3DS on a connected account.
- Agora audio join, running inside a foreground service.
- Goal: find SDK and Gradle problems now, not in Week 3 or 4.

**End of Week 1:** the app launches into an empty tab layout. Login and token refresh work against the real backend, tested with a temporary login screen.

---

## Week 2 (Oct 26–Nov 1): Auth, signup, realtime plumbing

### 1. Login flow
- Login, forgot password (OTP), set new password, password changed.

### 2. Signup flow (`SignupAccumulator.swift`)
- Initial step creates the account.
- Then the Overview hub, which the user can complete in any order: Basic Info + language picker, Timezone, Profile Photo, Verify Email, Tags.
- Then Review → Publish.
- Each step saves through `PATCH /expert/profile/setup`.
- Resume an unfinished signup at login/splash.
- Send the email verification code only when the user taps the button (30s cooldown). Don't send it automatically.
- No username or phone fields.

### 3. Onboarding and mode switch
- Onboarding carousel, shown on first launch into client home.
- Mode switch screen (local only, `ModeSwitchView`).

### 4. Realtime service (`Realtime/RealtimeService.swift`), infrastructure only
- socket.io-client-java v2+, websocket transport, JWT in `auth: {token}`, reconnect every 2s.
- Reconnect with the new token after each token refresh.
- Expose events as a `SharedFlow`: `message.*`, `typing.*`, `user.status`, `booking.update`, `call.ended`.
- No UI yet. Verify it with logging.

**End of Week 2:** a user can sign up, finish setup, log in again and switch modes. The socket connects and receives events.

---

## Week 3 (Nov 2–8): Client discovery and booking (highest risk)

### 1. Explore
- `getClientHome`: categories, featured topics, today's sessions, expert cards.
- Simplify the hero and visuals if time runs short.
- `CategoryStore` cache with a 7-day TTL.

### 2. Expert Profile
- Hero, stats, about, topics, reviews preview, Book button.
- Chat button and report/block are added in Week 6.

### 3. Payment Methods (Settings)
- List, set default, delete.
- Add a card with a SetupIntent and **Stripe PaymentSheet** (setup mode).
- Required because long-lead bookings charge a saved card.

### 4. Make a Booking (the biggest client screen)
- Topic picker, then a **custom month calendar and slot picker**, then meeting type and notes.
- **Timezones:**
  - Load the previous, current and next month.
  - Regroup slots by the client's local date using `startUtc`, and drop duplicates.
  - Show the expert's timezone.
  - Treat `held` slots as unavailable.
- **Payment paths:**
  - Free: `POST /bookings/request`.
  - Short-lead paid: `payment-intent` (with a `booking_attempt_id` UUID that is reused on retry), then setup-intent, then PaymentSheet `FlowController`, then `confirm`.
  - Long-lead paid: the server returns `422 LONG_LEAD_REQUIRES_SAVED_CARD`; confirm with the saved card, or send the user to Payment Methods if they have none.
  - 3DS: `402 PAYMENT_REQUIRES_ACTION`; run `PaymentLauncher` with `stripeAccountId`, then retry confirm.
- Port the ~15 error codes in `handlePaidFlowError`.
- Money units: minor units everywhere except `payment-intent` amounts. JPY has no decimal places.

### 5. Request Sent
- Confirmation screen; the staggered animation is optional.

**End of Week 3:** an Android client can book and pay (free, short-lead, long-lead and 3DS test cards), and an iOS expert receives the request.

---

## Week 4 (Nov 9–15): Client bookings and the call

### 1. My Bookings
- Upcoming/Past tabs. A `completed` booking whose end time hasn't passed stays in Upcoming.
- Past labels: completed, cancelled (cancelled or declined), missed (missed or expired).

### 2. Upcoming Booking Details
- "Waiting for expert" banner, join card, Cancel.
- **Wire up Join here.** The iOS push from `MyBookingsView.swift:108` doesn't pass `onJoinCall`, so the button does nothing on iOS. Report that to the iOS side.

### 3. Cancellation
- Reason picker, refund preview, `cancelClientBooking`.

### 4. Booking Summary (minimal)
- Session details.
- **Accept/decline an expert-proposed reschedule.** Required: without it, bookings stuck in `pending_reschedule` can't move.

### 5. Call (`Managers/CallCenter.swift`, `AgoraManager.swift`, `Call/Calling/`)
- **Setup:**
  - `POST /bookings/:id/join` returns either `agoraConfig` (audio only) or `externalConfig.joinUrl` (opened in a Custom Tab or an intent).
  - Ask for mic permission (rationale plus a denied dialog).
  - Run the call in a **foreground service** of type `microphone`, with audio focus and a persistent notification.
- **During the call:**
  - Root-level overlay host: full-screen call UI that **minimizes to a pill** and stays visible across tabs and modes. Show a "you're in a call" guard on restricted actions.
  - When both sides have joined, send `PATCH confirm-connection` once.
  - A peer's `call.ended` event switches to "waiting for peer"; it doesn't end the call.
  - Timer counts from the scheduled start, with an amber warning in the last 5 minutes.
- **Ending:**
  - Hang-up leaves the channel, then calls `POST /bookings/:id/end`. The booking stays open, and the user can rejoin for the whole window (CALL-END-SEMANTICS.md).
  - **Overtime cutoff: use 3600s.** iOS still has the 5-minute test value (`MeetingCallViewModel.swift:532`).
- Skip the `Call/Ready` screen; nothing in iOS uses it.

**End of Week 4:** the client side is complete. An Android client and an iOS expert can finish a full paid session, including the app going to the background mid-call.

---

## Week 5 (Nov 16–22): Expert core

### 1. Topics + Topic Editor
- Experts can't receive bookings without at least one topic.
- Price floor comes from `/me` `pricing_limits`. Don't cache it across launches.
- Show `422 PRICE_BELOW_MINIMUM` on the price field.

### 2. Availability
- Weekly editor with slots as minutes-from-midnight, sent as `HH:mm`.
- Overlap validation per day and a time-range picker sheet.
- Timezone picker. If the server timezone is UTC, quietly fill in the device timezone.

### 3. Expert Dashboard (simplified)
- Pending request count, upcoming sessions, weekly earnings text, payout onboarding card.

### 4. Incoming Requests → Request decision → Confirmed
- Accept with an optional message; decline with a reason code.
- Add-to-calendar uses an `Intent.ACTION_INSERT` calendar intent.

### 5. Expert Upcoming Bookings + Booking Summary
- Upcoming/Past tabs; Join button inside the join window (reuses the Week 4 call).
- Accept/decline client reschedule requests (handle `409 SLOT_UNAVAILABLE`), and Cancel.

**End of Week 5:** the expert side works. An iOS client books, the Android expert accepts, and both join the call.

---

## Week 6 (Nov 23–29): Chat, push, notifications

### 1. Chat
- Conversations list.
- Chatting screen:
  - Paging: 30 messages per page, newest first, `loadEarlier` prepends.
  - Optimistic send reconciled by `client_msg_id`.
  - Read and typing events.
  - Booking system-event rows.
  - Text only.
- `initiateChat` entry points from Expert Profile, bookings and requests.

### 2. Report / block / unblock
- Required by the Play Store. Also add report/block to Expert Profile.
- Follow CHAT-UNBLOCK.md and USER-REPORTING.md:
  - **Send the six report reason enum values**, not display labels (an iOS bug).
  - Disable the composer and show a banner when blocked.
  - Handle `THREAD_BLOCKED` and `RECIPIENT_UNAVAILABLE`.

### 3. Push (FCM)
- Register with `POST /notifications/tokens` and `platform: "android"` (the backend schema accepts it). Re-register on every app start; unregister on logout.
- Port `PushRouter`: category/sub_category plus the legacy `event_type` fallback, mapped to destinations.
- Deep links into the right tab's back stack, from both a cold start and a running app.
- Notification channels.
- `POST_NOTIFICATIONS` permission (Android 13+).

### 4. Notifications list
- Grouped by date. Tapping routes through the same `PushDestination` map.

### 5. Write Review
- Reached from the `session/ended` and `review/nudge` pushes and from Booking Summary.

### 6. Start internal testing
- Put a build on the internal testing track for a few real users on a mix of devices. This isn't a Play requirement for org accounts; it's for catching bugs early.

**End of Week 6:** every MVP flow except payouts and settings works, and a build is with testers.

---

## Week 7 (Nov 30–Dec 6): Payouts, settings, polish

### 1. Payouts (minimal)
- Balance and Connect status.
- Stripe Connect onboarding:
  - Open the hosted Account Link in a **Chrome Custom Tab**.
  - Catch the `clickme://connect/return|refresh` deep link. On refresh, call `refreshConnectLink`; on return, re-fetch the status.
- Show a short recent-earnings list. Headline `net_amount`, which can be null, so check `fee_source` first.
- Units: transactions are in cents, `earnings_breakdown` is in dollars.

### 2. Profile hubs and settings
- Client and expert profile hubs, edit profile and avatar.
- Account Security (change password).
- **Delete account** (Play Store requirement).
- Help/FAQ with a `mailto:` support link.

### 3. Polish
- Empty, loading and error states on every MVP screen.
- Process death and configuration changes.
- Edge-to-edge and insets.
- TalkBack check on core flows.

---

## Week 8 (Dec 7–13): QA and release
- Fix bugs from internal testing.
- Cross-platform matrix, both directions (Android client with iOS expert, and the reverse):
  - Stripe test cards, including 3DS
  - Push from a killed app
  - Call kept alive in the background and across rejoin
  - Payout onboarding
- Play Console:
  - Data safety form
  - Foreground service (microphone) declaration and video
  - Account-deletion URL
  - Content rating
  - Production release

**Target public release: ~Dec 11–13.** No mandatory testing wait applies, so if Weeks 1–7 stay on schedule, release can move up to as early as ~Dec 7–8. Treat Week 8 as buffer.

---

## If you need to cut further
Cut in this order while staying in the MVP:
1. Payouts: show the onboarding link only.
2. Notifications list: push deep links alone are enough.
3. Write Review: it can come in a quick update.

Don't cut Delete account, report/block, or the Stripe and call paths.

## Deferred (post-MVP), in suggested order
1. Client-initiated **Reschedule**, both client and expert sides. It's the riskiest flow; until then, users can cancel and book again.
2. Search tab with filters and sort.
3. Favorites, the full Reviews list, Booking Notes (both sides), Client Profile (expert side).
4. Client payment history (`/client/transactions`; a page past the end returns a 500, so treat it as end of list) and the expert transactions list.
5. Tax Profile; Stripe's hosted onboarding already collects identity details.
6. Notification preferences, Feedback, Expertise-tag editor in expert settings.
7. Cancellation Policies static page, Categories modal, cosmetic animations.
8. Analytics endpoints and the Firebase Ads conversion SDK.

## Do not port
- `Debug/PreviewSecrets.swift`: a hard-coded JWT. **Rotate it.**
- `PreviewNavHarness`, debug logging (use OkHttp's logging interceptor in debug builds only), the unused `Call/Ready` screen, and the unused `NotificationQuietHoursCard`.

## iOS issues found during the survey (report back)
- Client Upcoming Booking Details: the Join button does nothing when opened from My Bookings.
- Overtime cutoff is still the 5-minute test value.
- Report reasons are sent as display labels instead of enum values.
- Chat doesn't show a blocked state.
- The cancellation comment is collected but never sent.

## Biggest schedule risks
1. **Make a Booking and Stripe** (Week 3): timezone regrouping, three payment paths and 3DS. The Week 1 spike reduces this risk.
2. **Call service and overlay** (Week 4): background audio and keeping the call across navigation. The Week 1 spike reduces this risk.
3. **Push deep-link routing** into per-tab back stacks (Week 6).
4. **Play Console org verification** (D-U-N-S number plus Google's review): an outside wait you can't control. Start in Week 1.
