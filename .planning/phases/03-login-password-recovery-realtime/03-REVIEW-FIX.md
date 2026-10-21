---
phase: 03-login-password-recovery-realtime
fixed_at: 2026-10-21T00:33:25Z
review_path: .planning/phases/03-login-password-recovery-realtime/03-REVIEW.md
iteration: 1
findings_in_scope: 4
fixed: 4
skipped: 0
status: all_fixed
---

# Phase 3: Code Review Fix Report

**Fixed at:** 2026-10-21T00:33:25Z
**Source review:** .planning/phases/03-login-password-recovery-realtime/03-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 4 (WR-01..WR-04; fix_scope = critical_warning, 0 critical)
- Fixed: 4
- Skipped: 0

Every fix has a regression test. I confirmed each test fails on the pre-fix code and passes on the fixed code.

Final gate on main after all four commits:
- `./gradlew :app:testDevDebugUnitTest`: 436 tests, 0 failures, 0 errors
- `ktlintCheck`: passed
- `lintDevDebug`: 0 errors, 1 warning. The warning is the existing AndroidGradlePluginVersion notice about the Gradle wrapper and is unrelated to these fixes.
- `scripts/check-design-rules.sh`: OK

## Fixed Issues

### WR-01: Stale-socket guard in RealtimeService is a check-then-act race

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeService.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeServiceProtocolTest.kt`
**Commit:** b61ef63
**Applied fix:** `guarded` now checks the generation and runs its side effect (the `_state` write and the `failureBus` emit) inside `synchronized(lock)`. That is the same reentrant lock `connect()`/`disconnect()` use. Inbound events are decoded outside the lock; only the generation check and the `_events.tryEmit` happen under it. New test `oldSocketListenerAlreadyRunningWhenTokenRotatesDoesNotEmit`: a decoder hook calls `connect("good-2")` from another thread while the old socket's listener is still running. The test asserts that only the new socket's event is delivered. On the old code it receives the stale `TypingStart(old)` as well.
**Note:** the test exercises the inbound-event path. The `CONNECT`/`CONNECT_ERROR`/`DISCONNECT` state path uses the same lock-and-check pattern through `guarded`, but no test injects the race there, because the library gives no hook inside those listeners.

### WR-02: Socket can connect while backgrounded: `isDesired()` is not checked again after suspension points

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeConnectionManager.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeConnectionManagerTest.kt`
**Commit:** 35441a9
**Applied fix:** `connectWithStoredToken()` checks `isDesired()` again after the `withContext(io)` token read. This covers the Refreshed collector, the retry job and every other caller. `onTokenRejected()` checks `isDesired()` both before and after its own token read, so a failure queued before a background or logout teardown neither probes `/me` nor reconnects. In the test harness, the manager now gets a `HookedTokenStore` wrapper whose hook runs inside the token read, which makes the interleaving deterministic. Five new tests (all fail on the old code):
- `refreshedThatGoesToBackgroundDuringTokenReadDoesNotConnect`
- `retryThatGoesToBackgroundDuringTokenReadDoesNotConnect`
- `rotatedTokenRejectionQueuedBeforeBackgroundDoesNotReconnect`
- `tokenRejectionQueuedBeforeBackgroundDoesNotProbe`
- `tokenRejectionThatGoesToBackgroundDuringTokenReadDoesNotProbe`

### WR-03: Predictive back on Password Changed shows the used Set New Password form, then jumps to Login

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/AuthNavigator.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/navigation/AuthNavigatorTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/navigation/AuthNavDisplayTest.kt`
**Commit:** 48d0f79
**Applied fix:** `openPasswordChanged()` keeps its double-tap guard. It then calls `returnToLogin()` (`reselect(LoginKey)`) and pushes `PasswordChangedKey`, so the stack is `[Login, PasswordChanged]`. NavDisplay's `isPop` treats the change from `[L, F, S]` to `[L, PC]` as a push, because the stacks diverge at index 1, so the forward slide is kept. `handleBack()` is unchanged: on Password Changed it still clears to Login and reports the pre-fill, which is now a single pop. `AuthNavHost.kt` did not need changes.

Test changes:
- Replaced `recoveryFlowPushesEveryStep` with `recoveryFlowPushesForgotAndSetNewPassword` and `passwordChangedReplacesRecoveryScreensSoLoginIsDirectlyBelow`.
- Added `openPasswordChangedTwiceIsDoubleTapGuarded` and `plainPopOnPasswordChangedLandsOnLogin`.
- Added the NavDisplay test `passwordChangedReleasesRecoveryScreensAndTheirViewModels`, which checks that the Set New Password entry ViewModel is cleared.

**Needs a device check:** the predictive-back gesture preview itself, which should now show Login. It cannot be driven from Robolectric.

### WR-04: Leaving Set New Password during Saving or the 600 ms "Saved" hold loses the success

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/setnewpassword/SetNewPasswordScreen.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/AuthTopAppBar.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/login/setnewpassword/SetNewPasswordScreenTest.kt`
**Commit:** 3fcab9e
**Applied fix:** I used the review's primary option. `SetNewPasswordContent` registers `BackHandler(enabled = saveState != Idle) {}`. In activity-compose 1.13 this handler goes through the same NavigationEventDispatcher as NavDisplay's, and it takes priority as the inner handler. `AuthTopAppBar` gets a new `backEnabled: Boolean = true` parameter. The form passes `editable`, so the arrow is disabled for the same window. `SetNewPasswordViewModel` did not need changes. I did not use the alternative (setting `_passwordChanged` right after the API call and moving the hold into the screen), because it would still lose a success if the user left while the request was in flight. New tests:
- `topBarBack_isDisabledWhileSaving`
- `topBarBack_isDisabledDuringSavedHold`
- `systemBack_whileIdle_reachesNavigation` (control)
- `systemBack_whileSaving_isSwallowed`
- `systemBack_duringSavedHold_isSwallowed`

The system-Back tests put an outer `BackHandler` in place of NavDisplay's and fire Back through the activity's `OnBackPressedDispatcher`.

**Notes:**
- Back is now blocked while the reset request is in flight. The longest wait is the OkHttp call timeout.
- `ForgotPasswordViewModel` has the same hold pattern. The review calls it harmless and it was not changed.
- **Needs a device check:** system Back and the predictive gesture are inert during "Saving…" and "Password Updated!".

---

_Fixed: 2026-10-21T00:33:25Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
