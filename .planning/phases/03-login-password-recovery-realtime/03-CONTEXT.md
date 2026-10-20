# Phase 3: Login, Password Recovery & Realtime - Context

**Gathered:** 2026-10-20
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 3 delivers three things (AUTH-01, AUTH-02, RT-01, RT-02, RT-03):

1. **Real Login screen.** It replaces the temporary Phase 2 `screens/login/LoginScreen.kt`. It's a port of iOS `LoginView`: validation, forgot-password link, Sign Up footer, and routing after login by account state and mode.
2. **OTP password recovery.** Four pushed screens: Forgot Password (email) → Set New Password (6-digit code, new password and confirm) → Password Changed → back to Login. The user then logs in with the new password.
3. **App-scoped realtime client.** socket.io-client-java v2+ over websocket, authenticated with `auth: {token}`, reconnecting every 2s. It reconnects with the new token after a refresh and exposes the `message.*`, `typing.*`, `user.status`, `booking.update` and `call.ended` events as a `SharedFlow`.

Out of scope:
- The signup flow itself (Phase 4). Only a placeholder target for the Sign Up link is built here.
- Any chat or booking UI that consumes the events (Phases 8, 11, 12).
- FCM (Phase 13).

</domain>

<decisions>
## Implementation Decisions

Carried forward from Phase 2 (not re-opened):
- Routing after login goes through `UserManager.resolvePostAuthDestination` (Phase 2 D-05/D-07).
- The global `ACCOUNT_SUSPENDED` trap and dialog (D-08).
- Offline launch keeps the session (D-01).
- Refresh is reactive only (D-04). You're logged out only on a 401 from `/auth/refresh` or a missing refresh token (D-03).
- No offline indicator (D-06).

### Login screen
- **D-01:** **Field rules match iOS `LoginViewModel`.**
  - Email is trimmed and validated with the regex `^[^@\s]+@[^@\s]+\.[^@\s]+$`. Messages: "Email is required" / "Invalid email format".
  - Password messages: "Password is required" / "Password must be at least 8 characters".
- **D-02:** **Validation timing matches iOS.** No field errors show until the first submit. After that, fields re-validate live as the user types.
- **D-03:** **The submit button is always enabled.** Tapping it runs validation. It shows a spinner and is disabled only while the request is in flight. Reuse the existing `AsyncSaveButton`.
- **D-04:** **Error display deviates from iOS on purpose.**
  - **401 (wrong credentials):** show the server message **inline under the password field only**, with no dialog. iOS shows both inline and an alert.
  - **422 with `errors[].field == "email"`:** inline on email, no dialog (same as iOS).
  - **Everything else** (5xx, network, unknown): a Material `AlertDialog` with the server `message` (iOS title "Something went wrong").
- **D-05:** **The Sign Up footer renders as on iOS and goes to a placeholder.** The placeholder is a "coming soon" screen in the auth back stack. Phase 4 replaces it with `SignupInitialScreen`.
- **D-06:** **Keep the existing IME behavior:** Next moves from email to password, Done submits. Keep the existing password show/hide toggle too.
  - iOS has no special return-key handling. This is an Android convention and is acceptable.
- **D-07:** **The "Forgot Password?" link is right-aligned above the submit button, as on iOS.** It pushes the Forgot Password screen with the current email pre-filled.

### Password recovery flow
- **D-08:** **Screens and rules match iOS.**
  - **Forgot Password** (title "Reset Password"):
    - Email field (empty → "Please enter your email", plus the same regex).
    - Calls `POST /auth/password/forgot` with `{"identity": email}`.
    - Has a "Back to Login" button. Errors show in an AlertDialog, plus inline when the error field is `identity` or `email`.
  - **Set New Password** (title "Set New Password"):
    - A 6-digit code field (digits only, clipped to 6).
    - New password: at least 8 characters, at least one digit and at least one non-alphanumeric character. A password strength meter shows once the field isn't empty.
    - Confirm field ("Passwords do not match"). Each password field has its own eye toggle.
    - Calls `POST /auth/password/reset` with `{email, code, password, password_confirmation}`. These endpoints are public, with no auth header.
    - A 400 shows inline on the code: "Invalid or expired code. Please request a new one." A 422 maps errors to the `password`, `password_confirmation`, `code` and `email` fields.
- **D-09:** **No "Resend code" button and no cooldown (iOS parity).** If the code has expired, the user goes back to the email screen and sends it again.
- **D-10:** **Password Changed → "Go to login" clears the recovery screens and returns to Login with the reset email pre-filled and the password empty.** There's no auto-login (iOS parity). Pre-filling the email is a small deviation from iOS.
- **D-11:** **System Back on Password Changed works like the button** (back to Login, recovery screens cleared). The user can never go back into a used OTP form.

### Socket lifecycle
- **D-12:** **Connection config ports iOS `RealtimeService`.**
  - URL: the base URL with `/api/v1` removed.
  - Websocket transport only. Infinite reconnect attempts, 2s reconnect delay.
  - The token goes in the handshake **`auth: {token}`** (`IO.Options.auth`), **not** in query params.
  - `backend-reference/docs/SOCKET-CONTRACT.md` shows `.connectParams`, but it also says the server reads `handshake.auth.token`. iOS code and RT-01 win.
- **D-13:** **When the socket connects and disconnects:**
  - **Connect:** on session restore at launch, including the offline-launch path (Phase 2 D-01: use the stored token and let the 2s loop pick up when the network returns), on login, and on app foreground.
  - **Reconnect** with the current token after every `SessionEvents.Refreshed`. Connecting does nothing if the token hasn't changed and the socket is already connected (iOS parity).
  - **Disconnect:** on logout, on session rejected or suspended, and on app background.
- **D-14:** **Disconnect when the app goes to background, reconnect on foreground.** Use `ProcessLifecycleOwner` ON_STOP / ON_START. This matches what iOS does in practice (iOS suspends backgrounded apps) and saves battery. Background delivery is FCM's job in Phase 13.
  - **Note for Phase 9:** keep the socket alive while the call foreground service runs (for `call.ended`). The lifecycle hook should allow that later.
- **D-15:** **When the handshake fails with `INVALID_TOKEN`:**
  - Call the existing single-flight `TokenRefresher` over REST. When it succeeds, the `Refreshed` event reconnects the socket.
  - If the refresh gets a 401, follow the normal logout path (Phase 2 D-03).
  - If the refresh fails soft (network or 5xx), let the normal reconnect loop keep going.
  - Make sure this can't loop: at most one refresh attempt per access token.
  - `MISSING_TOKEN` means there's no session, so disconnect.
- **D-16:** **Expose connection state as a `StateFlow`** (connected / connecting / disconnected). Nothing in the UI uses it yet. It's for logs and for Phase 12 chat.

### Event surface & proof
- **D-17:** **Events are a sealed `RealtimeEvent` hierarchy** that ports iOS `RealtimeEvents.swift`. Moshi decodes the snake_case payloads and parses ISO-8601 dates.
  - `MessageNew {messageId, threadId, senderId, type, content?, timestamp, status}`
  - `MessageRead {threadId, messageIds, readBy}`
  - `UserStatus {userId, status: online|offline}`
  - `TypingStart` / `TypingStop {threadId, senderId}`
  - `BookingUpdate {bookingId, status, revieweeId?, actionsInvalidated?}`
  - `CallEnded {bookingId}`
  - Unknown or malformed payloads are logged and dropped. They must never crash the app or close the flow.
- **D-18:** **The SharedFlow is hot, with no replay:** `replay = 0`, `extraBufferCapacity ≈ 64`, `DROP_OLDEST`. Screens refetch when they appear, as on iOS.
- **D-19:** **The client→server emits are built now but nothing calls them yet.**
  - `typing.start` / `typing.stop` with `{thread_id, recipient_id}`.
  - `message.read` with `{thread_id, message_ids}`.
  - UUIDs are lowercased. Unit-test the payloads. Phase 12 only wires the UI.
  - There's no room join; the server joins the user's room automatically.
  - Sending a message stays a REST call.
- **D-20:** **Proof of success criterion 3 is a debug-only Logcat collector.**
  - It's app-scoped and logs every `RealtimeEvent` and every connection-state change under a `Realtime` tag.
  - The on-device check (trigger a message, typing and a booking update from iOS, then check Logcat; drop the network or force a refresh, then watch it reconnect within about 2s) is a **deferred human checkpoint**, following the project's manual-testing-deferred preference. It doesn't block the phase.

### Claude's Discretion
- **Auth flow navigation** for Login → Forgot → Set New Password → Password Changed, plus the Signup placeholder, under `AppRoute.Login`. Likely reuse the existing `navigation/` Navigator back-stack pattern so the Phase 4 signup screens slot in. A nested NavHost is acceptable if it fits better.
- Package layout for realtime. Suggestion: `realtime/RealtimeService.kt` and `realtime/RealtimeEvents.kt`, mirroring iOS `Realtime/`, provided as a Hilt singleton.
- Where lifecycle and session observation lives. For example, a `RealtimeConnectionManager` started from `Application` that collects `UserManager.isLoggedIn`, `SessionEvents` and `ProcessLifecycleOwner`.
- Socket.IO dependency version, and whether it shares the app's OkHttp client.
- Exact password strength meter visuals.
- File names for the recovery screens. Mirror iOS (`screens/login/forgetpassword/`, `screens/login/setnewpassword/`, `screens/login/passwordchanged/`).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Login (AUTH-01)
- `ios-reference/ClickMe2026/Views/Screens/Login Flow/Login/LoginView.swift`: layout, error display, post-login routing
- `ios-reference/ClickMe2026/Views/Screens/Login Flow/Login/LoginViewModel.swift`: validation rules and messages, 401/422 handling
- `ios-reference/ClickMe2026/Views/Screens/Login Flow/Login/Subviews/`: LoginButton, SignUpFooter
- `ios-reference/ClickMe2026/Views/Components/ValidatedField.swift`: field visuals and eye toggle

### Password recovery (AUTH-02)
- `ios-reference/ClickMe2026/Views/Screens/Login Flow/Forget Password/`: Forgot Password view and VM
- `ios-reference/ClickMe2026/Views/Screens/Login Flow/Set New Password/`: reset view and VM, password rules, PasswordStrengthMeter
- `ios-reference/ClickMe2026/Views/Screens/Login Flow/Password Changed/`: confirmation screen
- `ios-reference/ClickMe2026/Network/ClickMeAPI+Auth.swift`: forgot/reset request bodies
- `ios-reference/ClickMe2026/Network/APIRequestURLs.swift` (around lines 198–199): `auth/password/forgot`, `auth/password/reset`

### Realtime (RT-01..03)
- `ios-reference/ClickMe2026/Realtime/RealtimeService.swift`: config, handshake auth, handlers, emits
- `ios-reference/ClickMe2026/Realtime/RealtimeEvents.swift`: payload shapes
- `ios-reference/ClickMe2026/Managers/UserManager.swift` (connect points around lines 119, 174, 209, 466; disconnect around line 302)
- `backend-reference/docs/SOCKET-CONTRACT.md`: handshake, `MISSING_TOKEN`/`INVALID_TOKEN`, events, `booking.update` variants, presence, known gaps. Ignore its `.connectParams` example (see D-12).
- `backend-reference/docs/ios/SocketSmokeTest.swift`: a reference smoke test
- `backend-reference/docs/ios/CALL-END-SEMANTICS.md`: what `call.ended` means (context for Phase 9)

### Prior phase decisions
- `.planning/phases/02-networking-session-sdk-spikes/02-CONTEXT.md`: session, refresh, suspension and temporary-login decisions (D-01..D-09)
- `.planning/REQUIREMENTS.md`: AUTH-01, AUTH-02, RT-01, RT-02, RT-03

</canonical_refs>

<code_context>
## Existing Code Insights

Base: `app/src/main/java/com/uptrendinvestments/clickmeapp/`

### Reusable Assets
- `screens/login/LoginScreen.kt` + `LoginViewModel.kt`: the temporary login screen, to restyle and extend in place. It already has a trimmed email, a password visibility toggle, IME Next/Done, an `AsyncSaveButton` and a `LoginError` sealed type.
- `ValidatedField` and `AsyncSaveButton` (Phase 1 shared components). Use them for every recovery screen.
- `SuspendedAccountDialog.kt`: the global suspension dialog. It already covers login.
- `network/TokenRefresher.kt` (single-flight): reuse for the socket's `INVALID_TOKEN` handling (D-15).

### Established Patterns
- `managers/UserManager.kt`: StateFlows (`isLoggedIn`, `wasSuspended`, `currentMode`, `me`, …) plus `login`, `logout`, `restore`, `resolvePostAuthDestination(roles) → LaunchDestination`.
- `managers/SessionEvents.kt`: a SharedFlow of `Refreshed` / `Rejected` / `Suspended`. Nothing consumes `Refreshed` yet; this is the hook for RT-02.
- `network/ApiRequestUrls.kt` and `network/ClickMeApi.kt` (Retrofit + Moshi): add `auth/password/forgot` and `auth/password/reset` here as public endpoints with no auth header.
- Error mapping ported from `Error+UserMessage.swift` (Phase 2) handles the server `message` and 422 `errors[]`.

### Integration Points
- `AppRoute.kt` (enum: Splash, Login, SignupSetup, ClientHome, ExpertHome), `AppViewModel.kt` and `ClickMeApp.kt`: the app root switches on the route, with no NavHost. The auth flow's back stack lives under `AppRoute.Login`.
- `navigation/` (Navigator, NavigationState, HomeTabScaffold): the existing back-stack pattern.
- `gradle/libs.versions.toml`: has **no socket.io dependency yet**. Add `io.socket:socket.io-client` 2.x. OkHttp 5.5.0 is already there.
- `di/NetworkModule.kt` and `di/SessionModule.kt`: Hilt wiring for the realtime singleton.

</code_context>

<specifics>
## Specific Ideas

- Error copy follows iOS strings exactly (listed in D-01 and D-08).
- On iOS, every recovery screen is pushed onto the login NavigationStack, not shown as a sheet. Android pushes them the same way and doesn't use ModalBottomSheet.
- The SOCKET-CONTRACT/iOS conflict over how the token is passed is settled: use `auth: {token}`.

</specifics>

<deferred>
## Deferred Ideas

- **Resend code with a 30s cooldown on Set New Password.** Rejected for parity. Revisit in Polish (Phase 15) only if users ask.
- **Keeping the socket alive during a call.** Phase 9 decides this when the call foreground service exists (see D-14).
- **A UI that shows connection state.** Not planned (Phase 2 D-06). The StateFlow is ready if Polish wants it.

</deferred>

---

*Phase: 03-login-password-recovery-realtime*
*Context gathered: 2026-10-20*
