---
phase: 09-voice-call
plan: 04
subsystem: calls
tags: [contracts, ports, state-machine, test-fakes, agora, coroutines]

requires:
  - phase: 01-foundation
    provides: MeetingType enum pattern, ApiException, scriptable-fake test style
provides:
  - ConnectionType and EndReason wire enums
  - CallEngine, CallForegroundController, CallPermissionGate ports
  - CallSession model with ported wireAgoraStateSync derivation (sticky Ended)
  - CallDialog, RestrictedAction, JoinErrorReason, GuardMessage typed values
  - CallGuard and CallStarter interfaces
  - CallPermissionBroker with 60 s timeout
  - Five scriptable test fakes
affects: [09-voice-call wave 2 (CallCenter, AgoraManager, foreground service, call UI), wave 4 Join and guard sites]

tech-stack:
  added: []
  patterns:
    - "Ports over SDK/service/UI so call rules are JVM-testable"
    - "Immutable CallSession replaced via copy; per-start id detects replaced sessions"
    - "Suspendable permission gate fulfilled by the UI through a pending StateFlow"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/enums/ConnectionType.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/enums/EndReason.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallEngine.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallForegroundController.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallPermissionGate.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallSession.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallDialog.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallGuard.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeCallEngine.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeCallForegroundController.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeCallPermissionGate.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeCallGuard.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeCallStarter.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/CallSessionTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/CallPermissionBrokerTest.kt
  modified: []

key-decisions:
  - "CallNotificationInfo carries only booking id, peer name and scheduled start, so RTC credentials cannot reach the service intent (T-09-11)"
  - "Broker keeps a single in-flight request; a newer ensureMicrophone() completes the older one as Cancelled"
  - "call.ended is modelled as remote = Disconnected so a later RemoteJoined restores Active"

patterns-established:
  - "Fakes accept an optional shared log list for cross-fake ordering assertions"

requirements-completed: [CALL-02, CALL-03, CALL-04, CALL-05, CALL-06]

duration: 25min
completed: 2026-10-24
---

# Phase 9 Plan 04: Call contracts, ports and fakes Summary

**Platform-free call contracts (three ports, session model with the ported iOS status derivation, typed dialogs, guard/starter interfaces, 60 s mic permission broker) plus five scriptable fakes, so Wave 2 can build in parallel.**

## Performance

- **Tasks:** 2
- **Files created:** 15 (8 main, 5 fakes, 2 test classes)

## Accomplishments
- `deriveCallStatus` ported with the iOS branch order; all 9 (mine, remote) pairs are covered by tests. `withConnStates` keeps Ended sticky and stamps `endedAt` once.
- `CallPermissionBroker` suspends on a `CompletableDeferred`, publishes `pending` for the root overlay, ignores stale ids, times out to Cancelled at 60 s of virtual time, and cancels a superseded request.
- `toJoinErrorReason` maps Http (server message or Unknown), Network and Decoding.
- Fakes record calls and support shared-log ordering assertions.

## Task Commits

1. **Task 1: Contracts, session model, dialogs, guard interfaces, permission broker** - `ee54689` (feat)
2. **Task 2: Scriptable fakes** - `477035d` (test)

## Verification

- `:app:testDevDebugUnitTest --tests '*CallSessionTest' --tests '*CallPermissionBrokerTest' ktlintCheck`: BUILD SUCCESSFUL
- `:app:compileDevDebugUnitTestKotlin ktlintCheck`: BUILD SUCCESSFUL
- Acceptance greps: `enum class RestrictedAction` 1, `val isSpeakerOn: Boolean = true` 1, `import android.` in CallSession/CallEngine/CallDialog 0, `fun emit(event: EngineEvent)` 1, `fun emitHangUp` 1.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] ktlint backing-property-naming on the foreground fake**
- **Found during:** Task 2
- **Issue:** `_hangUp` backing property has no matching `hangUp` property, which ktlint rejects.
- **Fix:** Renamed to `hangUpFlow`.
- **Files modified:** `FakeCallForegroundController.kt`
- **Commit:** `477035d`

**2. [Rule 3 - Blocking] Worktree base**
- **Found during:** startup
- **Issue:** Worktree HEAD was `d1217fb`, not the required base `770937d` (a descendant, working tree clean).
- **Fix:** Ran the sanctioned `git reset --hard 770937d`.

The plan's `FakeCallEngine.joins` is exposed as `joins` (as specified); beyond that, the plan was executed as written.

## Known Stubs

None.

## Threat Flags

None. T-09-11 is mitigated by the type shape of `CallNotificationInfo`; T-09-13 by the broker timeout and single in-flight request (both tested or structural).

## Self-Check: PASSED

All 15 files exist and commits `ee54689` and `477035d` are present.
