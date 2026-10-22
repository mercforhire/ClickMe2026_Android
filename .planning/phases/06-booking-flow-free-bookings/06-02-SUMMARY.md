---
phase: 06-booking-flow-free-bookings
plan: 02
subsystem: navigation
tags: [navigation3, navigator, back-stack, request-sent]
requires: []
provides:
  - "Navigator.replaceTop(route)"
  - "Navigator.resetAndSelect(tab, reset)"
  - "RequestSentKey(bookingId) NavKey"
affects: [06-05, 06-10, 06-13]
tech-stack:
  added: []
  patterns: ["back-stack edits proven by plain JUnit tests with fake and real tab keys"]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/RequestSentKey.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/navigation/BookingFlowNavigationTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/Navigator.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/navigation/NavigatorTest.kt
key-decisions:
  - "resetAndSelect validates every reset key before popping any stack, so a bad call leaves all stacks untouched"
requirements-completed: [BOOK-04]
duration: ~15min
completed: 2026-10-22
---

# Phase 6 Plan 02: Navigator replaceTop / resetAndSelect and RequestSentKey Summary

Two back-stack operations for the booking flow (replace Make a Booking with Request Sent; reset Explore and Bookings then select Bookings) plus the `RequestSentKey(bookingId)` route, all proven by unit tests.

## Tasks

| Task | Name | Commits |
| ---- | ---- | ------- |
| 1 | Navigator.replaceTop and resetAndSelect (D-12, D-13) | 0cefb70 (RED test), 39a8402 (GREEN) |
| 2 | RequestSentKey and booking-flow stack scenario (D-12, D-13, D-14) | 90dd83e |

## Verification

`./gradlew :app:testDevDebugUnitTest --tests '*NavigatorTest' --tests '*BookingFlowNavigationTest' ktlintCheck` passes: NavigatorTest 20 tests (6 new), BookingFlowNavigationTest 5 tests, 0 failures. ktlintCheck exits 0.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Style] ktlint line-length in BookingFlowNavigationTest**
- **Found during:** Task 2 verification
- **Issue:** the `BookingKey(...)` construction exceeded 120 columns.
- **Fix:** wrapped arguments one per line.
- **Commit:** 90dd83e

Minor implementation choice: `resetAndSelect` maps and validates all reset keys first, then pops, so an invalid key does not leave a partially reset state (stricter than the plan text, same observable behavior for valid calls).

Worktree note: the branch started at d1217fb and was reset to the required base 6c2fd02 per the startup step.

## Known Stubs

None.

## Threat Flags

None. T-06-06 and T-06-08 mitigations are implemented and unit-tested; T-06-07 accepted (id only).

## Self-Check: PASSED

Created files exist; commits 0cefb70, 39a8402, 90dd83e exist; STATE.md and ROADMAP.md not modified.
