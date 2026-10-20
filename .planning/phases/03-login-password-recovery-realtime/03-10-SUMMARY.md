---
phase: 03-login-password-recovery-realtime
plan: 10
subsystem: auth-navigation
tags: [navigation3, back-stack, compose, hilt-viewmodel, robolectric]
requires: [03-04, 03-05, 03-06, 03-07]
provides:
  - navigation.LoginKey, ForgotPasswordKey(email), SetNewPasswordKey(email), PasswordChangedKey(email), SignUpKey
  - navigation.AuthNavigator(navigator) with openForgotPassword/openSetNewPassword/openPasswordChanged/openSignUp/back/returnToLogin/handleBack
  - navigation.slideTransition(forward) (shared, extracted from HomeTabScaffold)
  - navigation.AuthNavDisplay (Hilt-free) and navigation.AuthNavHost(onLoggedIn)
  - ClickMeApp AppRoute.Login -> AuthNavHost(onLoggedIn = appViewModel::onLoggedIn)
affects: [phase-4 signup (swap SignUpPlaceholderScreen for SignupInitialScreen in AuthNavHost)]
tech-stack:
  added: []
  patterns: [single auth back stack rooted at Login over Navigator, email carried in serializable NavKeys, Hilt-free NavDisplay wrapper for Robolectric tests]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/AuthKeys.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/AuthNavigator.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/AuthNavHost.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/NavTransitions.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/navigation/AuthNavigatorTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/navigation/AuthNavDisplayTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/HomeTabScaffold.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/ClickMeApp.kt
key-decisions:
  - "AuthNavigator exposes its wrapped Navigator as a public val so tests can read the stack"
  - "Double-tap guard compares key class (top::class), so Forgot with a different email is still blocked while Forgot is on top"
requirements-completed: [AUTH-01, AUTH-02]
duration: ~20 min
completed: 2026-10-20
---

# Phase 3 Plan 10: Auth navigation wiring Summary

One auth back stack under `AppRoute.Login`: Login at the root, with Forgot Password, Set New Password, Password Changed and the Sign Up placeholder pushed on it. Pushes use the Phase 1 slide transition, and the old empty `onForgotPassword`/`onSignUp` lambdas in `ClickMeApp` are gone.

## Tasks

| Task | Commits |
|------|---------|
| 1. Auth NavKeys, AuthNavigator, shared slide transition | RED 4334e65, GREEN 4655852 |
| 2. AuthNavHost, AuthNavDisplay, ClickMeApp Login branch | f078218 |

## Behavior delivered

- Forgot Password carries the Login email (D-07). Set New Password and Password Changed carry the trimmed email in their keys, so process death restores the stack.
- "Go to Login" and system Back on Password Changed both call `returnToLogin()` (`reselect(LoginKey)`), which pops every recovery entry. The reset email then goes to `loginPrefill`, a `rememberSaveable` value that survives rotation, and Login consumes it (D-10, D-11).
- Back on Set New Password pops to Forgot with its email kept. Back on Forgot or Sign Up pops to Login. At the Login root the stack has one entry, so the activity exits.
- Each pushed entry has its own ViewModel scope. Popping and re-pushing Forgot gives a new ViewModel (test asserts two distinct instances), and `createSavedStateHandle()` works inside an entry.
- Login success still goes through `AppViewModel.onLoggedIn`.
- No `ModalBottomSheet` in `AuthNavHost`.

## Verification

- `AuthNavigatorTest`: 9 JVM tests, all pass.
- `AuthNavDisplayTest`: 4 Robolectric tests, all pass.
- `NavigatorTest`, `HomeTabScaffoldTest` and `RootScreensTest` (5 tests) still pass.
- `ktlintCheck`, `lintDevDebug` and `assembleDevDebug` exit 0, so the Hilt graph builds every entry ViewModel.
- `scripts/check-design-rules.sh` prints "Design rules: OK".

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] ktlint chain-method-continuation violations**
- **Found during:** Task 1 verification
- **Issue:** Chained calls on one line in `AuthNavigator.top` and the test helper broke ktlint.
- **Fix:** Split them into a local `val` followed by a single call.
- **Files modified:** AuthNavigator.kt, AuthNavigatorTest.kt
- **Commit:** 4655852

**2. [Process] Task 2 RED commit skipped**
- `AuthNavDisplayTest` and `AuthNavHost.kt` were written before the first Gradle run, so Task 2 has a single feat commit rather than a RED/GREEN pair.
- The test file was authored first and exercises the contract listed in the plan, but there is no separate failing-test commit for this task.

**3. [Setup] Worktree base correction**
- The worktree started behind the expected base `521fe5e`, so HEAD was reset to it per the startup protocol. No work was lost.

## TDD Gate Compliance

Task 1 has the RED (`test(03-10)`) then GREEN (`feat(03-10)`) commits. Task 2 has only the GREEN commit (see deviation 2).

## Deferred / Open Items

- On-device check of the recovery loop (slide transitions, predictive back on Password Changed, OTP email delivery) was not done and is deferred per the manual-testing-deferred policy.

## Known Stubs

None. `SignUpPlaceholderScreen` is an intentional Phase 3 placeholder (D-05) that Phase 4 replaces.

## Threat Flags

None. T-03-37, T-03-39 and T-03-40 are mitigated and covered by tests. T-03-38 is accepted: the email is the only value placed in a key.

## Self-Check: PASSED

All created files exist and commits 4334e65, 4655852 and f078218 are in the branch history.
