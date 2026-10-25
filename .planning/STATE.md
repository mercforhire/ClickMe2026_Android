---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: verifying
stopped_at: Phase 10 UI-SPEC approved
last_updated: "2026-10-25T01:17:27.967Z"
last_activity: 2026-10-24
progress:
  total_phases: 17
  completed_phases: 8
  total_plans: 128
  completed_plans: 124
  percent: 47
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-10-19)

**Core value:** The ClickMe MVP is live on Google Play (org account) by ~2026-12-13, and Android users interoperate fully with the iOS app on the same backend.
**Current focus:** Phase 9 — Voice Call

## Current Position

Phase: 10
Plan: Not started
Status: Phase 9 plans complete, ready for /gsd-verify-work
Last activity: 2026-10-24

Progress: [█████████░] 94%

## Performance Metrics

**Velocity:**

- Total plans completed: 112
- Average duration: -
- Total execution time: 0.0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01 | 9 | - | - |
| 03 | 12 | - | - |
| 04 | 20 | - | - |
| 05 | 11 | - | - |
| 06 | 14 | - | - |
| 07 | 14 | - | - |
| 08 | 15 | - | - |
| 9 | 17 | - | - |

**Recent Trend:**

- Last 5 plans: -
- Trend: -

*Updated after each plan completion*
| Phase 05 P11 | 15min | 2 tasks | 2 files |
| Phase 06 P14 | 30min | 2 tasks | 2 files |
| Phase 07 P14 | 40min | 2 tasks | 2 files |
| Phase 08 P13 | 35min | 2 tasks | 5 files |
| Phase 08 P14 | 6min | 3 tasks | 9 files |
| Phase 08 P15 | 25min | 2 tasks | 3 files |
| Phase 9 P17 | 50min | 2 tasks | 6 files |

## Accumulated Context

### Decisions

Decisions are logged in the PROJECT.md Key Decisions table (15 locked DEC-* entries).
Recent decisions affecting current work:

- [Init]: Package is `com.uptrendinvestments.clickmeapp` (DEC-application-id). It is permanent after the first Play upload.
- [Init]: Reschedule follows Reading A. Responding is in the MVP (BOOK-09, EXP-09); starting one is deferred (DEF-1).
- [Init]: The user confirmed the Week 7 and Week 8 success criteria (Phases 14–17).
- [Init]: 17 phases, about 2 per PRD week (3 in Week 3, the highest-risk week).
- [Phase 08-14]: showMyBookings resets both Explore and Bookings stacks after a cancel so no screen for a cancelled booking is reachable by Back
- [Phase 9]: Scope sweeps kept literal (all 0): comments naming forbidden tokens were reworded instead of waived

### Pending Todos

- Report the iOS issues back to the iOS side: Join is a no-op from My Bookings, the 5-minute overtime test value, report reasons sent as labels, no blocked state in chat, the cancellation comment is dropped.
- The backend/iOS owners should rotate the hard-coded JWT in `Debug/PreviewSecrets.swift`. This is outside the Android code.
- Confirm on the iOS side: Environments.plist has Current = Production (pk_live) while the backend Stripe account is in test mode (Phase 1 D-05). Android dev flavor uses pk_test for day-to-day testing.
- Before release (Phase 13): verify against the current Google Play Payments policy that paying for live 1-on-1 expert calls via Stripe (not Play Billing) is permitted. Play Console "earning money" answer was payments for services outside Google Play.
- Report to the iOS side: RealtimeEvents BookingStatus has no `rescheduled` case, so iOS drops booking.update events with that status (Android keeps raw strings).
- Report to the iOS side (found while porting Phase 4): publishing without a photo always fails (server requires avatar), province is required on iOS but optional on the server, the verified-email state is lost on resume, tags/languages are sent alphabetically so the primary tag is accidental (and language order is not stored server-side at all), country_code is not normalized on publish, the photo zoom slider does nothing.
- [Phase 2 UAT, 2026-10-21] OPEN, must fix before Phase 7: Stripe spike Add card shows PaymentSheet briefly then closes, and paying fails with "client secret does not match any associated SetupIntent on this account". Likely cause: `pk_test_51Tha0r...` (Android dev flavor, spike, iOS dev/staging) belongs to a different Stripe account than the production backend's secret key (iOS prod uses `pk_live_51U8OTJ...`). Confirm the backend's account and use that account's test publishable key. See 02-UAT.md gaps (tests 7-8). Root cause confirmed 2026-10-23: live-mode backend vs sandbox key; tracked under "[Phase 7] Stripe key and mode" (BLOCKED, device tests).
- [Phase 6 hand-off] BookingKey(expertId, expertName, expertTitle, expertImageUrl) and its stub body are in screens/clientmode/makeabooking; Phase 6 replaces MakeABookingStubScreen's body, not the key or the shell entry. Recommended Experts does not exclude the caller (home_feed), so a multi-role tester can open their own profile and Book: map 422 CANNOT_BOOK_SELF in the Phase 6 booking flow. Money.format (util/Money.kt) is the shared formatter for Phases 6, 7, 11, 14.
- [Phase 8/9 hand-off] Today's Sessions cards on Explore are inert (D-01): Phase 8 wires the card tap to Upcoming Booking Details (ExploreTodaysSessionsSection). Join Call is always disabled (D-02): Phase 9 finds "// PHASE-9: join window + click (D-02)" and test tag todays_session_join, adds the join window, the click and the STARTING NOW badge. ClientBookingsRepository and ClientBookingItem (screens/clientmode/mybookings, model/responses/ClientBookingsPayload.kt) are the base for My Bookings.
- Report to the iOS side (found while porting Phase 5): Expert Profile fails silently on a 404 (suspended/deleted experts) and on network errors; empty Expertise/Topics/Reviews render blank headers and zero reviews shows "—"; featured topic cards ignore is_free; prices divide every currency by 100 (JPY shows 1/100 of the price) and drop decimals on whole amounts; Today's Sessions times use a fixed en_US "h:mm a".
- [Phase 7 hand-off] DONE 2026-10-23 (Phase 7 plans 07-10, 07-07, 07-08): the paid branch replaced `BookDisabledReason.PaidTopic`, bookingAttemptId regenerates on topic and slot change, D-10 chose the first topic as the default, and Request Sent shows the payment-lifecycle card for paid bookings. Original note: replace the `BookDisabledReason.PaidTopic` branch with the paid flow at the same footprint; add bookingAttemptId regeneration; decide the first-topic default; add the lifecycle card to Request Sent.
- [Phase 8 hand-off] `ClientBookingDetail` (model/responses) and `ClientBookingsRepository.detail(id)` are the base for Upcoming Booking Details; "View My Bookings" selects `ClientBookingsRoot`, which renders My Bookings since 08-14.
- [Backend notes] `POST /bookings/request` trusts the client `duration_minutes` and does not reject a past `scheduled_start` (Android guards both on the client); `/experts/:id/details` already returns `available_meeting_types` (D-08's premise is stale; Phase 6 kept D-08 as written by user decision; a pre-filter is a later option); `/experts/:id/availability` accepts `topic_id` (Android's API method has the parameter and passes null).
- [iOS follow-ups found while porting Phase 6] Request Sent is pushed on top of the booking form (re-submittable); a month change does not cancel the previous availability load (stale overwrite race); the slots card shows the raw IANA zone id; slot times are fixed to h:mm a; the Request Sent loading copy says "Sending your booking…" after the request was already sent.
- [iOS follow-ups found while porting Phase 7] Dismissing the FlowController sheet with a preselected card still authorizes it on iOS (Android cancels, D-18); after a confirm timeout iOS shows a plain slot-taken message for the user's own pending booking (Android points to My Bookings, D-19); the lifecycle card says "A hold is placed" for long-lead bookings that are only saved and charged at T-24h (UI-SPEC Open Item 3); iOS shows no alert when 3DS is cancelled (Android does, D-15); the "TODO(backend): stripe_account" comment in MakeABookingViewModel.swift is stale (the backend sends it); iOS paid copy for slot taken / meeting link differs from its free path (Android uses one copy, D-14); iOS Payment Methods updates optimistically and can show a default the server rejected.
- [Backend notes, Phase 7] Every short-lead attempt mints a SetupIntent against a 10 per 15 minutes limit (Android reuses an unconfirmed one for 10 minutes); a confirm timeout followed by a retry with the same booking_attempt_id returns SLOT_UNAVAILABLE for the user's own pending booking (an idempotent replay would be friendlier).
- [Phase 9 note] DONE 2026-10-24 (09-16): the active-call guard on Add Payment Method (Phase 7 UI-SPEC Open Item 4) shipped with the call work.
- [Phase 2 note] Phase 7's SC-1 to SC-4 device checks cover the goals of the open 02-09 Stripe spike run; the user decides whether 02-09 still needs its own run.
- [iOS follow-ups found while porting Phase 8] Accept/Decline exists only on Booking Summary and "Tap to review" on My Bookings leads nowhere (Android shows the response card on Upcoming Booking Details, D-01); the cancellation comment is collected but dropped (D-05); the refund preview always promises the full price while the backend voids the hold on a pre-session cancel (D-06); the Upcoming Booking status pill is hardcoded "Confirmed" and the fee is `amount / 100` (D-14); the Past tab shows nothing when empty (D-11); Keep Booking pops to the My Bookings root.
- [Backend notes, Phase 8] `POST /client/bookings/:id/cancel` validates `reason` but does not store it; there is no refund-preview endpoint, so clients mirror `refund.js` (D-06); with manual capture after the session every cancellable paid booking derives `payment_status = held`, so Android's "Full refund" / "No refund within 24 hours" lines are dormant but tested (RESEARCH Finding 2).
- [Later improvement, D-09] My Bookings loads page 1 only (20 per bucket). Older bookings, and a rejoinable completed session beyond the first 20 Past rows (Past sorts future-dated cancellations first), are not shown. The backend supports `page`/`limit`/`pagination`.
- [Phase 9 hand-off] DONE 2026-10-24 (09-14, 09-15): Join is wired on My Bookings, Upcoming Booking Details and Today's Sessions (`JoinCallButton` / `JoinClock`, `CallStarter.startCall`); Skype/Zoom open through `rememberExternalMeetingLauncher`. The `PHASE-9` markers are gone.
- [Phase 9 open item] The ongoing-call notification is the approved plain style and is swipe-dismissible on Android 14+. Switching to `NotificationCompat.CallStyle.forOngoingCall` is a one-file change in `CallNotificationFactory` (RESEARCH Open Question 1, Pitfall 3). Ask the user before release.
- [Phase 9 open item] Bluetooth headset routing is untested and unhandled beyond the Agora defaults (RESEARCH Open Question 4). Add explicit routing only if the device run shows a problem.
- [Phase 9 open item] Android 17 audio hardening: check background audio and focus on the `Medium_Phone_API_37.0` emulator with `adb shell cmd audio set-enable-hardening enable`, and that the targetSdk 37 local-network permission does not block Agora (RESEARCH State of the Art).
- [Phase 9 note] Predictive back is shipped (`PredictiveBackHandler`, 09-13; no BackHandler fallback), so there is no progress-animation open item. Its feel on a device is in the deferred list.
- [Phase 9 notes from plan summaries] 09-08: the pill keeps its merged semantics (contentDescription "Return to call with {name}", Role.Button) instead of `clearAndSetSemantics {}` so the timer text stays assertable; the timer has no live region. 09-10: the already-granted-mic, notification-only permission request is implemented but only recorded by a fake, not asserted in a Compose test. 09-13: a cold start after a task swipe shows Splash / auth first and the call layer appears once the route resolves (D-14); only the pure intent test covers it.
- [Backend notes, Phase 9] The join window in code is 120 min before / 120 min after, the docs say 10 / 0, with a "tighten before launch" comment. The client never authorizes on its own window (D-02) and ignores `can_join`. If the backend tightens it, early taps get `403 SESSION_NOT_ACTIVE`, shown verbatim (RESEARCH Open Question 3). `/end` requires `actual_duration` >= 1 (`.positive()`); Android clamps to 1.
- [iOS follow-ups found while porting Phase 9] `/end` with a 0 s duration is a 400 on iOS (it sends `max(0, d)`); iOS never reaches the Ended status from Agora (`didOfflineOfUid uid == 0` never fires); the Skype status copy promises an automatic end at the scheduled time; the Skype hero has no timer; Upcoming Details has no Join for Skype/Zoom. Already listed and now fixed on Android in Phase 9: Join is a no-op on My Bookings, and the 5-minute overtime TEMP value (Android cuts off at scheduled end + 3600 s, CALL-07).
- [Phase 10 hand-off] Add `RestrictedAction.EditAvailability` plus a `call_action_*` string and guard availability edits with `CallGuard.attempt` (D-13).
- [Phase 11 hand-off] Expert Join entry points call `CallStarter.startCall` (the call stack is role-agnostic). Guard expert reschedule with `CallGuard.attemptModifyBooking` (D-13).
- [Phase 12 hand-off] Enable the disabled in-call Chat control (`// PHASE-12` in `MeetingCallControls.kt`, D-11): `initiateChat`, minimize the call, switch to Chats, push the thread.
- [Phase 14 hand-off] Add `RestrictedAction` entries plus `call_action_*` strings and call `CallGuard.attempt` at change password, delete account and payouts (D-13).
- [Phase 17 hand-off] Play Console foreground-service (microphone) declaration and demo video for `CallService`. Optional APK size trim: depend on `io.agora.rtc:voice-rtc-basic` instead of the `voice-sdk` umbrella (RESEARCH Standard Stack). Unsigned prodRelease APK is about 121 MB (126.8 MB) after Agora.
- [Phase 12 hand-off] Message Expert is hidden on My Bookings cards and Booking Summary.
- [Phase 13 hand-off] Push routing to a booking uses `BookingSummaryKey(bookingId)` (`Navigator.openBookingSummary`); Leave a Review and the Feedback block are hidden on Booking Summary.

### Blockers/Concerns

- [Phase 1]: OUTSIDE WAIT (RESOLVED). Play Console organization account submitted for Google verification on 2026-10-20 and shown as verified on 2026-10-20. D-U-N-S: confirmed, details match. Responder: the user (watch the Play Console account email for any follow-up requests). Status: verified, no wait remaining. REL-03 (Phase 2 first signed upload registers com.uptrendinvestments.clickmeapp) and REL-05 (Phase 13 internal track) are unblocked on the account side. Closed-test requirement shown in Console: not shown yet (user unsure). Re-check in Play Console before Phase 13.
- [Phase 8]: RESOLVED (D-05, 2026-10-23). Android sends the comment inside `reason` as "<label>: <comment>" (comment capped at 450, whole string at 500); `cancelSchema` is `{reason?}.strict()` and the backend validates but does not store it.
- [Phase 2]: EncryptedSharedPreferences is deprecated upstream. The decision is locked and stands. Revisit on purpose, not by accident.
- Schedule risks to watch: Make a Booking + Stripe (Phases 6–7), call service and overlay (Phase 9), push deep links into per-tab back stacks (Phase 13). If the schedule slips, cut in this order: payouts down to the onboarding link only, then the notifications list, then Write Review.
- Spike inputs file (~/.clickme/spike-inputs.properties) deferred — needed before 02-09 and 02-12; manual testing deferred by user
- [Phase 2] DEFERRED (human): upload keystore `~/keys/clickme-upload.jks` + `keystore.properties` (02-05 Task 2), then signed `bundleProdRelease` + `jarsigner -verify` check (02-05 Task 3). Needed before 02-15/02-16.
- [Phase 2] DEFERRED (human): 02-09 Stripe spike device run (SetupIntent card save + PaymentLauncher 3DS on connected account) — needs spike inputs file; plan not executed.
- [Phase 2] SUPERSEDED (2026-10-24, Phase 9 plan 09-17, D-16): 02-12 Agora spike device run. The spike `:spikes:agora` was deleted after its `CallService` findings were absorbed into `:app`; the same device check is now the "[Phase 9] DEFERRED (human)" list below. Original note: Agora spike device run with a live iOS/simulator peer, 5+ min backgrounded, dumpsys/logcat evidence.
- [Phase 2] DEFERRED (human): 02-15 device end-to-end (login, relaunch auto-login, forced refresh, logout; ESP round-trip) + signed release APK R8 cold start, and 02-16 release smoke + Play internal-track upload (REL-03). Need keystore + Samsung device. Unsigned R8 `assembleProdRelease` builds clean (2026-10-20). Phase 2 cannot close until 02-09, 02-15, 02-16 run (02-12 superseded by Phase 9).
- [Phase 3] DEFERRED (human): non-blocking (D-20, user defers manual testing). Automated gate is green (421 tests per flavor, ktlint, lint, R8 `assembleProdRelease`, design rules). Run on the Samsung SM-A075F with the devDebug build against production, with the iOS app signed in as a second account. Backend limits: login 10, forgot 5, reset 10 per 15 min per IP. Phase 13 internal testing re-runs these. Note: D-15 was implemented as a GET /me probe through TokenAuthenticator, not a direct TokenRefresher call (orchestrator resolution 1).
  - [ ] SC-1 Login: wrong password shows the server message under the password field only (no dialog); correct credentials land in the right home for the account's mode; airplane mode shows the dialog "Check your connection and try again."
  - [ ] SC-2 Recovery: Login -> "Forgot Password?" (email pre-filled) -> Send Reset Code -> enter the emailed 6-digit code -> new password (strength meter, match check) -> Password Updated -> Password Changed -> "Go to Login" shows Login with the email pre-filled and an empty password -> log in with the new password. Repeat and press system Back on Password Changed: same result, and Back from Login never reaches the OTP form. Also check SMS/OTP autofill on the code field and focus moving to the first failing field on submit (FocusRequester on the ValidatedField outer Column, no unit test), PasswordChanged glow and TalkBack order.
  - [ ] Rotation on Forgot and Set New Password keeps the typed email, code and passwords (entry-scoped Hilt ViewModels, RESEARCH A3); all five screens at 200% font scale with the keyboard open wrap rather than clip. Also the Login visual layout, IME Next/Done and autofill.
  - [ ] SC-3 Events: `adb logcat -s Realtime` shows state=Connecting then state=Connected after login; sending a message, typing, and accepting or declining a booking from iOS logs MessageNew / TypingStart / TypingStop / BookingUpdate lines with ids only (no message text, no token).
  - [ ] SC-4 Reconnect: airplane mode on for 10 s then off: state returns to Connected within about 2 s of the network coming back and events flow again. If a silent drop takes much longer (A2: ping timeout up to ~45 s; the harness also showed a graceful server close frame is only noticed after that timeout), log it and consider a ConnectivityManager network-available nudge as a follow-up. Debug "Force token refresh" on the Profile tab: a SessionEvent Refreshed is followed by a reconnect (state cycles to Connected) with the new token.
  - [ ] Lifecycle: backgrounding the app logs state=Disconnected about 1 s later; returning logs state=Connected. Logging out logs state=Disconnected.
  - [ ] Release smoke (needs the deferred Phase 2 keystore): the signed prodRelease APK logs in and the socket connects (R8, RESEARCH A6, T-03-44/T-03-45).
- [Phase 4] DEFERRED (human): non-blocking (user defers manual testing). Automated gate green (809 tests per flavor, ktlint, lint, R8 `assembleProdRelease`, design rules). Run on the Samsung SM-A075F with the devDebug build against production.
  - [ ] SC-1 Signup: Login -> Sign Up -> Create Account with a new email (no username/phone) -> hub; complete Personal details (country search, language multi-select, English pre-selected), Timezone (device zone preselected), Profile Photo (pick, pinch/pan/double-tap crop, spring-back, Use Photo, Save), Verify Email (real emailed code), Add expertise (search, tap order) in any order; Review shows everything; Publish -> client home with the carousel.
  - [ ] SC-2 Verify Email: opening the step sends nothing; Send emails a code; Resend stays disabled for 30 s with the countdown; a second Send inside 30 s is treated as sent; a wrong code shows the inline error.
  - [ ] SC-3 Resume: quit mid-setup (force-stop), relaunch -> hub with completed steps checked; log out from the hub overflow, log in again -> hub; airplane mode at launch for an incomplete expert -> home, then back online and foreground -> hub (D-11).
  - [ ] SC-4 After publish: log out and log in -> carousel shows once on client home (Skip and Get Started both mark it; Back on page 1 leaves the app without marking it); avatar -> Mode Switcher -> Expert Mode -> expert home; force-stop and relaunch keeps expert mode; Client Mode switches back.
  - [ ] Photo picker on API 26-32 (no AVD available); 200% font scale and keyboard-open checks on all 12 surfaces; ModalBottomSheet search with the IME; TalkBack crop custom actions.
  - [ ] Consequence to watch (D-17, D-23): a client-only account that taps Expert Mode is converted to expert by the server and then resumes into setup on every launch until it publishes or logs out (iOS has the same relaunch behavior). Raise in Phase 16 QA if testers find it surprising.
- [Phase 5] DEFERRED (human): non-blocking (user defers manual testing). Automated gate green (1056 tests per flavor, ktlint, lint, R8 `assembleProdRelease`, design rules). Run on the Samsung SM-A075F with the devDebug build against production, logged in as a client.
  - [ ] SC-1 Explore: the Explore tab shows the hero, Trending Categories (6 chips with icons), Featured Topics and Recommended Experts from /client/home; a free featured topic reads "Free" in green (confirms live featured_topics carries is_free, RESEARCH A5); with a confirmed booking today (create via the iOS expert or the backend agent) Today's Sessions shows it with a disabled Join Call. Chip tap filters experts only and a second tap clears; View all -> sheet search, multi-select, Apply filters, the chip row shows the selection; a filter with no matches shows "No experts in this category yet" + Clear filter. Pull to refresh; airplane mode on first load shows per-section Retry.
  - [ ] SC-2 Category cache: open View all once online (GET /categories), force-stop, relaunch in airplane mode, open View all: the list shows from cache (no error). Optional: set the device date 8 days ahead and confirm a refetch.
  - [ ] SC-3 Expert Profile: tap an expert card -> hero (static glow, ring, no online dot), stats, Book Session, About, Expertise, Topics (Free in green, prices with full decimals), up to 3 reviews; empty sections absent; open from a Featured Topic -> hero fills in; Book Session -> "Book a Session" stub without the bottom bar; Back returns. A suspended or deleted expert id (backend agent) shows "This profile isn't available".
  - [ ] Visual and accessibility: 200% font scale on Explore, the sheet and the profile (cards grow, nothing clips); sheet search with the IME; pull-to-refresh feel; skeleton-to-content transitions on a slow network; release build smoke after the Phase 2 keystore exists.
- [Phase 6] DONE 2026-10-22 (was DEFERRED (human)): device UAT in `06-UAT.md` passed 15/16; SC-2 far-zone check skipped by the user (low priority, users mainly in Canada/US). Original note: non-blocking (user defers manual testing). Automated gate green (1288 tests per flavor, ktlint, lint, R8 `assembleProdRelease`, design rules). Run on the Samsung SM-A075F with the devDebug build against production, logged in as a client, with the iOS app signed in as the expert.
  - [x] SC-1 Booking form: Expert Profile -> Book Session -> pick a topic in the sheet (paid topics listed with prices; a paid topic disables Book Now with the coming-soon note), a day (past days and earlier months blocked; dots on open days), a slot, a meeting type and notes (500 cap, counter, keyboard does not cover Book Now).
  - [x] SC-2 Cross-timezone: set the iOS expert's timezone different from the device (for example Pacific/Auckland) with slots near midnight at month edges; Android shows each slot under the correct local date with no duplicates across neighbor months, the timezone note names the expert city and abbreviation, held slots (hold one from iOS) are dimmed and not selectable.
  - [x] SC-3 Free booking: book a free topic -> Request Sent loads the details ("Awaiting Approval"), the iOS expert receives the booking_request push and sees the request; Back from Request Sent lands on Expert Profile; View My Bookings selects the Bookings tab; Back to Home returns to Explore; a Skype/Zoom booking with an expert who has no link shows the D-08 alert and snaps back to In-app Voice; a slot taken from another account shows the D-09 alert and the slot turns dimmed.
  - [x] Visual and system: 200% font scale on both screens (06-10); rotation and "Don't keep activities" keep the topic, day, month, slot, meeting type and notes (confirms SavedStateHandle with assisted injection at runtime, research A1) and do not replay the Request Sent animation; animations off shows the final state at once (06-10, A2); system Back during an in-flight booking is ignored (06-12, BackHandler not JVM-tested); the topic sheet open state survives rotation (06-12, rememberSaveable not JVM-tested); prodRelease R8 smoke on a device (T-06-45, needs the deferred Phase 2 keystore).
- [Phase 7] Stripe key and mode (D-01, D-02): BLOCKED (device tests) 2026-10-23: the backend's Stripe account is in live mode, so no device test may use cards until a test-mode backend path exists (D-02). Evidence: every build (Android dev/prod, iOS Development/Staging/Production) targets the single `clickme-backend-production` Railway backend; the backend's `.env.production.example` is "Stripe — LIVE mode" (`sk_live_`), matching iOS prod `pk_live_51U8OTJ…`. The user confirmed `pk_test_` keys are sandbox mode; the dev `pk_test_51Tha0r…` key is a Stripe sandbox (its own account id), so it can never open intents created by the live backend. That is the root cause of Phase 2 UAT 7-8. No `pk_test_51U8OTJ…` key exists to swap in. iOS dev/staging builds have the same mismatch. User decision 2026-10-23: keep deferred, close Phase 7 with device checks open, revisit before release. Unblock options: (recommended) the backend owner adds a second Railway environment on the 51Tha0r sandbox (`sk_test_`, test DB, test webhooks) and the Android dev flavor points BASE_URL at it; or a real-card smoke test with the live key (real charges; test cards and forced 3DS not possible). No key changed in the build.
- [Phase 7] DEFERRED (human): non-blocking (D-03, user defers manual testing). Automated gate green (1535 tests per flavor, ktlint, lint, R8 `assembleProdRelease` with Stripe 23.24.0, design rules). Blocked on a test-mode backend path (see "[Phase 7] Stripe key and mode", BLOCKED 2026-10-23; never use real cards against a live backend). Run on the Samsung SM-A075F with the devDebug build, logged in as a client, with the iOS app signed in as the expert. Budget: setup-intent allows 10 calls per 15 minutes per user.
  - [ ] SC-1 Payment Methods: Profile -> Payment Methods -> Add Payment Method -> PaymentSheet (setup mode, dark on a light-mode phone, "Save card") -> add 4242 4242 4242 4242 -> listed; Set as default moves the badge after the reload; Remove asks first and the list shows the server's default (none promoted).
  - [ ] SC-2 Short-lead: book a paid slot within 6 days -> FlowController lists saved cards -> pick one -> Request Sent with the hold-placed card -> the iOS expert receives the request. Dismiss the sheet with a preselected card -> silent cancel, no charge (D-18). Force a failure (airplane mode during confirm) -> network copy with "Payment reference: pi_..."; Book Now again -> no duplicate booking; if the server already created it, the slot conflict says to check My Bookings (D-19).
  - [ ] SC-3 Long-lead and gate: with no saved card a paid topic shows "Add a Payment Method" -> Payment Methods -> add a card -> Back -> the form keeps every selection and shows "Book Now" -> book a slot more than 6 days out -> confirms with the default card, no sheet.
  - [ ] SC-4 3DS: 4000 0027 6000 3184 (then 4000 0025 0000 3155 as a second try, RESEARCH A2) -> challenge on the connected account -> passes -> booking confirms (A3); cancel the challenge -> "Payment wasn't completed. Your card wasn't charged." and Book Now retries.
  - [ ] SC-5 Errors and money: a JPY topic shows no decimals; rotation while the sheet and the challenge are open launches neither twice (A4); 200% font scale on Payment Methods, the info sheet and paid Request Sent; process death during the 3DS bank step returns an idle form (accepted, D-17); prodRelease R8 smoke with Stripe (needs the deferred Phase 2 keystore).
  - [ ] Also observed in 07-12 (A10): under Nav3 1.2.0 both LaunchedEffect(Unit) and ON_RESUME refresh on return from Payment Methods in Robolectric; confirm on device that returning refreshes the card list without a visible double load. The prodRelease APK grew about 10.4 MB with Stripe and gained the NFC permission (07-01): justify it in the Phase 13 data safety form and budget size in Phase 17.
- [Phase 8] DEFERRED (human): non-blocking (user defers manual testing). Automated gate green (1888 tests per flavor, ktlint, lint, R8 `assembleProdRelease`, design rules). Card-based checks are BLOCKED on the test-mode Stripe backend (see "[Phase 7] Stripe key and mode"). Run on the Samsung SM-A075F with the devDebug build, logged in as a client, with the iOS app signed in as the expert.
  - [ ] SC-1 My Bookings: Upcoming/Past tabs; the rejoin rule with a completed-but-not-ended session; Past labels; pull-to-refresh; realtime `booking.update` from an iOS action refreshes the list; quiet reload on tab return.
  - [ ] SC-2 Upcoming Booking Details: pending_approval (banner), confirmed (Join placeholder), Skype/Zoom link shown and copied, Today's Sessions card tap from Explore opens the details.
  - [ ] SC-3 Cancel: a free booking and a held paid booking (paid case BLOCKED on the test-mode Stripe backend) with a comment; check the Past tab and the result snackbar, and that the iOS expert sees the cancellation.
  - [ ] SC-4 Reschedule response: the iOS expert proposes a reschedule; Android accepts and declines (with and without a reason); the booking leaves `pending_reschedule`. `PAYMENT_METHOD_REQUIRED` recovery with a saved card and with none (BLOCKED on the test-mode Stripe backend). Booking Summary Book Again.
  - [ ] Visual and system: 200% font scale on My Bookings, Upcoming Details with a reschedule card, Cancel step 2 and Summary; rotation and process death on Cancel step 2; system Back during an in-flight cancel (covered by Robolectric in 08-11, device confirmation optional); JPY refund snackbar formatting.
- [Phase 9] DEFERRED (human): non-blocking (D-16, user defers manual testing). Automated gate green (2206 tests per flavor, ktlint, lint, R8 `assembleProdRelease` with Agora 4.6.4, design rules). Run on the Samsung SM-A075F (devDebug, production backend) as the client with the finished iOS app as the expert on a confirmed booking:
  - [ ] SC-1 Join from Upcoming Details opened from Explore and from My Bookings, and from the My Bookings card and Today's Sessions; first mic prompt, deny once -> rationale, deny permanently -> Open Settings; a Skype/Zoom booking opens the native app or a Custom Tab and minimizes.
  - [ ] SC-2 5+ minutes backgrounded with the screen off with two-way audio, the notification visible (title, Starts at / chronometer), notification tap expands after a task swipe, notification End call ends the call. Capture `adb logcat -s ClickMeCall` and `adb shell dumpsys audio` evidence.
  - [ ] SC-3 Pill across all tabs and a Client/Expert switch attempt (guard), logout attempt (guard), add payment method attempt (guard), Cancel of the live booking (guard); predictive back minimizes.
  - [ ] SC-4 The iOS expert hangs up -> Waiting for other person; rejoin within the window; the final-5-minute amber state and banner.
  - [ ] SC-5 A full paid session end to end with `confirm-connection` sent exactly once (backend logs). BLOCKED on the test-mode Stripe backend for the paid part (see "[Phase 7] Stripe key and mode"); a free booking covers everything else.
  - [ ] Android 17 check on the `Medium_Phone_API_37.0` emulator with `adb shell cmd audio set-enable-hardening enable` (background audio and focus while backgrounded); confirm the targetSdk 37 local-network permission does not block Agora.
  - [ ] 200% font scale on the Connected pair, the final-stretch banner and the external variant; TalkBack order on the layer; POST_NOTIFICATIONS denied on API 33+ (call still runs, pill remains; notification-only request path); Bluetooth headset routing (Open Question 4); an incoming GSM call during the call (A6); Samsung battery / sleeping-apps behaviour (Pitfall 14); cold-start notification tap after a task swipe; pill position over gesture nav versus 3-button nav. Phase 13 internal testing and Phase 16 QA re-run these.

## Deferred Items

Items acknowledged and carried forward from previous milestone close:

| Category | Item | Status | Deferred At |
|----------|------|--------|-------------|
| *(none)* | | | |

## Session Continuity

Last session: 2026-10-25T01:17:27.957Z
Stopped at: Phase 10 UI-SPEC approved
Resume file: .planning/phases/10-expert-topics-availability/10-UI-SPEC.md
