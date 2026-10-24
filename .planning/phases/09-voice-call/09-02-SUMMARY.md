---
phase: 09-voice-call
plan: 02
subsystem: call
tags: [copy, strings, timer, pure-logic]
requires: []
provides:
  - strings_call.xml (full Phase 9 copy contract)
  - CallTimer (callTimerState, overtimeCutoff, MinutesSeconds, FINAL_STRETCH_SECONDS, OVERTIME_CUTOFF_SECONDS)
  - CallTimerLabels (callTimerText, callRemainingChipText, callTimerSpokenDescription)
affects: [09-07, 09-08, 09-09, 09-10, 09-11]
tech-stack:
  added: []
  patterns: [pure injected-instant timer, plurals-built spoken description]
key-files:
  created:
    - app/src/main/res/values/strings_call.xml
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallTimer.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallTimerLabels.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/CallTimerTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallTimerLabelsTest.kt
  modified: []
key-decisions:
  - "Overtime cutoff is scheduledEnd + 3600 s (OVERTIME_CUTOFF_SECONDS), replacing the iOS 5 * 60 TEMP value; a test asserts it is not 10:35"
  - "CallTimerState exposes derived flags as properties so UI and CallCenter share one definition"
requirements-completed: [CALL-07]
duration: ~25 min
completed: 2026-10-24
---

# Phase 9 Plan 02: Copy contract and call timer Summary

One `strings_call.xml` holding all Phase 9 copy, plus a pure schedule-anchored call timer (final stretch, past-end, 3600 s cutoff instant) with composable text and spoken-description helpers.

## Tasks

| Task | Name | Commit |
|------|------|--------|
| 1 | strings_call.xml copy contract | 8a6786b |
| 2 (RED) | Failing tests for CallTimer and labels | e082ad0 |
| 2 (GREEN) | CallTimer, CallTimerLabels, lint-clean tests | c3acae1 |

## Verification

- `:app:mergeDevDebugResources` exits 0.
- `CallTimerTest` (13 tests) and `CallTimerLabelsTest` (9 tests) pass; `ktlintCheck` clean.
- `scripts/check-design-rules.sh`: OK.
- Acceptance greps: `OVERTIME_CUTOFF_SECONDS = 3600L` count 1; no `Instant.now()` / `System.currentTimeMillis` in `CallTimer.kt`; the four spot-check keys present; existing strings not duplicated.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] ktlint violations in first draft**
- **Found during:** Task 2 verify
- **Issue:** Expression-body test functions and one over-long `remainingChip` line broke ktlint.
- **Fix:** Rewrote tests with block bodies and ordered imports; reshaped `remainingChip`.
- **Files modified:** `CallTimerLabelsTest.kt`, `CallTimer.kt`
- **Commit:** c3acae1

### Plan inconsistencies (not fixed)

**2. Task 1 acceptance count of "at least 70" is unreachable from the specified keys**
- The plan's key list yields 68 `<string name="call_` / `<plurals name="call_` lines (counted by hand and confirmed by grep). Every listed key is present with the approved copy. No filler keys were added to hit 70; the other three Task 1 criteria pass.

### Process notes

- The worktree HEAD was behind the stated base (`d1217fb` vs `770937d`); reset to the base per the startup check before any work.
- RED for Task 2 is a compile failure (unresolved symbols), not a run of failing assertions; it was not run through gradle.
- A parallel agent's `gradlew --stop` killed one of my daemon runs; re-run succeeded.

## Known Stubs

None.

## Threat Flags

None. T-09-06 mitigated: Long arithmetic, minutes uncapped ("75:12" tested). T-09-05 accepted per plan.

## Self-Check: PASSED

All five created files exist; commits 8a6786b, e082ad0, c3acae1 present.
