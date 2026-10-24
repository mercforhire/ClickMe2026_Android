---
phase: 09-voice-call
plan: 15
subsystem: call
tags: [join, explore, compose, join-window]
requires: [09-02, 09-03, 09-04, 09-11]
provides:
  - TodaysSessionUi.expertId / meetingType / status
  - ExploreViewModel.joinNow and join(session) through CallStarter
  - Live Join Call on the Explore Today's Sessions card (JoinCallButton + isWithinJoinWindow)
  - StartingNowBadge and STARTING_NOW_BADGE_TAG
affects: [09-16, 09-17]
tech-stack:
  added: []
  patterns: [FlowRow name+badge wrap, shared JoinClock collected per Join site]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/subviews/StartingNowBadge.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/subviews/ExploreTodaysSessionsSection.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreSectionsTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreScreenTest.kt
key-decisions:
  - "Join reuses the existing todays_session_join test tag and the shared JoinCallButton; the PHASE-9 placeholder is gone"
  - "The badge sits in a FlowRow with the expert name so it wraps below at large font scales"
requirements-completed: [CALL-01]
duration: ~40min
completed: 2026-10-24
---

# Phase 9 Plan 15: Explore Today's Sessions Join Summary

The third Join site now starts calls through `CallStarter.startCall` with the live +-3600 s join window and the shared `JoinClock`, and Today's Sessions cards show a sentence-case "Starting now" primary pill when the start is inside that window.

## Tasks

| Task | Name | Commit |
|------|------|--------|
| 1 | Today's Sessions model and ViewModel join | 34ba4e5 |
| 2 | Live Join on the card and the Starting now badge | 8bad0c4 |

## What changed

- `TodaysSessionUi` gained `expertId`, `meetingType`, `status` (defaulted, so existing constructions compile). The mapping fills them from `ClientBookingItem`.
- `ExploreViewModel` takes `CallStarter` and `JoinClock`; `joinNow` mirrors the clock; `join(session)` builds a `CallStartRequest` (peer = expert, `MeetingType.fromWire`).
- `ExploreTodaysSessionsSection(sessions, now, onSessionClick, onJoin, modifier)`: `JoinCallButton` enabled by `isWithinJoinWindow(status, start, end, now)`; badge via `isStartingNow`. `ExploreContent` gets `joinNow` and `onJoinSession`; the public `ExploreScreen` signature is unchanged.

## Verification

- `:app:testDevDebugUnitTest --tests 'com.uptrendinvestments.clickmeapp.screens.clientmode.explore.*' :app:assembleDevDebug ktlintCheck`: BUILD SUCCESSFUL (ExploreViewModelTest 24, ExploreSectionsTest 26, ExploreScreenTest 14, 0 failures).
- `bash scripts/check-design-rules.sh`: OK.
- Acceptance greps: `callStarter.startCall` 1 in ExploreViewModel; `PHASE-9` in explore/ 0; `TODAYS_SESSION_JOIN_TAG` 2 in the section file.
- T-09-51: tests assert a Join tap calls `onJoin` and not `onSessionClick` (section and full screen); T-09-50: the window is a UX gate only, server decides on `/join`.

## Deviations from Plan

**1. [Process] Tests written alongside implementation, not as separate RED commits.** Each task is a single commit containing code and tests; tests were run to green before committing.

**2. [Rule 3 - Blocking] Worktree base.** HEAD was `d1217fb` (an ancestor of the required base, clean tree); ran the sanctioned `git reset --hard 3150a62` at startup.

**3. Environment.** One Gradle run died with a Kotlin compiler `OutOfMemoryError` (contention with parallel agents); a plain re-run succeeded.

Otherwise the plan was executed as written.

## Known Stubs

None.

## Threat Flags

None.

## Self-Check: PASSED

StartingNowBadge.kt and this SUMMARY exist; commits 34ba4e5 and 8bad0c4 are on the branch.
