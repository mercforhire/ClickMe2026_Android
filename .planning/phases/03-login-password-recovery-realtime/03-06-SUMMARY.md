---
phase: 03-login-password-recovery-realtime
plan: 06
subsystem: auth
tags: [compose, password-recovery, otp, viewmodel, password-strength]
requires:
  - phase: 03-login-password-recovery-realtime
    provides: "03-01 resetPassword API and request; 03-02 AuthValidators, FieldError, PasswordStrengthMeter, ValidatedField trailingBadge/contentType, AuthErrorDialog, AuthTopAppBar"
provides:
  - "SetNewPasswordViewModel (@HiltViewModel) with iOS validation, reset request, 400/422 mapping, 600 ms Saved hold"
  - "SetNewPasswordScreen (Hilt wrapper) and stateless SetNewPasswordContent"
  - "strings_set_new_password.xml"
affects: [03-10 auth back stack wiring, 03-11, 03-12]
tech-stack:
  added: []
  patterns:
    - "Code mirrored to SavedStateHandle; passwords in memory only"
    - "Focus request as one-shot StateFlow consumed by the Content composable"
    - "consumeWindowInsets(innerPadding).imePadding() inside Scaffold to avoid double inset"
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/setnewpassword/SetNewPasswordViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/setnewpassword/SetNewPasswordScreen.kt
    - app/src/main/res/values/strings_set_new_password.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/login/setnewpassword/SetNewPasswordViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/login/setnewpassword/SetNewPasswordScreenTest.kt
  modified: []
key-decisions:
  - "400 and 422 are mapped by HTTP status (iOS parity, Pitfall 10), not by ErrorCode"
  - "After a 422 that maps to fields, focus also moves to the first field with an error (UI-SPEC)"
requirements-completed: [AUTH-02]
duration: ~40 min
completed: 2026-10-20
---

# Phase 3 Plan 06: Set New Password Summary

Set New Password screen and ViewModel ported from iOS `LoginResetPasswordView`/`ViewModel`: OTP code entry with SMS autofill, new password with a live strength meter, confirmation with a match badge, and `POST auth/password/reset` with 400 and 422 field mapping.

## Tasks

| Task | Commits |
|------|---------|
| 1. SetNewPasswordViewModel | RED 58c5ee8 (compile-failing tests), GREEN d0c2433 |
| 2. SetNewPasswordScreen, strings, render tests | 16fb330 |

## Verification

- SetNewPasswordViewModelTest: 14 tests pass. SetNewPasswordScreenTest: 11 tests pass.
- `ktlintCheck` and `lintDevDebug` pass; `scripts/check-design-rules.sh` prints "Design rules: OK".
- Acceptance greps: one `ResetPasswordRequest(`, `status == 400` and `status == 422` present, `"password_confirmation"` once, `"reset_code"` present, no `savedStateHandle[...password...]`, no `fun resend`, no `fontSize`, `ContentType.SmsOtpCode` once.

## Deviations from Plan

**1. [Rule 1 - Bug] `KeyboardType.NumberPad` does not exist in Compose**
- **Found during:** Task 2 compile
- **Issue:** The plan (and its acceptance grep `KeyboardType.NumberPad`) names an iOS-style constant; Compose has `KeyboardType.Number`.
- **Fix:** Code field uses `KeyboardType.Number`. The `NumberPad` grep therefore prints 0, which is expected; `ContentType.SmsOtpCode` grep prints 1.
- **Files modified:** SetNewPasswordScreen.kt
- **Commit:** 16fb330

**2. [Rule 3 - Test fit] Test assertions relaxed to `assertExists` for off-viewport nodes**
- The Robolectric default viewport is small, so the label texts and match badge, which sit in the scrolling form, are not "displayed". Those assertions use `assertExists`; field placeholders, strength meter and button are still `assertIsDisplayed`.

**3. Label semantics wrapper**
- `clearAndSetSemantics {}` is applied to a `Box` wrapping the label `Text` rather than the `Text` itself, so the text stays findable in the unmerged tree while TalkBack skips it.

## Known Stubs

None.

## Threat Flags

None. T-03-20 (passwords never in SavedStateHandle), T-03-21 (single generic 400 message, no resend) and T-03-22 (no logging in these files) are implemented as planned.

## Deferred / Open Items

- On-device check (Samsung): SMS OTP autofill suggestion on the code field, and focus moving to the first failing field on submit. Not blocking; plan 03-10 wires navigation.
- `FocusRequester` is attached to the `ValidatedField` outer `Column` modifier (focus is delegated to the child text field). This is exercised by no unit test; confirm on device.

## Self-Check: PASSED

All five created files exist and commits 58c5ee8, d0c2433 and 16fb330 are in the branch log.
