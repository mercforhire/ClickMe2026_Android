---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: executing
stopped_at: Phase 3 UI-SPEC approved
last_updated: "2026-10-21T00:45:51.008Z"
last_activity: 2026-10-21
progress:
  total_phases: 17
  completed_phases: 2
  total_plans: 37
  completed_plans: 33
  percent: 12
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-10-19)

**Core value:** The ClickMe MVP is live on Google Play (org account) by ~2026-12-13, and Android users interoperate fully with the iOS app on the same backend.
**Current focus:** Phase 03 — login-password-recovery-realtime

## Current Position

Phase: 4
Plan: Not started
Status: Executing Phase 03
Last activity: 2026-10-21

Progress: [░░░░░░░░░░] 0%

## Performance Metrics

**Velocity:**

- Total plans completed: 21
- Average duration: -
- Total execution time: 0.0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01 | 9 | - | - |
| 03 | 12 | - | - |

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

## Deferred Items

Items acknowledged and carried forward from previous milestone close:

| Category | Item | Status | Deferred At |
|----------|------|--------|-------------|
| *(none)* | | | |

## Session Continuity

Last session: 2026-10-20T23:56:07.176Z
Stopped at: Phase 3 UI-SPEC approved
Resume file: .planning/phases/03-login-password-recovery-realtime/03-UI-SPEC.md
