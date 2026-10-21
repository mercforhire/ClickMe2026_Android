---
phase: 03-login-password-recovery-realtime
verified: 2026-10-20T00:00:00Z
status: human_needed
score: 4/4 roadmap success criteria verified in code (device proof deferred); 5/5 requirements satisfied in code
overrides_applied: 0
re_verification: false
gaps: []
human_verification:
  - test: "SC-1 Login on device: wrong password, correct account routing, offline"
    expected: "Wrong password shows the backend message inline under the password field only. A correct login lands in the right destination for the account state and mode. Offline shows the 'Something went wrong' dialog."
    why_human: "Needs a real backend account and device. Login rate limit is 10 per 15 min per IP."
  - test: "SC-2 Recovery loop with a real OTP email"
    expected: "Send Reset Code, then 'Code Sent!' for about 600 ms, then Set New Password. A valid code and password gives 'Password Updated!', then Password Changed, then Go to Login with the email pre-filled, then a login with the new password succeeds. System Back on Password Changed lands on Login. SMS/email OTP autofill works. Focus moves to the first failing field."
    why_human: "Needs a real OTP email. FocusRequester on the ValidatedField outer Column and OTP autofill are untested by any automated test (IN-01)."
  - test: "SC-3 Realtime events from iOS (adb logcat -s Realtime)"
    expected: "After login, `state=Connecting` then `state=Connected`. A message, typing and a booking update triggered from the iOS app appear as event lines (ids only, no content, no token)."
    why_human: "Needs a live socket against the production backend and a second signed-in iOS account."
  - test: "SC-4 Reconnect within about 2 s after airplane mode and after a forced token refresh"
    expected: "State goes Connecting then Connected within about 2 s of the network returning. After a forced refresh the socket reconnects with the new token. Events keep arriving."
    why_human: "Real network drop and real token rotation. The graceful server close frame may only be noticed after the roughly 45 s ping timeout (03-08 observation)."
  - test: "Layout and lifecycle on device"
    expected: "Rotation keeps state on the five auth screens. 200% font scale is usable. Backgrounding the app disconnects the socket and foregrounding reconnects it. Signed prodRelease cold start with R8 works (needs the Phase 2 keystore)."
    why_human: "Visual and process-lifecycle behavior. Already logged in STATE.md as [Phase 3] DEFERRED (human), non-blocking by user preference."
---

# Phase 3: Login, Password Recovery & Realtime Verification Report

**Phase Goal:** Users can log in and recover their password on the real screens, and the app keeps a live, authenticated realtime connection.
**Verified:** 2026-10-20
**Status:** human_needed
**Re-verification:** No, initial verification

All automated must-haves pass against the code. No real code gap was found. What remains is device proof, which the user deferred on purpose. It is logged in STATE.md as `[Phase 3] DEFERRED (human)`.

## Goal Achievement

### Observable Truths (ROADMAP success criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Real Login screen replaces the temporary one; wrong credentials show the backend's message | VERIFIED (code) | `LoginViewModel.kt` (165 lines) calls `userManager.login(email.trim(), password)` then `resolvePostAuthDestination`. A 401 puts `userMessage` inline under the password. A 422 on `email` shows inline. Everything else opens `AuthErrorDialog`. `ACCOUNT_SUSPENDED` is left to the global dialog. `LoginScreen.kt` (256 lines) has `LoginContent` with the Forgot link and Sign Up footer. `AuthNavHost` wires it as `entry<LoginKey>`, and `ClickMeApp.kt:35` routes `AppRoute.Login -> AuthNavHost`. `grep -i temporary` finds nothing left in screens or navigation. |
| 2 | Forgot, OTP, set new password, Password Changed, login with the new password | VERIFIED (code) | `ForgotPasswordViewModel.send()` posts `{identity}` via `api.forgotPassword`. It holds `Saved` for 600 ms, then sets `navigateToReset`. `SetNewPasswordViewModel.submit()` posts `{email, code, password, password_confirmation}`. A 400 gives the code-expired inline message. A 422 maps to code, password, confirm and email. It holds 600 ms, then sets `passwordChanged`. `AuthNavHost` chains Forgot, SetNewPassword, PasswordChanged. "Go to Login" calls `returnToLogin()` and pre-fills the email. `ApiRequestUrls` has `FORGOT_PASSWORD` and `RESET_PASSWORD`, both in `publicPaths` (no bearer, tested in `ClickMeApiTest` and `NetworkStackTest`). The final login uses the same verified Login path as truth 1. |
| 3 | After login the socket connects; iOS events appear in `SharedFlow` logs | VERIFIED (code), device proof deferred | `RealtimeConnectionManager` combines `isLoggedIn && isForeground`. When true it reads the stored token and calls `connection.connect(token)`. `ClickMeApplication.onCreate` calls `manager.start()`, and `AndroidManifest` registers `.ClickMeApplication`. `RealtimeService` builds a `forceNew`, websocket-only socket with `auth = {token}` and the bare OkHttp client. It registers listeners for all 7 inbound events, and `RealtimeEventDecoder` turns them into `RealtimeEvent`, emitted on `events: SharedFlow`. Event names match the backend `socket.js` (typing.*, message.read, user.status). `RealtimeDebugLogger` (debug source set) logs state, events and failures under tag `Realtime`. The release source set has a no-op stub. |
| 4 | After a token refresh or network drop, the socket reconnects within about 2 s with the current token | VERIFIED (code), device proof deferred | Network drop: `reconnection = true`, `reconnectionAttempts = Int.MAX_VALUE`, and min and max delay both set to 2000 ms (exactly 2 s, no jitter). Token refresh: `TokenAuthenticator:69` emits `SessionEvent.Refreshed` after `tokens.save`. The manager collects it and calls `connectWithStoredToken()`, so the new token is used. `RealtimeService.connect` tears down the old socket and builds a new one for a changed token. A rejected token (`INVALID_TOKEN`) goes through one `/me` probe per token and then a capped retry. Spot-checks below confirm this. |

**Score:** 4/4 truths verified in code. Truths 1-4 still need device confirmation (human_verification).

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `screens/login/LoginScreen.kt`, `LoginViewModel.kt` | Real login | VERIFIED | Substantive, wired through `AuthNavHost`, and data flows through `UserManager.login`. |
| `screens/login/forgetpassword/*` | Forgot screen and VM | VERIFIED | Wired in `AuthNavHost`; calls `ClickMeApi.forgotPassword`. |
| `screens/login/setnewpassword/*` | Reset screen and VM | VERIFIED | Wired; calls `ClickMeApi.resetPassword`; has strength meter and `trailingBadge`. |
| `screens/login/passwordchanged/PasswordChangedScreen.kt` | Confirmation screen | VERIFIED | Wired in `AuthNavHost`. |
| `navigation/AuthNavHost.kt`, `AuthNavigator.kt`, `AuthKeys.kt` | Auth back stack under `AppRoute.Login` | VERIFIED | Used by `ClickMeApp.kt`. |
| `network/ClickMeApi.kt`, `ApiRequestUrls.kt`, `AuthRequests.kt` | Public recovery endpoints and DTOs | VERIFIED | `@POST` constants, `password_confirmation` mapped, paths in `publicPaths`. |
| `realtime/RealtimeService.kt` (209) | socket.io connection | VERIFIED | forceNew, websocket, `auth {token}`, 2 s reconnect, 7 listeners, catch-all guards. |
| `realtime/RealtimeConnectionManager.kt` (177) | Login, foreground and refresh policy | VERIFIED | Started from `ClickMeApplication`. |
| `realtime/RealtimeEvents.kt`, `RealtimeEventDecoder.kt`, `RealtimeEmits.kt`, `RealtimeConnection.kt`, `RealtimeUrl.kt` | Sealed events, decoder, emits, seam, URL | VERIFIED | Decoder never throws (unknown or malformed gives null). `socketUrl` strips `/api/v1`. |
| `realtime/ProcessForeground.kt` | Foreground seam | VERIFIED | `ProcessLifecycleOwner` STARTED state. |
| `di/RealtimeModule.kt` | Hilt bindings | VERIFIED | Binds `RealtimeConnection` and `ProcessForeground`, provides `RealtimeConfig(socketUrl(AppConfig.baseUrl))`. |
| `app/src/debug/.../RealtimeDebugLogger.kt` and release stub | Debug-only Logcat collector | VERIFIED | Collectors start UNDISPATCHED. Redacting `toString` (ids only). |
| `gradle/libs.versions.toml`, `app/build.gradle.kts`, `keepRules/rules.keep` | Dependencies and R8 rules | VERIFIED | `socket.io-client` (org.json excluded), `lifecycle-process`, `-keep class io.socket.**`. |

### Key Link Verification

| From | To | Via | Status |
|------|----|-----|--------|
| `ClickMeApp` | `AuthNavHost` | `AppRoute.Login ->` | WIRED |
| `LoginViewModel.submit` | `UserManager.login` and `resolvePostAuthDestination` | `viewModelScope.launch` | WIRED |
| `ForgotPasswordViewModel.send` | `ClickMeApi.forgotPassword` | `apiCall` | WIRED |
| `SetNewPasswordViewModel.submit` | `ClickMeApi.resetPassword` | `apiCall` | WIRED |
| `forgotPassword`/`resetPassword` | `publicPaths` (no bearer) | `AuthHeaderInterceptor.needAuthToken` | WIRED |
| `ClickMeApplication.onCreate` | `RealtimeConnectionManager.start()` and `RealtimeDebugLogger.install` | Hilt field injection | WIRED |
| `UserManager.isLoggedIn` and `ProcessForeground` | `connection.connect(token)` | `combine ... collectLatest` | WIRED |
| `TokenAuthenticator` refresh success | `SessionEvent.Refreshed` | manager collector | WIRED |
| `RealtimeService` listener | `events: SharedFlow` | `decoder.decode` then `_events.tryEmit` | WIRED |
| `RealtimeModule` | bare `OkHttpClient` | `@Named("bare")` in `RealtimeService` | WIRED |

### Data-Flow Trace (Level 4)

| Artifact | Data variable | Source | Real data | Status |
|----------|---------------|--------|-----------|--------|
| `LoginContent` | `LoginUiState` errors and `destination` | `UserManager.login` (Retrofit to backend) | Yes, server message mapped from `ApiException` | FLOWING |
| `ForgotPasswordContent` and `SetNewPasswordContent` | `saveState`, errors | Retrofit `forgotPassword`/`resetPassword` | Yes | FLOWING |
| `RealtimeDebugLogger` | `events`, `state` | `RealtimeService` listeners | Yes (decoded socket frames) | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Phase 3 unit tests (realtime, login screens, auth nav) | `./gradlew --offline :app:testDevDebugUnitTest --tests '*realtime*' --tests '*screens.login*' --tests '*navigation.Auth*'` | Exit 0; 178 tests, 0 failures, 0 errors, 0 skipped, summed from the result XML | PASS |
| Socket protocol against MockWebServer | `RealtimeServiceProtocolTest` (in the run above) | Covers token in the CONNECT packet only (not URL or header), websocket-only, reconnect after drop with the configured delay, new-token rebuild, `INVALID_TOKEN`/`MISSING_TOKEN`/suspended classification, typed event delivery, throwing decoder never crashes | PASS |
| Manager policy | `RealtimeConnectionManagerTest` (in the run above) | Covers login, foreground, logout, `Refreshed` reconnects with the rotated token, a refresh in the background does not connect, one probe per token, capped backoff | PASS |
| Orchestrator gate (not re-run) | `testDevDebugUnitTest ktlintCheck lintDevDebug assembleDevDebug assembleProdRelease` + `check-design-rules.sh` | Reported green by the orchestrator; 421 tests per flavor per 03-12 | Accepted as reported |

### Probe Execution

Step 7c: SKIPPED. No `probe-*.sh` files are declared or exist for this phase.

### Requirements Coverage

| Requirement | Source plans | Description | Status | Evidence |
|-------------|--------------|-------------|--------|----------|
| AUTH-01 | 03-02, 03-04, 03-07, 03-10 | Log in with email and password, land in the right destination | SATISFIED (device check deferred) | `LoginViewModel`, `LoginScreen`, `AuthNavHost`, `resolvePostAuthDestination`. |
| AUTH-02 | 03-01, 03-02, 03-05, 03-06, 03-07, 03-10 | OTP request, verify, new password, confirmation, log in with the new password | SATISFIED (device check deferred) | Forgot, SetNewPassword and PasswordChanged screens, VMs, endpoints, auth back stack. |
| RT-01 | 03-03, 03-08, 03-09, 03-11 | socket.io (websocket) with JWT in `auth: {token}`, reconnect every 2 s | SATISFIED (device check deferred) | `RealtimeService.connect` options. 2000 ms min and max. |
| RT-02 | 03-09, 03-11 | Reconnect with the new token after each refresh | SATISFIED (device check deferred) | `TokenAuthenticator` emits `Refreshed`; the manager reconnects with the stored token. |
| RT-03 | 03-03, 03-08, 03-11 | `message.*`, `typing.*`, `user.status`, `booking.update`, `call.ended` exposed as `SharedFlow` | SATISFIED | `RealtimeEventNames.INBOUND` (7 events), sealed `RealtimeEvent`, `events: SharedFlow`. |

All five IDs in the phase requirement list appear in the PLAN frontmatter and are marked Complete in REQUIREMENTS.md. No orphaned requirement IDs map to Phase 3.

### Code Review Warnings (03-REVIEW.md) against must-haves

None of the four defeats a success criterion or requirement. All are real and worth fixing before Phase 13.

| ID | Defeats a must-have? | Assessment |
|----|----------------------|------------|
| WR-01 stale-socket check-then-act race in `RealtimeService` | No | It needs an interleaving of an old socket's callback with a new `connect()`. The consequence is a transient wrong `state` value or a needless probe or retry. It does not break connect, the `SharedFlow`, or the 2 s reconnect (SC-3, SC-4, RT-01, RT-03). The review's lock-around-guard fix is small. |
| WR-02 socket can connect while backgrounded (`isDesired()` not re-checked after the `io` hop) | No | It touches the D-14 foreground policy, which is not a ROADMAP criterion or requirement. It is a narrow race (a refresh, a retry or a stale failure landing at the instant of backgrounding). The login-driven connect, the `Refreshed` reconnect and the 2 s reconnect in the foreground all work. `refreshedInBackgroundDoesNotConnect` passes for the non-racy case. Recommend the one-line re-check. |
| WR-03 predictive back on Password Changed previews Set New Password | No | Cosmetic. The commit still lands on Login with the email pre-filled (D-10, D-11). The leftover cost is that recovery ViewModels stay alive while Password Changed is shown. |
| WR-04 back during Saving or the 600 ms hold drops the success navigation | No | AUTH-02's happy path works. The edge case is the user pressing Back in a 600 ms window after the server has already accepted the reset. They land on Forgot Password with a changed password, which is confusing but not a failure of the loop. Recommend blocking back while `saveState != Idle`. |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| (phase files) | - | `TBD`/`FIXME`/`XXX`/`TODO`/`HACK` grep across `realtime`, `screens/login`, `navigation/Auth*`, `di/RealtimeModule.kt` and the debug and release source sets | None found | Debt-marker gate clear. |
| `RealtimeService.kt` | 136, 161, 198 | `Log.w`/`Log.d` in `main` (IN-03) | Info | Event names and exception class names only. No token or content. Conflicts with the stub's "no realtime logging in release" KDoc. |
| `AuthRequests.kt`, `LoginViewModel.kt`, `SetNewPasswordViewModel.kt` | - | Default `toString` prints passwords (IN-04) | Info | Latent leak risk only. Nothing logs them today. |
| `SignUpPlaceholderScreen.kt` | - | Sign Up "coming soon" placeholder | Info | Intentional per D-05. Phase 4 replaces it. Not in the Phase 3 goal. |

### Human Verification Required

See the `human_verification` frontmatter: SC-1 login on device, SC-2 recovery with a real OTP email, SC-3 iOS events via `adb logcat -s Realtime`, SC-4 reconnect after airplane mode and a forced refresh, and layout and lifecycle checks. These are logged in STATE.md as `[Phase 3] DEFERRED (human)` and are non-blocking by user preference. Phase 13 internal testing re-runs them.

### Gaps Summary

There are no code gaps. Every success criterion has a complete, wired implementation with passing automated tests (178 Phase 3 tests re-run here, 0 failures). The status is `human_needed`, not `passed`, because SC-3 and SC-4 are claims about live behavior against the production backend, and SC-1 and SC-2 need a real account and OTP email. Those can only be confirmed on a device. One risk to watch when the device checks run: the review notes that a graceful server close may only be detected after the roughly 45 s ping timeout, so SC-4's "about 2 s" may need a ConnectivityManager nudge if it shows up in practice.

---

_Verified: 2026-10-20_
_Verifier: Claude (gsd-verifier)_
