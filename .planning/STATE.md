---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: verifying
stopped_at: Phase 5 context gathered
last_updated: "2026-10-21T22:28:47.248Z"
last_activity: 2026-10-21
progress:
  total_phases: 17
  completed_phases: 3
  total_plans: 57
  completed_plans: 53
  percent: 18
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-10-19)

**Core value:** The ClickMe MVP is live on Google Play (org account) by ~2026-12-13, and Android users interoperate fully with the iOS app on the same backend.
**Current focus:** Phase 05 — explore-&-expert-profile

## Current Position

Phase: 5
Plan: Not started
Status: Phase 04 complete (verified; 6 device checks deferred in 04-HUMAN-UAT.md). Phase 05 not yet discussed/planned.
Last activity: 2026-10-21

Progress: [░░░░░░░░░░] 0%

## Performance Metrics

**Velocity:**

- Total plans completed: 41
- Average duration: -
- Total execution time: 0.0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01 | 9 | - | - |
| 03 | 12 | - | - |
| 04 | 20 | - | - |

**Recent Trend:**

- Last 5 plans: -
- Trend: -

*Updated after each plan completion*

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

## Deferred Items

Items acknowledged and carried forward from previous milestone close:

| Category | Item | Status | Deferred At |
|----------|------|--------|-------------|
| *(none)* | | | |

## Session Continuity

Last session: 2026-10-21T22:28:47.237Z
Stopped at: Phase 5 context gathered
Resume file: .planning/phases/05-explore-expert-profile/05-CONTEXT.md
