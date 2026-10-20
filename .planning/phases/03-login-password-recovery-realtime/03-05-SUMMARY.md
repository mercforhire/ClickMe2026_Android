---
phase: 03-login-password-recovery-realtime
plan: 05
subsystem: auth-forgot-password
tags: [compose, password-recovery, otp, viewmodel, robolectric]
requires: [03-01, 03-02]
provides:
  - screens.login.forgetpassword.ForgotPasswordUiState
  - screens.login.forgetpassword.ForgotPasswordViewModel (Hilt, SavedStateHandle "forgot_email")
  - screens.login.forgetpassword.ForgotPasswordScreen(initialEmail, onCodeSent, onBack)
  - screens.login.forgetpassword.ForgotPasswordContent (stateless)
  - res/values/strings_forgot_password.xml
affects: [03-10-auth-navigation]
tech-stack:
  added: []
  patterns: [one-shot navigateToReset StateFlow consumed in LaunchedEffect, FocusRequester driven by a StateFlow flag]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/forgetpassword/ForgotPasswordViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/forgetpassword/ForgotPasswordScreen.kt
    - app/src/main/res/values/strings_forgot_password.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/login/forgetpassword/ForgotPasswordViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/login/forgetpassword/ForgotPasswordScreenTest.kt
  modified: []
key-decisions:
  - "ViewModel injects ClickMeApi directly (public endpoint, no session state)"
  - "start(initialEmail) applies once per instance and prefers the SavedStateHandle value"
  - "onNavigated() resets saveState to Idle so Back from Set New Password shows an active button"
requirements-completed: []
duration: ~25 min
completed: 2026-10-20
---

# Phase 3 Plan 05: Forgot Password screen Summary

Reset Password screen (iOS `LoginForgetPassView`/`LoginForgetPassViewModel` port): validates the email after the first send and then live, posts `{identity: trimmed}`, holds "Code Sent!" for 600 ms, then hands the trimmed email to `onCodeSent`. Errors show in the shared dialog and inline when the server flags `identity` or `email`.

## Tasks

| Task | Commits |
|------|---------|
| 1. ForgotPasswordViewModel (600 ms hold, D-08/D-09) | RED c808cc6, GREEN 16272ad |
| 2. ForgotPasswordScreen per UI-SPEC Screen 2 | RED 1705a35, GREEN a871423 |

## Verification

- `ForgotPasswordViewModelTest` 12 tests (including 599 ms vs 601 ms navigation) and `ForgotPasswordScreenTest` 8 tests pass.
- `ktlintCheck` and `lintDevDebug` pass; `scripts/check-design-rules.sh` prints "Design rules: OK".
- Acceptance greps: no `fun resend`, no `fontSize`, `AuthTopAppBar` and `AuthErrorDialog` used.

## Deviations from Plan

None in behavior. Notes:
- RED commits are compile-failing tests (types did not exist yet), the same RED state as plan 03-02.
- The worktree HEAD (d1217fb) was behind the expected base and was reset to 70ae788 at startup, as the startup check prescribes.
- The visible "Email" label uses `clearAndSetSemantics {}` per the UI-SPEC, so it is not in the semantics tree and cannot be asserted by text. The test asserts the field placeholder ("Email") instead.

## Known Stubs

None.

## Threat Flags

None. T-03-17 (success copy never claims the account exists) and T-03-18 (no resend or cooldown loop, send ignored unless Idle, 429 message shown as is) are implemented; T-03-19 accepted.

## Deferred Items

- Device check of the Reset Password screen (IME focus move on a failed validation, TalkBack reading order) is deferred to the phase's manual testing pass.
- REQUIREMENTS.md not touched: AUTH-02 completes with the Set New Password plan and navigation wiring (03-10).

## Self-Check: PASSED

All five created files exist; commits c808cc6, 16272ad, 1705a35, a871423 are in the branch log.
