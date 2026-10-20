# Roadmap: ClickMe Android

## Overview

This roadmap ports the finished ClickMe iOS app to Android as the v1.0 MVP. It runs from 2026-10-19 to ~2026-12-13: 7 build weeks plus 1 week for QA and release, from `docs/PORT_ROADMAP.md`. The work starts with the foundation and with Stripe/Agora SDK spikes, while the Play Console organization account's outside wait runs in the background. Then come auth, signup and realtime plumbing; client discovery and booking with every Stripe path; client bookings and the voice call; expert topics, availability, requests and bookings; chat, report/block and push; payouts, settings and polish; and finally cross-platform QA and the Google Play release. Each PRD week is split into 2 phases (3 for Week 3, the highest-risk week) so each phase can be planned and executed in 2–4 days. The finished iOS app is the "other side" of every end-to-end test. Phase success criteria are the PRD's end-of-week checkpoints, plus user-confirmed criteria for Weeks 7 and 8.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [ ] **Phase 1: Foundation, App Shell & Play Account** - Scaffolding, Material 3 theme, splash and two 5-tab scaffolds; D-U-N-S and Play org account started (Week 1)
- [ ] **Phase 2: Networking, Session & SDK Spikes** - Networking core, single-flight token refresh, session storage, Stripe and Agora spikes (Week 1)
- [ ] **Phase 3: Login, Password Recovery & Realtime** - Real login, OTP password reset, authenticated socket with event SharedFlow (Week 2)
- [ ] **Phase 4: Signup, Onboarding & Mode Switch** - Signup Initial → Overview hub → Review/Publish, resume, onboarding carousel, mode switch (Week 2)
- [ ] **Phase 5: Explore & Expert Profile** - Client Explore home with category cache and the Expert Profile screen (Week 3)
- [ ] **Phase 6: Booking Flow & Free Bookings** - Topic/calendar/slot picker with timezone regrouping, free booking, Request Sent (Week 3)
- [ ] **Phase 7: Payment Methods & Paid Booking** - Saved cards, short-lead, long-lead and 3DS paid paths, paid-flow errors (Week 3)
- [ ] **Phase 8: Client Bookings & Cancellation** - My Bookings, Upcoming Booking Details, cancellation with refund preview, reschedule response (Week 4)
- [ ] **Phase 9: Voice Call** - Join, Agora call in a microphone foreground service, overlay/pill, rejoin semantics (Week 4)
- [ ] **Phase 10: Expert Topics & Availability** - Topics + Topic Editor with price floor, weekly availability and timezone (Week 5)
- [ ] **Phase 11: Expert Requests & Bookings** - Expert Dashboard, Incoming Requests → decision → Confirmed, expert bookings with Join and reschedule response (Week 5)
- [ ] **Phase 12: Chat, Report & Block** - Conversations, realtime chatting, initiateChat entry points, report/block/unblock (Week 6)
- [ ] **Phase 13: Push, Notifications & Internal Testing** - FCM registration, PushRouter deep links, notifications list, Write Review, internal testing track (Week 6)
- [ ] **Phase 14: Payouts & Account Settings** - Stripe Connect onboarding and earnings, profile hubs, edit profile, change password, delete account, help (Week 7)
- [ ] **Phase 15: Polish & Hardening** - Loading/empty/error states everywhere, process death, edge-to-edge, TalkBack (Week 7)
- [ ] **Phase 16: Cross-Platform QA** - Internal-testing bug fixes and the two-way Android/iOS matrix (Week 8)
- [ ] **Phase 17: Play Store Release** - Play Console policy items and production release (Week 8)

## Phase Details

### Phase 1: Foundation, App Shell & Play Account
**Goal:** The themed app shell launches on a device, and the outside wait for the Play Console organization account has started.
**Target week:** Week 1 (Oct 19–25)
**Depends on:** Nothing (first phase)
**Requirements:** FND-01, FND-02, FND-09, FND-10, FND-11, FND-12, REL-01, REL-02
**Success Criteria** (what must be TRUE):
  1. The app launches through the Android 12+ splash screen into an empty, dark-themed tab layout. Both the client scaffold (Explore, Search placeholder, Bookings, Chats, Profile) and the expert scaffold (Dashboard, Bookings, Topics, Chats, Profile) render, and each tab keeps its own back stack when the user switches tabs. (PRD Week 1 checkpoint, part 1)
  2. The dev and prod flavors both build and target the production backend URL. The Stripe publishable key and Agora App ID come from build config. Lint and CI run and pass on every push.
  3. The shared components (press-scale button, async save button, validated text field, avatar, glow background, Custom Tab launcher) render in the Material 3 theme with the bundled rounded font.
  4. The corporation's D-U-N-S number is confirmed or requested, and the Play Console organization account is created and submitted for Google's verification. The outside wait is logged in STATE.md.
**Planning notes:** The Play account steps are non-code tasks for the user. Start them on day one, because a new D-U-N-S number can take weeks. Don't port `PreviewNavHarness` or the iOS debug logging; use OkHttp's logging interceptor in debug builds only.
**Plans:** 9 plans in 5 waves

Plans:
- [ ] 01-01-PLAN.md — Toolchain (Kotlin 2.4.21), locked dependency set, dev/prod flavors with BuildConfig keys, Hilt app, shared strings, ktlint, design-rules script, GitHub Actions CI (Wave 1)
- [ ] 01-02-PLAN.md — Play Console org account: confirm D-U-N-S, create account, pay fee, submit verification, log the outside wait in STATE.md (Wave 1, user checkpoints)
- [ ] 01-03-PLAN.md — Dark M3 design system: palette, extras, Nunito typography, shapes, spacing, motion, gallery theme section (Wave 2)
- [ ] 01-04-PLAN.md — Launcher icon and splash artwork from the SplashLogo mark, window/splash themes, manifest wiring, AppRoute + AppViewModel (Wave 2)
- [ ] 01-05-PLAN.md — Navigation 3 per-tab back stacks, Navigator (Back rule, re-tap, cross-tab), tab definitions, HomeTabScaffold (Wave 3)
- [ ] 01-06-PLAN.md — PressScaleButton, AsyncSaveButton, ValidatedField, EmptyState/ErrorState, PlaceholderTabContent, gallery controls section (Wave 3)
- [ ] 01-07-PLAN.md — Avatar, TopBarAvatarButton, GreenGlowBlobLayer, Custom Tab launcher, gallery media section (Wave 3)
- [ ] 01-08-PLAN.md — Client/expert home shells, Design system gallery, ClickMeApp AppRoute switch, MainActivity splash hook (Wave 4)
- [ ] 01-09-PLAN.md — Full suite + release build, push and green CI on GitHub, emulator smoke, device checklist (Wave 5)
**UI hint**: yes

### Phase 2: Networking, Session & SDK Spikes
**Goal:** A user can log in against the real backend and stay logged in through token refreshes, and the Stripe and Agora SDKs are proven on a device before the features that depend on them.
**Target week:** Week 1 (Oct 19–25)
**Depends on:** Phase 1
**Requirements:** FND-03, FND-04, FND-05, FND-06, FND-07, FND-08, FND-13, FND-14, REL-03
**Success Criteria** (what must be TRUE):
  1. A user can log in through a temporary login screen against the production backend and land in the tab layout. After a relaunch they're logged in automatically. Logging out returns them to login with the session cleared. (PRD Week 1 checkpoint, part 2)
  2. When the access token expires, concurrent requests trigger exactly one refresh and then succeed. A failed refresh logs the user out instead of reusing the refresh token. (PRD Week 1 checkpoint, part 2)
  3. A `403 ACCOUNT_SUSPENDED` response logs the user out with a message. Backend errors surface as parsed `ErrorCode`s, and unknown codes don't crash. Microsecond ISO-8601 dates decode correctly.
  4. The throwaway spike apps show three things working: a card saved through PaymentSheet in SetupIntent mode, a completed `PaymentLauncher` 3DS challenge on a connected account, and Agora audio that keeps playing from a microphone foreground service while the app is in the background.
  5. A signed release bundle of `com.uptrendinvestments.clickmeapp` builds. If org verification has cleared, the bundle is uploaded to the internal track, which registers the package name. If not, the upload is logged as an open blocker in STATE.md.
**Planning notes:** Port only the API groups this phase needs (auth, `/me`). Other groups come with their features. Token storage is locked to EncryptedSharedPreferences, which is deprecated upstream; keep it.
**Plans:** TBD

### Phase 3: Login, Password Recovery & Realtime
**Goal:** Users can log in and recover their password on the real screens, and the app keeps a live, authenticated realtime connection.
**Target week:** Week 2 (Oct 26–Nov 1)
**Depends on:** Phase 2
**Requirements:** AUTH-01, AUTH-02, RT-01, RT-02, RT-03
**Success Criteria** (what must be TRUE):
  1. A user can log in on the real Login screen, which replaces the temporary one, and sees the backend's error message when the credentials are wrong.
  2. A user who forgot their password can request an OTP, enter it, set a new password, see the password-changed screen, and log in with the new password.
  3. After login the socket connects. Events triggered from the iOS app (a message, typing, a booking update) appear in the `SharedFlow` logs. (PRD Week 2 checkpoint, part 2)
  4. After a token refresh or a network drop, the socket reconnects within about 2 seconds with the current token and keeps receiving events.
**Plans:** TBD
**UI hint**: yes

### Phase 4: Signup, Onboarding & Mode Switch
**Goal:** A new user can create an account, finish expert setup, and move between client and expert modes.
**Target week:** Week 2 (Oct 26–Nov 1)
**Depends on:** Phase 3
**Requirements:** SIGN-01, SIGN-02, SIGN-03, SIGN-04, SIGN-05, ONB-01, ONB-02
**Success Criteria** (what must be TRUE):
  1. A new user creates an account on the Initial step (no username or phone fields). They complete Basic Info + language, Timezone, Profile Photo, Verify Email and Tags in any order, then review and publish. (PRD Week 2 checkpoint, part 1)
  2. The verification code is sent only when the user taps the button, and resend stays disabled for 30 seconds afterward.
  3. A user who leaves signup halfway is resumed into setup at the next login or launch, with completed steps kept.
  4. After publishing, the user can log out and log in again. The onboarding carousel appears only on the first launch into client home. The user can switch between client and expert modes, and the mode is kept across relaunches. (PRD Week 2 checkpoint, part 1)
**Planning notes:** Profile Photo uses `PickVisualMedia` (platform swap). Each step saves through `PATCH /expert/profile/setup`.
**Plans:** TBD
**UI hint**: yes

### Phase 5: Explore & Expert Profile
**Goal:** A client can browse experts from the Explore home and open an expert's full profile to start a booking.
**Target week:** Week 3 (Nov 2–8)
**Depends on:** Phase 4
**Requirements:** DISC-01, DISC-02, DISC-03
**Success Criteria** (what must be TRUE):
  1. The client's Explore tab shows categories, featured topics, today's sessions and expert cards from `getClientHome`.
  2. Categories load from the local cache without a network fetch for 7 days, and refresh after the TTL expires.
  3. Tapping an expert card opens Expert Profile with the hero, stats, about, topics and reviews preview. Its Book button takes the client into the booking flow.
**Planning notes:** If time runs short, simplify the hero and visuals. The Chat button and report/block on Expert Profile come in Phase 12.
**Plans:** TBD
**UI hint**: yes

### Phase 6: Booking Flow & Free Bookings
**Goal:** A client can pick a topic and a correct local-time slot, then send a free booking request that reaches the expert.
**Target week:** Week 3 (Nov 2–8)
**Depends on:** Phase 5
**Requirements:** BOOK-01, BOOK-02, BOOK-03, BOOK-04
**Success Criteria** (what must be TRUE):
  1. From Expert Profile, the client picks a topic, a day on the custom month calendar, a slot, a meeting type and optional notes.
  2. With the iOS expert set to a different timezone, slots from the previous, current and next month appear under the client's correct local date with no duplicates. The expert's timezone is shown, and `held` slots can't be selected.
  3. An Android client books a free topic and sees Request Sent, and the iOS expert receives the request. (PRD Week 3 checkpoint, free path)
**Planning notes:** Highest-risk week. Regroup by `startUtc` into the client's local date. Test with the iOS app as the expert.
**Plans:** TBD
**UI hint**: yes

### Phase 7: Payment Methods & Paid Booking
**Goal:** A client can save cards and pay for a booking on every Stripe path, including 3DS.
**Target week:** Week 3 (Nov 2–8)
**Depends on:** Phase 6
**Requirements:** PAY-01, PAY-02, PAY-03, PAY-04, PAY-05, PAY-06, PAY-07
**Success Criteria** (what must be TRUE):
  1. In Payment Methods, the client can add a card through PaymentSheet in setup mode, set it as the default and delete it.
  2. An Android client books a short-lead paid slot through the PaymentSheet `FlowController`, and the iOS expert receives the request. A retry after a failure reuses the same `booking_attempt_id` and doesn't create a duplicate booking. (PRD Week 3 checkpoint)
  3. A long-lead paid booking confirms with the saved card. A client with no saved card is sent to Payment Methods and can finish the booking after adding one. (PRD Week 3 checkpoint)
  4. A 3DS test card triggers the challenge through `PaymentLauncher`, and the booking confirms once the challenge passes. (PRD Week 3 checkpoint)
  5. Paid-flow errors (the ~15 `handlePaidFlowError` codes) show the right message or next action. Prices display correctly in minor units, including JPY with no decimals.
**Planning notes:** Reuse what the Phase 2 Stripe spike found. `payment-intent` amounts are the one exception to minor units. Never cut.
**Plans:** TBD
**UI hint**: yes

### Phase 8: Client Bookings & Cancellation
**Goal:** A client can track their bookings, cancel with a refund preview, and respond to an expert's reschedule proposal.
**Target week:** Week 4 (Nov 9–15)
**Depends on:** Phase 7
**Requirements:** BOOK-05, BOOK-06, BOOK-07, BOOK-08, BOOK-09
**Success Criteria** (what must be TRUE):
  1. My Bookings shows Upcoming and Past tabs. A completed booking whose end time hasn't passed stays in Upcoming, and Past items are labeled completed, cancelled or missed.
  2. Upcoming Booking Details shows the "Waiting for expert" banner on pending bookings, plus the join card and Cancel. There is no Reschedule action.
  3. The client cancels with a reason after seeing the refund preview. The booking moves to Past as cancelled, and the iOS expert sees the cancellation.
  4. When the iOS expert proposes a reschedule, the Android client accepts or declines it from Booking Summary, and the booking leaves `pending_reschedule`.
**Planning notes:** OPEN ITEM to settle in planning: should Android collect and send the cancellation comment? iOS collects it but never sends it. Check the `cancelClientBooking` contract in `backend-reference/` first. Starting a reschedule is deferred (DEF-1). Only responding to one is in scope.
**Plans:** TBD
**UI hint**: yes

### Phase 9: Voice Call
**Goal:** An Android user can join and hold a full voice session that survives backgrounding and navigation, which completes the client side.
**Target week:** Week 4 (Nov 9–15)
**Depends on:** Phase 8 (also builds on the Phase 2 Agora spike and Phase 3 realtime events)
**Requirements:** CALL-01, CALL-02, CALL-03, CALL-04, CALL-05, CALL-06, CALL-07, CALL-08
**Success Criteria** (what must be TRUE):
  1. An Android client taps Join on Upcoming Booking Details, including when it was opened from My Bookings, and talks with an iOS expert. Bookings with an external join URL open in a Custom Tab instead. Denying microphone permission shows the rationale and recovery dialogs.
  2. The call keeps going, with a persistent notification, when the app is in the background or the screen is off. (PRD Week 4 checkpoint)
  3. The call UI minimizes to a pill that stays visible across tabs and mode switches, and restricted actions show the "you're in a call" guard.
  4. When the peer hangs up, the UI shows "waiting for peer" and the call doesn't end. After hanging up, the user can rejoin within the window. The timer counts from the scheduled start, turns amber in the last 5 minutes and cuts off at 3600 seconds.
  5. An Android client and an iOS expert finish a full paid session end to end, and `confirm-connection` is sent exactly once. (PRD Week 4 checkpoint: the client side is complete)
**Planning notes:** Second-biggest schedule risk (background audio, keeping the call across navigation). Don't port `Call/Ready`. Use 3600s, not the iOS 5-minute test value. Hang-up semantics follow CALL-END-SEMANTICS.md. Never cut.
**Plans:** TBD
**UI hint**: yes

### Phase 10: Expert Topics & Availability
**Goal:** An Android expert can publish bookable topics and weekly availability that clients can see.
**Target week:** Week 5 (Nov 16–22)
**Depends on:** Phase 9 (functionally needs only Phase 4's expert account and mode switch)
**Requirements:** EXP-01, EXP-02, EXP-03, EXP-04
**Success Criteria** (what must be TRUE):
  1. An Android expert creates and edits a topic in the Topics tab. A price below the floor from `/me` `pricing_limits` shows the `PRICE_BELOW_MINIMUM` error on the price field.
  2. The expert edits weekly availability with the time-range picker sheet. Overlapping ranges on the same day are rejected, and saved slots round-trip as `HH:mm`.
  3. The expert picks an availability timezone. If the server's timezone is UTC, the device timezone is filled in without a prompt.
  4. An iOS client sees the Android expert's new topic, with booking slots that match the saved availability.
**Plans:** TBD
**UI hint**: yes

### Phase 11: Expert Requests & Bookings
**Goal:** An Android expert can see their workload, accept requests, manage bookings and join sessions, so the expert side works end to end.
**Target week:** Week 5 (Nov 16–22)
**Depends on:** Phase 10
**Requirements:** EXP-05, EXP-06, EXP-07, EXP-08, EXP-09
**Success Criteria** (what must be TRUE):
  1. The expert Dashboard shows the pending request count, upcoming sessions, weekly earnings text and a payout onboarding card.
  2. When an iOS client books, the Android expert sees the request in Incoming Requests. They accept it with an optional message (or decline with a reason code), see the Confirmed screen, and can add the session to the device calendar.
  3. In expert bookings (Upcoming/Past), Join appears inside the join window, and the iOS client and the Android expert both join the call. (PRD Week 5 checkpoint)
  4. The expert can accept or decline a client's reschedule request, sees a clear message on `409 SLOT_UNAVAILABLE`, and can cancel a booking.
**Plans:** TBD
**UI hint**: yes

### Phase 12: Chat, Report & Block
**Goal:** Clients and experts can message each other in real time, and either side can report, block or unblock safely.
**Target week:** Week 6 (Nov 23–29)
**Depends on:** Phase 11 (entry points from bookings and requests; realtime from Phase 3)
**Requirements:** CHAT-01, CHAT-02, CHAT-03, CHAT-04, CHAT-05, SAFE-01, SAFE-02, SAFE-03, SAFE-04
**Success Criteria** (what must be TRUE):
  1. A user can open their conversations list and start a chat from Expert Profile, a booking or a request.
  2. Text messages between Android and iOS arrive in real time. Sent messages appear immediately and reconcile without duplicates. Typing and read indicators update live. Scrolling up loads earlier messages 30 at a time, and booking system events appear inline.
  3. A user can report another user (the backend accepts the reason enum value) and block them, from chat and from Expert Profile.
  4. In a blocked thread the composer is disabled and a banner shows. Only the blocker sees Unblock. The blocked side sees "You can't reply to this conversation". `THREAD_BLOCKED` and `RECIPIENT_UNAVAILABLE` show clear messages.
**Planning notes:** Where iOS differs, follow CHAT-UNBLOCK.md and USER-REPORTING.md (iOS sends display labels and has no blocked state). Report/block is store-required and never cut.
**Plans:** TBD
**UI hint**: yes

### Phase 13: Push, Notifications & Internal Testing
**Goal:** Push notifications bring users to the right screen, reviews can be written, and a build is with real testers, so every MVP flow except payouts and settings works.
**Target week:** Week 6 (Nov 23–29)
**Depends on:** Phase 12 (the internal track also needs Play org verification from Phase 1 to have cleared)
**Requirements:** PUSH-01, PUSH-02, PUSH-03, PUSH-04, PUSH-05, REV-01, REL-04, REL-05
**Success Criteria** (what must be TRUE):
  1. The device registers its FCM token with `platform: "android"` on every app start and unregisters it on logout. On Android 13+ the user is asked for notification permission, and notifications use the defined channels.
  2. Tapping a booking, chat or session push opens the right screen in the right tab's back stack, both from a killed app and while the app is running.
  3. The notifications list groups items by date, and tapping an item routes the same way a push does.
  4. A user can write a review from the `session/ended` and `review/nudge` pushes and from Booking Summary.
  5. A build is on the internal testing track with a few real users on a mix of devices. The store listing, data safety form and foreground-service declaration are drafted in Play Console. (PRD Week 6 checkpoint: every MVP flow except payouts and settings works, and a build is with testers)
**Planning notes:** Third-biggest schedule risk: deep links into per-tab back stacks. If the schedule slips, the notifications list is cut #2 and Write Review is cut #3.
**Plans:** TBD
**UI hint**: yes

### Phase 14: Payouts & Account Settings
**Goal:** Experts can get paid through Stripe Connect, and any user can manage or delete their account.
**Target week:** Week 7 (Nov 30–Dec 6)
**Depends on:** Phase 13
**Requirements:** PAYOUT-01, PAYOUT-02, PAYOUT-03, PROF-01, PROF-02, PROF-03, PROF-04, PROF-05
**Success Criteria** (what must be TRUE):
  1. An Android expert completes Stripe Connect onboarding in a Custom Tab. Returning through `clickme://connect/return` refreshes the status, and an expired link recovers through `clickme://connect/refresh`. The expert sees their balance and Connect status. (Week 7 checkpoint, user-confirmed)
  2. The recent-earnings list shows correct amounts: cents for transactions and dollars for the breakdown. A null `net_amount` is handled by checking `fee_source`.
  3. Any user can open their profile hub, edit their profile and avatar, and change their password. (Week 7 checkpoint, user-confirmed)
  4. Any user can delete their account from the app and is then logged out, and Help/FAQ opens a `mailto:` support link. (Week 7 checkpoint, user-confirmed)
**Planning notes:** If the schedule slips, payouts are cut #1: keep only the onboarding link (PAYOUT-02). Account deletion is never cut.
**Plans:** TBD
**UI hint**: yes

### Phase 15: Polish & Hardening
**Goal:** Every MVP screen handles loading, empty and error states and holds up under real-device conditions.
**Target week:** Week 7 (Nov 30–Dec 6)
**Depends on:** Phase 14
**Requirements:** POL-01, POL-02, POL-03, POL-04
**Success Criteria** (what must be TRUE):
  1. Every MVP screen shows a loading state, an empty state and an error state with retry. (Week 7 checkpoint, user-confirmed)
  2. Rotating the device, or killing the process with "Don't keep activities" on, during signup, booking, chat or a call brings the user back to the same place with their input kept.
  3. The app draws edge-to-edge, and no content is hidden behind the system bars, display cutouts or the keyboard.
  4. A TalkBack user can log in, book a session, join a call and send a chat message.
**Plans:** TBD
**UI hint**: yes

### Phase 16: Cross-Platform QA
**Goal:** The Android app interoperates with the iOS app in both directions on every money and session path.
**Target week:** Week 8 (Dec 7–13)
**Depends on:** Phase 15
**Requirements:** QA-01, QA-02, QA-03
**Success Criteria** (what must be TRUE):
  1. Every bug reported from internal testing is triaged, and the release-blocking ones are fixed and verified.
  2. With an Android client and an iOS expert, the matrix passes: Stripe test cards including 3DS, push from a killed app, a call kept alive in the background and across rejoin, and payout onboarding. (Week 8 checkpoint, user-confirmed)
  3. With an iOS client and an Android expert, the same matrix passes. (Week 8 checkpoint, user-confirmed)
**Plans:** TBD

### Phase 17: Play Store Release
**Goal:** ClickMe Android is publicly available on Google Play from the organization account.
**Target week:** Week 8 (Dec 7–13)
**Depends on:** Phase 16
**Requirements:** REL-06, REL-07
**Success Criteria** (what must be TRUE):
  1. Play Console's release requirements are complete and accepted: data safety form, foreground-service (microphone) declaration and video, account-deletion URL, and content rating.
  2. The production release is live on Google Play. The target is ~Dec 11–13, or as early as ~Dec 7–8 if Weeks 1–7 hold. (Week 8 checkpoint, user-confirmed)
  3. A fresh install from Google Play can sign up, book a session with the iOS app on the other side, and join the call against the production backend.
**Plans:** TBD

## Progress

**Execution Order:**
Phases run in numeric order: 1 → 2 → 3 → … → 17. The Play Console organization account (Phase 1) is an outside wait that runs alongside everything else until the first internal upload.

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Foundation, App Shell & Play Account | 0/9 | Planned | - |
| 2. Networking, Session & SDK Spikes | 0/TBD | Not started | - |
| 3. Login, Password Recovery & Realtime | 0/TBD | Not started | - |
| 4. Signup, Onboarding & Mode Switch | 0/TBD | Not started | - |
| 5. Explore & Expert Profile | 0/TBD | Not started | - |
| 6. Booking Flow & Free Bookings | 0/TBD | Not started | - |
| 7. Payment Methods & Paid Booking | 0/TBD | Not started | - |
| 8. Client Bookings & Cancellation | 0/TBD | Not started | - |
| 9. Voice Call | 0/TBD | Not started | - |
| 10. Expert Topics & Availability | 0/TBD | Not started | - |
| 11. Expert Requests & Bookings | 0/TBD | Not started | - |
| 12. Chat, Report & Block | 0/TBD | Not started | - |
| 13. Push, Notifications & Internal Testing | 0/TBD | Not started | - |
| 14. Payouts & Account Settings | 0/TBD | Not started | - |
| 15. Polish & Hardening | 0/TBD | Not started | - |
| 16. Cross-Platform QA | 0/TBD | Not started | - |
| 17. Play Store Release | 0/TBD | Not started | - |
