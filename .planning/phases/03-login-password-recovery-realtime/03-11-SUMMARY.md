---
phase: 03-login-password-recovery-realtime
plan: 11
subsystem: realtime
tags: [hilt, realtime, application, debug-logging, r8]
requires: [03-08, 03-09]
provides:
  - di.RealtimeModule (RealtimeConnection, ProcessForeground, RealtimeConfig singletons)
  - ClickMeApplication starts RealtimeConnectionManager and installs RealtimeDebugLogger
  - realtime.RealtimeDebugLogger (debug Logcat collector, release no-op twin)
affects: [03-12 device verification, Phase 9 call keep-alive, Phase 12 chat]
tech-stack:
  added: []
  patterns: [debug/release source-set twin objects, undispatched collectors so a replay-less flow is subscribed before install returns]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/di/RealtimeModule.kt
    - app/src/debug/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeDebugLogger.kt
    - app/src/release/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeDebugLogger.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeDebugLoggerTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/ClickMeApplication.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/UserManager.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/SessionEvents.kt
key-decisions:
  - "Logger collectors launch with CoroutineStart.UNDISPATCHED so the events SharedFlow (no replay) is subscribed before install() returns"
  - "RealtimeDebugLoggerTest wraps FakeRealtimeConnection with a local event bus instead of editing the shared fake, which is outside this plan's files"
requirements-completed: [RT-01, RT-02, RT-03]
duration: ~25min
completed: 2026-10-20
---

# Phase 3 Plan 11: Realtime DI Wiring and Debug Logger Summary

The realtime layer now runs app-wide. `ClickMeApplication.onCreate` starts the app-scoped `RealtimeConnectionManager`, Hilt provides the socket service, foreground provider and config as singletons, and debug builds log every state change, event and handshake failure under Logcat tag `Realtime`.

## What was built

- `RealtimeModule` (abstract, `@InstallIn(SingletonComponent)`): two `@Binds @Singleton` (`RealtimeService` to `RealtimeConnection`, `LifecycleProcessForeground` to `ProcessForeground`) and a companion `@Provides @Singleton RealtimeConfig(socketUrl = socketUrl(AppConfig.baseUrl))`. No OkHttp binding is added; the service keeps its `@Named("bare")` client.
- `ClickMeApplication`: field-injected manager and connection, `onCreate` calls `realtimeConnectionManager.start()` then `RealtimeDebugLogger.install(realtimeConnection)`. KDoc explains why it lives in Application, not MainActivity.
- `RealtimeDebugLogger`: debug object with three collectors (state, events, handshake failures) on its own scope, printing only the redacting `toString` output; release twin is an empty `install` with no `android.util.Log` import.
- `UserManager` and `SessionEvents`: comment/KDoc changes only (RealtimeConnectionManager reconnects on `Refreshed`, RT-02).

## Tasks

| Task | Commit | Notes |
|------|--------|-------|
| 1 Debug logger, release stub, test | 60b4a08 | 3 tests |
| 2 RealtimeModule, Application start, comments | 6b85201 | full verify command exits 0 |

Verification: `ktlintCheck lintDevDebug lintProdDebug testDevDebugUnitTest assembleDevDebug assembleProdDebug assembleProdRelease` exits 0 (the Hilt graph resolves for both flavors, the unsigned R8 prodRelease assembles with the socket library); `scripts/check-design-rules.sh` prints "Design rules: OK". All acceptance greps match (@Binds 2, `socketUrl(AppConfig.baseUrl)` 1, `realtimeConnectionManager.start()` 1, release logger `import android.util.Log` 0).

## Deviations from Plan

**1. [Rule 3 - Blocking] FakeRealtimeConnection cannot emit events.** The 03-09 fake keeps its events flow private and has no emit method, which the MessageNew redaction test needs. Rather than edit a file outside this plan's `files_modified`, the test declares a private `EventingFakeConnection` that delegates to the fake (`RealtimeConnection by base`) and overrides `events` with its own `MutableSharedFlow`.

**2. [Rule 1 - Bug prevention] Undispatched collector start.** Because `events` has no replay, collectors launched on `Dispatchers.Default` could miss an event emitted right after `install`. They are launched with `CoroutineStart.UNDISPATCHED`, which also makes the test deterministic.

**3. [Process] Worktree base correction.** The worktree HEAD was at an older commit than the expected base; it was reset to 521fe5e as the startup check prescribes.

## Deferred Items (human/device)

- On-device proof of success criterion 3 (`adb logcat -s Realtime` shows Connecting then Connected after login, reconnect after a token refresh, disconnect in background) and the on-device prodRelease smoke for R8 (T-03-44) are deferred to plan 03-12, per D-20.
- `keystore.properties` is unavailable, so only the unsigned `assembleProdRelease` was verified.

## Known Stubs

None.

## Threat Flags

None beyond the plan's threat model. T-03-41 (redacting toString, test asserts content and token absent), T-03-42 (release no-op, no Log import), T-03-43 (no client binding in the module) are mitigated; T-03-44 is mitigated at build level (R8 prodRelease assembles) with the device smoke deferred.

## Self-Check: PASSED

All seven source/test files exist; commits 60b4a08 and 6b85201 are in the log.
