---
phase: 03-login-password-recovery-realtime
reviewed: 2026-10-21T00:15:57Z
depth: standard
files_reviewed: 74
files_reviewed_list:
  - app/build.gradle.kts
  - app/src/debug/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeDebugLogger.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/ClickMeApp.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/ClickMeApplication.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/PasswordStrength.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/PasswordStrengthMeter.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/ValidatedField.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/di/RealtimeModule.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/SessionEvents.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/UserManager.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/AuthRequests.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/SuccessMessageResponse.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/AuthKeys.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/AuthNavHost.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/AuthNavigator.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/HomeTabScaffold.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/NavTransitions.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrls.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ClickMeApi.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/realtime/ProcessForeground.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeConnection.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeConnectionManager.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeEmits.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeEventDecoder.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeEvents.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeService.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeUrl.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/AuthErrorDialog.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/AuthTopAppBar.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/AuthValidators.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/FieldError.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/LoginScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/LoginViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/forgetpassword/ForgotPasswordScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/forgetpassword/ForgotPasswordViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/passwordchanged/PasswordChangedScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/setnewpassword/SetNewPasswordScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/setnewpassword/SetNewPasswordViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/SignUpPlaceholderScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/ui/theme/Motion.kt
  - app/src/main/keepRules/rules.keep
  - app/src/main/res/values/strings_auth.xml
  - app/src/main/res/values/strings_auth_screens.xml
  - app/src/main/res/values/strings_forgot_password.xml
  - app/src/main/res/values/strings_login.xml
  - app/src/main/res/values/strings_session.xml
  - app/src/main/res/values/strings_set_new_password.xml
  - app/src/release/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeDebugLogger.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/components/AuthComponentsTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/components/PasswordStrengthTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/navigation/AuthNavDisplayTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/navigation/AuthNavigatorTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrlsTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ClickMeApiTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/network/NetworkStackTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeConnectionContractsTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeConnectionManagerTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeDebugLoggerTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeEmitsTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeEventDecoderTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeServiceProtocolTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeUrlTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/login/AuthStaticScreensTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/login/AuthValidatorsTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/login/LoginScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/login/LoginViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/login/forgetpassword/ForgotPasswordScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/login/forgetpassword/ForgotPasswordViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/login/setnewpassword/SetNewPasswordScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/login/setnewpassword/SetNewPasswordViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeClickMeApi.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeProcessForeground.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeRealtimeConnection.kt
  - gradle/libs.versions.toml
findings:
  critical: 0
  warning: 4
  info: 10
  total: 14
status: issues_found
---

# Phase 3: Code Review Report

**Reviewed:** 2026-10-21T00:15:57Z
**Depth:** standard
**Files Reviewed:** 74
**Status:** issues_found

## Summary

I reviewed the Phase 3 login, password recovery and realtime socket work: 48 production files plus 26 test files. I checked behavior against the iOS source of truth (`LoginViewModel.swift`, `LoginResetPasswordViewModel.swift`, `LoginForgetPassViewModel.swift`, `RealtimeService.swift`) and the backend `src/services/socket.js`. I also checked the socket.io-client 2.1.2 claims against the shipped bytecode: `Backoff.duration()` clamps to `[ms, max]`, so equal min and max really give exactly 2 s; a `CONNECT_ERROR` packet calls `destroy()` and does not retry; a DISCONNECT packet emits `"io server disconnect"` after `destroy()`. I checked the nav3 1.2.0 and lifecycle 2.11.0 sources for back-stack and ViewModel-scope behavior.

I found no blockers. JWT handling is sound. The token goes only in the CONNECT packet (`auth`), never in the URL or a header. `HandshakeFailure.toString` redacts it. The debug logger only prints redacted `toString` output. The release stub is a no-op. The bare OkHttp client only logs at BASIC level in debug (request line only, no token in the socket URL). Listeners do not leak on reconnect: `off()` runs before `disconnect()`, and every connection gets a fresh `forceNew` Manager.

Answers to the orchestrator's specific questions:

- **SetNewPassword focus-on-error:** it works. `FocusRequester.requestFocus()` on a node that cannot take focus itself hands focus to its first child focus target. Inside `ValidatedField`'s outer `Column` that child is the `BasicTextField` (the reveal `IconButton` cannot take focus in touch mode). No test covers it, though (IN-01).
- **Realtime threading:** policy state is confined to a `limitedParallelism(1)` view. But two paths can still connect after the app goes to the background, because `isDesired()` is not checked again after a suspension point (WR-02). In `RealtimeService`, the generation guard is not atomic with the state write (WR-01). Reconnect every 2 s, reconnect on token refresh, and handling of a graceful server close (`io server disconnect` → own backoff of 2/4/8/16/30 s) all behave as documented.
- **Auth back stack:** "Go to Login" and system Back on Password Changed both clear the stack to `[LoginKey]` and pre-fill the email. However, predictive back (enabled in the manifest) shows Set New Password during the gesture and then lands on Login (WR-03).

## Warnings

### WR-01: Stale-socket guard in RealtimeService is a check-then-act race

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeService.kt:156-188`
**Issue:** `guarded` reads `generation` (volatile) on the socket.io EventThread, then writes `_state` and emits to `failureBus` without holding `lock`. `connect()`/`disconnect()` update `generation` and `_state` under `lock` on the policy (IO) thread. `teardownLocked()` calls `off()`, but a listener that is already running keeps going. So this interleaving can happen:

1. EventThread: the old socket's `EVENT_CONNECT_ERROR` (or `EVENT_CONNECT`) listener passes `gen == generation`.
2. IO thread: `connect(newToken)` tears down, bumps `generation` and sets `_state = Connecting` for the new socket.
3. EventThread: the old listener writes `_state = Disconnected` (or `Connected`) and emits a `HandshakeFailure` for a socket that no longer exists.

A stale `Connected` state lets `send()` emit on the new socket before it is connected, so the library buffers the emit and flushes it later. That is exactly the stale-emit case `send()` is meant to prevent. A stale `Disconnected` state turns the next same-token `connect()` into a needless teardown of a healthy socket. A stale failure reaches `RealtimeConnectionManager.onFailure`, which can start an unneeded `/me` probe or a retry. The KDoc promises "callbacks from a superseded socket [are] no-ops", but that is only true outside this window.
**Fix:** Do the generation check and the side effect under the same lock. Nothing that holds `lock` ever waits on the EventThread (`connect()`/`disconnect()` only post to it), and the lock is reentrant, so this cannot deadlock. Decode outside the lock:
```kotlin
fun guarded(block: (Array<Any?>) -> Unit) =
    Emitter.Listener { args ->
        try {
            synchronized(lock) { if (gen == generation) block(args) }
        } catch (t: Throwable) {
            Log.w(TAG, "listener failed: ${t.javaClass.simpleName}")
        }
    }
// For inbound events, decode before taking the lock:
s.on(name) { args ->
    try {
        val event = (args.firstOrNull() as? JSONObject)?.let { decoder.decode(name, it.toString()) }
        synchronized(lock) { if (gen == generation && event != null) _events.tryEmit(event) }
    } catch (t: Throwable) { Log.w(TAG, "listener failed: ${t.javaClass.simpleName}") }
}
```

### WR-02: Socket can connect while backgrounded: `isDesired()` is not checked again after suspension points

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeConnectionManager.kt:84, 97-101, 123-127, 160-163`
**Issue:** The `serial` view gives one coroutine at a time, but each `withContext(io) { tokens.accessToken }` suspends and frees `serial` for other collectors. Three paths can connect after the user has gone to the background:

1. **Refreshed collector (line 84).** It checks `isDesired()` and enters `connectWithStoredToken()`. While it waits on `io`, the `combine` collector runs `desired=false` → `disconnect()`. Then the Refreshed coroutine resumes and calls `connection.connect(token)`. `combine` has `distinctUntilChanged`, so it never emits `false` again, and the socket stays connected in the background until the next foreground/background cycle. This breaks D-14.
2. **Retry job (line 160-163).** The job sets `retryJob = null` and then calls `connectWithStoredToken()`. A `cancelRetry()` from the `combine` collector during the `io` hop finds `null` and cannot cancel it, so the same race applies.
3. **`onTokenRejected`, `stored != rejected` branch (line 127).** It calls `connectWithStoredToken()` with no `isDesired()` gate at all. A `HandshakeFailure` that was emitted just before teardown sits in `failureBus` and gets processed after `disconnect()`. Only `afterProbe` checks the condition.

Logout is safe only because `logout()` clears the tokens before it flips `isLoggedIn`, so the read returns null. Going to the background is not protected.
**Fix:** Check the policy again after the suspension, inside the shared helper:
```kotlin
private suspend fun connectWithStoredToken() {
    cancelRetry()
    val token = withContext(io) { tokens.accessToken }
    if (!isDesired()) return          // state may have flipped while we were on io
    if (token == null) connection.disconnect() else connection.connect(token)
}

private suspend fun onTokenRejected(rejected: String) {
    if (!isDesired()) return
    ...
}
```
Add a manager test: emit `Refreshed`, flip `foreground` to false before `runCurrent()`, and assert there is no extra `connect` call. The fake token store would need a suspending or delayed read to reproduce the interleaving.

### WR-03: Predictive back on Password Changed shows the used Set New Password form, then jumps to Login

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/AuthNavigator.kt:23, 37-45`; `app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/AuthNavHost.kt:100-107`
**Issue:** The manifest sets `android:enableOnBackInvokedCallback="true"` and `AuthNavDisplay` defines a `predictivePopTransitionSpec`. NavDisplay builds the predictive "previous scene" from the back stack (nav3 `SceneState`: `previousScenes` comes from `previousEntries`). On Password Changed that previous entry is `SetNewPasswordKey`. So during the back gesture the user sees the Set New Password form slide in, with its ViewModel still alive and the code restored from `SavedStateHandle`. On commit, `handleBack` pops three entries and the screen snaps to Login. The animation shows a destination the user never reaches, and it shows the used OTP screen that D-10/D-11 says must be unreachable. This also leaves the recovery ViewModels (and the new password, held in memory in `SetNewPasswordUiState`) alive for as long as Password Changed is on screen.
**Fix:** When pushing Password Changed, drop the recovery screens first so the stack is `[Login, PasswordChanged]`. Then a normal pop goes to Login, the predictive animation previews the right screen, and `handleBack` only has to report the pre-fill:
```kotlin
fun openPasswordChanged(email: String) {
    if (top is PasswordChangedKey) return
    navigator.reselect(LoginKey)                 // pops ForgotPassword + SetNewPassword (onPop clears their VMs)
    navigator.navigate(PasswordChangedKey(email))
}

fun handleBack(onReturnToLogin: (String) -> Unit) {
    val current = top
    back()
    if (current is PasswordChangedKey) onReturnToLogin(current.email)
}
```
Update `AuthNavigatorTest.recoveryFlowPushesEveryStep` to match.

### WR-04: Leaving Set New Password during Saving or the 600 ms "Saved" hold loses the success

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/setnewpassword/SetNewPasswordViewModel.kt:136-156`; `SetNewPasswordScreen.kt:128`
**Issue:** The top-bar back arrow and system Back stay enabled while `saveState` is `Saving` or `Saved`. If the user leaves after the server has accepted the reset (during the `delay(SAVED_CONFIRMATION_HOLD_MS)` hold, or while the response is in flight), the entry is popped and `viewModelScope` is cancelled, so `_passwordChanged` is never set. The user lands on Forgot Password with a reset button that is active again. Their password is already changed and the code is used up, but nothing told them. The likely next step is requesting a new code for a reset that already happened. `ForgotPasswordViewModel` (lines 89-94) has the same pattern: leaving during the hold drops the navigation, which is harmless there.
**Fix:** Block back while a submit is in progress or finished, or make the success outlive the hold:
```kotlin
// SetNewPasswordContent
BackHandler(enabled = state.saveState != SaveState.Idle) { /* swallow during submit/hold */ }
AuthTopAppBar(title = ..., onBack = { if (state.saveState == SaveState.Idle) onBack() })
```
Another option is to set `_passwordChanged` right after the API call succeeds and run the 600 ms hold in the screen's `LaunchedEffect` before calling `onPasswordChanged`.

## Info

### IN-01: Focus-on-error works but is untested; a request on a disabled field is silently dropped

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/setnewpassword/SetNewPasswordScreen.kt:113-121`; `components/ValidatedField.kt:143`; same pattern in `LoginScreen.kt:122-129` and `ForgotPasswordScreen.kt:103-109`
**Issue:** Attaching `focusRequester` to `ValidatedField`'s outer `Column` works: the request goes to the first child focus target, which is the `BasicTextField`. But no screen test asserts focus. `grep -n focus` finds nothing in `SetNewPasswordScreenTest`, `LoginScreenTest` or `ForgotPasswordScreenTest`. Also, if the field is `enabled = false` when the effect runs, `requestFocus()` does nothing and `onFocusRequestConsumed()` is still called, so the request is lost.
**Fix:** Add a Robolectric test per screen, for example render `SetNewPasswordContent(focusRequest = ResetField.Confirm)` and then `onNodeWithText("Confirm your new password").assertIsFocused()`. Call `onFocusRequestConsumed()` only after `requestFocus()` returns `true` (Compose 1.8+ returns Boolean).

### IN-02: AuthNavDisplayTest never goes through NavDisplay's back handler

**File:** `app/src/test/java/com/uptrendinvestments/clickmeapp/navigation/AuthNavDisplayTest.kt:74-77`
**Issue:** `backLikeNavDisplay()` calls `nav().handleBack { prefill = it }` directly. So the `onBack = { authNavigator.handleBack(onReturnToLogin) }` wiring in `AuthNavDisplay` and the system/predictive back path (WR-03) are not covered. The test's `onReturnToLogin` lambda is not the one being exercised.
**Fix:** Drive the real back path: `composeRule.runOnIdle { (composeRule.activity as ComponentActivity).onBackPressedDispatcher.onBackPressed() }`. This needs `createAndroidComposeRule<ComponentActivity>()`.

### IN-03: Realtime logging still ships in release builds

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeService.kt:136, 161, 198`
**Issue:** The release stub says "no realtime logging ships in release builds", but `RealtimeService` is in `main` and calls `Log.w`/`Log.d` unconditionally. The content is safe (event names and exception class names only), but it contradicts D-20 and the stub's KDoc.
**Fix:** Gate on `BuildConfig.DEBUG`, or send these through `RealtimeDebugLogger` too (add a `warn(tag, msg)` that is a no-op in the release stub).

### IN-04: Data classes holding passwords use the default `toString`

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/AuthRequests.kt:8-26`; `screens/login/LoginViewModel.kt:26-34`; `screens/login/setnewpassword/SetNewPasswordViewModel.kt:29-43`
**Issue:** `LoginRequest`, `ResetPasswordRequest`, `LoginUiState` and `SetNewPasswordUiState` print plaintext passwords from `toString()`. Elsewhere this phase redacts on purpose (`HandshakeFailure`, `MessageNew`). One future `Log.d("$state")` or crash-reporter breadcrumb would leak credentials.
**Fix:** Override `toString()` to redact `password`, `passwordConfirmation`, `newPassword` and `confirmPassword`, for example `"LoginRequest(email=$email, password=***)"`.

### IN-05: Forgot Password counts the bottom inset twice when the keyboard is open

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/forgetpassword/ForgotPasswordScreen.kt:116-123`
**Issue:** `.padding(innerPadding).imePadding()` does not consume `innerPadding`, so the IME padding adds the navigation-bar height again on top of the Scaffold's bottom inset. `SetNewPasswordScreen` (line 134-136) does this correctly with `consumeWindowInsets(innerPadding)`.
**Fix:** Change it to `.padding(innerPadding).consumeWindowInsets(innerPadding).imePadding()`.

### IN-06: Protocol tests depend on wall-clock timing

**File:** `app/src/test/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeServiceProtocolTest.kt:279-280, 305-306, 337-338`
**Issue:** These tests use `Thread.sleep(1_500)` / `Thread.sleep(500)` to prove that nothing happened, and they assert the reconnect delay falls in `150..1500` ms of real time. A loaded CI machine can push the delay outside the window or let a late upgrade arrive after the sleep, which makes the tests flaky.
**Fix:** Widen the windows a lot. Better, assert the configured delay through `IO.Options`/`Manager.reconnectionDelay()` and keep only a loose upper bound on real time.

### IN-07: The "Go to Login" button can be tapped while invisible

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/passwordchanged/PasswordChangedScreen.kt:135-144`
**Issue:** `slideFade` only sets `graphicsLayer.alpha`, so for the first ~700 ms the fully transparent button still receives taps and accessibility actions.
**Fix:** Pass `enabled` (or a `SaveState`) based on `entrance.buttonAlpha.value > 0.5f`, or move the button inside an `AnimatedVisibility`.

### IN-08: Manager collectors start dispatched, so early events can be missed

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeConnectionManager.kt:66-91`
**Issue:** `sessionEvents.events` and `connection.handshakeFailures` have no replay. `start()` launches their collectors with the default start mode, so they subscribe some time after `start()` returns. Anything emitted before that is lost. In practice the first `connect` happens later than that, so this is a latent problem rather than a live one. `RealtimeDebugLogger` handles the same situation with `CoroutineStart.UNDISPATCHED`.
**Fix:** Use `policyScope.launch(start = CoroutineStart.UNDISPATCHED) { ... }` for the two SharedFlow collectors.

### IN-09: FakeRealtimeConnection does not model the service contract

**File:** `app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeRealtimeConnection.kt:34-46`
**Issue:** The fake records every `connect(token)`, even where the real service would do nothing (same token while not `Disconnected`). `emitFailure` also leaves the state at `Connecting`, whereas the real service sets `Disconnected`. So manager tests such as `probeThatRefreshesReconnectsWithNewTokenOnly` count connects the real service would ignore. Their assertions describe calls, not the resulting socket behavior.
**Fix:** Mirror the contract in the fake: `if (token == active && state != Disconnected) return` in `connect`, and set `_state.value = Disconnected` in `emitFailure`. Assert on "effective" connects.

### IN-10: OTP code goes into saved state despite the "no OTP" claim

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/setnewpassword/SetNewPasswordViewModel.kt:58, 74`; `navigation/AuthKeys.kt:6-9`
**Issue:** `AuthKeys` says "No password or OTP is ever placed in a key". That is literally true, but the OTP is written to the same entry's `SavedStateHandle` and is persisted with the activity's saved-state bundle on process death. This was a deliberate choice (T-03-20), but the docs read as if the OTP never leaves memory.
**Fix:** Reword the `AuthKeys` KDoc to say the code is kept in entry saved state on purpose, or drop `KEY_CODE` (the code is short-lived and quick to re-enter).

---

_Reviewed: 2026-10-21T00:15:57Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
