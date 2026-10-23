---
phase: 08-client-bookings-cancellation
plan: 07
subsystem: client-bookings
tags: [viewmodel, realtime, refresh, my-bookings]
requires: [08-01, 08-02]
provides:
  - MyBookingsViewModel (@HiltViewModel) with combined two-bucket load
  - MyBookingsUiState / MyBookingsLoad
affects: [08-08, 08-15]
tech-stack:
  added: []
  patterns: [single cancellable load job with Loud/Quiet/Pull modes, loadIsCurrent coalescing flag, consume-on-read launch hand-off]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsViewModel.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsViewModelTest.kt
  modified: []
key-decisions:
  - "One loadAll fetches upcoming and past in parallel (page 1, limit 20) and derives both tabs with buildBookingLists; one combined load state (RESEARCH Finding 1)"
  - "A single loadJob; every start cancels the previous one, so realtime bursts and pull/quiet overlaps are latest-wins"
  - "Every completed load clears isRefreshing so a superseded pull cannot spin forever"
  - "Launch request is consumed in init (posted before the ViewModel exists, no reload) and on each later pending emission (forces a reload)"
requirements-completed: [BOOK-05]
duration: ~30 min
completed: 2026-10-23
---

# Phase 8 Plan 07: My Bookings state machine Summary

`MyBookingsViewModel` loads both server buckets together into one load state, supports pull-to-refresh and the three D-10 reload triggers with coalescing, hands over the post-cancel launch exactly once, and saves the selected sub-tab.

## Tasks

| Task | Commit | Result |
| ---- | ------ | ------ |
| 1. UI state, two-bucket load, retry, pull-to-refresh, saved sub-tab | fcca9c5 | 11 tests |
| 2. Resume coalescing, realtime reloads, launch request | ddeb865 | 10 more tests (21 total) |

Verification: `:app:testDevDebugUnitTest --tests '*MyBookingsViewModelTest' ktlintCheck` exits 0 (21 tests, 0 failures). Acceptance greps hold: `filterIsInstance<RealtimeEvent.BookingUpdate>` x1, `launchRequest.consume()` x2, `fun onScreenShown|fun onScreenHidden` x2, `PAGE_SIZE = 20` x1, no `loadUpcoming`/`loadPast`.

## Deviations from Plan

**1. [Rule 3 - Blocking] Worktree base correction.** The worktree started at `d1217fb`, an ancestor of the required base `e682101`; reset to the base per the startup check (nothing committed).

**2. [Test adjustment] Failed quiet reload call count.** `coroutineScope { async; async }` cancels the sibling when the first bucket throws, so a failing reload makes 1 or 2 calls, not always 2. The test asserts more than 2 total calls instead of exactly 4. Production behavior is unchanged (a failing bucket fails the whole load, as the plan specifies).

**3. [Style] ktlint.** One test chain was split into a local variable.

TDD note: tests and implementation for each task were written together and committed per task (no separate RED commits).

## Threat model

T-08-24 mitigated: each realtime event and each new load cancels the in-flight job (latest wins, tested with two gated events); resume calls coalesce while a current load runs (tested). T-08-25 mitigated: `BookingUpdate` only triggers a refetch, no payload field is read. T-08-26 mitigated: no logging in the ViewModel.

## Notes for downstream plans

- The screen (08-08) must call `onScreenShown()` from `LifecycleEventEffect(ON_RESUME)` and `LaunchedEffect(Unit)`, and `onScreenHidden()` from `ON_PAUSE` and `DisposableEffect` disposal. The Bookings tab re-selection should also call `onScreenShown()`.
- `consumeNotice()` and `consumeRefreshFailure()` must be called after the snackbar is shown.
- The ViewModel is not yet wired into navigation; no screen consumes it in this plan.

## Known Stubs

None.

## Self-Check: PASSED

Created files exist (MyBookingsUiState.kt, MyBookingsViewModel.kt, MyBookingsViewModelTest.kt); commits fcca9c5 and ddeb865 are in the branch log.
