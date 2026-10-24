---
phase: 08-client-bookings-cancellation
plan: 12
subsystem: ui
tags: [screen, viewmodel, booking-summary, reschedule]
requires: [08-01, 08-02, 08-04, 08-05, 08-06]
provides:
  - "BookingSummaryKey NavKey, BookingSummaryDetails display model and toSummaryDetails mapping"
  - "Assisted BookingSummaryViewModel with loud/quiet latest-wins load and the shared reschedule controller"
  - "BookingSummaryScreen / BookingSummaryContent / BookingSummaryCard with Book Again"
affects: [08-13, 08-14, 08-15]
tech-stack:
  added: []
  patterns: ["assisted @HiltViewModel keyed by booking id", "last-proposal remembered so the reschedule card can fade out after the reload clears it"]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryKey.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/subviews/BookingSummaryCard.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryScreenTest.kt
  modified: []
key-decisions:
  - "Free topics always get the Session fee label regardless of payment status; captured is Price paid, refunded is Refunded, everything else is Session fee"
  - "A quiet reload that fails keeps the previous summary; only loud loads can end in Failed"
  - "The reschedule card sits in AnimatedVisibility with its own bottom padding (no spacedBy) so a hidden card leaves no gap"
requirements-completed: [BOOK-08, BOOK-09]
duration: 35min
completed: 2026-10-23
---

# Phase 8 Plan 12: Booking Summary Summary

Booking Summary ("Session Summary") opened by booking id: summary card with status badge and payment-based price label, Book Again seeded for Make a Booking, and the shared reschedule card, dialogs and snackbar for a pending proposal, with nothing from Phases 12-13.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 | Key, summary display model and BookingSummaryViewModel (16 JVM tests) | 7e20386 |
| 2 | Summary card and Booking Summary screen (12 Robolectric tests) | c47ced9 |

## Verification

- `:app:testDevDebugUnitTest --tests '*BookingSummaryScreenTest' --tests '*BookingSummaryViewModelTest' ktlintCheck` passes (ViewModel 16 tests, screen 12 tests, 0 failures); `bash scripts/check-design-rules.sh` prints "Design rules: OK".
- Grep gates: `summaryStatusDisplay(status)` x1, `RescheduleResponseController(` x1, `@Serializable` x1, reschedule card and outcome dialog references x2, forbidden Phase 12-13 terms (non-comment lines) x0.
- Threat mitigations: T-08-41 (same controller and server checks as Upcoming), T-08-42 (no private note field declared or rendered, grep gate and a screen test for the absent texts). T-08-40 accepted (display seed only).

## Deviations from Plan

None - plan executed exactly as written, with process notes:
- The worktree base was behind the expected commit (`d1217fb`) and was reset to `2aa7762` per the branch check before any work.
- Tests and implementation were written together per task and committed once per task (no separate RED commits).
- A screen test originally used `boundsInRoot` to compare vertical order; it is clipped to the viewport for off-screen nodes, so it uses `positionInRoot` instead.

## Known Stubs

None. The screen is not yet routed; wiring the `BookingSummaryKey` entry and the Past-card tap is in plan 08-14.

## Threat Flags

None.

## Self-Check: PASSED

All seven created files exist; commits 7e20386 and c47ced9 are in the branch log.
