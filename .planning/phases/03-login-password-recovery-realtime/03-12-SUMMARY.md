---
phase: 03-login-password-recovery-realtime
plan: 12
subsystem: testing
tags: [phase-gate, verification, deferred-human-verify, device]
requires: [03-10, 03-11]
provides:
  - Green Phase 3 automated gate (ktlint, lint, unit tests, assembles, R8 prodRelease, design rules)
  - Finalized 03-VALIDATION.md (nyquist_compliant, wave_0_complete)
  - Deferred, non-blocking Phase 3 device checklist in STATE.md
affects: [Phase 13 internal testing, Phase 9, Phase 12]
key-files:
  modified:
    - .planning/phases/03-login-password-recovery-realtime/03-VALIDATION.md
    - .planning/STATE.md
decisions:
  - "Device verification of success criteria 1-4 is logged as deferred and non-blocking (D-20, user preference)"
requirements-completed: [AUTH-01, AUTH-02, RT-01, RT-02, RT-03]
duration: ~15 min
completed: 2026-10-20
---

# Phase 3 Plan 12: Phase gate and deferred device checklist Summary

The full Phase 3 automated gate passes with no code changes, the validation map is finalized, and the on-device checks for success criteria 1-4 are recorded as deferred, non-blocking items in STATE.md.

## Task Commits

| Task | Commit |
|------|--------|
| 1. Full suite, R8 build, dependency check, VALIDATION.md update | 20242d5 |
| 2. Deferred device checklist and iOS todo in STATE.md | 92ee102 |

## Gate evidence

Command: `./gradlew ktlintCheck lintDevDebug lintProdDebug testDevDebugUnitTest testProdDebugUnitTest assembleDevDebug assembleProdDebug assembleProdRelease :spikes:stripe:assembleDebug :spikes:agora:assembleDebug` finished BUILD SUCCESSFUL (284 tasks, exit 0).

- Unit tests: 421 tests in `testDevDebugUnitTest`, 421 in `testProdDebugUnitTest`, 0 failures, 0 errors, 0 skipped.
- `bash scripts/check-design-rules.sh` printed "Design rules: OK".
- Unsigned R8 `assembleProdRelease` assembled (signed build still needs the deferred Phase 2 keystore).
- `:app:dependencies --configuration prodReleaseRuntimeClasspath`:
  - `+--- com.squareup.okhttp3:okhttp:5.5.0 (*)` (transitive 4.12.0 and 3.12.12 requests resolve to 5.5.0)
  - `+--- io.socket:socket.io-client:2.1.2`
  - `org.json:json` is absent (assumption A5 holds).

## Validation map

03-VALIDATION.md: all automated rows green; rows added for AuthComponentsTest, ForgotPasswordScreenTest, SetNewPasswordScreenTest, AuthStaticScreensTest, AuthNavigatorTest + AuthNavDisplayTest (these replaced the planned AuthNavigationTest), RealtimeConnectionContractsTest, RealtimeDebugLoggerTest; the three manual rows are marked "deferred (non-blocking)"; sign-off boxes ticked; frontmatter `status: complete`, `nyquist_compliant: true`, `wave_0_complete: true`.

## Deferred items (human/device, non-blocking, not marked done)

Recorded in STATE.md under `[Phase 3] DEFERRED (human)`:
- SC-1 Login: wrong-password inline message only, correct routing by account mode, offline dialog.
- SC-2 Recovery loop with a real OTP email, Back behavior on Password Changed, SMS OTP autofill, focus-on-error in SetNewPassword (FocusRequester on the ValidatedField outer Column, untested), PasswordChanged glow and TalkBack order.
- Rotation state retention and 200% font scale on the five auth screens; Login visual layout and IME behavior.
- SC-3 Realtime events from iOS via `adb logcat -s Realtime` (ids only, no content or tokens).
- SC-4 Reconnect within about 2 s after airplane mode and after forced token refresh; the graceful server close frame may only be detected after the ~45 s ping timeout (03-08 observation), consider a ConnectivityManager nudge if confirmed.
- Lifecycle disconnect/connect logging; signed prodRelease smoke for R8 (needs keystore).

Pending Todo added: report to iOS that `BookingStatus` lacks a `rescheduled` case. D-15 was implemented as a GET /me probe through TokenAuthenticator (orchestrator resolution 1), noted in STATE.md.

## Deviations from Plan

None. The plan executed as written. No gate failures occurred, so no fixes were needed. (The orchestrator's earlier transient lint crash on AppViewModelTest.kt did not recur.) STATE.md Current Position and progress fields were intentionally left unchanged per the plan; the phase transition workflow owns them.

## Known Stubs

None added by this plan.

## Threat Flags

None. Checklist names accounts by role only; no credentials or tokens were written to planning docs (T-03-47).

## Self-Check: PASSED

Commits 20242d5 and 92ee102 exist; 03-VALIDATION.md and STATE.md contain the required markers.
