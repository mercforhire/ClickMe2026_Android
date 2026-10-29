---
phase: 15-polish-hardening
plan: 05
subsystem: login-recovery
tags: [login, recovery, talkback, ime, saved-state, secrets, tracer]
requires:
  - phase: 15-01
    provides: R.string.a11y_in_progress
  - phase: 15-02
    provides: Modifier.contentMaxWidth(), InsetsTestSupport
  - phase: 15-04
    provides: A11yAudit helpers, AsyncSaveButton busyDescription
provides:
  - Login screen meeting UI-SPEC Flow 1 (heading, one sign-up node, "In progress" busy state, password-error focus)
  - Reset code and passwords never written to SavedStateHandle
  - contentMaxWidth on Login, Forgot Password, Set New Password and Password Changed
affects: [15-verification, device-checklist]
tech-stack:
  added: []
  patterns:
    - "Sign-up prompt as one clickable Row (merged node, link half is styled text only)"
    - "contentMaxWidth() placed after verticalScroll() so the scroll container stays full width"
key-files:
  created:
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/login/LoginTalkBackFlowTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/login/LoginInsetsTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/login/passwordchanged/PasswordChangedScreenTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/LoginScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/setnewpassword/SetNewPasswordViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/setnewpassword/SetNewPasswordScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/forgetpassword/ForgotPasswordScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/passwordchanged/PasswordChangedScreen.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/login/LoginScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/login/setnewpassword/SetNewPasswordViewModelTest.kt
key-decisions:
  - "Kept the SavedStateHandle constructor parameter on SetNewPasswordViewModel (unused, suppressed) so the tests can prove no secret is ever written to it"
  - "Focus on a server wrong-password error is driven from LoginContent (LaunchedEffect on passwordError + isSubmitting), not the ViewModel, because LoginViewModel is outside this plan's file list"
  - "Tests use the real string resources (button 'Login', dialog 'OK'), not the plan's prose 'Log in' / 'Got it'; copy was not changed"
patterns-established:
  - "One-node link row: Row.clickable(role = Button) with plain Text children"
status: complete
commits: 2
plan_head_before: e808da6b9c3dd62717fc017104c562ebca5b1350
plan_head_after: 8dc09b49
actuals:
  tokens: 30000
  tasks: 2
  commits: 2
---

# Phase 15 Plan 05: Login and recovery polish Summary

**Login now reads in the Flow 1 TalkBack order with a heading, a single sign-up node and an "In progress" busy state, and the Set New Password reset code no longer touches SavedStateHandle.**

## Accomplishments

- LoginScreen: wordmark has `heading()`; "Don't have an account? Sign Up" is one clickable merged node with a 48 dp row; the Log in `AsyncSaveButton` passes `busyDescription = stringResource(R.string.a11y_in_progress)`; the form column is capped with `contentMaxWidth()` inside the scroll container (glow and scroll area stay full width).
- Wrong-password: `LaunchedEffect(hasPasswordError, isSubmitting)` requests focus on the password field once it is enabled again; `ValidatedField` already supplies `error()` semantics. Other failures keep using `AuthErrorDialog` and typed values are untouched.
- SetNewPasswordViewModel: `KEY_CODE` / `"reset_code"` mirror removed; KDoc rewritten to state D-09 overrides T-03-20.
- Forgot Password, Set New Password and Password Changed are width-capped. On Set New Password the cap sits on the outer column so the pinned Save button lines up with the form.
- Password Changed already had a `FocusRequester` on its heading headline (requested once in a `LaunchedEffect`); this plan only added the test that locks it in.

## Test results

- Task 1: LoginTalkBackFlowTest 6/6, LoginInsetsTest 2/2, LoginScreenTest 9/9, LoginViewModelTest 19/19.
- Task 2: SetNewPasswordViewModelTest 17/17 (new: `codeNeverInSavedState`, `passwordsNeverInSavedState`, `restoredHandleStartsEmpty`, `codeIsSanitized`), PasswordChangedScreenTest 2/2, SetNewPasswordScreenTest 16/16, ForgotPasswordScreenTest 8/8, AuthNavigatorTest 12/12, AuthNavDisplayTest 5/5, AuthStaticScreensTest 5/5.
- `ktlintCheck` clean; `bash scripts/check-design-rules.sh` prints "Design rules: OK".

## Deviations from Plan

**1. [Rule 1 - plan/code mismatch] Button and dialog copy**
- The plan names the button "Log in" and the dialog action "Got it"; the real resources are `login_submit` = "Login" and `auth_dialog_ok` = "OK". Tests assert the real strings via `R.string`; no copy was changed (out of scope).

**2. [Rule 3] LoginInsetsTest scrolls the button into view before measuring**
- Content (about 490 dp) plus the 300 px IME inset leaves a scrolling viewport of 500 dp, so the button is reached by scroll. The test calls `performScrollTo()` then asserts the bottom is at or above 500 dp, which is the real guarantee for a scrollable form.

**3. [Rule 3] SavedStateHandle parameter kept**
- The plan allows removing it when no key uses it. It was kept (unused, `@Suppress("unused")`) because the plan's own behavior tests (`codeNeverInSavedState`, `restoredHandleStartsEmpty`) inspect the handle; a future re-added mirror would fail them.

**4. [Rule 3] PasswordChanged focus code already present**
- Plan step 2 asked for the `FocusRequester`; it was already in place from Phase 3, so only the test was added.

## Known Stubs

None.

## Threat Flags

None. T-15-09 (reset code in SavedStateHandle) is mitigated and covered by `codeNeverInSavedState` and `restoredHandleStartsEmpty`.

## Open items for the device checklist (human checks deferred by the user)

- TalkBack on a device: Login reading order, heading announcement, single "Don't have an account? Sign Up" action, "In progress" on the busy Log in button, focus landing on the password field after a wrong password.
- Real keyboard on a small phone: the Log in button reachable above the IME.
- Process-death check on Set New Password: after "Don't keep activities" and returning, code and passwords are empty with no error text.
- Tablet or resized window: Login and recovery screens centered at 600 dp.
- Robolectric proves exposure of semantics, not TalkBack speech (RESEARCH Pitfall 9).

## Self-Check: PASSED

- Commits e46ce1c7 and 8dc09b49 exist on the worktree branch; all created files present.
