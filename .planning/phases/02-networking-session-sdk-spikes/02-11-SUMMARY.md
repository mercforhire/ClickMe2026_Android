---
phase: 02-networking-session-sdk-spikes
plan: 11
subsystem: auth
tags: [session, auto-login, logout, suspension, datastore, coroutines, tdd]

requires:
  - phase: 02-networking-session-sdk-spikes
    provides: ApiException/apiCall (02-03), SecureTokenStore and SessionEvents (02-06), ClickMeApi/SessionStore/qualifiers (02-07)
provides:
  - UserManager Hilt singleton: login, restore (auto-login), logout, mode and onboarding persistence, suspension flag, stale /me retry
  - LaunchDestination sealed interface (Login, SignupSetup, Home(mode))
  - PushTokenRegistrar no-op hook (D-09) for Phase 13
  - FakeClickMeApi test helper
affects: [02-13, 02-14, 03, 13]

tech-stack:
  added: []
  patterns: [session ends only when the transport cleared the tokens (hasSession check after failure), UNDISPATCHED event subscription, NonCancellable logout, session-generation guard on background cache writes]

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/UserManager.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/LaunchDestination.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/PushTokenRegistrar.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeClickMeApi.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/UserManagerTest.kt
  modified: []

key-decisions:
  - "restore() routes by 'does the token store still have a session after /me failed': no session -> logout + Login, session kept -> cached Home(currentMode) with meStale set (D-01/D-03, deliberate deviation from iOS isAuthRejection)"
  - "A suspension seen in the /me failure goes through handleAccountSuspended directly (not only via the event) so wasSuspended is set deterministically before restore() returns"
  - "refreshProfile guards cache writes with both a session-generation counter and a hasSession check (T-02-37)"
  - "No timeout inside UserManager; the splash cap belongs to AppViewModel (offlineDestination() is its fallback)"

patterns-established:
  - "Session tests: Harness wiring UserManager over FakeClickMeApi + FakeSecureTokenStore + real file DataStore, using runCurrent() to drain backgroundScope work"
  - "Fake transport behavior in tests: meHandler clears the fake token store and emits the SessionEvent before throwing, mimicking the OkHttp Authenticator/SuspensionInterceptor"

requirements-completed: [FND-06, FND-07, FND-08]

duration: ~25min
completed: 2026-10-20
---

# Phase 2 Plan 11: UserManager Session Brain Summary

**Hilt-singleton UserManager porting iOS login/auto-login/logout with iOS launch routing, D-01/D-03 soft-failure cached path, single-shot suspension flag and stale-/me retry, covered by 30 JVM tests.**

## Performance

- **Tasks:** 2 (both TDD)
- **Files:** 5 created, 0 modified

## Accomplishments
- `login` persists the token pair, flips `isLoggedIn`, clears the suspension flag and refreshes the cached profile on the application scope.
- `logout` is the one clear path (push-unregister hook first, then tokens, in-memory state, DataStore) under `NonCancellable`.
- `restore()` loads cached profile/mode/onboarding first, then reproduces SplashViewModel/LoginView routing (SignupSetup for incomplete expert setup, stored mode, expert-with-topics) and never throws or times out.
- IO errors, timeouts, 5xx and a 401/403 that left tokens in place keep the session and route from cache; `refreshIfStale()` retries `/me` and the profile once.
- Required plan-checker case covered: `restoreKeepsSessionWhenTokensStillPresentAfterMe401`.

## Task Commits

1. **RED: failing tests + FakeClickMeApi** - `cc0117a` (test)
2. **Tasks 1 and 2 GREEN: UserManager, LaunchDestination, PushTokenRegistrar** - `215db3c` (feat)

Both tasks share the same two files, so the test file was written in full up front (30 tests) and the implementation landed in one GREEN commit.

## Verification
`./gradlew :app:testDevDebugUnitTest --tests '*UserManagerTest' ktlintCheck` exits 0; 30 tests, 0 failures. Acceptance greps: UNDISPATCHED 1, NonCancellable present, `sessionStore.clear()` 1, `tokens.clear()` 1 (non-comment), `pushTokenRegistrar.unregister()` 1, `withTimeout` 0, `LaunchDestination.SignupSetup` 1.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Tests drain background work with runCurrent(), not advanceUntilIdle()**
- **Found during:** Task 1 GREEN run
- **Issue:** `advanceUntilIdle()` ignores work that is only pending on `backgroundScope` (the application scope and the DataStore scope), so event handling and background profile refresh never ran and 5 tests failed.
- **Fix:** Use `testScheduler.runCurrent()` (imported `runCurrent`) wherever the plan said `advanceUntilIdle()`.
- **Files modified:** UserManagerTest.kt
- **Commit:** 215db3c

**2. [Rule 2 - Missing critical] Deterministic suspension flag in restore**
- **Found during:** Task 2 design
- **Issue:** Relying only on the Suspended event meant `wasSuspended` could still be false when `restore()` returns Login, because event delivery is asynchronous.
- **Fix:** `afterMeFailure` routes an `ACCOUNT_SUSPENDED` Http error through `handleAccountSuspended()` (guarded, so the later event is a no-op). Added `sessionGeneration` guard to `refreshProfile` on top of the planned `hasSession()` check.
- **Files modified:** UserManager.kt
- **Commit:** 215db3c

**3. Worktree base reset**
- The worktree branch started at `d1217fb`; per the startup check it was reset to `7d4fab9` before any work (clean tree, no commits lost).

## Known Stubs
None. `PushTokenRegistrar.unregister()` is an intentional no-op (D-09), implemented in Phase 13 (PUSH-01). `SessionEvent.Refreshed` is intentionally ignored until Phase 3 socket reconnect.

## Threat Flags
None beyond the plan's threat model (T-02-37 to T-02-41 mitigations are implemented and tested).

## Self-Check: PASSED
- Files exist: UserManager.kt, LaunchDestination.kt, PushTokenRegistrar.kt, FakeClickMeApi.kt, UserManagerTest.kt
- Commits cc0117a and 215db3c exist on the worktree branch
