---
phase: 04-signup-onboarding-mode-switch
plan: 10
subsystem: signup
tags: [compose, signup, validation, auth-navigation]
requires:
  - phase: 04-03
    provides: AuthValidators.signupEmail/signupPassword, AuthErrorDialog(title=), AuthTopAppBar options
  - phase: 04-06
    provides: UserManager.signup
  - phase: 04-07
    provides: SignupSetupRepository.seedFromSignup, SignupTestHarness
provides:
  - SignupInitialViewModel, SignupInitialScreen and stateless SignupInitialContent (SIGN-01)
  - AuthNavHost SignUpKey entry renders the real Create Account step
affects: [04-11, 04-18]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/initial/SignupInitialViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/initial/SignupInitialScreen.kt
    - app/src/main/res/values/strings_signup_initial.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/initial/SignupInitialViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/initial/SignupInitialScreenTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/AuthNavHost.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/AuthKeys.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/AuthNavigator.kt
    - app/src/main/res/values/strings_auth_screens.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/login/AuthStaticScreensTest.kt
  deleted:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/SignUpPlaceholderScreen.kt
key-decisions:
  - "Only the email is mirrored to SavedStateHandle (signup_email); the password stays in ViewModel memory (T-04-26)"
  - "A 422 that carries email/password errors maps inline; any other 422 falls through to the dialog"
requirements-completed: [SIGN-01]
completed: 2026-10-21
---

# Phase 4 Plan 10: Signup Initial Step Summary

Create Account (email and password only) with silent-then-live validation, the server password rule, a strength meter, 409/422 inline mapping, and an auth-stack swap that hands the root route to the setup hub.

## Accomplishments
- `SignupInitialViewModel`: validates with `AuthValidators.signupEmail/signupPassword`, trims the email, calls `userManager.signup`, seeds the draft via `seedFromSignup(email, emailVerified)`, then emits `LaunchDestination.SignupSetup`. 409 or EMAIL_TAKEN goes under the email (server message, else local string); 422 maps `email`/`password` fields inline, otherwise the dialog; everything else uses `toDialogError()`; suspended accounts are left to the global dialog. Double submit is ignored, `isSubmitting` always clears.
- `SignupInitialScreen`/`SignupInitialContent`: glow background, transparent `AuthTopAppBar` (Back disabled while submitting), heading semantics, email and password fields (`ContentType.NewPassword`), animated `PasswordStrengthMeter` once the password is non-empty, rule hint turning destructive on error, always-enabled Continue via `AsyncSaveButton`, dialog titled "Couldn't create account".
- `AuthNavHost`: `entry<SignUpKey>` now renders `SignupInitialScreen(onSignedUp = onLoggedIn, ...)`. Placeholder screen, its four strings and its two tests removed; KDocs updated.

## Task Commits
1. Task 1 (ViewModel + 14 tests): `53d8989`
2. Task 2 (screen, nav swap, placeholder removal, 10 screen tests): `f33af86`

## Verification
- `SignupInitialViewModelTest` 14 tests, `SignupInitialScreenTest` 10 tests, `AuthStaticScreensTest`, `AuthNavDisplayTest`, `AuthNavigatorTest` all pass (`:app:testDevDebugUnitTest`).
- `ktlintCheck` and `lintDevDebug` pass; `bash scripts/check-design-rules.sh` prints "Design rules: OK".
- Acceptance greps hold: one `userManager.signup(`, one `seedFromSignup(`, no password in SavedStateHandle, no username/phone in the screen or strings, no remaining `SignUpPlaceholderScreen`/`signup_placeholder_` references.

## Deviations from Plan
- Tests were written together with the implementation per task (single commit per task, no separate RED commit), as in earlier phase-4 plans.
- `tappingContinueCallsOnSubmit` needed `performScrollTo()` because the Continue button sits below the fold of the small Robolectric screen (test-only adjustment).

## Manual checks deferred (project policy)
- On device: Login > Sign Up > create an account lands on the setup hub and system Back does not return to Create Account.
- On device: an existing email shows the server message under the email field; airplane mode shows the network dialog.
- On device: password manager offers to save/generate on the password field; keyboard does not cover Continue.

## Known Stubs
None.

## Threat Flags
None. T-04-26 and T-04-28 mitigations implemented as planned (email-only saved state, `isSubmitting` guard with a test).

## Self-Check: PASSED
