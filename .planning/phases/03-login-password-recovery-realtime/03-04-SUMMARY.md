---
phase: 03-login-password-recovery-realtime
plan: 04
subsystem: login-ui
tags: [compose, login, viewmodel, validation, robolectric]
requires: [03-02]
provides:
  - screens.login.LoginViewModel (D-02 timing, D-04 error split, prefill hook, focus requests)
  - screens.login.LoginScreen / LoginContent (real iOS LoginView port)
  - res/values/strings_login.xml
affects: [03-10 auth nav host]
tech-stack:
  added: []
  patterns: [SavedStateHandle mirrors email only, FieldError Res/Text split for local vs server messages]
key-files:
  created:
    - app/src/main/res/values/strings_login.xml
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/LoginViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/LoginScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/ClickMeApp.kt
    - app/src/main/res/values/strings_session.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/login/LoginViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/login/LoginScreenTest.kt
key-decisions:
  - "A server (Text) password error is cleared on the next edit of either field; email edit does not re-validate the password"
  - "Forgot Password and Sign Up are required callbacks; ClickMeApp passes empty lambdas until plan 03-10 swaps in AuthNavHost"
requirements-completed: [AUTH-01]
duration: ~45 min
completed: 2026-10-20
---

# Phase 3 Plan 04: Login screen Summary

The temporary Phase 2 login was rewritten in place into the iOS `LoginView` port: no validation until the first submit then live re-validation, the D-04 error split (401 under the password, 422 email inline, everything else in a dialog, suspension silent), the Forgot Password link and the Sign Up footer.

## Tasks

| Task | Commit |
|------|--------|
| 1. LoginViewModel (validation timing, error split, SavedStateHandle email, prefill) + 19 tests | 87b280e |
| 2. LoginScreen layout, strings_login.xml, trimmed strings_session.xml, ClickMeApp wiring + 9 tests | 4c6fb90 |

## Verification

- `:app:testDevDebugUnitTest --tests '*LoginViewModelTest' --tests '*LoginScreenTest'` (19 + 9 tests), `ktlintCheck`, `lintDevDebug`, `assembleDevDebug` all pass.
- `scripts/check-design-rules.sh` prints "Design rules: OK".
- Acceptance greps: no `LoginError` or `fontSize` left, no password key in SavedStateHandle (only `login_email`, asserted by a test).

## Deviations from Plan

None in behavior.

- Commit 87b280e alone does not compile the app module (LoginScreen still referenced the removed `LoginError` until the next commit); the pair compiles and passes. Tasks were verified together.
- `LoginScreenTest` scrolls to the "Sign Up" footer before clicking because it sits below the fold of the small Robolectric screen.
- Test and test-helper TDD RED commits were not made separately: the ViewModel test could not compile before the screen was updated, so tests and implementation were committed per task.

## Known Stubs

`onForgotPassword = {}` and `onSignUp = {}` in `ClickMeApp.kt` are intentional until plan 03-10 replaces the Login branch with `AuthNavHost`.

## Threat Flags

None. T-03-13 (password never in SavedStateHandle), T-03-15 (isSubmitting guard) and T-03-16 (ACCOUNT_SUSPENDED shows nothing) are implemented and tested.

## Deferred Items (human/device)

- On-device visual check of the Login layout, glow background, IME Next/Done behavior and autofill suggestions on the Samsung test device (manual testing deferred per project memory).

## Self-Check: PASSED

strings_login.xml and the six modified files exist; commits 87b280e and 4c6fb90 are on the branch.
