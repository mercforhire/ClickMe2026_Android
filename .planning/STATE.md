---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: executing
stopped_at: Phase 6 execution complete (06-14), pending deferred device checks and verification
last_updated: "2026-10-22T19:40:07.563Z"
last_activity: 2026-10-22 -- Phase 06 execution started
progress:
  total_phases: 17
  completed_phases: 5
  total_plans: 82
  completed_plans: 78
  percent: 29
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-10-19)

**Core value:** The ClickMe MVP is live on Google Play (org account) by ~2026-12-13, and Android users interoperate fully with the iOS app on the same backend.
**Current focus:** Phase 06 — booking-flow-free-bookings

## Current Position

Phase: 06 (booking-flow-free-bookings) — EXECUTION COMPLETE (pending deferred device checks and verification)
Plan: 14 of 14 (all complete)
Status: Phase complete - ready for verification
Last activity: 2026-10-22 -- Phase 06 execution started

Progress: [█████████░] 94%

## Performance Metrics

**Velocity:**

- Total plans completed: 52
- Average duration: -
- Total execution time: 0.0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01 | 9 | - | - |
| 03 | 12 | - | - |
| 04 | 20 | - | - |
| 05 | 11 | - | - |

**Recent Trend:**

- Last 5 plans: -
- Trend: -

*Updated after each plan completion*
| Phase 05 P11 | 15min | 2 tasks | 2 files |
| Phase 06 P14 | 30min | 2 tasks | 2 files |

## Accumulated Context

### Decisions

Decisions are logged in the PROJECT.md Key Decisions table (15 locked DEC-* entries).
Recent decisions affecting current work:

- [Init]: Package is `com.uptrendinvestments.clickmeapp` (DEC-application-id). It is permanent after the first Play upload.
- [Init]: Reschedule follows Reading A. Responding is in the MVP (BOOK-09, EXP-09); starting one is deferred (DEF-1).
- [Init]: The user confirmed the Week 7 and Week 8 success criteria (Phases 14–17).
- [Init]: 17 phases, about 2 per PRD week (3 in Week 3, the highest-risk week).

### Pending Todos

- Report the iOS issues back to the iOS side: Join is a no-op from My Bookings, the 5-minute overtime test value, report reasons sent as labels, no blocked state in chat, the cancellation comment is dropped.
- The backend/iOS owners should rotate the hard-coded JWT in `Debug/PreviewSecrets.swift`. This is outside the Android code.
- Confirm on the iOS side: Environments.plist has Current = Production (pk_live) while the backend Stripe account is in test mode (Phase 1 D-05). Android dev flavor uses pk_test for day-to-day testing.
- Before release (Phase 13): verify against the current Google Play Payments policy that paying for live 1-on-1 expert calls via Stripe (not Play Billing) is permitted. Play Console "earning money" answer was payments for services outside Google Play.
- Report to the iOS side: RealtimeEvents BookingStatus has no `rescheduled` case, so iOS drops booking.update events with that status (Android keeps raw strings).
- Report to the iOS side (found while porting Phase 4): publishing without a photo always fails (server requires avatar), province is required on iOS but optional on the server, the verified-email state is lost on resume, tags/languages are sent alphabetically so the primary tag is accidental (and language order is not stored server-side at all), country_code is not normalized on publish, the photo zoom slider does nothing.
- [Phase 2 UAT, 2026-10-21] OPEN, must fix before Phase 7: Stripe spike Add card shows PaymentSheet briefly then closes, and paying fails with "client secret does not match any associated SetupIntent on this account". Likely cause: `pk_test_51Tha0r...` (Android dev flavor, spike, iOS dev/staging) belongs to a different Stripe account than the production backend's secret key (iOS prod uses `pk_live_51U8OTJ...`). Confirm the backend's account and use that account's test publishable key. See 02-UAT.md gaps (tests 7-8).
- [Phase 6 hand-off] BookingKey(expertId, expertName, expertTitle, expertImageUrl) and its stub body are in screens/clientmode/makeabooking; Phase 6 replaces MakeABookingStubScreen's body, not the key or the shell entry. Recommended Experts does not exclude the caller (home_feed), so a multi-role tester can open their own profile and Book: map 422 CANNOT_BOOK_SELF in the Phase 6 booking flow. Money.format (util/Money.kt) is the shared formatter for Phases 6, 7, 11, 14.
- [Phase 8/9 hand-off] Today's Sessions cards on Explore are inert (D-01): Phase 8 wires the card tap to Upcoming Booking Details (ExploreTodaysSessionsSection). Join Call is always disabled (D-02): Phase 9 finds "// PHASE-9: join window + click (D-02)" and test tag todays_session_join, adds the join window, the click and the STARTING NOW badge. ClientBookingsRepository and ClientBookingItem (screens/clientmode/mybookings, model/responses/ClientBookingsPayload.kt) are the base for My Bookings.
- Report to the iOS side (found while porting Phase 5): Expert Profile fails silently on a 404 (suspended/deleted experts) and on network errors; empty Expertise/Topics/Reviews render blank headers and zero reviews shows "—"; featured topic cards ignore is_free; prices divide every currency by 100 (JPY shows 1/100 of the price) and drop decimals on whole amounts; Today's Sessions times use a fixed en_US "h:mm a".
- [Phase 7 hand-off] Replace the `BookDisabledReason.PaidTopic` branch (MakeABookingBookButton note + the `!topic.isFree` early return in MakeABookingViewModel.book()) with the paid flow at the same footprint; add bookingAttemptId regeneration on topic and slot change (iOS); decide whether to keep the D-02 first-free default or revert to first; add the payment-lifecycle card to Request Sent for `BookingRequestSentDetails.isFree == false`.
- [Phase 8 hand-off] `ClientBookingDetail` (model/responses) and `ClientBookingsRepository.detail(id)` are the base for Upcoming Booking Details; "View My Bookings" already selects `ClientBookingsRoot`, which is still the placeholder.
- [Backend notes] `POST /bookings/request` trusts the client `duration_minutes` and does not reject a past `scheduled_start` (Android guards both on the client); `/experts/:id/details` already returns `available_meeting_types` (D-08's premise is stale; Phase 6 kept D-08 as written by user decision; a pre-filter is a later option); `/experts/:id/availability` accepts `topic_id` (Android's API method has the parameter and passes null).
- [iOS follow-ups found while porting Phase 6] Request Sent is pushed on top of the booking form (re-submittable); a month change does not cancel the previous availability load (stale overwrite race); the slots card shows the raw IANA zone id; slot times are fixed to h:mm a; the Request Sent loading copy says "Sending your booking…" after the request was already sent.

### Blockers/Concerns

- [Phase 1]: OUTSIDE WAIT (RESOLVED). Play Console organization account submitted for Google verification on 2026-10-20 and shown as verified on 2026-10-20. D-U-N-S: confirmed, details match. Responder: the user (watch the Play Console account email for any follow-up requests). Status: verified, no wait remaining. REL-03 (Phase 2 first signed upload registers com.uptrendinvestments.clickmeapp) and REL-05 (Phase 13 internal track) are unblocked on the account side. Closed-test requirement shown in Console: not shown yet (user unsure). Re-check in Play Console before Phase 13.
- [Phase 8]: OPEN ITEM. Decide whether Android collects and sends the cancellation comment; iOS collects it but never sends it. Check the `cancelClientBooking` contract during Phase 8 planning.
- [Phase 2]: EncryptedSharedPreferences is deprecated upstream. The decision is locked and stands. Revisit on purpose, not by accident.
- Schedule risks to watch: Make a Booking + Stripe (Phases 6–7), call service and overlay (Phase 9), push deep links into per-tab back stacks (Phase 13). If the schedule slips, cut in this order: payouts down to the onboarding link only, then the notifications list, then Write Review.
- Spike inputs file (~/.clickme/spike-inputs.properties) deferred — needed before 02-09 and 02-12; manual testing deferred by user
- [Phase 2] DEFERRED (human): upload keystore `~/keys/clickme-upload.jks` + `keystore.properties` (02-05 Task 2), then signed `bundleProdRelease` + `jarsigner -verify` check (02-05 Task 3). Needed before 02-15/02-16.
- [Phase 2] DEFERRED (human): 02-09 Stripe spike device run (SetupIntent card save + PaymentLauncher 3DS on connected account) — needs spike inputs file; plan not executed.
- [Phase 2] DEFERRED (human): 02-12 Agora spike device run with a live iOS/simulator peer, 5+ min backgrounded, dumpsys/logcat evidence — needs spike inputs file (confirmed booking); plan not executed.
- [Phase 2] DEFERRED (human): 02-15 device end-to-end (login, relaunch auto-login, forced refresh, logout; ESP round-trip) + signed release APK R8 cold start, and 02-16 release smoke + Play internal-track upload (REL-03). Need keystore + Samsung device. Unsigned R8 `assembleProdRelease` builds clean (2026-10-20). Phase 2 cannot close until 02-09, 02-12, 02-15, 02-16 run.
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
- [Phase 6] DEFERRED (human): non-blocking (user defers manual testing). Automated gate green (1288 tests per flavor, ktlint, lint, R8 `assembleProdRelease`, design rules). Run on the Samsung SM-A075F with the devDebug build against production, logged in as a client, with the iOS app signed in as the expert.
  - [ ] SC-1 Booking form: Expert Profile -> Book Session -> pick a topic in the sheet (paid topics listed with prices; a paid topic disables Book Now with the coming-soon note), a day (past days and earlier months blocked; dots on open days), a slot, a meeting type and notes (500 cap, counter, keyboard does not cover Book Now).
  - [ ] SC-2 Cross-timezone: set the iOS expert's timezone different from the device (for example Pacific/Auckland) with slots near midnight at month edges; Android shows each slot under the correct local date with no duplicates across neighbor months, the timezone note names the expert city and abbreviation, held slots (hold one from iOS) are dimmed and not selectable.
  - [ ] SC-3 Free booking: book a free topic -> Request Sent loads the details ("Awaiting Approval"), the iOS expert receives the booking_request push and sees the request; Back from Request Sent lands on Expert Profile; View My Bookings selects the Bookings tab; Back to Home returns to Explore; a Skype/Zoom booking with an expert who has no link shows the D-08 alert and snaps back to In-app Voice; a slot taken from another account shows the D-09 alert and the slot turns dimmed.
  - [ ] Visual and system: 200% font scale on both screens (06-10); rotation and "Don't keep activities" keep the topic, day, month, slot, meeting type and notes (confirms SavedStateHandle with assisted injection at runtime, research A1) and do not replay the Request Sent animation; animations off shows the final state at once (06-10, A2); system Back during an in-flight booking is ignored (06-12, BackHandler not JVM-tested); the topic sheet open state survives rotation (06-12, rememberSaveable not JVM-tested); prodRelease R8 smoke on a device (T-06-45, needs the deferred Phase 2 keystore).

## Deferred Items

Items acknowledged and carried forward from previous milestone close:

| Category | Item | Status | Deferred At |
|----------|------|--------|-------------|
| *(none)* | | | |

## Session Continuity

Last session: 2026-10-22T19:40:01.689Z
Stopped at: Completed 06-14-PLAN.md (Phase 6 execution complete; deferred device checks and phase verification pending)
Resume file: .planning/phases/06-booking-flow-free-bookings/06-14-SUMMARY.md
