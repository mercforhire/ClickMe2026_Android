---
phase: 10-expert-topics-availability
plan: 19
subsystem: expert-mode/availability
tags: [gap-closure, wr-01, wr-04, availability, viewmodel]
requires: []
provides:
  - "AvailabilityScheduleLogic.isLossless: hydrate keeps every server range"
  - "Lossless-gated silent D-15 UTC PUT"
  - "Saving guard on edit/refresh; cancellable Saved hold"
affects:
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityScheduleLogic.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityViewModel.kt
tech-stack:
  added: []
  patterns: ["shared private range parser for hydrate and isLossless", "saveJob handle so an edit can cancel the Saved hold"]
key-files:
  created: []
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityScheduleLogic.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityViewModel.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityScheduleLogicTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/availability/AvailabilityViewModelTest.kt
decisions:
  - "WR-04: skip the silent PUT when hydrate dropped any server range (reversible); zone is still filled locally and goes out with the next explicit Save. Timezone-only PUT recorded but not taken (changes D-15 body)."
  - "WR-01: ViewModel is the single gate (edit/refresh return early while Saving); no screen change."
requirements: [EXP-03, EXP-04]
status: complete
duration: ~20min
completed: 2026-10-26
actuals:
  tokens: 18000
  tasks: 2
  commits: 2
plan_head_before: 72baa1e3504f1989f96bede47f10e7523aafb11c
plan_head_after: 312cc2e6729c6f8db28739a2f03afb5142bbad8c
---

# Phase 10 Plan 19: Availability WR-04 and WR-01 gap closure Summary

Silent UTC timezone auto-fill PUT is now sent only when hydrate kept every server slot (`isLossless`), and Availability edits and pull-to-refresh are ignored while a Save is in flight, with an edit during the Saved hold ending the hold.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 | Skip silent UTC auto-fill PUT when hydrate dropped a server slot (WR-04) | fbeae44 |
| 2 | Ignore edits and refresh while saving; edit ends the Saved hold (WR-01) | 312cc2e |

## What changed

- `AvailabilityScheduleLogic`: private `parseRange` shared by `hydrate` and the new `isLossless(raw)`, so the keep/drop rule cannot drift.
- `AvailabilityViewModel`:
  - `rawRanges(data)` extracted from `hydrate(data)`.
  - `applyInitial` calls `silentSaveIfNeeded()` only when `fill != null` and `isLossless(rawRanges(data))`. The local fill and `didAutoDetect` bookkeeping are unchanged. The PUT body is still the hydrated schedule when sent.
  - `edit()` returns early while `Saving`; in `Saved` it cancels `saveJob` and applies the edit with `Idle` (and still mirrors into `SavedStateHandle`).
  - `refresh()` returns early while `Saving` (no request, `isRefreshing` stays false).
  - `save()` stores its coroutine in `saveJob`. Cancellation only happens in `Saved`, after the PUT inside `NonCancellable`, so only the hold delay is cut.
  - KDoc updated for both rules. `AvailabilityScreen` untouched.
- Tests added: `isLossless_trueWhenEveryServerRangeHydrates`, `isLossless_falseWhenAnyRangeIsMalformedOrInverted`, `aSilentPutIsSkippedWhenHydrateDroppedAServerSlot`, `editsAndRefreshAreIgnoredWhileSaving`, `anEditDuringTheSavedHoldEndsTheHoldAndANewSaveIsNotCutShort`.

## Verification

`:app:testDevDebugUnitTest --tests '*AvailabilityScheduleLogicTest' --tests '*AvailabilityViewModelTest' ktlintCheck` passed (task 1), and `--tests '*AvailabilityViewModelTest' --tests '*AvailabilityScreenTest' ktlintCheck` passed (task 2). Result XML: 23 logic tests and 16 ViewModel tests, 0 failures, 0 skipped. All existing D-15 and hydrate tests stay green. Acceptance greps: `fun isLossless` = 1, `isLossless(` in ViewModel >= 1, `SaveState.Saving` in ViewModel = 4.

## Deviations from Plan

The RED step was satisfied by compilation failure for the logic tests (`isLossless` did not exist) and was not run as a separate Gradle invocation, to save a ~4 minute build; the new ViewModel tests were written before the implementation in both tasks. Otherwise none - plan executed as written.

## Known Stubs

None.

## Threat Flags

None. T-10-58 and T-10-59 mitigated as planned; T-10-60 accepted.

## Self-Check: PASSED

- fbeae44 and 312cc2e are ancestors of HEAD.
- Modified source and test files exist; STATE.md and ROADMAP.md untouched.
