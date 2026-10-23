---
phase: 08-client-bookings-cancellation
plan: 08
subsystem: cancellation
tags: [viewmodel, cancellation, saved-state]
requires: [08-01, 08-02, 08-03, 08-04]
provides:
  - CancelBookingKey Nav3 route (bookingId)
  - ClientCancellationUiState, CancelStep, CancellationLoad, CancellationSummary, CancelErrorDialog
  - ClientCancellationViewModel (assisted Hilt) with KEY_STEP / KEY_REASON / KEY_COMMENT saved state
affects: [08-11, 08-15]
tech-stack:
  added: []
  patterns: [assisted HiltViewModel with SavedStateHandle, latest-wins load, single-flight submit, singleton launch hand-off]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/CancelBookingKey.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/ClientCancellationUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/ClientCancellationViewModel.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/ClientCancellationViewModelTest.kt
  modified: []
key-decisions:
  - "Confirm keeps isSubmitting true after success so nothing is tappable while the screen navigates away (isFinished drives navigation)"
  - "Refund preview is recomputed on continueToConfirm and on load when the restored step is Confirm, so the 24h boundary uses the moment step 2 was reached"
requirements-completed: [BOOK-07]
duration: ~25min
completed: 2026-10-23
---

# Phase 8 Plan 08: Client cancellation ViewModel Summary

Assisted `ClientCancellationViewModel` that loads the booking by id, runs the two-step reason/confirm flow with SavedStateHandle-backed step, reason and comment, sends the D-05 reason once, and hands the server's refund result to My Bookings through `BookingsLaunchRequest`.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 | Key, UI state, load, steps and saved state | 72c53e9 |
| 2 | Confirm, result hand-off and error outcomes | 72c53e9 |

## Verification

`:app:testDevDebugUnitTest --tests '*ClientCancellationViewModelTest'` (23 tests, 0 failures) and `ktlintCheck` pass. Acceptance greps: `launchRequest.post(` = 2, `repository.cancel(bookingId, cancelReason(` = 1, `isSubmitting` guards = 4, `Log.` = 0.

## Deviations from Plan

- **Single commit for both tasks.** The ViewModel was written in one pass and the test file covers both tasks' behaviors, so both tasks share commit 72c53e9 and there is no separate RED commit (same approach as 08-03).
- Worktree base was behind the expected base commit; reset to e682101 per the startup check (worktree was clean).
- Beyond the plan's behaviors, tests also cover: unknown saved values falling back to defaults, the refund preview moving across the 24h boundary between two visits to step 2, a refund with a missing currency, and confirm ignored before the booking loads.

## Known Stubs

None.

## Threat Flags

None. T-08-27 (isSubmitting guard, tested), T-08-28 (notice only from the response refund_amount and currency) and T-08-30 (server 404 mapped via `toCancelFailure`) are implemented; T-08-29 accepted (comment only in SavedStateHandle, never logged).

## Self-Check: PASSED

- All four created files exist; commit 72c53e9 exists.
