---
phase: 10-expert-topics-availability
plan: 12
subsystem: expert-mode
tags: [viewmodel, call-guard, saved-state, availability, timezone]
status: complete
requires: [10-01, 10-02, 10-04, 10-05]
provides:
  - AvailabilityKey / AvailabilityOrigin (Nav3 route, origin distinguishes the tab stack)
  - AvailabilityUiState / AvailabilityLoad (derived overlappingDays, timezoneLabel, timezoneEntries, canSave)
  - AvailabilityViewModel (load, retry, refresh, edits, timezone, guarded save, silent UTC fill)
affects: [10-14]
tech-stack:
  added: []
  patterns:
    - SavedStateHandle mirror of unsaved edits via AvailabilityScheduleLogic.encode/decode
    - guard-first Save (CallGuard.attempt) and read-only isCallActive for the silent save
    - withContext(NonCancellable) around the PUT so leaving the screen does not cancel an in-flight save
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityKey.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityViewModel.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityViewModelTest.kt
  modified: []
decisions:
  - Save order is idle check, then CallGuard.attempt(EditAvailability), then the overlap re-check, so a blocked save never changes state
  - The silent UTC fill PUT runs in its own NonCancellable coroutine so a refresh() cannot cancel it mid-flight
  - didAutoDetect is persisted in SavedStateHandle, so a process restore never re-runs the fill even before any edit
requirements: [EXP-03, EXP-04]
commits: 1
plan_head_before: 524f6cfa67bbcdd96c63e885d041474a5e6591ba
plan_head_after: 81bcff7dcfe7ec553e9a04cef6bed5e6f9cf23e4
completed: 2026-10-25
---

# Phase 10 Plan 12: Availability ViewModel Summary

Availability state holder with hydration, local edits and overlap blocking, a call-guarded Save (Saving -> Saved 2000 ms -> Idle), timezone pick/auto-detect that stays local until Save, a once-per-session silent UTC auto-fill PUT, pull-to-refresh and process-death restore.

## What was built

- **Task 1** state and load: `AvailabilityKey` (origin keeps keys distinct per tab stack), `AvailabilityUiState` with the derived overlap, label, picker-entry and `canSave` properties, and the ViewModel's initial load (default 17:00-21:00 schedule until loaded, server-message `Failed`, Retry), `refresh()` (overwrites edits on success, keeps them and flags `refreshFailed` on failure), edit functions, `selectTimezone`/`autoDetectTimezone` (local only, D-14) and the `availability_schedule` SavedStateHandle mirror (D-16).
- **Task 2** Save and silent fill: `save()` calls `callGuard.attempt(RestrictedAction.EditAvailability)` right after the idle check, re-checks overlaps, PUTs timezone plus all seven days as sorted `HH:mm` (22:00-24:00 sent as "24:00"), shows Saved for `Motion.AVAILABILITY_SAVED_HOLD_MS`, removes the saved-state key, and maps failures through `toExpertDialogError()` (field message first). The D-15 fill replaces a UTC/Etc/UTC server zone with the detected device zone in the same state update as the hydrated days, then PUTs once silently unless saving, overlapping, or `isCallActive`; failures are swallowed and it never repeats in a session or after a restore.
- Tests: `AvailabilityViewModelTest`, 20 cases covering every behavior line of both tasks (device zone set with `TimeZone.setDefault` and restored in `@After`).

## Verification

`:app:testDevDebugUnitTest --tests '*AvailabilityViewModelTest'` (20 tests, 0 failures), `:app:assembleDevDebug` and ktlint (`ktlintMainSourceSetCheck`, `ktlintTestSourceSetCheck`) pass. Acceptance greps: exactly 1 non-comment `callGuard.attempt(` call, `isCallActive.value` present, 1 `AVAILABILITY_SAVED_HOLD_MS`, `availability_schedule` present, 1 `enum class AvailabilityOrigin`.

## Deviations from Plan

### Process

**1. Single commit for both tasks.** The ViewModel was written in one pass (Task 1 behavior and Task 2 save/silent-fill share the same class and test file), so both tasks landed in commit `81bcff7` rather than two per-task commits. The tests and acceptance criteria of both tasks are all satisfied.

### Auto-fixed Issues

**1. [Rule 3 - Blocking] ktlint chain-wrapping in tests**
- **Found during:** final verify
- **Issue:** `ktlintTestSourceSetCheck` rejected multi-call dot chains in the new test file.
- **Fix:** Added small helpers (`AvailabilityViewModel.day(...)`, `firstEntryLabel`) and replaced the chains.
- **Commit:** 81bcff7

## Known Stubs

None.

## Threat Flags

None. All threats T-10-37 to T-10-40 are mitigated and tested (guard before state change, silent save reads `isCallActive`, local overlap re-check, once-per-session fill, validated `decode`).

## Self-Check: PASSED

- FOUND: AvailabilityKey.kt, AvailabilityUiState.kt, AvailabilityViewModel.kt, AvailabilityViewModelTest.kt
- FOUND: commit 81bcff7
