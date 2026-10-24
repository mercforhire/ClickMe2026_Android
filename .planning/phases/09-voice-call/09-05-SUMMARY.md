---
phase: 09-voice-call
plan: 05
subsystem: calls
tags: [call-center, state-machine, coroutines, tdd]

requires:
  - phase: 09-voice-call (plan 01)
    provides: joinCall / confirmCallConnection API, JoinCallResponse, AgoraConfig, ExternalConfig
  - phase: 09-voice-call (plan 04)
    provides: CallEngine / CallForegroundController / CallPermissionGate ports, CallSession model, dialogs, guard interfaces, fakes
provides:
  - CallCenter singleton (session, isMinimized, dialogs, isCallActive, startCall, attempt, attemptModifyBooking, minimize, expand, toggleMute, toggleSpeaker, onExternalLinkOpened, dismissDialog)
  - CallCenterHarness shared test builder
affects: [09-11 (hang-up, cutoff, logout teardown, DI bindings in the same file), call UI plans, Join and guard call sites]

tech-stack:
  added: []
  patterns:
    - "Main.immediate-confined singleton; session id checked after every suspension"
    - "Ported-state-machine tested with shared fake log for cross-fake ordering"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallCenter.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/CallCenterHarness.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/CallCenterStartTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/CallCenterPermissionTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/CallCenterGuardTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/CallCenterConfirmTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/CallCenterPeerTest.kt
  modified: []

key-decisions:
  - "Tests use runCurrent(), not advanceUntilIdle(): coroutines launched from backgroundScope are background work, which advanceUntilIdle does not run"
  - "CallCenter has no Hilt binding yet (not requested by any entry point); 09-11 adds it with the cleaner set"

patterns-established:
  - "CallCenterHarness(backgroundScope) + MainDispatcherRule + runTest(main.testDispatcher) + runCurrent() for Main-confined singletons"

requirements-completed: [CALL-01, CALL-02, CALL-03, CALL-04, CALL-05, CALL-06]

duration: 70min
completed: 2026-10-24
---

# Phase 9 Plan 05: CallCenter core Summary

**Main-confined `CallCenter` singleton that starts both call variants with the permission -> foreground -> engine ordering, derives status from Agora and call.ended, confirms the connection once, and exposes the guard API and dialog queue; 36 JVM tests pass.**

## Accomplishments
- `startCall` creates the Connecting session at once, then `POST /join`; the response `connection_type` overrides the hint. In-app order is agora_config check, mic permission, visible-app check, `startAndAwait`, `engine.join` (asserted as `["permission","foreground.start","engine.join"]`). Skype/Zoom never touches mic, service or Agora and treats a null/blank `join_url` as a join error.
- Every failure (server, network, unknown type, missing config or link, foreground refusal, backgrounded, engine rejection) tears down with no `/end`. A permanent mic denial queues the mic dialog; one-time denial and cancel are silent.
- Agora events and same-booking `call.ended` both map to mine/remote connection state via `withConnStates`; `call.ended` is remote Disconnected, never an end. Other bookings ignored; LocalFailed ends stickily.
- `confirmConnectionOnce` sets `confirmSent` before the request, truncates the timestamp to seconds, and ignores every error (409, network). `onExternalLinkOpened` confirms once with method `skype_zoom` and minimizes.
- `toggleMute` / `toggleSpeaker` change state only when the engine accepts (not optimistic).
- Guard API: `attempt`, `attemptModifyBooking` (same booking only), already-on-a-call guard, FIFO dialog queue with tail de-duplication.

## Task Commits
1. **Task 1: Start flow, ordering, failures, guard API, dialog queue** - `8581b56` (feat; the file also contains the Task 2 logic, see deviations)
2. **Task 2: Tests for engine/realtime status, confirm-once, controls, external Open** - `a71c475` (test)

## Verification
- `:app:testDevDebugUnitTest --tests 'com.uptrendinvestments.clickmeapp.managers.CallCenter*'` (36 tests: Start 12, Permission 3, Guard 7, Confirm 8, Peer 6), `ktlintCheck` and `:app:assembleDevDebug` all pass.
- Acceptance greps: no `UserManager`, no `Log.`/`println`, no `api.endCall`, one `truncatedTo(ChronoUnit.SECONDS)`, one `filterIsInstance<RealtimeEvent.CallEnded>`.

## Deviations from Plan

**1. [Process] TDD red/green not committed separately.** I wrote the implementation before the tests, so no failing-test (RED) commit exists. Tests were then written against the behavior list and the first run exposed a test-side problem (below), not an implementation bug. Task 1 and Task 2 production code landed together in `8581b56`; Task 2 only adds tests.

**2. [Rule 3 - Blocking] `advanceUntilIdle()` did not run the start coroutine.** `backgroundScope` work is flagged as background in the test scheduler, so `advanceUntilIdle()` skips it. Tests use `runCurrent()` instead (no delays are involved).

**3. [Rule 3 - Blocking] ktlint `chain-method-continuation`** in the new tests; fixed with `ktlintTestSourceSetFormat`.

**4. [Rule 3] Worktree base reset.** The worktree HEAD was behind the wave-1 base; reset to `c22e86e` per the startup check.

## Issues Encountered
None beyond the above.

## Known Stubs
None. The engine, foreground controller and permission gate are ports; their Android implementations and the Hilt binding for `CallCenter` come in later plans (no entry point requests `CallCenter` yet).

## Threat Flags
None. T-09-14 (bookingId filter), T-09-15 (visible-app check before the foreground start), T-09-16 (token only in the local start scope, no logging), T-09-17 (confirmSent before request) and T-09-18 (session id after every suspension) are implemented and covered by tests or grep gates.

## Self-Check: PASSED
- All 7 created files exist; commits `8581b56` and `a71c475` exist.
