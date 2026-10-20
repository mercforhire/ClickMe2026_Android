# Phase 3: Login, Password Recovery & Realtime - Research

**Researched:** 2026-10-20
**Domain:** Android auth UI (Compose + Navigation 3 + Hilt), OTP password recovery over REST, socket.io-client-java 2.1.2 realtime client lifecycle
**Confidence:** HIGH for the realtime stack (verified by running experiments, see "Verified Experiments"); MEDIUM for UI wiring (follows an approved UI-SPEC, entry-scoped Hilt ViewModels not yet exercised on a device)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

Carried forward from Phase 2 (not re-opened):
- Routing after login goes through `UserManager.resolvePostAuthDestination` (Phase 2 D-05/D-07).
- The global `ACCOUNT_SUSPENDED` trap and dialog (D-08).
- Offline launch keeps the session (D-01).
- Refresh is reactive only (D-04). You're logged out only on a 401 from `/auth/refresh` or a missing refresh token (D-03).
- No offline indicator (D-06).

#### Login screen
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

#### Password recovery flow
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

#### Socket lifecycle
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

#### Event surface & proof
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

### Deferred Ideas (OUT OF SCOPE)
- **Resend code with a 30s cooldown on Set New Password.** Rejected for parity. Revisit in Polish (Phase 15) only if users ask.
- **Keeping the socket alive during a call.** Phase 9 decides this when the call foreground service exists (see D-14).
- **A UI that shows connection state.** Not planned (Phase 2 D-06). The StateFlow is ready if Polish wants it.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| AUTH-01 | Log in with email/password; land in the right destination for account state and mode | Existing `UserManager.login` + `resolvePostAuthDestination` is reused unchanged. Validators, error mapping, dialog/inline split and the auth nav host are specified in "Architecture Patterns" 1-3 and "Code Examples". |
| AUTH-02 | Forgot password: OTP request, verify, new password, confirmation, then login | Two public Retrofit endpoints + DTOs, backend behavior verified in `auth.controller.js` (always-200 forgot, 400 `EXPIRED_TOKEN` reset, 422 field errors, rate limits). See "Password recovery contract" and Pitfalls 9-10. |
| RT-01 | socket.io connection (websocket, JWT in `auth: {token}`), reconnect every 2s | `io.socket:socket.io-client:2.1.2` verified end to end against a Socket.IO 4.8.3 server and the production backend (wss). Reconnect delay measured at 2.00-2.01s. See "Verified Experiments". |
| RT-02 | After each token refresh, reconnect with the new token | `SessionEvent.Refreshed` -> rebuild the socket with a fresh `auth` map. A back-to-back `disconnect()`/`connect()` on the SAME socket is broken (Pitfall 2), so the rule is "new Socket per connection". |
| RT-03 | `message.*`, `typing.*`, `user.status`, `booking.update`, `call.ended` as a `SharedFlow` | Sealed `RealtimeEvent`, Moshi decoder on `String` JSON, `tryEmit` into a `replay=0, buffer=64, DROP_OLDEST` flow. Listener exceptions would crash the app (Pitfall 1), so every handler is wrapped. |
</phase_requirements>

## Summary

The realtime half of this phase is lower risk than expected, because every question about the library was settled by running it. `socket.io-client` 2.1.2 (released 2025-02-18, latest on Maven Central) compiles unchanged against OkHttp 5.5.0 and connected to both a local Socket.IO 4.8.3 server and the production backend over wss, sharing a plain OkHttp 5.5.0 client for `callFactory` and `webSocketFactory`. The handshake token travels in the Socket.IO CONNECT packet (`40{"token":"..."}`), which is what `IO.Options.auth` produces. A rejected handshake surfaces as `connect_error` with a `JSONObject` payload `{"message":"INVALID_TOKEN"}` (or `MISSING_TOKEN`), whereas transport failures surface as an `EngineIOException`.

Three library behaviors shape the design and are not in any doc the planner has: (1) after a middleware rejection (INVALID_TOKEN) the Java client **stops reconnecting** (it destroys the socket), so CONTEXT D-15's "let the normal reconnect loop keep going" does not hold and the app must schedule its own retry; (2) the `auth` map is captured when the socket is created, so a new token needs a **new Socket** (`forceNew`), and `disconnect()` immediately followed by `connect()` on the same Socket loses the connection; (3) all listeners run on one static `EventThread`, and **an exception thrown from a listener is rethrown on that thread and would crash the app**, so every handler needs a catch-all.

The Login and recovery UI is a straightforward port. The main structural decisions: reuse `Navigator`/`NavigationState` with a single top-level `LoginKey` for the auth back stack (push via `navigate`, "clear to Login" via `reselect`); add two public endpoints; drive the 600ms "Saved" hold from the ViewModel. One discrepancy to flag for the planner: CONTEXT D-15 says to call "the existing single-flight `TokenRefresher`", but `TokenRefresher` is only the HTTP call. The single-flight guard (a `Mutex` plus the "token already rotated" check) lives in `TokenAuthenticator`. Calling `TokenRefresher` directly from the socket code would bypass it and risk the refresh-token-reuse revocation. The safe way to honor D-15 is to make one authenticated REST call (`GET /me`) and let the Authenticator refresh.

**Primary recommendation:** Add `io.socket:socket.io-client:2.1.2` (exclude `org.json`) and `androidx.lifecycle:lifecycle-process:2.11.0`; build `RealtimeService` as "one fresh `forceNew` Socket per connection, bound with catch-all handlers, sharing the `@Named("bare")` OkHttp client", and drive it from an app-scoped `RealtimeConnectionManager` that combines `isLoggedIn`, process foreground and `SessionEvent.Refreshed`, and handles handshake rejections with a single REST probe per token plus its own 2s retry.

## Project Constraints (from CLAUDE.md)

- Jetpack Compose + Material 3, MVVM with Compose State, Kotlin coroutines (no RxJava), Hilt for DI, Retrofit + Moshi, `EncryptedSharedPreferences` for JWTs. minSdk 26, targetSdk 37, compileSdk 37.
- iOS code under `ios-reference/` is the source of truth for behavior; deviate only for Android conventions: `.sheet` to `ModalBottomSheet` (not used here, recovery screens are pushed), iOS back chevron to `TopAppBar` back arrow, `.alert` to Material `AlertDialog`, iOS switch to Material `Switch`, `.task` to `LaunchedEffect(Unit)`.
- Mirror the iOS folder tree under `app/src/main/java/com/uptrendinvestments/clickmeapp/` (screen + ViewModel side by side).
- Backend contract is shared with iOS; `backend-reference/docs/` is authoritative.
- Project guard rails seen in CI (`scripts/check-design-rules.sh`): no `Color(0x...)` literals outside `ui/theme/Color.kt`, no `*Brand` types, no Stripe secret keys. ktlint is enforced (`max_line_length = 120`, `ktlint_official`). Lint `abortOnError = true`.
- Moshi is reflection-free: every DTO needs `@JsonClass(generateAdapter = true)` and `@Json(name = "...")` on every multi-word field (R8-safe; see `MoshiProvider`).
- Memory: manual device testing is deferred (log human checkpoints as open items, do not block); JDK for Gradle is the Android Studio JBR.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Credential and OTP form validation, field-timing rules | Client (ViewModel) | API (server revalidates) | iOS validates client-side first; backend is the authority and its 400/422 field errors are mapped back inline. |
| Login, forgot, reset requests | API / Backend (public `/auth/*`) | Client (Retrofit + `apiCall`) | Server owns OTP issue/consume, rate limiting (5 and 10 per 15 min per IP), enumeration safety. |
| Session state (tokens, isLoggedIn) | Client (`UserManager`, `SecureTokenStore`) | — | Already built in Phase 2; unchanged. |
| Token refresh for the socket | Client (OkHttp `TokenAuthenticator`) | API (`/auth/refresh`) | Single-flight lives in the Authenticator. The socket layer must go through it. |
| Realtime transport, auth, rooms, presence | API / Backend (Socket.IO 4.8.3) | Client (socket.io-client-java) | Server verifies the JWT on every handshake and auto-joins the user room. Client never joins rooms. |
| Realtime connection policy (login, foreground, refresh) | Client (app-scoped manager) | — | Pure client lifecycle: combines `isLoggedIn`, `ProcessLifecycleOwner`, `SessionEvents`. |
| Event fan-out to screens | Client (`SharedFlow` in `RealtimeService`) | — | Hot, no replay; consumers are Phase 8/11/12 ViewModels. |
| Auth screen navigation | Client (Navigation 3 back stack under `AppRoute.Login`) | — | Root route stays app state; the auth stack is a local `NavDisplay`. |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `io.socket:socket.io-client` | 2.1.2 (Maven Central, released 2025-02-18, latest) | Socket.IO v3/v4 client for Java/Android | Official client from the `socketio` GitHub org; the only maintained JVM client, named in REQUIREMENTS RT-01. Client 2.x targets Socket.IO server 3.x/4.x [CITED: github.com/socketio/socket.io-client-java]. Server is 4.8.3 [VERIFIED: backend-reference/package.json]. |
| `androidx.lifecycle:lifecycle-process` | 2.11.0 (stable; matches the catalog's `lifecycle = "2.11.0"`) | `ProcessLifecycleOwner` for foreground/background | Already present transitively in the Gradle cache (2.11.0 .aar), but NOT declared in `libs.versions.toml`; declare it explicitly. [VERIFIED: dl.google.com maven group-index lists 2.11.0] |

Transitive: `engine.io-client:2.1.0` pulls `okhttp:3.12.12`; Gradle conflict resolution lifts it to the app's `okhttp:5.5.0` (see Assumption A5 for the one thing to confirm on first build). `org.json:json:20090211` is excluded (Android provides `org.json`) [CITED: socketio.github.io/socket.io-client-java/installation.html].

### Supporting (already in the project, reused)
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| OkHttp | 5.5.0 | Shared HTTP/WebSocket stack | Pass the `@Named("bare")` client as `callFactory` and `webSocketFactory`. |
| Moshi (codegen) | 1.15.2 | Event decoding, request DTOs | Same reflection-free setup as Phase 2. |
| kotlinx-coroutines | 1.11.0 | `SharedFlow`/`StateFlow`, manager loops | `tryEmit` from the EventThread. |
| mockwebserver3 | 5.5.0 (test) | Public-endpoint body tests; Engine.IO/Socket.IO protocol test | Already a test dependency. |
| Robolectric | 4.17 (test, sdk=35) | Runs socket.io tests with a real `org.json` | Needed because `org.json` is excluded and `android.jar` stubs throw "not mocked" in plain JVM tests. |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| socket.io-client-java | A raw OkHttp WebSocket speaking Engine.IO/Socket.IO frames | The contract says raw WebSocket "WILL NOT WORK" without the framing; hand-rolling heartbeats/packets is exactly what the library does. Locked by RT-01 anyway. |
| New Socket per connection | Reuse one Socket and mutate the `auth` map | Mutating works only if there is a gap between `disconnect()` and `connect()`; back-to-back it fails (Pitfall 2). Rebuilding is what iOS does and is robust. |
| `GET /me` probe for INVALID_TOKEN | Call `TokenRefresher.refresh` directly | Bypasses the Authenticator's `Mutex`/rotated-token check; a concurrent REST 401 would reuse the same refresh token and the backend revokes the session on reuse. |
| Shared authed `apiClient` for the socket | `@Named("bare")` client | The authed client would attach `Authorization: Bearer` to the websocket upgrade, run the Authenticator and suspension interceptor on the upgrade request. The bare client has none of those and is already a singleton. |

**Installation (`gradle/libs.versions.toml` + `app/build.gradle.kts`):**
```toml
[versions]
socketio = "2.1.2"

[libraries]
socket-io-client = { group = "io.socket", name = "socket.io-client", version.ref = "socketio" }
androidx-lifecycle-process = { group = "androidx.lifecycle", name = "lifecycle-process", version.ref = "lifecycle" }
```
```kotlin
implementation(libs.socket.io.client) {
    // Android provides org.json; the 2009 jar would duplicate it.
    exclude(group = "org.json", module = "json")
}
implementation(libs.androidx.lifecycle.process)
```

**Version verification:** `https://repo1.maven.org/maven2/io/socket/socket.io-client/maven-metadata.xml` lists `<release>2.1.2</release>`, last updated 2025-02-18. `engine.io-client` latest is 2.1.0. `lifecycle-process` 2.11.0 appears in Google Maven's group index and in `~/.gradle/caches`.

## Package Legitimacy Audit

| Package | Registry | Age | Downloads | Source Repo | slopcheck | Disposition |
|---------|----------|-----|-----------|-------------|-----------|-------------|
| `io.socket:socket.io-client` 2.1.2 | Maven Central | Multi-year (0.6.x through 2.1.2; 2.0.0 shipped Dec 2020) | Not measured (Maven Central does not expose counts) | github.com/socketio/socket.io-client-java (official `socketio` org) | n/a (slopcheck does not cover Maven coordinates) | Approved. Coordinate is printed on the official installation page `[CITED: socketio.github.io/socket.io-client-java/installation.html]` and the library is named by the user in REQUIREMENTS RT-01 and CONTEXT D-12, so no human-verify gate is needed. |
| `io.socket:engine.io-client` 2.1.0 (transitive) | Maven Central | Same project | — | github.com/socketio/engine.io-client-java | n/a | Approved (transitive; sources downloaded, compiled and run during research). |
| `androidx.lifecycle:lifecycle-process` 2.11.0 | Google Maven | AndroidX, 2.0.0 in 2018 | — | android.googlesource.com (AndroidX) | n/a | Approved (first-party AndroidX; already resolved transitively in this project). |

**Packages removed due to slopcheck [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none
*slopcheck was installed (PyPI) but it scans PyPI/npm-style names and has no Maven mode, so legitimacy was established through official documentation, Maven Central metadata and by compiling and running the artifacts. No postinstall concept exists for Maven artifacts.*

## Verified Experiments

These were run in this session (scratch directories under `/tmp`, nothing in the repo was changed). They are the evidence behind the "HIGH" ratings.

| # | Experiment | Result |
|---|------------|--------|
| E1 | `javac` engine.io-client 2.1.0 sources against `okhttp-jvm:5.5.0` + `okio:3.18.1` | Compiles. `MediaType.parse`, `RequestBody.create(MediaType,String)`, `Response.code()/body()`, `WebSocket.Factory`, `Call.Factory` all exist in 5.5.0 (`javap`). OkHttp 5.5.0 requires okio 3.18.1; with okio 3.16.0 the run fails with `NoSuchMethodError ByteString.base64$default`, so never pin okio lower than OkHttp wants. |
| E2 | Local Socket.IO **4.8.3** server (middleware copied from `backend-reference/src/services/socket.js`), Java client with `transports=["websocket"]`, `auth`, shared `OkHttpClient` 5.5.0 | Handshake log shows `auth={"token":"good-1"}` and `query={"EIO":"4","transport":"websocket"}`: token is in `handshake.auth`, no polling request. Events arrive as `JSONObject` args on thread `EventThread`. |
| E3 | Missing / bad token | `connect_error` arg is `org.json.JSONObject` `{"message":"MISSING_TOKEN"}` / `{"message":"INVALID_TOKEN"}`. **No further handshake attempts in 7 s**: the client does not retry after a middleware rejection. |
| E4 | Transport failure (server killed 6 s, restarted) | `disconnect` reason `transport error`, then `reconnect_attempt` at +2.006 s, +2.002 s, +2.005 s, +2.006 s; `connect_error` arg is `EngineIOException: websocket error`; reconnect succeeded with the same token, events flowed. With `reconnectionDelay == reconnectionDelayMax == 2000`, `Backoff` clamps jitter away, so the delay is exactly 2000 ms. |
| E5 | Token swap on the same Socket: `auth.put(token); disconnect(); connect()` back to back | **Failed**: client logged `io client disconnect` then `forced close` and never reconnected (server never saw the new token). The same sequence 1.5 s later worked. Cause: the old engine's late `close` event hits the re-subscribed Manager. |
| E6 | Rebuild: `old.off(); old.disconnect(); new = IO.socket(forceNew)` x3 back to back | Only the last token (`good-4`) reached the server, no cross-talk. This is the robust pattern. |
| E7 | Production `https://clickme-backend-production.up.railway.app` (also with trailing `/`) | wss + websocket-only + shared OkHttp 5.5.0 works. No token gives `MISSING_TOKEN`; garbage token gives `INVALID_TOKEN` (3 unauthenticated handshake attempts, no data touched). |
| E8 | MockWebServer 5.5.0 `webSocketUpgrade` speaking Engine.IO v4 | Client sends exactly `40{"token":"good-1"}`; server frames `0{"sid":..,"pingInterval":25000,"pingTimeout":20000}`, `40{"sid":"x"}`, `42["typing.start",{...}]` yield `connect` and the event. `44{"message":"INVALID_TOKEN"}` is the reject frame. The server-side `WebSocket` must be closed before `MockWebServer.close()` or it blocks ("Gave up waiting for queue to shut down"). |

Also confirmed from source: `RealConnection.useAsSocket()` sets `soTimeout = 0` for upgraded sockets, so the shared client's 60 s read timeout does not kill an idle websocket; `RealCall.timeoutEarlyExit()` ends the call timeout after the upgrade [CITED: square/okhttp `parent-5.5.0` RealConnection.kt, RealCall.kt]. Compose UI 1.12.1 has `ContentType.SmsOtpCode`, `EmailAddress`, `Password` and `NewPassword` (`javap`).

## Architecture Patterns

### System Architecture Diagram

```
                         ┌──────────────────────────────────────────────────────────┐
 Application.onCreate ──▶│ RealtimeConnectionManager (app-scoped, @Singleton)        │
                         │  desired = isLoggedIn && foreground                       │
 UserManager.isLoggedIn ─▶  collectLatest(desired) ──▶ connectWithStoredToken()      │
 ProcessLifecycleOwner ──▶                          └─▶ service.disconnect()         │
 SessionEvents.Refreshed ─▶ if desired: connectWithStoredToken()  (RT-02)            │
 service.handshakeFailures ▶ MissingToken ─▶ disconnect                              │
                         │   InvalidToken/Suspended ─▶ token rotated? connect again  │
                         │       else once per token: userManager.refreshMe() probe  │
                         │            ├ 401 ─▶ TokenAuthenticator (single-flight) ──┼─▶ POST /auth/refresh
                         │            │         ├ ok ─▶ Refreshed ─▶ (loop above)   │
                         │            │         └ 401 ─▶ Rejected ─▶ logout ─▶ isLoggedIn=false
                         │            └ ok/network ─▶ own 2s..30s retry (lib is dead)│
                         └───────────────┬──────────────────────────────────────────┘
                                         │ connect(token) / disconnect()
                                         ▼
 ┌───────────────────────────── RealtimeService (@Singleton) ──────────────────────────┐
 │ per connection: IO.socket(url, Options{forceNew, websocket, auth={token},           │
 │                  reconnectionDelay=Max=2000, infinite, callFactory=webSocketFactory})│
 │  EventThread callbacks (try/catch Throwable) ──▶ decode(JSON string, Moshi)         │
 │     ├─▶ events: MutableSharedFlow(replay=0, buffer=64, DROP_OLDEST).tryEmit         │
 │     ├─▶ state:  StateFlow<Disconnected|Connecting|Connected>                        │
 │     └─▶ handshakeFailures: SharedFlow<HandshakeFailure>  (connect_error JSONObject) │
 │  emitTypingStart/Stop, emitMessageRead  (only when Connected)                       │
 └───────────────┬────────────────────────────────────────────┬─────────────────────────┘
                 │ wss, websocket only, 40{"token":...}        │ @Named("bare") OkHttpClient
                 ▼                                             ▼
        Socket.IO 4.8.3 (Railway)  ◀── JWT verified every handshake, user room auto-joined
 debug only: RealtimeDebugLogger collects events + state ──▶ Logcat tag "Realtime"

 Auth UI:  AppRoute.Login ─▶ AuthNavHost (Navigator, start = LoginKey)
   Login ─push─▶ ForgotPassword(email) ─push─▶ SetNewPassword(email) ─push─▶ PasswordChanged(email)
     │                                                       PasswordChanged ─(button | system Back)─▶ reselect(LoginKey) + prefill email
     └─push─▶ SignUpPlaceholder ─back─▶ Login        POST auth/password/forgot, auth/password/reset  (public, no bearer)
```

### Recommended Project Structure
```
app/src/main/java/com/uptrendinvestments/clickmeapp/
├── realtime/
│   ├── RealtimeEvents.kt            # sealed RealtimeEvent + Moshi payload data classes (redacting toString)
│   ├── RealtimeEventDecoder.kt      # (eventName, jsonString) -> RealtimeEvent?  (pure, Moshi, never throws)
│   ├── RealtimeEmits.kt             # payload builders returning JSON strings (lowercased UUIDs)
│   ├── RealtimeUrl.kt               # socketUrl(baseUrl): strips /api/v1
│   ├── RealtimeConnection.kt        # interface + ConnectionState + HandshakeFailure (the seam for tests)
│   ├── RealtimeService.kt           # @Singleton, owns Socket instances
│   ├── RealtimeConnectionManager.kt # @Singleton policy: login/foreground/refresh/rejection
│   └── ProcessForeground.kt         # interface + ProcessLifecycleOwner impl (isForeground: StateFlow<Boolean>)
├── di/RealtimeModule.kt             # provides RealtimeConfig, binds RealtimeConnection, ProcessForeground
├── network/ApiRequestUrls.kt        # + FORGOT_PASSWORD, RESET_PASSWORD
├── network/ClickMeApi.kt            # + forgotPassword, resetPassword
├── model/requests/AuthRequests.kt   # + ForgotPasswordRequest, ResetPasswordRequest
├── model/responses/SuccessMessageResponse.kt
├── components/PasswordStrength.kt, PasswordStrengthMeter.kt, ValidatedField.kt (+trailingBadge, contentType)
├── navigation/AuthNavHost.kt, AuthKeys.kt   # (+ extract slide/fade transition helpers from HomeTabScaffold)
└── screens/login/
    ├── LoginScreen.kt, LoginViewModel.kt (rewrite in place), AuthValidators.kt, FieldError.kt
    ├── forgetpassword/ForgotPasswordScreen.kt + ViewModel
    ├── setnewpassword/SetNewPasswordScreen.kt + ViewModel
    ├── passwordchanged/PasswordChangedScreen.kt
    └── signup placeholder (screens/signup/SignUpPlaceholderScreen.kt)
app/src/debug/.../realtime/RealtimeDebugLogger.kt   # real collector
app/src/release/.../realtime/RealtimeDebugLogger.kt # empty stub, same signature (pattern of DebugNetwork)
```

### Pattern 1: One fresh Socket per connection, catch-all handlers
**What:** `RealtimeService.connect(token)` tears down the current socket (`off()`, `disconnect()`) and builds a new one with `forceNew = true` and `auth = mapOf("token" to token)`. A `generation` counter makes callbacks from a superseded socket no-ops. All state mutation sits under one lock; `IO.socket(...)` and `connect()` are cheap and safe from any thread because the library posts to its `EventThread`.
**When to use:** every connect, including after `Refreshed`. Same-token calls are a no-op unless the state is `Disconnected` (covers D-13 "does nothing if the token hasn't changed").
**Example:** see "Code Examples: RealtimeService".

### Pattern 2: Library loop for network loss, app-driven retry for rejection
**What:** The library's loop (2 s) handles `transport error`/`ping timeout`/`transport close` and failed reconnect attempts. It does **not** handle (a) `connect_error` carrying a server middleware rejection and (b) `disconnect` reason `io server disconnect`; in both cases the socket is destroyed and stays dead until someone calls `connect()` again (E3; Socket.IO docs: denied in a middleware, automatic reconnection "NO" [CITED: socket.io/docs/v4/client-socket-instance/]). The manager must retry these itself.
**When to use:** `HandshakeFailure` and `ServerDisconnected` handling in the manager.

### Pattern 3: Probe through the Authenticator, never refresh by hand
**What:** On `INVALID_TOKEN` (and the undocumented `ACCOUNT_SUSPENDED`, see Pitfall 5) with the token that was rejected still being the stored one, call `userManager.refreshMe()` once per token. An expired access token 401s, `TokenAuthenticator` refreshes under its mutex, emits `Refreshed`, and the manager reconnects. A rejected refresh emits `Rejected`, `UserManager.logout()` flips `isLoggedIn`, and the manager disconnects. A suspended account 403s, `SuspensionInterceptor` emits `Suspended`, same outcome. If the probe succeeds or fails softly, schedule the manager's own retry (2 s growing to 30 s for a repeated same-token rejection).
**Why:** satisfies D-15 ("single-flight", "at most one refresh attempt per access token", "401 means normal logout", "soft failure keeps trying") without touching Phase 2 code.

### Pattern 4: Connection policy as one derived boolean
**What:** `desired = isLoggedIn && isForeground`. `combine(...).distinctUntilChanged().collectLatest { if (it) connectWithStoredToken() else service.disconnect() }`, plus a collector for `SessionEvent.Refreshed` that calls `connectWithStoredToken()` when `desired`. `connectWithStoredToken()` reads `SecureTokenStore.accessToken` on `@IoDispatcher` (EncryptedSharedPreferences), and disconnects if null. This covers login (flag flips true after tokens are saved), restore including the offline-launch path (flag is set before `/me`), logout/rejection/suspension (every path ends in `logout()`), and foreground/background. No change to `UserManager`'s constructor is needed, so `UserManagerTest`/`LoginViewModelTest` construction sites are untouched.

### Pattern 5: Auth back stack with the existing `Navigator`
**What:** `rememberNavigationState(startRoute = LoginKey, topLevelRoutes = setOf(LoginKey))` plus `Navigator(state)`. `navigate(ForgotPasswordKey(email))` pushes (any key not in `backStacks.keys` is pushed). "Clear recovery screens, back to Login" is `navigator.reselect(LoginKey)` (pops to the stack root). System Back at the root is a no-op and `NavDisplay` then lets the activity exit, as the shell does. Keys are `@Serializable data class ... : NavKey` carrying the email, so process death restores the stack. Handle D-11 inside `NavDisplay(onBack = ...)`: if the top key is `PasswordChangedKey`, call the same "go to login" function as the button. Extract the private `slide()` helper from `HomeTabScaffold.kt` to a shared internal file instead of duplicating it.
**Pre-fill (D-10):** hold `var loginPrefill by rememberSaveable { mutableStateOf<String?>(null) }` in `AuthNavHost`; "Go to Login" sets it then calls `reselect`; `LoginScreen` receives `prefillEmail` and calls `viewModel.prefill(email)` once (sets email, clears password and errors) in a `LaunchedEffect`, then reports consumed.

### Pattern 6: Field state and error types
**What:** keep the existing "StateFlow of UiState + one-shot `destination`" shape. Use a small `sealed interface FieldError { Res(@StringRes id); Text(value) }` so local validation messages are string resources (UI-SPEC: nothing hard-coded) while server messages stay verbatim. `didAttemptSubmit` flag gives D-02 timing (nothing until first submit, then validate the edited field on every change). Dialog error is a nullable state cleared on dismiss. "Saved" hold: set `SaveState.Saved`, `delay(600)`, then emit the navigation event; `Back` resets to Idle.

### Anti-Patterns to Avoid
- **Mutating `auth` and calling `disconnect(); connect()` on the same Socket** (E5). Rebuild instead.
- **Doing work in a socket listener that can throw** (Pitfall 1).
- **Using the authed `apiClient` for the websocket.**
- **Calling `TokenRefresher.refresh` from the realtime code.**
- **Logging `JSONObject`/event payloads** (chat `content` is user data; the token is in the handshake).
- **Persisting password fields into saved instance state.** (See Open Question 3.)
- **Starting the connection manager from `MainActivity`.** An Activity can be recreated; use `Application`.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Socket.IO framing, heartbeats, packet encoding | A raw OkHttp WebSocket client | `socket.io-client` 2.1.2 | Engine.IO v4 pings (25 s / 20 s), CONNECT/EVENT/ACK packets, binary attachments. |
| Reconnect scheduling after transport loss | Own timer loop around the socket | Library `reconnectionDelay=Max=2000` | Verified deterministic 2.00 s; only rejection cases need app-side retry. |
| Single-flight token refresh | A new refresh mutex in the realtime layer | `TokenAuthenticator` via an authenticated request | Refresh-token reuse revokes the session (Phase 2 T-02-18). |
| Foreground/background detection | `Activity` start/stop counters | `ProcessLifecycleOwner` (`lifecycle.currentStateFlow`) | Survives rotation (ON_STOP is debounced), exposes a `StateFlow`. |
| Password strength | A new scoring scheme | Port of iOS `PasswordStrength.evaluate` (+1 each: length>=8, length>=12, `[A-Z]`, `[0-9]`, `[^A-Za-z0-9]`; 0-1 Weak, 2-3 Fair, 4-5 Strong) | UI-SPEC says "ported unchanged". |
| OTP autofill | Custom SMS/clipboard reading | `Modifier.semantics { contentType = ContentType.SmsOtpCode }` | Platform autofill; needs the new `ValidatedField.contentType` parameter. |
| Error body parsing and message selection | New JSON parsing | `ApiErrorParser`, `ApiException.Http.userMessage/status/fieldErrors` | Already handles HTML bodies, unknown codes, field errors. |
| Event JSON handling | `JSONObject` field-by-field access | Moshi codegen on the JSON string | Matches the REST stack, testable on a plain JVM when the decoder takes a `String`. |
| Date parsing | New formatter | Existing `InstantAdapter` | Already handles microsecond offsets (`...983286+00:00`). |

**Key insight:** the only genuinely custom logic this phase is the connection policy (when to connect, what to do after a rejection). Everything else has a library or an existing Phase 2 component.

## Common Pitfalls

### Pitfall 1: A listener exception crashes the app
**What goes wrong:** `EventThread.nextTick` catches `Throwable`, logs SEVERE and then **rethrows** inside an executor worker. The uncaught exception reaches Android's handler and kills the process. Moshi `JsonDataException`, `JSONException`, `ClassCastException`, a `NullPointerException` from a missing field are all candidates (and D-17 says malformed payloads must never crash).
**Why:** `EventThread` is a single static thread shared by every socket instance (source read in `io/socket/thread/EventThread.java`).
**How to avoid:** wrap the whole body of every `on(...)` listener (including `connect`, `disconnect`, `connect_error`) in `try { } catch (t: Throwable) { log class name only }`. Unit-test with a payload that makes the decoder throw.
**Warning signs:** `SEVERE: Task threw exception` in Logcat followed by a crash.

### Pitfall 2: Token swap by `disconnect(); connect()` on the same Socket silently fails
**What goes wrong:** the new connection never happens (E5). Also the `auth` map is captured at Socket construction, so even without the race you cannot "set the token later".
**How to avoid:** always build a new Socket (`forceNew = true`), after `off()` + `disconnect()` on the old one (E6). Guard callbacks with a generation counter so a late `disconnect` from the old socket cannot flip the new socket's state.

### Pitfall 3: Assuming the library keeps retrying after INVALID_TOKEN
**What goes wrong:** CONTEXT D-15 expects the reconnect loop to continue on a soft refresh failure. For a middleware rejection the Java client destroys itself (`Socket.onpacket CONNECT_ERROR -> destroy()`; E3). Without app-side retry the app stays disconnected until the next foreground or refresh.
**How to avoid:** the manager schedules its own retry (Pattern 2/3). Keep a `lastProbedToken` so a token the server rejects but REST accepts cannot cause a probe per retry; back off 2 s, 4 s, ... capped at 30 s for the repeated case (discretionary values, tunable).

### Pitfall 4: Server-initiated disconnect is also terminal
**What goes wrong:** `disconnect` with reason `io server disconnect` runs `destroy()` in the client; no automatic reconnect. The current backend does not call `socket.disconnect()`, but future suspension/deletion handling might.
**How to avoid:** in the `disconnect` listener, if the reason is `io server disconnect`, emit a `ServerDisconnected` failure so the manager retries after 2 s (guarded by `desired`).

### Pitfall 5: `ACCOUNT_SUSPENDED` can arrive as a connect_error message
**What goes wrong:** the backend handshake middleware (`socket.js`, quick 261016-m5y) rejects suspended users with `ACCOUNT_SUSPENDED` and deleted users with `INVALID_TOKEN`. `SOCKET-CONTRACT.md` documents only `MISSING_TOKEN` and `INVALID_TOKEN` [VERIFIED: backend-reference/src/services/socket.js]. A client that treats every unknown message as "retry forever at 2 s" hammers the server for a suspended account.
**How to avoid:** classify `ACCOUNT_SUSPENDED` with `INVALID_TOKEN` (probe with `GET /me`, which yields 403 `ACCOUNT_SUSPENDED` and the existing global trap/dialog). A deleted user: `INVALID_TOKEN` then the probe's refresh 401s and logs out.

### Pitfall 6: The Java client buffers emits while disconnected
**What goes wrong:** `Socket.emit` queues into `sendBuffer` when not connected and flushes on the next connect, so a stale `typing.start` fires seconds later.
**How to avoid:** the emit helpers return early unless `state == Connected` (iOS silently drops via `socket?`). `message.read` is socket-only per the contract; Phase 12 should only call it while connected and re-emit on the next screen appearance.

### Pitfall 7: `org.json` in unit tests
**What goes wrong:** with `org.json:json` excluded, JVM unit tests link to `android.jar` stubs that throw "Method ... not mocked" for `JSONObject`.
**How to avoid:** keep `org.json` out of pure logic: decoder takes a `String`, emit builders return JSON strings (convert with `JSONObject(str)` only at the socket boundary). Run the one test that touches the real library under `RobolectricTestRunner` (real `org.json`).

### Pitfall 8: ProcessLifecycleOwner threading and the 700 ms debounce
**What goes wrong:** ON_STOP is delivered ~700 ms after the last activity stops, so a quick app switch does not drop the socket (desired). Collecting the lifecycle on a non-main dispatcher can race `LifecycleRegistry`'s main-thread checks.
**How to avoid:** wrap in `ProcessForeground { isForeground: StateFlow<Boolean> }` created on Main (`ProcessLifecycleOwner.get().lifecycle.currentStateFlow.map { it.isAtLeast(STARTED) }`, `stateIn`/`callbackFlow` on `Dispatchers.Main.immediate`). Tests inject a fake MutableStateFlow. `[ASSUMED]` the 700 ms figure is from training knowledge of `ProcessLifecycleOwner.TIMEOUT_MS`.

### Pitfall 9: Backend rate limits during manual testing
**What goes wrong:** `forgotLimiter` = 5 requests / 15 min per IP, `resetLimiter` = 10 / 15 min, `loginLimiter` = 10 / 15 min, all answering `429 {"code":"TOO_MANY_REQUESTS","message":"Too many attempts. Please try again in 15 minutes."}` [VERIFIED: rateLimiter.js]. A tester can lock themselves out of the whole recovery flow.
**How to avoid:** the generic dialog already shows the server message; the manual checklist should warn about it. No client change.

### Pitfall 10: Recovery contract details that differ from the iOS comments
- Forgot returns `200` for every well-formed email (enumeration-safe) and `400 VALIDATION_FAILED` + `errors[]` (field `identity`) for a malformed identity. Both screens' copy must not imply the account exists.
- Reset returns `400 EXPIRED_TOKEN` for wrong, expired, used, max-attempts (5) and unknown-email alike (oracle-free), `422 VALIDATION_ERROR` with fields `password`, `password_confirmation`, `code`, `email`. Map by **HTTP status 400** (iOS does), not by `ErrorCode`.
- A code is single-use and valid 15 min; requesting a new code invalidates earlier ones.
- Response shape is `{status, message, data:{redirect_to}}` (reset) or `{status, message}` (forgot), not the `{status, data}` envelope, so `SuccessDataResponse<T>` does not fit; add `SuccessMessageResponse`.

### Pitfall 11: Entry-scoped `hiltViewModel()` is unproven at runtime
**What goes wrong:** Phase 1 research recorded that `hiltViewModel()` inside Navigation 3 entries compiled but "runtime scoping on a device was not exercised". Phase 3 is the first phase whose screens rely on it (Forgot/Set New Password VMs per pushed entry).
**How to avoid:** carry cross-screen data in `NavKey` arguments (email), never in a shared VM; keep each VM self-contained; add a Robolectric test that pushes, pops and re-pushes Forgot and asserts a fresh VM; list "rotate on each recovery screen keeps typed state" in the deferred device checklist.

### Pitfall 12: ValidatedField change must stay backward compatible
`trailingBadge` and `contentType` are optional, default-null parameters. The secure toggle already occupies `trailingIcon` when `isSecure`; the badge goes in the same slot before the toggle (a `Row`). Existing `ControlsTest`/gallery call sites must compile unchanged.

## Code Examples

### Dependency exclusion and R8 (defensive)
```kotlin
// app/build.gradle.kts
implementation(libs.socket.io.client) { exclude(group = "org.json", module = "json") }
```
```
# app/src/main/keepRules/rules.keep  (defensive; release currently uses packageScope androidx/kotlin/kotlinx,
# so io.socket.* and okhttp3.* are outside R8's optimization scope. The library uses no reflection:
# grep for Class.forName/ServiceLoader/reflect over both source jars returned nothing.)
-keep class io.socket.** { *; }
-dontwarn io.socket.**
```
`[CITED: developer.android.com/topic/performance/app-optimization/optimize-specified-packages]` for packageScope semantics. A `assembleProdRelease` + one Robolectric-free smoke is cheap insurance (Assumption A6).

### Socket URL from the base URL
```kotlin
// "https://host/api/v1" or ".../api/v1/" -> "https://host"
fun socketUrl(baseUrl: String): String {
    val u = baseUrl.toHttpUrl()
    val defaultPort = u.port == HttpUrl.defaultPort(u.scheme)
    return "${u.scheme}://${u.host}${if (defaultPort) "" else ":${u.port}"}"
}
```
Unit-test: dev and prod `BuildConfig.BASE_URL`, trailing slash, a custom port, `http` stays `http`, result never contains `/api`. (Both `https://host` and `https://host/` connected in E7; the no-slash form was used in E2.)

### RealtimeService (core of Pattern 1)
```kotlin
@Singleton
class RealtimeService @Inject constructor(
    @Named("bare") private val client: OkHttpClient,
    private val config: RealtimeConfig,           // socketUrl, reconnectDelayMs = 2_000
    private val decoder: RealtimeEventDecoder,
) : RealtimeConnection {
    private val lock = Any()
    private var socket: Socket? = null
    private var activeToken: String? = null
    private var generation = 0

    private val _events = MutableSharedFlow<RealtimeEvent>(
        replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val events: SharedFlow<RealtimeEvent> = _events.asSharedFlow()
    private val _state = MutableStateFlow(ConnectionState.Disconnected)
    override val state: StateFlow<ConnectionState> = _state.asStateFlow()
    private val _failures = MutableSharedFlow<HandshakeFailure>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val handshakeFailures: SharedFlow<HandshakeFailure> = _failures.asSharedFlow()

    override fun connect(token: String) = synchronized(lock) {
        if (token == activeToken && _state.value != ConnectionState.Disconnected) return
        teardownLocked()
        val gen = ++generation
        val options = IO.Options().apply {
            forceNew = true
            transports = arrayOf("websocket")
            reconnection = true
            reconnectionAttempts = Int.MAX_VALUE
            reconnectionDelay = config.reconnectDelayMs
            reconnectionDelayMax = config.reconnectDelayMs   // == min => no backoff growth, no jitter
            callFactory = client
            webSocketFactory = client
            auth = mapOf("token" to token)                   // handshake.auth.token (D-12)
        }
        val s = IO.socket(URI.create(config.socketUrl), options)
        bind(s, gen, token)
        socket = s; activeToken = token
        _state.value = ConnectionState.Connecting
        s.connect()
    }

    override fun disconnect() = synchronized(lock) { teardownLocked(); _state.value = ConnectionState.Disconnected }

    private fun teardownLocked() {
        generation++                       // invalidates callbacks of the old socket
        socket?.let { it.off(); it.disconnect() }
        socket = null; activeToken = null
    }

    private fun bind(s: Socket, gen: Int, token: String) {
        fun guarded(block: (Array<Any?>) -> Unit) = Emitter.Listener { args ->
            try { if (gen == generation) block(args) } catch (t: Throwable) { Log.w(TAG, "listener failed: ${t.javaClass.simpleName}") }
        }
        s.on(Socket.EVENT_CONNECT, guarded { _state.value = ConnectionState.Connected })
        s.on(Socket.EVENT_DISCONNECT, guarded { args ->
            _state.value = ConnectionState.Connecting          // lib loop is running, except for the case below
            if (args.firstOrNull() == "io server disconnect") _failures.tryEmit(HandshakeFailure(Reason.ServerDisconnected, token))
        })
        s.on(Socket.EVENT_CONNECT_ERROR, guarded { args ->
            // JSONObject = server middleware rejection (client is now dead); Exception = transport (lib retries)
            (args.firstOrNull() as? JSONObject)?.let {
                _state.value = ConnectionState.Disconnected
                _failures.tryEmit(HandshakeFailure(classify(it.optString("message")), token))
            }
        })
        for (name in RealtimeEventDecoder.EVENT_NAMES) {
            s.on(name, guarded { args ->
                val raw = (args.firstOrNull() as? JSONObject)?.toString() ?: return@guarded  // non-object payload: drop
                decoder.decode(name, raw)?.let { _events.tryEmit(it) }                       // tryEmit never suspends here
            })
        }
    }
}
```
`[VERIFIED by running]` for every option and callback shape above (E2-E8). Notes: `IO.Options.auth` is `Map<String,String>`; the two `Int/Long` options are public fields; `reconnectionAttempts = 0` also means infinite, but `Int.MAX_VALUE` is explicit.

### Event decoder (pure, never throws)
```kotlin
@JsonClass(generateAdapter = true)
data class MessageNew(
    @Json(name = "message_id") val messageId: String,
    @Json(name = "thread_id") val threadId: String,
    @Json(name = "sender_id") val senderId: String,
    val type: String,
    val content: String? = null,
    val timestamp: Instant,                 // InstantAdapter handles "2026-10-20T16:17:09.983286+00:00"
    val status: String,
) : RealtimeEvent {
    override fun toString() = "MessageNew(messageId=$messageId, threadId=$threadId)"   // never print content
}
// BookingUpdate(bookingId, status: String, revieweeId?, actionsInvalidated?, reviewEligible?) ; CallEnded(bookingId, endReason?)

class RealtimeEventDecoder(private val moshi: Moshi = MoshiProvider.moshi) {
    fun decode(event: String, json: String): RealtimeEvent? = try {
        when (event) {
            "message.new" -> moshi.adapter(MessageNew::class.java).fromJson(json)
            "message.read" -> moshi.adapter(MessageRead::class.java).fromJson(json)
            "user.status" -> moshi.adapter(UserStatus::class.java).fromJson(json)
            "typing.start" -> moshi.adapter(TypingPayload::class.java).fromJson(json)?.let { TypingStart(it.threadId, it.senderId) }
            "typing.stop" -> moshi.adapter(TypingPayload::class.java).fromJson(json)?.let { TypingStop(it.threadId, it.senderId) }
            "booking.update" -> moshi.adapter(BookingUpdate::class.java).fromJson(json)
            "call.ended" -> moshi.adapter(CallEnded::class.java).fromJson(json)
            else -> null
        }
    } catch (e: Exception) {          // JsonDataException, IOException, DateTimeParseException ...
        Log.w("Realtime", "dropped $event: ${e.javaClass.simpleName}"); null
    }
    companion object { val EVENT_NAMES = listOf("message.new", "message.read", "user.status", "typing.start", "typing.stop", "booking.update", "call.ended") }
}
```
Keep IDs as `String` (Moshi has no `UUID` adapter; iOS uses `UUID` only because Swift has it). Keep `status`/`type` as raw `String` plus a lenient `fromWire` enum accessor: the contract lists booking status `rescheduled`, which the iOS `BookingStatus` enum does not contain (iOS would silently drop that event), and iOS has `pending_reschedule`, `in_progress`, `missed` which the contract does not list. An Android enum should be the union with an unknown fallback.

### Emit payload builders (testable without org.json)
```kotlin
object RealtimeEmits {
    fun typing(threadId: UUID, recipientId: UUID) = """{"thread_id":"${threadId.toString().lowercase()}","recipient_id":"${recipientId.toString().lowercase()}"}"""
    // prefer Moshi @JsonClass TypingEmit(thread_id, recipient_id) / ReadEmit(thread_id, message_ids) over string templates
}
// at the boundary: socket.emit("typing.start", JSONObject(json));  message.read: JSONObject with message_ids as a JSON array
// iOS parity: message.read with empty ids is a no-op (guard !messageIds.isEmpty)
```
Server ignores `typing.*` without `recipient_id` and `message.read` with empty `message_ids` [VERIFIED: socket.js]. Always `if (state.value != Connected) return` first (Pitfall 6).

### Connection manager policy (Patterns 3 and 4)
```kotlin
@Singleton
class RealtimeConnectionManager @Inject constructor(
    private val service: RealtimeConnection,
    private val userManager: UserManager,
    private val tokens: SecureTokenStore,
    sessionEvents: SessionEvents,
    private val foreground: ProcessForeground,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val desired = combine(userManager.isLoggedIn, foreground.isForeground) { a, b -> a && b }.distinctUntilChanged()
    private var lastProbedToken: String? = null
    private var retryJob: Job? = null

    fun start() {
        scope.launch { desired.collectLatest { if (it) connectWithStoredToken() else service.disconnect() } }
        scope.launch { sessionEvents.events.filter { it == SessionEvent.Refreshed }.collect { if (desired.first()) connectWithStoredToken() } }
        scope.launch { service.handshakeFailures.collect(::onFailure) }
    }
    private suspend fun connectWithStoredToken() {
        retryJob?.cancel()
        val token = withContext(io) { tokens.accessToken }
        if (token == null) service.disconnect() else service.connect(token)
    }
    private suspend fun onFailure(f: HandshakeFailure) {
        when (f.reason) {
            Reason.MissingToken -> service.disconnect()
            Reason.InvalidToken, Reason.AccountSuspended -> {
                val current = withContext(io) { tokens.accessToken }
                when {
                    current == null -> service.disconnect()                       // session ended meanwhile
                    current != f.token -> connectWithStoredToken()                // already rotated
                    f.token != lastProbedToken -> { lastProbedToken = f.token; runCatching { userManager.refreshMe() }; afterProbe(f.token) }
                    else -> scheduleRetry()
                }
            }
            Reason.ServerDisconnected, Reason.Other -> scheduleRetry()
        }
    }
    // afterProbe: if still logged in and stored token == f.token -> scheduleRetry(); a Refreshed/logout already handled the rest
}
```
Start it from `ClickMeApplication.onCreate()` (`@Inject lateinit var`, Hilt injects in `super.onCreate()`). `scheduleRetry` = `delay(2_000, growing to 30_000 for the same token)` then, only if still `desired` and the token is unchanged, `connectWithStoredToken()`. All delays are constructor parameters so tests use virtual time.

### Public recovery endpoints
```kotlin
// ApiRequestUrls: const val FORGOT_PASSWORD = "auth/password/forgot"; const val RESET_PASSWORD = "auth/password/reset"
// (publicPaths already lists "/auth/password/forgot" and "/auth/password/reset", so no bearer is attached)
@POST(ApiRequestUrls.FORGOT_PASSWORD) suspend fun forgotPassword(@Body body: ForgotPasswordRequest): SuccessMessageResponse
@POST(ApiRequestUrls.RESET_PASSWORD)  suspend fun resetPassword(@Body body: ResetPasswordRequest): SuccessMessageResponse

@JsonClass(generateAdapter = true) data class ForgotPasswordRequest(val identity: String)
@JsonClass(generateAdapter = true) data class ResetPasswordRequest(
    val email: String, val code: String, val password: String,
    @Json(name = "password_confirmation") val passwordConfirmation: String,
)
@JsonClass(generateAdapter = true) data class SuccessMessageResponse(val status: String? = null, val message: String? = null)
```
`FakeClickMeApi` (test) must gain matching handlers or it stops compiling (Wave 0).

### Validators (ports; unit-test every branch)
```kotlin
object AuthValidators {
    private val email = Regex("""^[^@\s]+@[^@\s]+\.[^@\s]+$""")
    fun loginEmail(raw: String): FieldError? = raw.trim().let { when { it.isEmpty() -> res(R.string.error_email_required); !email.matches(it) -> res(R.string.error_email_invalid); else -> null } }
    fun loginPassword(p: String): FieldError? = when { p.isEmpty() -> ...; p.length < 8 -> ...; else -> null }
    fun resetCode(raw: String) /* trim; empty -> "Please enter the 6-digit code"; not ^\d{6}$ -> "Code must be 6 digits" */
    fun newPassword(p: String) /* empty / <8 / missing [0-9] or [^A-Za-z0-9] -> "Password must include a number and a symbol" */
    fun confirm(p: String, c: String)
}
```
Note on the iOS regex: it uses `\s` inside a character class; Kotlin's `Regex("""...""")` behaves the same for ASCII whitespace. Swift's `count < 8` counts grapheme clusters, Kotlin's `length` counts UTF-16 units, which only differ for emoji or combining characters (the server validates in JS, which also counts UTF-16), so `length` is the closer match.

### Engine.IO/Socket.IO protocol test (CI-safe, no Node)
```kotlin
// RobolectricTestRunner (real org.json). Server side of E8:
val listener = object : WebSocketListener() {
    override fun onOpen(ws: WebSocket, r: Response) { ws.send("""0{"sid":"e1","upgrades":[],"pingInterval":25000,"pingTimeout":20000,"maxPayload":1000000}""") }
    override fun onMessage(ws: WebSocket, text: String) {
        received += text
        if (text.startsWith("40")) {
            if ("good" in text) { ws.send("""40{"sid":"s1"}"""); ws.send("""42["typing.start",{"thread_id":"t1","sender_id":"u2"}]""") }
            else ws.send("""44{"message":"INVALID_TOKEN"}""")
        }
    }
}
server.enqueue(MockResponse.Builder().webSocketUpgrade(listener).build())
// assert received.first() == """40{"token":"good-1"}"""; assert the TypingStart event reaches service.events;
// assert state Connected; for the reject case assert a HandshakeFailure(InvalidToken) and no second request;
// in tearDown close the server-side WebSocket BEFORE server.close() (E8).
```

### Debug-only collector (D-20) with redaction
```kotlin
// debug source set
object RealtimeDebugLogger {
    fun install(service: RealtimeConnection, scope: CoroutineScope) {
        scope.launch { service.state.collect { Log.d("Realtime", "state=$it") } }
        scope.launch { service.events.collect { Log.d("Realtime", it.toString()) } }   // toString already redacts content
    }
}
// release source set: object RealtimeDebugLogger { fun install(service: RealtimeConnection, scope: CoroutineScope) = Unit }
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `.connectParams`/`query` token (SOCKET-CONTRACT.md example) | Socket.IO v4 `handshake.auth` (`IO.Options.auth`; Swift `connect(withPayload:)`) | Socket.IO 3.0 | Server reads `socket.handshake.auth.token`; iOS comments confirm query params are silently rejected. |
| socket.io-client-java 1.x (Socket.IO v2 servers) | 2.x (v3/v4 servers), `auth` option from 2.0.0, catch-all listeners from 2.1.0 | 2020-2022 | 2.1.2 (Feb 2025) is the maintained line. |
| `Lifecycle.addObserver` boilerplate | `Lifecycle.currentStateFlow` (confirmed present in `lifecycle-common-jvm:2.11.0` via `javap`) | Lifecycle 2.6+ | Foreground as a `StateFlow`. |
| Compose autofill via `Modifier.autofill` | `semantics { contentType = ContentType.X }` | Compose UI 1.8+ | Already the pattern in `ValidatedField` (Password). `NewPassword` also exists in 1.12.1 for the reset fields (optional improvement over UI-SPEC's `Password`). |

**Deprecated/outdated:**
- `socket.io-client` < 2.0.1 has CVE-2022-25867 (null deref on malformed packet); 2.1.2 is fine.
- `SOCKET-CONTRACT.md` sections on `.connectParams` and on the polling fallback: ignore both (D-12; websocket only).

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Forgot Password validates the **trimmed** email (iOS validates the raw string, so a trailing space gives "Invalid email format" there). Recommend trimming for parity with Login. | Validators | Trivial: one-line change either way. User/UI-SPEC says "trimmed email" is sent, so trimming before validation is consistent. |
| A2 | On Android, losing Wi-Fi or toggling airplane mode produces an immediate socket error so the 2 s loop starts quickly; a silent drop (NAT timeout) falls back to Engine.IO ping detection (server pingInterval 25 s + pingTimeout 20 s, i.e. up to ~45 s). | Pitfalls / D-20 checkpoint | Success criterion 4 "within about 2s" may only hold for visible drops. Covered by the deferred device check; if it fails, add a `ConnectivityManager` default-network callback to force a reconnect on `onAvailable`/`onLost`. |
| A3 | Entry-scoped `hiltViewModel()` works inside Navigation 3 entries (compiled in Phase 1; not run on a device). | Pitfall 11 | VMs could be recreated on each recomposition of an entry, losing form state. Mitigated by keeping cross-screen data in NavKeys; verify with a Robolectric push/pop test. |
| A4 | The MockWebServer Engine.IO test can run under `RobolectricTestRunner` (verified on a plain JVM with real `org.json`, not under Robolectric). | Validation Architecture | If it cannot, add `testImplementation("org.json:json:<current>")` and use plain JUnit. |
| A5 | Gradle resolves `okhttp` to 5.5.0 for engine.io's transitive `3.12.12` request (standard highest-version conflict resolution; the project's Gradle was not run in research). | Standard Stack | A `NoSuchMethodError` at runtime would reveal a stale 3.x. Check once with `./gradlew :app:dependencies --configuration devDebugRuntimeClasspath | grep -i okhttp`. All APIs engine.io uses exist in 5.5.0, so even a mismatch is source compatible. |
| A6 | With `packageScope = androidx/kotlin/kotlinx`, R8 does not shrink or obfuscate `io.socket.*`, so no keep rules are required; the defensive `-keep` is harmless. The library uses no reflection (grep). | Code Examples | A release-only failure of the socket. Smoke the `prodRelease` build once on a device (add to the existing release checklist). |
| A7 | `ProcessLifecycleOwner` ON_STOP is delivered ~700 ms after the last activity stops (training knowledge). | Pitfall 8 | Only affects how quickly the socket drops after backgrounding. |
| A8 | Password fields should survive configuration change via the ViewModel but not be written to saved instance state (process death clears them; code and email survive via NavKey/saved state). UI-SPEC says "Field text survives via rememberSaveable / saved state". | Open Question 3 | If the user wants literal saved-state persistence of passwords, remove the exclusion; the security cost is plaintext passwords in the saved-state Bundle. |
| A9 | Retry back-off values for a repeated same-token `INVALID_TOKEN` (2 s doubling to 30 s) and the `GET /me` probe are the discretionary details of D-15. | Pattern 3 | None functionally; tune freely. |

## Open Questions

1. **D-15 wording versus the code.**
   - What we know: `TokenRefresher` is a plain blocking HTTP call; the mutex, "token already rotated" check and single-flight are in `TokenAuthenticator`.
   - What's unclear: whether the user meant "reuse the refresh machinery" (what this research does) or literally `TokenRefresher`.
   - Recommendation: implement as "authenticated `GET /me` probe". If the user prefers a dedicated method, add `suspend fun refreshIfStale(rejectedToken)` to the authenticator package rather than calling `TokenRefresher` directly. Flag in the plan summary.

2. **`call.ended` payload.** CONTEXT D-17 lists only `{bookingId}` (iOS), but the server sends `{booking_id, end_reason}`. Recommend an optional `endReason: String?` on `CallEnded` (Phase 9 may use it).

3. **Persisting passwords in saved state.** See A8. Recommend the ViewModel-only approach; confirm or override.

4. **Where does the connection manager start?** Recommendation is `Application.onCreate`. Note FCM-triggered process starts (Phase 13) will also run it, but with the process not STARTED the `desired` flag stays false, so nothing connects.

5. **iOS `rescheduled` status.** The contract lists `rescheduled` and the iOS `BookingStatus` enum has no such case, so iOS drops that event today. Android keeps raw strings so it is not dropped; confirm with the iOS side whether that is a bug there (add to the iOS-issues todo list).

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK for Gradle | Build/tests | ✓ (Android Studio JBR per project memory) | JBR | — |
| Maven Central / Google Maven | New dependencies | ✓ | — | — |
| Production backend (Railway) | Manual verification of login, OTP email, socket | ✓ (handshake reachable, E7) | Socket.IO 4.8.3 | — |
| iOS app + a second account | D-20 manual check (trigger message/typing/booking events) | Not checked from here | — | Deferred human checkpoint (project memory: do not block). |
| Physical Android device (Samsung) | D-20 manual check, release smoke | Not checked from here | — | Deferred human checkpoint. |
| Node + `socket.io@4.8.3` (research only) | Not needed by the phase | ✓ locally | Node 26 | The MockWebServer protocol test replaces it in CI. |

**Missing dependencies with no fallback:** none.
**Missing dependencies with fallback:** iOS app and Samsung device are only needed for the deferred checkpoint.

## Validation Architecture

(`workflow.nyquist_validation` is absent from `.planning/config.json`, so this section is required.)

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4.13.2 + Robolectric 4.17 (sdk=35) + kotlinx-coroutines-test 1.11.0 + mockwebserver3 5.5.0 + Compose UI test (`createComposeRule` under Robolectric) |
| Config file | `app/src/test/resources/robolectric.properties`; `unitTests.isIncludeAndroidResources = true` in `app/build.gradle.kts` |
| Quick run command | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest --tests '<pattern>'` |
| Full suite command | `./gradlew ktlintCheck lintDevDebug lintProdDebug testDevDebugUnitTest testProdDebugUnitTest assembleDevDebug assembleProdDebug` (plus `bash scripts/check-design-rules.sh`) |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| AUTH-01 | Email/password rules and exact messages; silent until first submit then live per field | unit | `--tests '*AuthValidatorsTest' --tests '*LoginViewModelTest'` | ❌ Wave 0 (LoginViewModelTest exists but must be rewritten) |
| AUTH-01 | 401 -> password inline only; 422 `email` -> email inline; 5xx/network -> dialog; suspended -> nothing; success -> `resolvePostAuthDestination` | unit | `--tests '*LoginViewModelTest'` | ❌ rewrite |
| AUTH-01 | Login screen renders header, links, footer; dialog shows server message; dismiss clears | Robolectric Compose | `--tests '*LoginScreenTest'` | ❌ rewrite (current test asserts the old "Log in" copy) |
| AUTH-01 | Navigation: Login pushes Forgot with email; Sign Up pushes placeholder; Back pops | unit/Compose | `--tests '*AuthNavigationTest'` | ❌ Wave 0 |
| AUTH-02 | forgot/reset request bodies (`identity`; `email, code, password, password_confirmation`), no `Authorization` header | MockWebServer | `--tests '*ClickMeApiTest'` (extend) and `--tests '*ApiRequestUrlsTest'` (assert both paths public) | ✅ extend |
| AUTH-02 | Forgot VM: validation, 200 -> Saved 600 ms then navigate with trimmed email, 400 `identity` inline + dialog, 429 dialog, reset to Idle on return | unit (virtual time) | `--tests '*ForgotPasswordViewModelTest'` | ❌ Wave 0 |
| AUTH-02 | Set New Password VM: code digits-only/clip 6, all rules/messages, 400 -> code inline, 422 field mapping (`password`, `password_confirmation`, `code`, `email`), missing-email dialog, focus-first-error signal | unit | `--tests '*SetNewPasswordViewModelTest'` | ❌ Wave 0 |
| AUTH-02 | `PasswordStrength.evaluate` table (empty, boundary scores 0-5) | unit | `--tests '*PasswordStrengthTest'` | ❌ Wave 0 |
| AUTH-02 | Password Changed: button and system Back both clear to Login, email pre-filled, password empty; used OTP form unreachable | Compose/unit | `--tests '*AuthNavigationTest'` | ❌ Wave 0 |
| RT-01 | Socket URL derivation (no `/api`, scheme kept) | unit | `--tests '*RealtimeUrlTest'` | ❌ Wave 0 |
| RT-01 | Handshake sends `40{"token":...}`, websocket only (no polling request), state Connecting -> Connected, MISSING/INVALID mapped to `HandshakeFailure`, no auto-retry after rejection | Robolectric + MockWebServer (E8) | `--tests '*RealtimeServiceProtocolTest'` | ❌ Wave 0 |
| RT-01 | Reconnect after server close using a short injected delay; reconnect uses the original token | same | `--tests '*RealtimeServiceProtocolTest'` | ❌ Wave 0 |
| RT-02 | Manager: `Refreshed` -> reconnect with new stored token; same token + not Disconnected -> no-op; logout/Rejected/Suspended -> disconnect; background -> disconnect, foreground -> connect | unit with fake `RealtimeConnection`, fake `ProcessForeground`, `SessionEvents`, `FakeSecureTokenStore` | `--tests '*RealtimeConnectionManagerTest'` | ❌ Wave 0 |
| RT-02 | INVALID_TOKEN: exactly one `/me` probe per token (FakeClickMeApi `meCalls`), token-rotated shortcut, retry scheduled (virtual time), MISSING_TOKEN disconnects, no loop | same | `--tests '*RealtimeConnectionManagerTest'` | ❌ Wave 0 |
| RT-03 | Decode each contract payload (incl. microsecond timestamp, `end_reason`, `booking.update` variants completed/expired, unknown extra keys); malformed/missing field/non-object -> null, no throw | unit (plain JVM, `String` input) | `--tests '*RealtimeEventDecoderTest'` | ❌ Wave 0 |
| RT-03 | Redaction: `MessageNew.toString()` never contains `content` | unit | same | ❌ Wave 0 |
| RT-03 | Flow semantics: replay 0 (late collector sees nothing), 64 buffer, DROP_OLDEST never blocks the producer; a throwing handler path does not kill later events | unit/protocol | `--tests '*RealtimeServiceProtocolTest'` | ❌ Wave 0 |
| RT-03 (D-19) | Emit payloads: keys, lowercase UUIDs, empty `message_ids` -> no emit, no emit unless Connected | unit | `--tests '*RealtimeEmitsTest'` | ❌ Wave 0 |
| RT-01..03 | On-device: iOS-triggered message/typing/booking update appear in Logcat `Realtime`; airplane-mode toggle and forced refresh reconnect within ~2 s | **manual, deferred** | — (D-20; log as open item) | n/a |

### Sampling Rate
- **Per task commit:** the quick command scoped to the touched test class (< 60 s).
- **Per wave merge:** `./gradlew testDevDebugUnitTest ktlintCheck` and `bash scripts/check-design-rules.sh`.
- **Phase gate:** full suite green (including `lintProdDebug`, `assembleProdDebug`, and, once, `assembleProdRelease` for the R8 smoke) before `/gsd-verify-work`.

### Wave 0 Gaps
- [ ] `app/src/test/.../testing/FakeClickMeApi.kt`: add `forgotPassword`/`resetPassword` handlers and call counters (the class implements `ClickMeApi`, so it will not compile otherwise).
- [ ] `app/src/test/.../testing/FakeRealtimeConnection.kt` and `FakeProcessForeground.kt`: scriptable seam doubles for the manager tests.
- [ ] Rewrite `LoginViewModelTest` and `LoginScreenTest` (they assert the temporary screen: "Log in", `LoginError.MissingFields`).
- [ ] New test files: `AuthValidatorsTest`, `PasswordStrengthTest`, `ForgotPasswordViewModelTest`, `SetNewPasswordViewModelTest`, `AuthNavigationTest`, `RealtimeUrlTest`, `RealtimeEventDecoderTest`, `RealtimeEmitsTest`, `RealtimeConnectionManagerTest`, `RealtimeServiceProtocolTest`.
- [ ] Extend `ClickMeApiTest` (bodies, no bearer) and `ApiRequestUrlsTest`.
- [ ] Framework install: none. (`org.json` is not needed on the test classpath if the protocol test runs under Robolectric, Assumption A4.)

## Security Domain

(`security_enforcement` is not set to false in `.planning/config.json`, so it is enabled.)

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes | Server-side login and OTP reset; client never stores or logs passwords/OTP; `ContentType.Password`/`NewPassword`/`SmsOtpCode` autofill; no auto-login after reset (D-10); used OTP form is unreachable (D-11). |
| V3 Session Management | yes | Existing `EncryptedSharedPreferences` token store and single-flight refresh; socket disconnects on logout/rejection/suspension and on background; the socket is rebuilt (not reused) whenever the token changes. |
| V4 Access Control | yes (server-side) | Server verifies the JWT on every handshake and joins only the user's own room; `message.read` is IDOR-guarded server-side. The client never trusts socket events for authorization decisions. |
| V5 Input Validation | yes | Client validators mirror server rules (UX only; the server stays authoritative); event payloads are decoded with Moshi and anything malformed is dropped; JSON built with Moshi/`JSONObject`, never string-concatenated user text. |
| V6 Cryptography | yes | TLS only: socket URL derived from the https base URL gives wss (unit-test that `http` never appears for dev/prod); no custom crypto; tokens already in `EncryptedSharedPreferences`. |
| V7 Error Handling and Logging | yes | Debug collector logs event type and ids only (`toString()` redacts chat `content`), never the token; library logging stays at the JUL default (its `FINE` messages include packets and are off by default); exception messages from decoding are not logged with payloads. |
| V9 Communications | yes | `wss` via the app's OkHttp (system trust store); bare client (no bearer) for the upgrade; token only in the Socket.IO CONNECT packet inside TLS, not in the URL/query/logs. |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Token leakage via URL query (access logs, proxies) | Information disclosure | Token goes in `auth` (CONNECT packet payload), never `query` (D-12; E2 shows `handshake.query` carries only `EIO`/`transport`). |
| Token or message content in Logcat | Information disclosure | Redacting `toString()`; no payload logging; debug-only collector (release stub is empty); do not enable `java.util.logging` FINE for `io.socket`. |
| Crash from malicious or malformed event payload | Denial of service | Catch-all in every listener (Pitfall 1); decoder returns null on any exception; bounded `DROP_OLDEST` buffer. |
| Reconnect storm on a rejected or suspended token | Denial of service (against our own backend) | One probe per token, capped backoff for repeated rejection, disconnect on background/logout, `ACCOUNT_SUSPENDED` routed to the existing logout path. |
| Stale socket delivering a previous user's events after logout/login | Information disclosure / Elevation | `off()` + `disconnect()` + generation guard; `forceNew` per connection; disconnect on `isLoggedIn == false`. |
| Refresh-token reuse revoking the session | Elevation / DoS | Refresh only through `TokenAuthenticator` (mutex, rotated-token check). |
| OTP brute force / enumeration | Spoofing / Information disclosure | Server: 5 attempts per code, 15 min expiry, per-IP limiters, identical responses. Client: no resend UI (D-09), generic copy, 429 message surfaced as is, nothing logged. |
| Public endpoints carrying a bearer | Information disclosure | `ApiRequestUrls.needAuthToken` already excludes both paths; add a MockWebServer assertion that `Authorization` is absent. |
| Passwords persisted in the saved-state Bundle | Information disclosure | Keep password fields ViewModel-only (Assumption A8). |
| Cleartext transport | Tampering / Disclosure | Base URLs are https; assert derived socket URL scheme; no `usesCleartextTraffic`. |

## Sources

### Primary (HIGH confidence)
- Executed experiments E1-E8 (this session): socket.io-client 2.1.2, engine.io-client 2.1.0, OkHttp 5.5.0, okio 3.18.1, mockwebserver3 5.5.0, Node 26 + socket.io 4.8.3, and the production backend handshake.
- Source read: `socket.io-client-2.1.2-sources.jar`, `engine.io-client-2.1.0-sources.jar` (`Manager`, `Socket`, `Backoff`, `EventThread`, `IO`, `Transport`); `backend-reference/src/services/socket.js`, `events.js`, `src/controllers/auth.controller.js`, `src/middleware/rateLimiter.js`, `src/routes/auth.js`, `docs/SOCKET-CONTRACT.md`, `package.json` (socket.io 4.8.3).
- iOS reference: `Realtime/RealtimeService.swift`, `RealtimeEvents.swift`, `LoginViewModel.swift`, `LoginForgetPassViewModel.swift`, `LoginResetPasswordViewModel.swift`, `LoginResetPasswordView.swift` (PasswordStrength), `ClickMeAPI+Auth.swift`, `UserManager.swift` connect points.
- Project code: `UserManager`, `SessionEvents`, `TokenAuthenticator`, `TokenRefresher`, `NetworkModule`, `NetworkFactory`, `ApiRequestUrls`, `ClickMeApi`, `ApiException`, `ApiErrorParser`, `LoginViewModel/Screen`, `ValidatedField`, `AsyncSaveButton`, `Navigator`, `NavigationState`, `HomeTabScaffold`, `AppViewModel`, `ClickMeApp`, `MainActivity`, test fakes.
- [Maven Central socket.io-client metadata](https://repo1.maven.org/maven2/io/socket/socket.io-client/maven-metadata.xml) (2.1.2 latest, 2025-02-18) and [engine.io-client 2.1.0 pom](https://repo1.maven.org/maven2/io/socket/engine.io-client/2.1.0/engine.io-client-2.1.0.pom) (okhttp 3.12.12).
- [socket.io-client-java installation page](https://socketio.github.io/socket.io-client-java/installation.html) (coordinate and `org.json` exclusion).
- [Socket.IO client socket instance docs](https://socket.io/docs/v4/client-socket-instance/) (middleware rejection does not auto-reconnect; `socket.connect()` must be called manually).
- [OkHttp 5.5.0 RealConnection.kt](https://github.com/square/okhttp/blob/parent-5.5.0/okhttp/src/commonJvmAndroid/kotlin/okhttp3/internal/connection/RealConnection.kt) (`useAsSocket` sets `soTimeout = 0`) and RealCall.kt (`timeoutEarlyExit`).
- [Android Developers: optimize specified packages with packageScope](https://developer.android.com/topic/performance/app-optimization/optimize-specified-packages).
- [AndroidX Hilt release notes](https://developer.android.com/jetpack/androidx/releases/hilt) (hiltViewModel moved to `hilt-lifecycle-viewmodel-compose`).

### Secondary (MEDIUM confidence)
- [socket.io-client-java GitHub README](https://github.com/socketio/socket.io-client-java) (compatibility table: client 2.x with server 3.x/4.x) and releases page (2.0.0-2.1.2 notes).
- Phase 1/2 research and summaries in this repo (Hilt + Navigation 3 recipe, R8 and Moshi rules).

### Tertiary (LOW confidence)
- `ProcessLifecycleOwner` 700 ms ON_STOP debounce and Android's socket-error behavior on interface loss (training knowledge; see A2, A7).

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH. Versions read from Maven metadata; compatibility proven by compiling and running against OkHttp 5.5.0 (one open item: confirm Gradle's resolution on first build, A5).
- Architecture: HIGH for socket behavior and the policy layer (empirical); MEDIUM for auth navigation (reuses proven `Navigator`, but entry-scoped Hilt VMs are unexercised, A3).
- Pitfalls: HIGH for 1-7, 10 (source-verified or reproduced); MEDIUM for 8, 11, 12.

**Research date:** 2026-10-20
**Valid until:** 2026-11-19 for the library facts (stable; socket.io-client has had one release in the last 20 months); re-check if AGP/OkHttp are bumped before execution.
