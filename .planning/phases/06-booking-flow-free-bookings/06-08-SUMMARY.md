---
phase: 06-booking-flow-free-bookings
plan: 08
subsystem: booking
tags: [compose, calendar, time-slots, flowrow, timezone, accessibility]
requires: [06-03, 06-04]
provides:
  - MakeABookingCalendarCard / calendarDayTestTag
  - MakeABookingTimeSlotsCard / slotChipTestTag
affects: [06-11, 06-12]
tech-stack:
  added: []
  patterns: [Column of Rows calendar grid, FlowRow slot chips, selectable + stateDescription semantics]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingCalendarCard.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingTimeSlotsCard.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingCalendarSlotsTest.kt
  modified: []
key-decisions:
  - "Slot chips wrap in a FlowRow instead of the iOS horizontal scroller, so no scroll-to-first-open logic exists"
  - "Day cell total height is 48 dp: 4 dp top pad, 36 dp circle, 4 dp gap, 4 dp dot slot (always reserved so rows do not jump)"
requirements-completed: [BOOK-01, BOOK-02]
duration: ~45min
completed: 2026-10-22
---

# Phase 6 Plan 08: Calendar card and time slots card Summary

Custom month calendar (locale week start, selected/today/past states, open-time dots, D-06 arrows, full-date accessibility labels) and a time slots card (friendly timezone note, wrapping chips, dimmed unavailable slots, skeleton/empty/failed states).

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 | Month calendar card | aeb91d1 |
| 2 | Time slots card | c7adaca |

## Verification

`./gradlew :app:testDevDebugUnitTest --tests '*MakeABookingCalendarSlotsTest' ktlintCheck` passes: 16 tests (7 calendar, 9 slots), 0 failures. `bash scripts/check-design-rules.sh` prints "Design rules: OK". No LazyVerticalGrid, no hardcoded "h:mm a" in main code.

## Deviations from Plan

None - plan executed as written. TDD note: tests and implementation were committed together per task (same precedent as 06-04). The worktree base was behind the expected base commit and was reset to it per the startup check (clean tree).

## Known Stubs

None.

## Threat Flags

None. T-06-25 (chip enabled = isSelectable(now), tested for past, held and open), T-06-26 (null/same zone fallback tested) and T-06-27 (test asserts no node contains "America/") are mitigated.

## Self-Check: PASSED
