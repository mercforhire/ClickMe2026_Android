---
phase: 11-expert-requests-bookings
fixed_at: 2026-10-26T00:00:00Z
review_path: .planning/phases/11-expert-requests-bookings/11-REVIEW.md
iteration: 1
findings_in_scope: 2
fixed: 2
skipped: 0
status: all_fixed
---

# Phase 11: Code Review Fix Report

**Fixed at:** 2026-10-26
**Source review:** .planning/phases/11-expert-requests-bookings/11-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 2
- Fixed: 2
- Skipped: 0

## Fixed Issues

### WR-01: Engine cleanup on setup failure (the 11-17 headline fix) has no test

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraManager.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/managers/AgoraManagerCreateEngineTest.kt`, `app/build.gradle.kts`, `gradle/libs.versions.toml`
**Commit:** ff49ad4
**Applied fix:** Added an internal primary-constructor parameter `destroyRtcEngine: () -> Unit` (default `{ RtcEngine.destroy() }`, placed before `createRtcEngine` so existing trailing-lambda test calls still bind to the factory). The Hilt `@Inject` secondary constructor passes the real `RtcEngine.destroy` and `RtcEngine.create`, so DI wiring is unchanged and compiles (assembleDevDebug passed). `createEngine` now calls the seam. `RtcEngine` is an abstract class with 337 abstract methods and no mocking library was on the test classpath, so `mockito-core` 5.24.0 was added as a `testImplementation` dependency. New tests: a stub engine whose `setAudioProfile` throws after creation asserts destroy ran once, `join` returned false, `joinChannel` was never called, exactly one redacted warning was logged, and a second `join` calls the factory again (and destroys again). A second test asserts a healthy setup does not call destroy.

### WR-02: `destroy()` is not exception-safe, unlike the new setup-failure cleanup

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraManager.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/managers/AgoraManagerCreateEngineTest.kt`
**Commit:** 754b8f5
**Applied fix:** `destroy()` now calls the `destroyRtcEngine` seam inside `try`, catches `Exception` and `LinkageError`, logs `engine destroy failed <describeEngineFailure>` (class name and top frame only, no message, uid, token or channel), and resets `engine`, `isLeaving` and `leaveSignal` in `finally`. The failure is swallowed rather than rethrown, so `CallCenter`'s remaining teardown is not skipped; this goes slightly beyond the review's bare `try/finally` suggestion and uses the review's alternative of logging with `describeEngineFailure`. New test: destroy throws after a successful join, asserts the redacted warning and sentinel-free logs, and that the next `join` creates a new engine (factory called twice) instead of reusing the dead one.

## Verification

Ran in the main checkout (no worktree; edits and commits made directly on `main`) with `JAVA_HOME` set to the Android Studio JBR:

`./gradlew testDevDebugUnitTest --tests '*Agora*' --tests '*CallCenter*' ktlintCheck assembleDevDebug` -> BUILD SUCCESSFUL.

- `AgoraManagerCreateEngineTest`: 8 tests (5 pre-existing, 3 new), 0 failures.
- All `Agora*` and `CallCenter*` suites: 0 failures, 0 errors.
- `ktlintCheck` and `assembleDevDebug` passed (Hilt graph compiles with the new constructor parameter).
- Logic-level changes (WR-02 behavior) are covered by the new tests; no human verification flagged.

Unrelated dirty files (`.idea/`, `.planning/config.json`, `.planning/milestone.lock`, `.planning/state.json`) were not touched or staged.

---

_Fixed: 2026-10-26_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
