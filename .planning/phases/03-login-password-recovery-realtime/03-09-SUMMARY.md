---
phase: 03-login-password-recovery-realtime
plan: 09
subsystem: realtime
tags: [realtime, lifecycle, session, coroutines, virtual-time]
requires: [03-01, 03-03]
provides:
  - realtime.ProcessForeground seam and LifecycleProcessForeground (ProcessLifecycleOwner)
  - realtime.RealtimeConnectionManager (app-scoped connect/disconnect/reconnect policy)
  - testing.FakeRealtimeConnection and testing.FakeProcessForeground
affects: [03-11 DI wiring and start(), 03-08 socket service, Phase 9 call keep-alive, Phase 12 chat]
tech-stack:
  added: []
  patterns: [combine+distinctUntilChanged+collectLatest connection gate, one /me probe per rejected token, policy state serialized with limitedParallelism(1)]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/realtime/ProcessForeground.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeConnectionManager.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeRealtimeConnection.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeProcessForeground.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeConnectionManagerTest.kt
  modified: []
key-decisions:
  - "Handshake rejection recovery is one authenticated GET /me per rejected access token via UserManager.refreshMe; TokenRefresher is never referenced in realtime/"
  - "Retry delay runs on the io dispatcher and hops back to the serial view to act, because a limitedParallelism view does not forward delay to a test scheduler"
requirements-completed: [RT-02]
duration: ~40min
completed: 2026-10-20
---

# Phase 3 Plan 09: Realtime Connection Manager Summary

The socket now follows login state and the process foreground. Every `SessionEvent.Refreshed` reconnects with the stored token, and handshake rejections recover through the existing refresh and suspension machinery with one `/me` probe per token and a 2 s to 30 s capped retry.

## What was built

- `ProcessForeground` interface and `LifecycleProcessForeground` (`@Singleton`, `ProcessLifecycleOwner.currentStateFlow` mapped to `isAtLeast(STARTED)`). KDoc records the main-thread construction rule, the ~700 ms ON_STOP grace, the FCM-started-process behavior and the Phase 9 keep-alive hook (`isDesired` is the single place to OR it in).
- `RealtimeConnectionManager.start()` (idempotent) runs four collectors on `scope + serial`: the `combine(isLoggedIn, isForeground)` gate, the `Refreshed` reconnect, the `Connected` backoff reset and the handshake failure handler.
- Failure policy: MissingToken disconnects; InvalidToken and AccountSuspended compare the rejected token to the stored one (null disconnects, rotated reconnects at once, same and not yet probed makes one `refreshMe()` probe, already probed schedules a retry); ServerDisconnected and Other schedule a retry. `afterProbe` reconnects if the token changed, else retries.
- No logging and no `TokenRefresher` reference in the manager (grep verified).

## Tasks

| Task | Commit | Notes |
|------|--------|-------|
| 1 Foreground seam, doubles, connect/disconnect/refresh policy | 11cd494 | 11 tests |
| 2 Handshake rejection policy and capped retry | f1f6965 | 13 more tests, 24 total |

Verification: `:app:testDevDebugUnitTest --tests '*RealtimeConnectionManagerTest' --tests '*UserManagerTest' ktlintCheck lintDevDebug` exits 0 (RealtimeConnectionManagerTest: 24 tests, 0 failures; UserManagerTest unchanged and green).

## Deviations from Plan

**1. [Rule 1 - Bug] Retry job cancelled itself.** `connectWithStoredToken()` calls `cancelRetry()`, and when invoked from inside the retry job that cancelled the job mid-flight, so its token read threw `CancellationException` and no connect happened. The job now detaches (`retryJob = null`) before connecting.

**2. [Plan-anticipated fallback] Virtual time did not advance through `limitedParallelism(1)`.** The `delay` on the limited view ran on real time, so all timed-retry tests failed. Kept `serial` for all policy state, but the retry job is launched with `scope.launch(io)`, does `delay` there, then `withContext(serial)` to act. No Mutex was needed. Tests with virtual time pass for 2, 4, 8, 16, 30 and 30 s.

**3. [Style] ktlint reformatted the test file** (`ktlintFormat`) after the Task 2 test additions; no behavior change.

## Notes for downstream plans

- 03-11 must bind `RealtimeConnection`, `ProcessForeground` (to `LifecycleProcessForeground`) and `RealtimeConfig`, then call `RealtimeConnectionManager.start()` once from `Application.onCreate`. Nothing requests the manager yet, so the Hilt graph is unchanged.
- A probe that refreshes can produce two `connect(newToken)` calls (the `Refreshed` collector and `afterProbe`). That is intentional and safe: the service ignores a same-token connect while alive (03-08).
- RT-01 is not marked complete: its service half is plan 03-08. RT-02 (reconnect after each refresh) is complete here.

## Deferred Items

None. No human or device checkpoints in this plan. (Real-device check of ON_STOP disconnect and offline-launch connect is naturally covered when 03-11 wires the manager.)

## Known Stubs

None.

## Threat Flags

None beyond the plan's threat model. T-03-32 to T-03-35 are mitigated and tested (single probe site, no `TokenRefresher`, one probe per token, capped backoff, background and logout cancel retries, `lastProbedToken` reset on logout).

## Self-Check: PASSED

All five source/test files exist; commits 11cd494 and f1f6965 are in the log.
