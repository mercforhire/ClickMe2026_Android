---
phase: 08-client-bookings-cancellation
plan: 09
subsystem: ui
tags: [compose, screen, my-bookings, tabs, pull-to-refresh]
requires: [08-01, 08-02, 08-04, 08-06, 08-07]
provides:
  - "MyBookingsScreen (Hilt) and stateless MyBookingsContent"
  - "UpcomingBookingCard, PastBookingCard, MyBookingsSkeleton / MyBookingsEmptyState / MyBookingsError"
affects: [08-08, 08-15]
tech-stack:
  added: []
  patterns: [Crossfade between per-tab lazy lists with separate LazyListState, scrollable empty/error columns inside PullToRefreshBox, snackbar launched on composition scope then consumed]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/subviews/UpcomingBookingCard.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/subviews/PastBookingCard.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/subviews/MyBookingsStates.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsScreenTest.kt
  modified: []
key-decisions:
  - "Skeleton shows on the first combined load only, not per tab (RESEARCH Finding 1 deviation adopted by the orchestrator)"
  - "Empty and error states live in a verticalScroll column so pull-to-refresh works from them"
  - "Each tab keeps its own LazyListState so switching tabs preserves scroll positions"
requirements-completed: [BOOK-05]
duration: ~40 min
completed: 2026-10-23
---

# Phase 8 Plan 09: My Bookings screen Summary

My Bookings now renders from `MyBookingsUiState`: an Upcoming / Past `PrimaryTabRow`, the two card lists, skeleton / empty / error states, pull-to-refresh, the post-cancel and refresh-failed snackbars, and lifecycle hooks that drive the ViewModel's quiet reloads.

## Tasks

| Task | Commit | Result |
| ---- | ------ | ------ |
| 1. Upcoming and Past cards, loading / empty / error states | aa98096 | 15 tests |
| 2. MyBookingsContent and MyBookingsScreen | 5273d70 | 12 more tests (27 total) |

Verification: `:app:testDevDebugUnitTest --tests '*MyBookingsScreenTest' ktlintCheck` exits 0 (27 tests, 0 failures); `bash scripts/check-design-rules.sh` prints "Design rules: OK". Acceptance greps hold: `PHASE-9` x1, `PrimaryTabRow(` x1, `PullToRefreshBox(` x1, `viewModel.onScreenShown()` x2, `viewModel.onScreenHidden()` x2, no `fontSize` / `Color(0x`, no private-note / review / message / reschedule identifiers in the cards.

## Deviations from Plan

**1. [Rule 3 - Blocking] Worktree base correction.** The worktree started at `d1217fb`, an ancestor of the required base `2aa7762`; reset to the base per the startup check (nothing committed yet).

**2. [Style] ktlint.** One preview loop and two long test lines were split to satisfy the 120-column limit.

TDD note: tests and implementation were written together and committed per task (no separate RED commits), same as 08-07.

## Threat model

T-08-31 mitigated: server text is plain `Text` with `maxLines` and ellipsis. T-08-32 mitigated: Join is `PressScaleButton(onClick = {}, enabled = false)` and the test asserts `assertIsNotEnabled`. T-08-33 mitigated: LazyColumn keys are booking ids from `buildBookingLists` (`distinctBy`).

## Notes for downstream plans

- 08-08 / navigation wiring: `MyBookingsScreen(onOpenUpcoming, onOpenPast, onExploreExperts)` is not yet referenced from navigation in this plan. Re-selecting the Bookings tab should keep relying on the screen re-entering composition (`LaunchedEffect(Unit)`) or on calling `onScreenShown()` through the host.
- The reschedule badge text is chosen by `initiatedBy == "expert"`; any other value (including null) shows the "pending expert's response" wording.

## Known Stubs

The Join button on Upcoming cards is an intentional disabled placeholder (`// PHASE-9: join window + click`), resolved by Phase 9.

## Self-Check: PASSED

All five created files exist; commits aa98096 and 5273d70 are in the branch log.
