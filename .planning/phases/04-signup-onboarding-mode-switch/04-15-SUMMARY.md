---
phase: 04-signup-onboarding-mode-switch
plan: 15
subsystem: signup
tags: [compose, email-verification, countdown, rate-limit, otp]
requires:
  - phase: 04-03
    provides: AuthTopAppBar, AuthErrorDialog
  - phase: 04-07
    provides: SignupSetupRepository (verificationSend, recordCodeSent), SignupTestHarness
provides:
  - MonotonicClock and ClockModule (injectable elapsed-realtime clock)
  - SignupVerifyEmailViewModel (send on tap, 30 s cooldown, verify, error mapping)
  - SignupVerifyEmailScreen and stateless SignupVerifyEmailContent
affects: [04-19]
tech-stack:
  added: []
  patterns:
    - "Cooldown stored as an absolute end time on an injected monotonic clock, in the singleton repository and mirrored to SavedStateHandle"
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/MonotonicClock.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/di/ClockModule.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/verifyemail/SignupVerifyEmailViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/verifyemail/SignupVerifyEmailScreen.kt
    - app/src/main/res/values/strings_signup_verify_email.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/verifyemail/SignupVerifyEmailViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/verifyemail/SignupVerifyEmailScreenTest.kt
  modified: []
key-decisions:
  - "A verify 429 TOO_MANY_REQUESTS shows the server message inline (fallback string), no lock UI, no retry"
  - "uiState.verified is suppressed while verifyState is not Idle so the Verified! confirmation shows before the done variant"
requirements-completed: [SIGN-03, SIGN-02]
duration: long (Gradle runs slow with parallel worktrees)
completed: 2026-10-21
---

# Phase 4 Plan 15: Verify Email Summary

**Verify Email step: code sent only on tap, 30 s resend cooldown that survives step re-entry, rotation and process death (end time on an injected MonotonicClock), 429 RATE_LIMITED treated as sent, inline verify errors.**

## Accomplishments
- `MonotonicClock` / `ClockModule` (`SystemClock.elapsedRealtime()`).
- ViewModel: no call on creation (D-07); first send holds "Email Sent!" for 600 ms before the code field appears; resend clears the typed code; ticker sleeps exactly to the next displayed-second change; repository wins over saved state, saved state seeds a fresh repository after process death.
- Screen: before-send, after-send and verified variants; code field with SmsOtpCode autofill; resend button (disabled "Resend in Ns", underlined "Resend code" at 0); a zero-size Polite live region announces once at zero; Back is swallowed while a send/verify is in flight.

## Task Commits
1. Task 1: MonotonicClock, ClockModule, ViewModel, strings, tests - `424d0cb`
2. Task 2: SignupVerifyEmailScreen and tests - `b937edd`

## Verification
- `SignupVerifyEmailViewModelTest` 14 tests and `SignupVerifyEmailScreenTest` 8 tests pass.
- `ktlintCheck`, `lintDevDebug` and `scripts/check-design-rules.sh` ("Design rules: OK") pass.

## Deviations from Plan
- **[Rule 1 - Bug] `KeyboardType.NumberPad` does not exist in Compose.** Used `KeyboardType.Number` (same as SetNewPasswordScreen). The literal acceptance grep for `KeyboardType.NumberPad` therefore prints 0; behavior (numeric keypad) is as intended.
- **Strings file committed with Task 1** (the ViewModel references two of its keys) instead of Task 2.
- Tests and implementation were written together and committed once per task, not as separate RED/GREEN commits.
- Screen tests use `@Config(qualifiers = "w411dp-h1200dp-xhdpi")` so the centered scrolling column is fully on screen under Robolectric.
- `send()` and `resend()` share one private `requestCode()`; the first send (no code recorded yet) uses `sendState`, later sends use `isResending`.
- `sendState` stays `Saved` after the 600 ms hold (the send button is hidden once `codeSent` is true).

## Known Stubs
None.

## Threat Flags
None. T-04-38 and T-04-39 mitigations implemented as planned; T-04-40 accepted (code mirrored to SavedStateHandle, never logged).

## Manual checks (deferred by project policy)
- On device: send a code, leave Verify Email, re-enter within 30 s and confirm resend is still disabled and the code field is shown.
- On device: rotate during the countdown and confirm it continues; kill the process during the countdown and confirm it resumes.
- On device with a real backend: confirm a second send within 30 s shows the code field with no dialog (429 RATE_LIMITED), and 6 wrong codes show the 15-minute message inline.

## Self-Check: PASSED
All seven created files exist; commits `424d0cb` and `b937edd` exist.
