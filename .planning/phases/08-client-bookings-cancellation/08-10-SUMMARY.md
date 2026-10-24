---
phase: 08-client-bookings-cancellation
plan: 10
subsystem: ui
tags: [viewmodel, upcoming-booking, realtime, reschedule]
requires: [08-01, 08-02, 08-04, 08-05]
provides:
  - "UpcomingBookingKey: Nav3 route carrying the booking id"
  - "UpcomingBookingDetails / UpcomingBookingUiState / ClientBookingDetail.toUpcomingDetails: pure D-14 / D-15 display mapping"
  - "UpcomingBookingViewModel: assisted Hilt VM with load, quiet reloads, own-id realtime and reschedule delegation"
affects: [08-11, 08-12, 08-13]
tech-stack:
  added: []
  patterns: ["assisted @HiltViewModel with latest-wins startLoad(quiet) returning a Job so the reschedule controller can join the reload"]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingKey.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingViewModel.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingViewModelTest.kt
  modified: []
key-decisions:
  - "A quiet reload that fails while the state is Failed leaves Failed in place; only loud loads change the message"
  - "Optional strings (expert title, topic title, timezone, meeting type, join link, notes) are blank-to-null in the mapping, so the screen only checks null"
  - "The join link stays in memory only and is never logged (T-08-36); opening it is Phase 9"
requirements-completed: [BOOK-06, BOOK-09]
duration: 20min
completed: 2026-10-23
---

# Phase 8 Plan 10: Upcoming Booking Details ViewModel Summary

Route key, tested display mapping (real status, Money or Free, payment pill, cancel eligibility, reschedule proposal) and a ViewModel that loads by id, reloads quietly on resume and on this booking's realtime updates, and delegates accept and decline to the shared reschedule controller.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 (RED) | Failing mapping tests | f8e6e37 |
| 1 (GREEN) | UpcomingBookingKey and UpcomingBookingUiState | ad8da9d |
| 2 (RED) | Failing ViewModel tests | 3c3fe85 |
| 2 (GREEN) | UpcomingBookingViewModel | d240d0f |

## Verification

- `:app:testDevDebugUnitTest --tests '*UpcomingBookingViewModelTest' ktlintCheck` passes: 22 tests, 0 failures (11 mapping, 11 ViewModel).
- Grep gates: `RescheduleResponseController(` 1, `filterIsInstance<RealtimeEvent.BookingUpdate>` 1, `fun refreshQuietly|fun onScreenHidden` 2, no `/ 100` or `privateNote` in the UI state file.
- Threat mitigations: T-08-34 (cancel gate from `CANCELLABLE_BOOKING_STATUSES`, tested), T-08-35 (other-id events ignored, latest-wins loads, coalesced resume calls, all tested), T-08-36 (no logging of the link).

## Deviations from Plan

None - plan executed exactly as written, with process notes:
- The worktree base was behind the expected commit and was reset to 2aa7762 per the worktree branch check.
- The Task 1 acceptance grep (`detailStatusDisplay(status)\|paymentPillFor(...)\|CANCELLABLE_BOOKING_STATUSES`) would print 4 rather than 3 because the import line and the `canCancel` line both contain `CANCELLABLE_BOOKING_STATUSES`; all three rules are present as required.

## TDD Gate Compliance

Both tasks have a `test(08-10)` RED commit (compile failure, types absent) followed by a `feat(08-10)` GREEN commit.

## Known Stubs

None.

## Threat Flags

None.

## Self-Check: PASSED

All four files exist and commits f8e6e37, ad8da9d, 3c3fe85, d240d0f are in the log.
