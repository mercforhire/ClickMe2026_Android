---
phase: 11-expert-requests-bookings
plan: 16
subsystem: call
tags: [gap-closure, agora, call, regression-test, diagnostics]
requires:
  - phase: 09-voice-call
    provides: AgoraManager, CallCenter, CallEngine contract
provides:
  - AgoraManager.createEngine builds the RtcEngineConfig from the injected application context (G-11-1 fixed)
  - buildRtcEngineConfig and describeEngineFailure helpers (AgoraEngineConfig.kt)
  - JVM regression test driving join() to the SDK factory boundary
affects: [call, expert-bookings, phase-13-internal-testing, phase-16-qa]
tech-stack:
  added: []
  patterns:
    - "Internal primary constructor takes the SDK factory as a lambda; @Inject secondary constructor passes the real SDK call"
    - "Redacted failure logs: simple class name plus top frame, never message or cause"
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraEngineConfig.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/AgoraEngineConfigTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/AgoraManagerCreateEngineTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraManager.kt
    - .planning/phases/11-expert-requests-bookings/11-VALIDATION.md
key-decisions:
  - "Application context read into a local (appContext) before the builder; builder parameter named appContext so the SDK getter context can never shadow it"
  - "A null RtcEngine.create result is an explicit failure with its own log line, before any audio setup"
  - "No try/catch added around joinChannel; a thrown exception still propagates to CallCenter as before"
requirements-completed: [EXP-08]
status: complete
duration: ~45 min
completed: 2026-10-26
actuals:
  tokens: 14000
  tasks: 3
  commits: 4
plan_head_before: 865e6cde989c96d988da008254c19375e14c099b
plan_head_after: 665324680ccf372260181e642d2afdfc17f9dc04
---

# Phase 11 Plan 16: Agora engine config context (G-11-1) Summary

**Fixed the NullPointerException that made every in-app voice join fail: AgoraManager now builds the Agora config from its own application context, with a Robolectric regression test through join() and redacted, diagnosable failure logs.**

## Performance

- Tasks: 3 of 3
- Production files: `AgoraManager.kt` changed, `AgoraEngineConfig.kt` added. CallCenter and the Join sites are untouched.

## Accomplishments

- G-11-1 fixed. In the old `RtcEngineConfig().apply { mContext = context.applicationContext }`, the bare `context` resolved to the SDK getter `getContext()` (null). Now `createEngine` reads `context.applicationContext` into a local and calls `buildRtcEngineConfig(appContext, appId, handler)`.
- Factory seam: `AgoraManager` has an `internal` primary constructor taking `createRtcEngine: (RtcEngineConfig) -> RtcEngine?`; the single `@Inject` secondary constructor passes `RtcEngine.create`. Hilt still builds (`assembleDevDebug`, `assembleProdDebug`, R8 `assembleProdRelease` all green).
- Null create returns false with one log line, `engine create returned null`, before any audio setup call.
- Failure logs: both catch blocks log `engine create failed <Class> at <class.method:line>` through `describeEngineFailure`; a non-zero `joinChannel` result logs `joinChannel failed code=N`. No message, cause, token, channel or uid is logged.

## Task Commits

| Task | Commit | Description |
|------|--------|-------------|
| 1 (RED) | e82ae72 | Failing regression test + factory seam |
| 1 (GREEN) | f5bbbc0 | Builder extraction, context fix, explicit null create |
| 2 | d181cc3 | describeEngineFailure, joinChannel code log, redaction tests |
| 3 | 6653246 | VALIDATION gap rows (gate run, no code change) |

## RED evidence (Task 1)

Before the fix, `AgoraManagerCreateEngineTest.joinReachesFactoryWithApplicationContextAndNullCreateReturnsFalse` failed with `expected:<1> but was:<0>`: the factory was never called, because the NullPointerException is thrown while the config is built. That reproduces the device failure on the JVM. (The "engine create failed NullPointerException" log line was not independently captured from the RED run; ShadowLog output is not written to the XML report. The assertion on factory call count is what failed.) After the fix the same test passes.

## Gate results (Task 3)

All run in this worktree, all exit 0:
- `testDevDebugUnitTest testProdDebugUnitTest`: 2954 tests per flavor, 0 failures (above the 2947 baseline)
- `ktlintCheck`, `assembleDevDebug`, `assembleProdDebug`, `:spikes:stripe:assembleDebug`
- `scripts/check-design-rules.sh` and `scripts/check-design-rules.sh spikes`: "Design rules: OK" twice
- `lintDevDebug lintProdDebug`
- `assembleProdRelease` (R8)

Acceptance greps: `RtcEngineConfig()` outside comments 0; `buildRtcEngineConfig(appContext` 1; `engine create returned null` 1; `describeEngineFailure(e)` 2; `javaClass.simpleName` 0; privacy guard grep 0.

## Deviations from Plan

### Skipped by orchestrator instruction

**1. STATE.md open item not written**
- The plan's Task 3 step 3 (STATE.md edits) was skipped because the orchestrator owns STATE.md and ROADMAP.md writes in this worktree wave. The orchestrator must add the open item below.
- Suggested STATE.md text (Blockers/Concerns, directly after the "[Phase 11] DEFERRED (human)" entry): "[Phase 11] G-11-1 fixed in code by 11-16; DEFERRED (human), non-blocking". Cause: inside the engine config builder lambda `context` resolved to the SDK's own getter, so engine creation threw an NPE and every in-app voice join, client and expert, showed "Couldn't join session". Re-check on the Samsung SM-A075F (`-s R8YYC1QFA6H`, devDebug, production backend) with the iOS app as peer, capturing `adb -s R8YYC1QFA6H logcat -s ClickMeCall CallService RtcEngine`. Checkboxes: 11-UAT test 1; 09-HUMAN-UAT item 1; 09-HUMAN-UAT item 2; 09-HUMAN-UAT item 5. Residual risk: nothing past engine creation has run on a device (joinChannel result, onJoinChannelSuccess, remote join, audio route, leave/destroy/rejoin, confirm-connection); `joinChannel failed code=N` carries the Agora error code; `engine create returned null` means checking for an `RtcEngine` "failed to load library" line. Also append " (could not pass before 11-16, G-11-1; see the G-11-1 entry below)" to item (c) of the existing Phase 11 entry.

### Auto-fixed Issues

**2. [Rule 3 - Blocking] ktlint violations in new test**
- **Found during:** Task 2
- **Issue:** two long `assertTrue(... startsWith ...)` lines failed ktlint.
- **Fix:** extracted the message into a local val.
- **Files modified:** AgoraManagerCreateEngineTest.kt
- **Commit:** d181cc3

**3. TDD note (Task 2)**
Task 2 tests and implementation were committed together (the helper tests cannot compile before the helper exists); Task 1 has a separate RED commit.

## Known Stubs

None.

## Threat Flags

None. No new network, auth or file surface; log lines carry only class names, a stack frame and an integer SDK code (T-11-53 mitigated and tested).

## Open item

Device re-check of the Join flows (client and expert) is non-blocking and deferred per the user's manual-testing rule; see the suggested STATE text above.

## Self-Check: PASSED

Files and commits verified present (see git log on branch worktree-agent-a76599d9b2bfd5344).
