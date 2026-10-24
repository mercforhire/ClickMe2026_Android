---
phase: 08-client-bookings-cancellation
plan: 13
subsystem: ui
tags: [compose, screen, upcoming-booking, reschedule]
requires: [08-02, 08-04, 08-06, 08-10]
provides:
  - "UpcomingBookingScreen / UpcomingBookingContent: Upcoming Booking Details in UI-SPEC block order with the shared reschedule card and dialogs"
  - "Subviews: status row, payment pill, expert / topic / date-time / timezone-note / message cards, inert join card, pending banner"
  - "Test tags upcoming_booking_status, _payment_pill, _join, _copy_link, _cancel"
affects: [08-14, 08-15]
tech-stack:
  added: []
  patterns: ["LocalClipboard + ClipEntry for an explicit-tap copy with a 2 s Copied state", "Column with explicit Spacers (no spacedBy) so a hidden AnimatedVisibility leaves no gap"]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/subviews/UpcomingBookingStatusRow.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/subviews/UpcomingBookingCards.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/subviews/UpcomingBookingJoinCard.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingScreenTest.kt
  modified: []
key-decisions:
  - "The join card never opens the meeting link: Skype/Zoom shows the link with a copy button only, and in-app voice shows a disabled Join Call marked PHASE-9"
  - "No extra navigation-bar inset on the scroll column: the Scaffold content padding already covers it"
requirements-completed: [BOOK-06, BOOK-09]
duration: 35min
completed: 2026-10-23
---

# Phase 8 Plan 13: Upcoming Booking Details Screen Summary

Upcoming Booking Details renders the real status, the paid-only payment pill, an inert join card or the pending banner, and a Cancel Booking button for the three cancellable statuses. A pending reschedule shows the shared Accept / Decline card first, with its dialogs and snackbar.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 (RED) | Failing subview tests | 36e2d69 |
| 1 (GREEN) | Status row, payment pill, cards, timezone note, join card, pending banner | 07cdaa2 |
| 2 | UpcomingBookingContent and UpcomingBookingScreen (16 screen tests) | 03731a6 |

## Verification

- `:app:testDevDebugUnitTest --tests '*UpcomingBookingScreenTest' --tests '*UpcomingBookingViewModelTest' ktlintCheck` passes: screen test 33 tests (17 subview + 16 screen), ViewModel test 22, 0 failures. `bash scripts/check-design-rules.sh` prints "Design rules: OK".
- Grep gates: `PHASE-9` in the join card 1; URI handler / Intent / Custom Tab references 0; `zoneCity(` / `zoneAbbreviation(` lines in the cards file 2; `RescheduleResponseCard(` / `RescheduleOutcomeDialog(` 2; `enabled = !rescheduleState.isSubmitting` 1; `viewModel.refreshQuietly()` 2.
- Threat mitigations: T-08-43 (link displayed and copied only, grep gate), T-08-45 (plain Text / SelectionContainer), T-08-46 (Cancel gated by `canCancel`, tested for in-progress and completed). T-08-44 accepted (copy only on an explicit tap).

## Deviations from Plan

None in behavior. One acceptance-criteria note:
- The last Task 2 grep (`onReschedule|onMessage|privateNote` on non-comment lines of `UpcomingBookingScreen.kt` should print 0) prints 3, because the plan's own required parameter name `onRescheduleNoticeConsumed` contains `onReschedule`. There is no Reschedule, Message or Private Note action; a screen test asserts none of those texts exist.

Process note: the RED commit for Task 1 is a compile failure (types absent). Task 2 tests and implementation were committed together.

## TDD Gate Compliance

Task 1 has `test(08-13)` 36e2d69 before `feat(08-13)` 07cdaa2. Task 2 was committed as a single `feat(08-13)` commit with its tests.

## Known Stubs

The Join Call button is intentionally disabled, and the meeting link is not opened. Both are marked `PHASE-9` in `UpcomingBookingJoinCard.kt` and are completed in Phase 9 (D-02, D-12). The screen is not yet routed; the Nav3 entry and the card taps are wired in 08-14.

## Threat Flags

None.

## Self-Check: PASSED

All five created files exist; commits 36e2d69, 07cdaa2 and 03731a6 are in the log.
