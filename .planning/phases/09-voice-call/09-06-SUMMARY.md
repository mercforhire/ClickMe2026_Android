---
phase: 09-voice-call
plan: 06
subsystem: calls
tags: [agora, sdk, voice, engine]

requires:
  - phase: 09-voice-call
    provides: CallEngine port and EngineEvent (plan 04)
provides:
  - AgoraManager, the production CallEngine over RtcEngine
  - Pure Agora callback mapping (mapConnectionStateChanged, mapUserOffline)
  - Agora voice-sdk 4.6.4 dependency on :app
affects: [09-11 CallCenter wiring, D-16 UAT]

tech-stack:
  added: [io.agora.rtc:voice-sdk 4.6.4 (already in catalog) on :app]
  patterns:
    - "SDK callbacks only tryEmit into a buffered MutableSharedFlow; pure functions map them to events"
    - "Engine created lazily by join, destroyed off-main under a Mutex"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraManager.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraEventMapping.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/AgoraEventMappingTest.kt
  modified:
    - app/build.gradle.kts

key-decisions:
  - "Engine creation failure (exception from RtcEngine.create) returns false from join rather than throwing"
  - "Only conn state/reason and error code integers are logged (T-09-19)"

patterns-established:
  - "isLeaving flag suppresses LocalFailed from our own leave"

requirements-completed: [CALL-03, CALL-06]

duration: 20min
completed: 2026-10-24
---

# Phase 9 Plan 06: AgoraManager Summary

**Production CallEngine wrapping Agora RtcEngine with iOS audio defaults, a tested pure callback-to-EngineEvent mapping, and off-main destroy.**

## Accomplishments
- `libs.agora.voice.sdk` added to `:app`; `assembleDevDebug` succeeds.
- `AgoraEventMapping.kt`: CONNECTED -> LocalJoined; FAILED, or DISCONNECTED with banned / invalid token / token expired -> LocalFailed unless we are leaving; RECONNECTING / CONNECTING -> nothing; user offline always -> RemoteOffline. Six JUnit tests pass.
- `AgoraManager.kt`: speech-standard profile, default scenario, speakerphone default on, communication profile broadcaster with mic published and audio auto-subscribed. `leave()` awaits `onLeaveChannel` with a 2 s timeout; `destroy()` runs on the IO dispatcher under a mutex.

## Task Commits
1. Task 1: dependency and mapping with tests - 6c11b5f
2. Task 2: AgoraManager - 7eb66a3

## Deviations from Plan
None - plan executed exactly as written. The worktree was reset to the expected base commit c22e86e at startup as the branch-check instructs.

## Verification
- `:app:testDevDebugUnitTest --tests '*AgoraEventMappingTest' ktlintCheck`: passed.
- `:app:assembleDevDebug ktlintCheck`: passed.
- Grep gates: 0 credential log lines, 4 audio-default matches, 0 `onTaskRemoved` / `enableAudioVolumeIndication`.

## Known Stubs
None.

## Threat Flags
None. T-09-19/20/21 mitigations implemented as planned.

## Self-Check: PASSED
