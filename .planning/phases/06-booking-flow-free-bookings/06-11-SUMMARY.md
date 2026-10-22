---
phase: 06-booking-flow-free-bookings
plan: 11
subsystem: booking
tags: [viewmodel, free-booking, single-flight, error-handling]
requires:
  - phase: 06-06
    provides: MakeABookingViewModel state machine, BookingRepository, BookingTestFixtures
  - phase: 06-04
    provides: toBookingFailure, BookingFailure, BookingTimeSlot.isSelectable
provides:
  - MakeABookingViewModel.book() (free path), dismissError(), consumeCompletedBooking()
affects: [06-12, 06-13, 06-14]
tech-stack:
  added: []
  patterns:
    - "isSubmitting early-return single flight; failure mapping via sealed BookingFailure"
key-files:
  created:
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModelBookTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt
key-decisions:
  - "A stale slot (not after now) is cleared with its saved key and never sent, since the server accepts a past scheduled_start"
  - "409 SLOT_UNAVAILABLE reloads the displayed month with keepVisible = true so the taken slot dims in place"
requirements-completed: [BOOK-03]
duration: ~20 min
completed: 2026-10-22
---

# Phase 6 Plan 11: Free Booking Action Summary

`MakeABookingViewModel.book()` sends the exact D-11 free booking body once per tap sequence, never for paid topics or stale slots, hands the booking id to the screen as a one-shot, and maps every failure to its D-08/D-09/D-10 dialog and side effect.

## Tasks

| Task | Commits | Content |
| ---- | ------- | ------- |
| 1 | 7a8a6e0 (RED), 7d70b31 (GREEN) | book() body, single flight, paid/no-slot/past-slot guards, completedBookingId + consumeCompletedBooking() |
| 2 | a37227f (RED), e527664 (GREEN) | handleBookingFailure: Show/Silent, meeting reset (D-08), slot clear + keep-visible reload (D-09), dismissError() |

## Verification

- `./gradlew :app:testDevDebugUnitTest --tests '*MakeABookingViewModelBookTest' --tests '*MakeABookingViewModelTest' ktlintCheck assembleDevDebug` exits 0.
- MakeABookingViewModelBookTest: 18 tests, 0 failures (8 happy-path/guard, 10 error). MakeABookingViewModelTest: 23 tests, 0 failures.
- Acceptance greps: `fun book()` 1, `if (s.isSubmitting) return` 1, `ifEmpty { null }` 1, `toBookingFailure()` 1, `keepVisible = true` 1, `fun dismissError()` 1, non-comment `runCatching` 0, payment-intent/PaymentSheet/setupIntent 0.

## Deviations from Plan

None - plan executed exactly as written. The worktree started on an older commit than the expected base and was reset to 0b8241c per the startup check.

## Known Stubs

None.

## Threat Flags

None. Threat mitigations T-06-34, T-06-35, T-06-37 and T-06-38 are covered by tests (single request for two taps, past slot not sent, paid topic not sent, suspended account shows no dialog).

## Self-Check: PASSED

MakeABookingViewModelBookTest.kt and the modified ViewModel exist; commits 7a8a6e0, 7d70b31, a37227f, e527664 exist on branch worktree-agent-a260d69a676eeaacf.
